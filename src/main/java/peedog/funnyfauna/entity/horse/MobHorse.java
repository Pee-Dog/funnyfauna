package peedog.funnyfauna.entity.horse;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.client.input.PlayerInput;
import net.minecraft.core.WeightedRandomLootObject;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.block.tag.BlockTags;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.item.Items;
import net.minecraft.core.item.tag.ItemTags;
import net.minecraft.core.net.packet.PacketSetRiding;
import net.minecraft.core.util.collection.NamespaceID;
import net.minecraft.core.util.helper.DamageType;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.World;
import net.minecraft.server.MinecraftServer;
import peedog.funnyfauna.entity.MobFunnyRideable;
import peedog.funnyfauna.item.FunnyFaunaItems;
import peedog.funnyfauna.net.message.EjectRiderNetworkMessage;
import turniplabs.halplibe.helper.network.NetworkHandler;

import java.util.List;
import java.util.Random;

public class MobHorse extends MobFunnyRideable {
	// ===== Synced data IDs =====
	private static final int DATA_FLAGS = 16;
	private static final int DATA_GROWTH_TIMER = 17;
	private static final int DATA_SKIN_VARIANT = 18;
	private static final int DATA_BODY_VARIANT = 19;
	private static final int DATA_LEGS_VARIANT = 20;

	// ===== Flag bits =====
	private static final int FLAG_TAMED   = 1;
	private static final int FLAG_SADDLED = 2;
	private static final int FLAG_BABY    = 4;

	int annoyance = 0;
	int chanceForTame = 0;
	int tameCounter = 0;
	public String ownerName;

	// --- Breeding fields ---
	public boolean canBreed = false;
	public int breedingCooldown = 0;
	private MobHorse mateTarget = null;

	// --- Stats ---
	public float jumpStrength; // upward velocity
	public float movementSpeed; // blocks/tick internally

	public int customMaxHealth = 20;

	// --- Wild spawn ranges ---
	public final float WILD_MIN_SPEED_BPS = 4f;
	public final float WILD_MAX_SPEED_BPS = 9f;
	public final float WILD_MIN_JUMP  = 1.25f;
	public final float WILD_MAX_JUMP  = 2.0f;

	// --- Absolute breeding caps ---
	public final float MAX_SPEED_BPS = 12f;
	public final float MAX_JUMP  = 3.0f;

	// --- Texture variants ---
	public int skinVariant;   // 0-9
	public int bodyVariant;   // 0-4 (-1 = none)
	public int legsVariant;   // 0-2 (-1 = none)

	private final Random rand = new Random();
	private final float gravity = 0.08f;


	public MobHorse(World world) {
		super(world);
		this.textureIdentifier = NamespaceID.getPermanent("funnyfauna", "horse");
		this.setSize(1F, 1.8F);
		this.rideFootSize = 1.5f;
		this.mobDrops.add(new WeightedRandomLootObject(
			Items.LEATHER.getDefaultStack(), 2, 5
		));

		// --- Random wild jump ---
		float desiredJump = WILD_MIN_JUMP + rand.nextFloat() * (WILD_MAX_JUMP - WILD_MIN_JUMP);
		this.jumpStrength = (float)Math.sqrt(2 * gravity * desiredJump);

		// --- Random wild speed ---
		float speedBps = WILD_MIN_SPEED_BPS + rand.nextFloat() * (WILD_MAX_SPEED_BPS - WILD_MIN_SPEED_BPS);
		this.movementSpeed = speedBps / 20f; // convert blocks/sec to blocks/tick

		this.customMaxHealth = 15 + rand.nextInt(11); // 15-25
		this.setHealthRaw(customMaxHealth);

		// --- Random variants ---
		setSkinVariant(rand.nextInt(10));
		setBodyVariant(rand.nextInt(5));
		setLegsVariant(rand.nextInt(3));
	}
	@Override
	public void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(DATA_FLAGS, (byte)0, Byte.class);
		this.entityData.define(DATA_GROWTH_TIMER, 0, Integer.class);
		this.entityData.define(DATA_SKIN_VARIANT, 0, Integer.class);
		this.entityData.define(DATA_BODY_VARIANT, -1, Integer.class);
		this.entityData.define(DATA_LEGS_VARIANT, -1, Integer.class);
	}


	@Override
	public int getMaxHealth() { return customMaxHealth; }

	private byte getFlags() {
		return this.entityData.getByte(DATA_FLAGS);
	}

	private void setFlags(byte flags) {
		this.entityData.set(DATA_FLAGS, flags);
	}

	public boolean isTamed() {
		return (getFlags() & FLAG_TAMED) != 0;
	}

	public void setTamed(boolean value) {
		byte f = getFlags();
		setFlags(value ? (byte)(f | FLAG_TAMED) : (byte)(f & ~FLAG_TAMED));
	}

	public boolean isSaddled() {
		return (getFlags() & FLAG_SADDLED) != 0;
	}

	public void setSaddled(boolean value) {
		byte f = getFlags();
		setFlags(value ? (byte)(f | FLAG_SADDLED) : (byte)(f & ~FLAG_SADDLED));
	}

	public boolean isBaby() {
		return (getFlags() & FLAG_BABY) != 0;
	}

	public void setBaby(boolean value) {
		byte f = getFlags();
		setFlags(value ? (byte)(f | FLAG_BABY) : (byte)(f & ~FLAG_BABY));
	}
	public int getGrowthTimer() {
		return this.entityData.getInt(DATA_GROWTH_TIMER);
	}

	public void setGrowthTimer(int value) {
		this.entityData.set(DATA_GROWTH_TIMER, value);
	}
	public int getSkinVariant() { return entityData.getInt(DATA_SKIN_VARIANT); }
	public void setSkinVariant(int value) {
		value = MathHelper.clamp(value, 0, 9);
		entityData.set(DATA_SKIN_VARIANT, value);
	}


	public int getBodyVariant() { return entityData.getInt(DATA_BODY_VARIANT); }
	public void setBodyVariant(int value) {
		value = (value < 0) ? -1 : MathHelper.clamp(value, 0, 4);
		entityData.set(DATA_BODY_VARIANT, value);
	}

	public int getLegsVariant() { return entityData.getInt(DATA_LEGS_VARIANT); }
	public void setLegsVariant(int value) {
		value = (value < 0) ? -1 : MathHelper.clamp(value, 0, 2);
		entityData.set(DATA_LEGS_VARIANT, value);
	}


	@Override
	public boolean interact(Player player) {
		ItemStack item = player.getHeldItem();

		// --- Feeding for breeding ---
		if (item != null && !isBaby() && breedingCooldown <= 0) {
			if (item.itemID == Items.FOOD_APPLE_GOLD.id) {
				canBreed = true;
				if (player.getGamemode().consumeBlocks()) {
					player.swingItem();
					item.stackSize--;
				}
				for (int i = 0; i < 6; i++) {
					double rx = x + rand.nextDouble();
					double ry = y + 0.5 + rand.nextDouble() * bbHeight;
					double rz = z + rand.nextDouble();
					world.spawnParticle("heart", rx, ry, rz, 0, 0.2, 0, 0);
				}
			}
		}

		// --- Taming, healing, saddling ---
		if (item != null) {
			if (!isTamed()) {
				if (item.itemID == Items.WHEAT.id) chanceForTame += 1;
				if (item.itemID == Items.FOOD_APPLE.id) chanceForTame += rand.nextInt(5) + 1;
				if (item.itemID == Items.DUST_SUGAR.id) chanceForTame += rand.nextInt(8) + 1;

				if (player.getGamemode().consumeBlocks() &&
					(item.itemID == Items.WHEAT.id || item.itemID == Items.FOOD_APPLE.id || item.itemID == Items.DUST_SUGAR.id)) {
					player.swingItem();
					item.stackSize--;
				}
			}

			if (isTamed()) {
				if (item.itemID == Items.SADDLE.id) {
					setSaddled(true);
					if (player.getGamemode().consumeBlocks()) {
						player.swingItem();
						item.stackSize--;
					}
				}

				if (getHealth() < getMaxHealth()) {
					if (item.itemID == Items.WHEAT.id) {
						heal(Math.min(2, getMaxHealth() - getHealth()));
						if (player.getGamemode().consumeBlocks()) {
							player.swingItem();
							item.stackSize--;
						}
					}
					if (item.itemID == Items.FOOD_APPLE.id) {
						heal(Math.min(4, getMaxHealth() - getHealth()));
						if (player.getGamemode().consumeBlocks()) {
							player.swingItem();
							item.stackSize--;
						}
					}
				}
			}
		} else if (!isBaby()) {
			player.startRiding(this);
		}

		return super.interact(player);
	}

	public boolean getSaddled() { return this.isSaddled(); }

	@Override
	public void updateAI() {
		super.updateAI();
		if (canBreed && breedingCooldown <= 0 && !isBaby()) {
			spawnBreedingHearts();
		}

		// --- Wild horse taming behavior ---
		if (passenger instanceof Player && !isTamed()) {
			Player player = (Player) passenger;

			// Increase annoyance randomly or per tick
			annoyance += rand.nextInt(6) == 0 ? 20 : 0;

			if (annoyance >= 300) {
				if (!world.isClientSide) {
					buckRider();
				}
				annoyance = 0;
			}

			tameCounter += chanceForTame;
			if (tameCounter >= 1000) {
				setTamed(true);
				ownerName = player.username;
				chanceForTame = 0;
				annoyance = 0;
				for (int i = 0; i < 8; i++) {
					world.spawnParticle("heart", x + rand.nextDouble(), y + rand.nextDouble(), z + rand.nextDouble(), 0, 0.2, 0, 0);
				}
			}
		}


		// --- AI movement ---
		if (mateTarget != null) {
			double dx = mateTarget.x - this.x;
			double dz = mateTarget.z - this.z;
			double dist = Math.sqrt(dx * dx + dz * dz);
			if (dist > 0.1) {
				double moveFactor = movementSpeed; // blocks/tick
				this.xd += (dx / dist) * moveFactor;
				this.zd += (dz / dist) * moveFactor;
			}
		}

		handleGrowth();
		handleBreedingAI();
	}

	private void buckRider() {
		if (!(passenger instanceof Player)) return;

		Player player = (Player) passenger;
		this.ejectRider();

		if (!world.isClientSide) {
			// Send packet to sync riding state with clients
			// Assuming you have the same networking system as camels
			NetworkHandler.sendToAllAround(
				this.x, this.y, this.z, 32, this.world.dimension.id,
				new EjectRiderNetworkMessage(this)
			);

			MinecraftServer.getInstance().playerList.sendPacketToPlayersAroundPoint(
				x, y, z, 32, this.world.dimension.id,
				new PacketSetRiding(this, null)
			);
		}
	}

	private void handleGrowth() {
		if (isBaby()) {
			setGrowthTimer(getGrowthTimer() - 1);
			float scale = Math.max(0.5f, 0.5f + 0.5f * (1 - (getGrowthTimer() / 1200f)));
			this.setSize(1f * scale, 1.8f * scale);
			if (getGrowthTimer() <= 0) {
				setBaby(false);
				setSize(1f, 1.8f);
			}
		}
	}

	private void handleBreedingAI() {
		if (breedingCooldown > 0) breedingCooldown--;
		if (isBaby() || breedingCooldown > 0 || !canBreed) {
			mateTarget = null; // stop chasing if we can't breed
			return;
		}

		List<Entity> list = world.getEntitiesWithinAABBExcludingEntity(this,
			bb.expand(16, 16, 16).move(-8, -8, -8));
		MobHorse closest = null;
		double closestDist = Double.MAX_VALUE;

		for (Entity e : list) {
			if (e instanceof MobHorse) {
				MobHorse horse = (MobHorse) e;
				if (horse == this || horse.isBaby() || horse.breedingCooldown > 0 || !horse.canBreed) continue;
				double dist = distanceToSqr(horse);
				if (dist < closestDist) {
					closestDist = dist;
					closest = horse;
				}
			}

		}

		mateTarget = closest;

		if (mateTarget != null) {
			double dx = mateTarget.x - this.x;
			double dz = mateTarget.z - this.z;
			double dist = Math.sqrt(dx * dx + dz * dz);
			if (dist < 0.001) dist = 0.001;

			double speed = this.movementSpeed * 0.5;

			this.xd = dx / dist * speed;
			this.zd = dz / dist * speed;

			faceMovement();

			if (this.onGround && mateTarget.y - this.y > 0.5) {
				this.yd = 0.3;
				this.isJumping = true;
			} else {
				this.isJumping = false;
			}

			if (dist < 2.0) {
				breedWith(mateTarget);
				mateTarget = null;
			}
		}

	}


	private void breedWith(MobHorse mate) {
		this.canBreed = false;
		mate.canBreed = false;

		this.breedingCooldown = 20 * 60 * 5;
		mate.breedingCooldown = 20 * 60 * 5;

		MobHorse baby = new MobHorse(this.world);
		baby.setBaby(true);
		baby.setGrowthTimer(1200);
		baby.setSize(1f * 0.5f, 1.8f * 0.5f);

		// --- Inherit and mutate jump height ---
		float avgJumpHeight = (this.getJumpHeight() + mate.getJumpHeight()) / 2f;
		avgJumpHeight += (rand.nextFloat() - 0.5f) * 0.2f;
		avgJumpHeight = Math.min(avgJumpHeight, MAX_JUMP);
		baby.jumpStrength = (float)Math.sqrt(2 * gravity * avgJumpHeight);

		// --- Inherit and mutate speed ---
		float avgSpeedBps = ((this.movementSpeed + mate.movementSpeed) / 2f) * 20f;
		avgSpeedBps += (rand.nextFloat() - 0.5f) * 0.5f;
		avgSpeedBps = Math.min(avgSpeedBps, MAX_SPEED_BPS);
		baby.movementSpeed = avgSpeedBps / 20f;

		// --- Health ---
		baby.customMaxHealth = Math.max(1, (this.customMaxHealth + mate.customMaxHealth) / 2 + rand.nextInt(3) - 1);
		baby.setHealthRaw(baby.customMaxHealth);
		baby.setTamed(false);

		// --- Variants ---
		baby.setSkinVariant(rand.nextBoolean() ? this.getSkinVariant() : mate.getSkinVariant());
		baby.setBodyVariant(rand.nextBoolean() ? this.getBodyVariant() : mate.getBodyVariant());
		baby.setLegsVariant(rand.nextBoolean() ? this.getLegsVariant() : mate.getLegsVariant());

		baby.moveTo(this.x, this.y, this.z, 0, 0);
		world.entityJoinedWorld(baby);

		for (int i = 0; i < 7; i++) {
			double offsetX = (rand.nextDouble() - 0.5) * bbWidth;
			double offsetY = rand.nextDouble() * bbHeight;
			double offsetZ = (rand.nextDouble() - 0.5) * bbWidth;
			world.spawnParticle("heart", this.x + offsetX, this.y + offsetY, this.z + offsetZ, 0, 0, 0, 0);
		}
	}

	/** Converts current jumpStrength back into approximate jump height in blocks */
	public float getJumpHeight() {
		return (jumpStrength * jumpStrength) / (2 * gravity);
	}

	@Override
	public float getYRotDelta() { return 0; }
	@Override
	public float getXRotDelta() { return 0; }
	@Override
	protected boolean canDespawn() {
		if (isTamed()) return false; // never despawn if tamed
		return super.canDespawn();
	}

	@Override
	public String getLivingSound() { return "funnyfauna:mob.horse.idle"; }
	@Override
	protected String getHurtSound() { return "funnyfauna:mob.horse.hurt"; }
	@Override
	protected String getDeathSound() { return "funnyfauna:mob.horse.death"; }
	@Override
	protected void dropDeathItems() {
		if (this.getSaddled()) this.dropItem(Items.SADDLE.id, 1);
		super.dropDeathItems();
	}
	@Override
	public double getRideHeight() { return (double) bbHeight * 0.7; }

	@Override
	protected float getRideJumpStrength() {
		return this.jumpStrength;
	}
	@Override
	protected float getRideMovementSpeed() {
		return this.movementSpeed;
	}

	// --- NBT save/load ---
	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);

		// Use getters that read from entityData
		tag.putBoolean("Tamed", isTamed());
		tag.putBoolean("Saddled", isSaddled());
		tag.putBoolean("isBaby", isBaby());
		tag.putInt("growthTimer", getGrowthTimer());

		// Server-only fields that aren’t synced can stay as-is
		tag.putInt("ChanceForTame", chanceForTame);
		tag.putInt("Annoyance", annoyance);
		tag.putInt("TameCounter", tameCounter);
		tag.putInt("breedingCooldown", breedingCooldown);
		tag.putBoolean("canBreed", canBreed);

		tag.putFloat("jumpStrength", jumpStrength);
		tag.putFloat("movementSpeed", movementSpeed);
		tag.putInt("customMaxHealth", customMaxHealth);

		// Variants should also use getters
		tag.putInt("SkinVariant", getSkinVariant());
		tag.putInt("BodyVariant", getBodyVariant());
		tag.putInt("LegsVariant", getLegsVariant());

		if (isTamed()) tag.putString("Owner", ownerName);
	}


	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);

		// ✅ Use setters for synced flags and variants
		setTamed(tag.getBoolean("Tamed"));
		setSaddled(tag.getBoolean("Saddled"));
		setBaby(tag.getBoolean("isBaby"));
		setGrowthTimer(tag.getInteger("growthTimer"));

		// Server-only fields can stay as direct assignments
		chanceForTame = tag.getInteger("ChanceForTame");
		annoyance = tag.getInteger("Annoyance");
		tameCounter = tag.getInteger("TameCounter");
		breedingCooldown = tag.getInteger("breedingCooldown");
		canBreed = tag.getBoolean("canBreed");

		jumpStrength = tag.getFloat("jumpStrength");
		movementSpeed = tag.getFloat("movementSpeed");
		customMaxHealth = tag.getInteger("customMaxHealth");

		setSkinVariant(tag.getInteger("SkinVariant"));
		setBodyVariant(tag.getInteger("BodyVariant"));
		setLegsVariant(tag.getInteger("LegsVariant"));

		if (isTamed()) ownerName = tag.getString("Owner");

		// Make sure health does not exceed custom max
		this.setHealthRaw(Math.min(getHealth(), customMaxHealth));
	}


	@Override
	public boolean hurt(Entity attacker, int damage, DamageType type) {
		if (type == DamageType.FALL) {
			if (this.fallDistance <= this.getJumpHeight()) {
				return false;
			}
		}
		if (!world.isClientSide) {
			if (attacker instanceof Player && !isTamed()) tameCounter -= 150;
		}
		return super.hurt(attacker, damage, type);
	}

	@Override
	protected boolean canBeControlled() { return isSaddled() && !isBaby(); }

	public boolean isFavouriteItem(ItemStack itemStack) {
		return itemStack != null && itemStack.getItem().hasTag(ItemTags.COWS_FAVOURITE_ITEM);
	}
	private void spawnBreedingHearts() {
		if (world.isClientSide) return;

		if (rand.nextInt(10) == 0) { // once every ~10 ticks
			world.spawnParticle(
				"heart",
				x + (rand.nextDouble() - 0.5) * bbWidth,
				y + 0.5 + rand.nextDouble() * bbHeight,
				z + (rand.nextDouble() - 0.5) * bbWidth,
				0, 0.2, 0, 0
			);
		}
	}
	private void faceMovement() {
		this.yRot = (float)(Math.atan2(this.zd, this.xd) * 180.0 / Math.PI) - 90.0F;
		this.yRotO = this.yRot;
	}




}
