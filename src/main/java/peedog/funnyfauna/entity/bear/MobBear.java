package peedog.funnyfauna.entity.bear;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.core.WeightedRandomLootObject;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.Items;
import net.minecraft.core.util.collection.NamespaceID;
import net.minecraft.core.util.helper.DamageType;
import net.minecraft.core.enums.Difficulty;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.World;
import peedog.funnyfauna.entity.MobTaskrunner;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.controllers.BearTask;
import peedog.funnyfauna.entity.ai.interfaces.IAttacker;
import peedog.funnyfauna.item.FunnyFaunaItems;

import java.util.ArrayList;
import java.util.List;

public class MobBear extends MobTaskrunner implements IAttacker {

	public int attackAnimTime;
	private int attackCooldown = 0;
	/** Tracks which paw to swipe with next — true = right, false = left */
	public boolean attackSwipeRight = true;

	private int angerCounter = 0;
	private static final float AGGRO_RANGE = 4.0F;

	public List<WeightedRandomLootObject> burningMobDrops = new ArrayList<>();

	public MobBear(World world) {
		super(world);
		setSize(1.2F, 1.4F);
		this.textureIdentifier = NamespaceID.getPermanent("funnyfauna", "bear");
		mobDrops.add(new WeightedRandomLootObject(Items.FOOD_PORKCHOP_RAW.getDefaultStack(), 0, 2));
		mobDrops.add(new WeightedRandomLootObject(FunnyFaunaItems.COARSEHIDE.getDefaultStack(), 1, 3));
		burningMobDrops.add(new WeightedRandomLootObject(Items.FOOD_PORKCHOP_COOKED.getDefaultStack(), 1, 2));
		burningMobDrops.add(new WeightedRandomLootObject(FunnyFaunaItems.COARSEHIDE.getDefaultStack(), 1, 3));
	}

	@Override
	public Task<MobBear> createTask() {
		return new BearTask(this);
	}

	@Override
	public void tick() {
		super.tick();

		if (attackAnimTime > 0) attackAnimTime--;
		if (attackCooldown > 0) attackCooldown--;
		if (angerCounter > 0) angerCounter--;

		// Proximity aggression — check every tick so we never miss the window
		if (!world.isClientSide && getTarget() == null && world.getDifficulty() != Difficulty.PEACEFUL) {
			Player nearest = (Player) world.getClosestPlayerToEntity(this, AGGRO_RANGE);
			if (nearest != null) {
				setTarget(nearest);
				angerCounter = 400;
			}
		}
	}

	@Override
	public boolean hurt(Entity attacker, int damage, DamageType type) {
		boolean result = super.hurt(attacker, damage, type);

		if (!world.isClientSide && attacker != null && attacker.isAlive()) {
			setTarget(attacker);
			angerCounter = 400;
		}

		return result;
	}

	// -------------------------------------------------------------------------
	// IAttacker
	// -------------------------------------------------------------------------

	@Override
	public boolean isAttacking() {
		return attackAnimTime > 0;
	}

	@Override
	public int getAttackCooldown() {
		return attackCooldown;
	}

	@Override
	public void setAttackCooldown(int ticks) {
		this.attackCooldown = ticks;
	}

	@Override
	public float getAttackRange() {
		return 2.5F;
	}

	@Override
	public void startAttack(Entity target) {
		if (target == null || !target.isAlive()) return;

		if (attackCooldown <= 0) {
			target.hurt(this, 3, DamageType.COMBAT);
			attackCooldown = 20;
			// Alternate paws each hit
			attackSwipeRight = !attackSwipeRight;
		}

		if (attackAnimTime <= 0) {
			attackAnimTime = 10; // 0.5 s at 20 tps — matches animation_length
		}
	}

	// -------------------------------------------------------------------------
	// Spawning
	// -------------------------------------------------------------------------

	public boolean canSpawnHere() {
		int x = MathHelper.floor(this.x);
		int y = MathHelper.floor(this.bb.minY);
		int z = MathHelper.floor(this.z);

		int id = this.world.getBlockId(x, y - 1, z);

		if (id == 0 || id == 8 || id == 9 || id == 10 || id == 11 || y < 128) {
			return false;
		}
		return true;
	}

	// -------------------------------------------------------------------------
	// Sounds
	// -------------------------------------------------------------------------

	@Override
	public void playLivingSound() {
		String s = getLivingSound();
		if (s != null && !world.isClientSide) {
			world.playSoundAtEntity(null, this, s, getSoundVolume(), (random.nextFloat() - random.nextFloat()) * 0.2F + 0.6F);
		}
	}

	@Override
	public void playHurtSound() {
		String s = getHurtSound();
		if (s != null && !world.isClientSide) {
			world.playSoundAtEntity(null, this, s, getSoundVolume(), (random.nextFloat() - random.nextFloat()) * 0.2F + 0.6F);
		}
	}

	@Override
	public void playDeathSound() {
		String s = getDeathSound();
		if (s != null && !world.isClientSide) {
			world.playSoundAtEntity(null, this, s, getSoundVolume(), (random.nextFloat() - random.nextFloat()) * 0.2F + 0.6F);
		}
	}

	public String getLivingSound() { return "mob.pig"; }
	protected String getHurtSound() { return "mob.pig"; }
	protected String getDeathSound() { return "mob.pigdeath"; }

	// -------------------------------------------------------------------------
	// Save / Load
	// -------------------------------------------------------------------------

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putBoolean("SwipeRight", attackSwipeRight);
		tag.putInt("Anger", angerCounter);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		attackSwipeRight = tag.getBoolean("SwipeRight");
		angerCounter = tag.getInteger("Anger");
	}

	@Override
	protected List<WeightedRandomLootObject> getMobDrops() {
		return remainingFireTicks > 0 ? burningMobDrops : mobDrops;
	}
}
