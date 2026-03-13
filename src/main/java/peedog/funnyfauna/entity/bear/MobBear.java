package peedog.funnyfauna.entity.bear;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.core.WeightedRandomLootObject;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.enums.Difficulty;
import net.minecraft.core.item.Item;
import net.minecraft.core.item.ItemFood;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.item.Items;
import net.minecraft.core.util.collection.NamespaceID;
import net.minecraft.core.util.helper.DamageType;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.World;
import peedog.funnyfauna.block.FunnyFaunaBlocks;
import peedog.funnyfauna.entity.MobTaskrunner;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.controllers.BearTask;
import peedog.funnyfauna.entity.ai.interfaces.IAttacker;
import peedog.funnyfauna.entity.ai.interfaces.IHomeable;
import peedog.funnyfauna.item.FunnyFaunaItems;

import java.util.ArrayList;
import java.util.List;

public class MobBear extends MobTaskrunner implements IAttacker, IHomeable {

	// -------------------------------------------------------------------------
	// Synced data slots
	// -------------------------------------------------------------------------
	/** Belly level 1–4. 1 = hungry/aggressive, 4 = full/passive. */
	public static final int DATA_BELLY      = 21;
	/** Item currently held in mouth (shown by renderer). */
	public static final int DATA_MOUTH_ITEM = 22;

	// -------------------------------------------------------------------------
	// Attack state
	// -------------------------------------------------------------------------
	public int attackAnimTime;
	private int attackCooldown = 0;
	/** Which paw swipes next — true = right, false = left */
	public boolean attackSwipeRight = true;
	private int angerCounter = 0;

	// -------------------------------------------------------------------------
	// Sleep state
	// -------------------------------------------------------------------------
	public boolean sleeping = false;

	// -------------------------------------------------------------------------
	// Threat display state  (read by renderer and BearThreatTask)
	// -------------------------------------------------------------------------
	/** Incremented every tick while sway is active at all. Drives the sin wave. */
	public int   headSwayTick   = 0;
	/**
	 * Smoothed sway amplitude — lerped toward headSwayTarget every tick()
	 * so the sway always fades in/out smoothly even if the task is cancelled
	 * mid-sway (player walked away, bear got hurt, etc.).
	 * Renderer multiplies sway angle by this value.
	 */
	public float headSwayAmount = 0.0F;
	/**
	 * Target for headSwayAmount. BearThreatTask sets this to 1.0 during
	 * HEAD_SWAY and 0.0 for all other behaviors / when it stops.
	 */
	public float headSwayTarget = 0.0F;

	// -------------------------------------------------------------------------
	// IHomeable
	// -------------------------------------------------------------------------
	public int homeX = -1;
	public int homeY = -1;
	public int homeZ = -1;
	public boolean hasHome = false;
	private boolean homeInitialized = false;

	// -------------------------------------------------------------------------
	// Mouth item timer (server-side only — synced item is cleared when this hits 0)
	// -------------------------------------------------------------------------
	private int mouthItemTimer = 0;

	// -------------------------------------------------------------------------
	// Constants
	// -------------------------------------------------------------------------
	/** Bear considers itself home within this squared distance (3 blocks). */
	private static final double HOME_REACH_SQ    = 3.0;
	/** Non-crouching player within this range wakes a sleeping bear. */
	public  static final float  SLEEP_WAKE_RANGE = 6.0F;

	// =========================================================================

	public MobBear(World world) {
		super(world);
		setSize(1.8F, 1.8F);
		this.textureIdentifier = NamespaceID.getPermanent("funnyfauna", "bear");
		mobDrops.add(new WeightedRandomLootObject(FunnyFaunaBlocks.FUR.getDefaultStack(), 5, 8));
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(DATA_BELLY,      (byte) 1, Byte.class);
		this.entityData.define(DATA_MOUTH_ITEM, null,     ItemStack.class);
	}

	@Override
	public Task<MobBear> createTask() {
		return new BearTask(this);
	}

	// =========================================================================
	// Belly
	// =========================================================================

	public int getBelly() {
		return this.entityData.getByte(DATA_BELLY) & 0xFF;
	}

	public void setBelly(int b) {
		this.entityData.set(DATA_BELLY, (byte) MathHelper.clamp(b, 1, 4));
	}

	/**
	 * Range at which a bear begins its threat display (pre-attack intimidation).
	 * Belly 1 (hungry) = 10 blocks; belly 4 (full) = 3 blocks.
	 */
	public float getThreatRange() {
		switch (getBelly()) {
			case 4: return  4.0F;
			case 3: return  6.0F;
			case 2: return  8.0F;
			default: return 12.0F; // belly 1
		}
	}

	/**
	 * Max chase range — if the chased player is further than this,
	 * a non-hungry bear gives up and drops its target.
	 * Belly 1 = effectively infinite pursuit.
	 */
	public float getAggroRange() {
		switch (getBelly()) {
			case 4: return 7.0F;
			case 3: return 10.0F;
			case 2: return 15.0F;
			default: return 128.0F; // belly 1
		}
	}

	// =========================================================================
	// Mouth item
	// =========================================================================

	public ItemStack getMouthItem() {
		return this.entityData.getItemStack(DATA_MOUTH_ITEM);
	}

	private void setMouthItem(ItemStack stack) {
		this.entityData.set(DATA_MOUTH_ITEM, stack);
	}

	// =========================================================================
	// Tick
	// =========================================================================

	@Override
	public void tick() {
		super.tick();

		if (attackAnimTime > 0) attackAnimTime--;
		if (attackCooldown > 0) attackCooldown--;
		if (angerCounter > 0) angerCounter--;

		// -----------------------------------------------------------------------
		// Belly-based walk speed — fatter bears are slower.
		// This scales the entity's moveSpeed field which all AI movement uses.
		// -----------------------------------------------------------------------
		float baseSpeed = 0.6F;
		switch (getBelly()) {
			case 4: this.moveSpeed = baseSpeed * 0.60F; break;
			case 3: this.moveSpeed = baseSpeed * 0.75F; break;
			case 2: this.moveSpeed = baseSpeed * 0.88F; break;
			default: this.moveSpeed = baseSpeed;         break; // belly 1 — full speed
		}

		// -----------------------------------------------------------------------
		// Head sway — lerp always runs regardless of which task is active so
		// the sway fades out smoothly even when the threat task is cancelled.
		// -----------------------------------------------------------------------
		headSwayAmount += (headSwayTarget - headSwayAmount) * 0.08F;
		if (headSwayAmount > 0.01F) {
			headSwayTick++;
		} else {
			headSwayTick   = 0;
			headSwayAmount = 0.0F;
		}

		if (world.isClientSide) return;

		// Capture spawn position as home on the very first server tick.
		if (!homeInitialized) {
			homeInitialized = true;
			setHome(MathHelper.floor(this.x), MathHelper.floor(this.y), MathHelper.floor(this.z));
		}

		// Tick down mouth item display timer
		if (mouthItemTimer > 0) {
			mouthItemTimer--;
			if (mouthItemTimer == 0) {
				setMouthItem(null);
			}
		}
	}

	// =========================================================================
	// Interaction — feeding
	// =========================================================================

	@Override
	public boolean interact(Player player) {
		ItemStack held = player.inventory.getCurrentItem();

		if (held != null
			&& Item.itemsList[held.itemID] instanceof ItemFood
			&& ((ItemFood) Item.itemsList[held.itemID]).getIsWolfsFavoriteMeat()) {

			if (!world.isClientSide) {
				// Copy before consuming so we always store a size-1 stack in the mouth
				ItemStack mouthCopy = held.copy();
				mouthCopy.stackSize = 1;

				if (player.getGamemode().consumeBlocks()) {
					held.stackSize--;
					if (held.stackSize <= 0) {
						player.inventory.setItem(player.inventory.getCurrentItemIndex(), null);
					}
				}

				setBelly(Math.min(getBelly() + 1, 4));
				setMouthItem(mouthCopy);
				mouthItemTimer = 40;

				setTarget(null);
				angerCounter = 0;
			}

			return true;
		}

		return false;
	}

	// =========================================================================
	// Sleeping helpers
	// =========================================================================

	public boolean shouldSleep() {
		return hasHome && !world.isDaytime() && getTarget() == null;
	}

	public boolean isAtHome() {
		return hasHome && getDistanceToHomeSq() <= HOME_REACH_SQ;
	}

	public Player findPlayerToWake() {
		Player p = (Player) world.getClosestPlayerToEntity(this, SLEEP_WAKE_RANGE);
		if (p != null && !p.isSneaking()) return p;
		return null;
	}

	public void wakeInRage(Entity aggressor) {
		sleeping = false;
		if (aggressor != null && aggressor.isAlive()) {
			setTarget(aggressor);
			angerCounter = 600;
		}
	}

	// =========================================================================
	// Hurt
	// =========================================================================

	@Override
	public boolean hurt(Entity attacker, int damage, DamageType type) {
		boolean result = super.hurt(attacker, damage, type);

		if (!world.isClientSide && attacker != null && attacker.isAlive()) {
			if (sleeping) wakeInRage(attacker);
			setTarget(attacker);
			angerCounter = 400;
		}

		return result;
	}

	// =========================================================================
	// IAttacker
	// =========================================================================

	@Override public boolean isAttacking()           { return attackAnimTime > 0; }
	@Override public int    getAttackCooldown()       { return attackCooldown; }
	@Override public void   setAttackCooldown(int t)  { this.attackCooldown = t; }
	@Override public float  getAttackRange()          { return 2.5F; }

	@Override
	public void startAttack(Entity target) {
		if (target == null || !target.isAlive()) return;

		if (attackCooldown <= 0) {
			target.hurt(this, 3, DamageType.COMBAT);
			attackCooldown = 20;
			attackSwipeRight = !attackSwipeRight;
		}
		if (attackAnimTime <= 0) {
			attackAnimTime = 10;
		}
	}

	// =========================================================================
	// IHomeable
	// =========================================================================

	@Override public boolean hasHome() { return hasHome; }
	@Override public int getHomeX()    { return homeX; }
	@Override public int getHomeY()    { return homeY; }
	@Override public int getHomeZ()    { return homeZ; }

	@Override
	public void setHome(int x, int y, int z) {
		this.homeX   = x;
		this.homeY   = y;
		this.homeZ   = z;
		this.hasHome = true;
	}

	@Override
	public double getDistanceToHomeSq(double x, double y, double z) {
		if (!hasHome) return Double.MAX_VALUE;
		double dx = homeX + 0.5 - x;
		double dy = homeY + 0.5 - y;
		double dz = homeZ + 0.5 - z;
		return dx * dx + dy * dy + dz * dz;
	}

	@Override
	public double getDistanceToHomeSq() {
		return getDistanceToHomeSq(this.x, this.y, this.z);
	}

	// =========================================================================
	// Spawning
	// =========================================================================

	@Override
	public boolean canSpawnHere() {
		int x  = MathHelper.floor(this.x);
		int y  = MathHelper.floor(this.bb.minY);
		int z  = MathHelper.floor(this.z);
		int id = this.world.getBlockId(x, y - 1, z);
		if (id == 0 || id == 8 || id == 9 || id == 10 || id == 11 || y < 128) return false;
		return true;
	}

	@Override
	protected boolean canDespawn() {
		return false;
	}

	// =========================================================================
	// Sounds
	// =========================================================================

	@Override
	public void playLivingSound() {
		String s = getLivingSound();
		if (s != null && !world.isClientSide)
			world.playSoundAtEntity(null, this, s, getSoundVolume(), (random.nextFloat() - random.nextFloat()) * 0.2F + 0.6F);
	}

	@Override
	public void playHurtSound() {
		String s = getHurtSound();
		if (s != null && !world.isClientSide)
			world.playSoundAtEntity(null, this, s, getSoundVolume(), (random.nextFloat() - random.nextFloat()) * 0.2F + 0.6F);
	}

	@Override
	public void playDeathSound() {
		String s = getDeathSound();
		if (s != null && !world.isClientSide)
			world.playSoundAtEntity(null, this, s, getSoundVolume(), (random.nextFloat() - random.nextFloat()) * 0.2F + 0.6F);
	}

	public    String getLivingSound() { return "mob.pig"; }
	protected String getHurtSound()   { return "mob.pig"; }
	protected String getDeathSound()  { return "mob.pigdeath"; }

	public void playHuffSound() {
		if (!world.isClientSide)
			world.playSoundAtEntity(null, this, "mob.bear.huff", getSoundVolume(),
				(random.nextFloat() - random.nextFloat()) * 0.2F + 0.9F);
	}

	public void playJawClackSound() {
		if (!world.isClientSide)
			world.playSoundAtEntity(null, this, "mob.bear.clack", getSoundVolume(),
				(random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F);
	}

	// =========================================================================
	// Save / Load
	// =========================================================================

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putBoolean("SwipeRight", attackSwipeRight);
		tag.putInt("Anger",         angerCounter);
		tag.putBoolean("Sleeping",  sleeping);
		tag.putBoolean("HasHome",   hasHome);
		tag.putInt("HomeX",         homeX);
		tag.putInt("HomeY",         homeY);
		tag.putInt("HomeZ",         homeZ);
		tag.putInt("Belly",         getBelly());

		ItemStack mouth = getMouthItem();
		if (mouth != null && mouth.stackSize > 0) {
			CompoundTag itemTag = new CompoundTag();
			mouth.writeToNBT(itemTag);
			tag.putCompound("MouthItem", itemTag);
		}
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		attackSwipeRight = tag.getBoolean("SwipeRight");
		angerCounter     = tag.getInteger("Anger");
		sleeping         = tag.getBoolean("Sleeping");
		hasHome          = tag.getBoolean("HasHome");
		homeX            = tag.getInteger("HomeX");
		homeY            = tag.getInteger("HomeY");
		homeZ            = tag.getInteger("HomeZ");
		homeInitialized  = hasHome;
		if (tag.containsKey("Belly")) setBelly(tag.getInteger("Belly"));

		if (tag.containsKey("MouthItem")) {
			ItemStack stack = ItemStack.readItemStackFromNbt(tag.getCompound("MouthItem"));
			setMouthItem(stack);
		}
	}
}
