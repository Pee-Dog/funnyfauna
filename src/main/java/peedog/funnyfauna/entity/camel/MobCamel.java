
	package peedog.funnyfauna.entity.camel;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.core.WeightedRandomLootObject;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.item.Items;
import net.minecraft.core.net.packet.PacketSetRiding;
import net.minecraft.core.util.collection.NamespaceID;
import net.minecraft.core.util.helper.DamageType;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.IVehicle;
import net.minecraft.core.world.World;
import net.minecraft.server.MinecraftServer;
import peedog.funnyfauna.FunnyFauna;
import peedog.funnyfauna.PlayerInventoryDisplay;
import peedog.funnyfauna.entity.MobFunnyRideable;
import peedog.funnyfauna.gui.InventoryCamel;
import peedog.funnyfauna.item.FunnyFaunaItems;
import peedog.funnyfauna.net.message.EjectRiderNetworkMessage;
import turniplabs.halplibe.helper.EnvironmentHelper;
import turniplabs.halplibe.helper.network.NetworkHandler;

import java.util.List;
import java.util.Random;

public class MobCamel extends MobFunnyRideable {

	/* =========================
	   === Synced Flags ===
	   ========================= */
	private static final int DATA_FLAGS = 16;
	private static final int DATA_CHEST_COUNT = 17; // next available data ID
	private static final int DATA_SKIN_VARIANT = 18;
	private static final int DATA_GROWTH_TIMER = 19;

	private byte getFlags() {
		return this.entityData.getByte(DATA_FLAGS);
	}

	private void setFlags(byte flags) {
		this.entityData.set(DATA_FLAGS, flags);
	}

	public boolean isTamed() {
		return (getFlags() & 1) != 0;
	}

	public void setTamed(boolean tamed) {
		byte f = getFlags();
		setFlags(tamed ? (byte) (f | 1) : (byte) (f & ~1));
	}

	public boolean isSaddled() {
		return (getFlags() & 2) != 0;
	}

	public void setSaddled(boolean saddled) {
		byte f = getFlags();
		setFlags(saddled ? (byte) (f | 2) : (byte) (f & ~2));
	}

	public boolean isBaby() {
		return (getFlags() & 4) != 0;
	}

	public void setBaby(boolean baby) {
		byte f = getFlags();
		setFlags(baby ? (byte) (f | 4) : (byte) (f & ~4));
	}

	public void setSkinVariant(int variant) {
		// Clamp to valid range if needed
		variant = Math.max(0, Math.min(3, variant));
		this.entityData.set(DATA_SKIN_VARIANT, variant);
	}

	public int getSkinVariant() {
		return this.entityData.getInt(DATA_SKIN_VARIANT);
	}

	/* =========================
	   === Other fields ===
	   ========================= */
	public String ownerName;
	private int annoyance = 0;
	private int chanceForTame = 0;
	private int tameCounter = 0;

	public int growthTimer = 0;
	public boolean canBreed = false;
	public int breedingCooldown = 0;
	private MobCamel mateTarget = null;

	public float jumpStrength;
	public float movementSpeed;
	public int customMaxHealth;


	private InventoryCamel camelInventory;

	private static final Random rand = new Random();
	private static final float GRAVITY = 0.08f;

	public static final float WILD_MIN_SPEED_BPS = 5.0f;
	public static final float WILD_MAX_SPEED_BPS = 7.0f;
	public static final float WILD_MIN_JUMP = 0.9f;
	public static final float WILD_MAX_JUMP = 1.4f;

	public static final float MAX_SPEED_BPS = 9.0f;
	public static final float MAX_JUMP = 1.8f;

	public MobCamel(World world) {
		super(world);

		this.textureIdentifier = NamespaceID.getPermanent("funnyfauna", "camel");
		this.setSize(1.0F, 1.8F);
		this.rideFootSize = 1F;

		this.mobDrops.add(new WeightedRandomLootObject(
			FunnyFaunaItems.COARSEHIDE.getDefaultStack(), 2, 5
		));

		float desiredJump = WILD_MIN_JUMP + rand.nextFloat() * (WILD_MAX_JUMP - WILD_MIN_JUMP);
		this.jumpStrength = (float) Math.sqrt(2 * GRAVITY * desiredJump);

		float speedBps = WILD_MIN_SPEED_BPS + rand.nextFloat() * (WILD_MAX_SPEED_BPS - WILD_MIN_SPEED_BPS);
		this.movementSpeed = speedBps / 20f;

		this.customMaxHealth = 20 + rand.nextInt(11);
		this.setHealthRaw(customMaxHealth);

		setSkinVariant(rand.nextInt(4));


		this.camelInventory = new InventoryCamel(this);
	}

	public InventoryCamel getCamelInventory() {
		return camelInventory;
	}

	public int getChestCount() {
		return this.entityData.getInt(DATA_CHEST_COUNT);
	}

	private void setChestCount(int count) {
		count = Math.max(0, Math.min(2, count));
		this.entityData.set(DATA_CHEST_COUNT, count); // synced to clients
		if (camelInventory != null) {
			camelInventory.updateSize(count * 27); // update inventory
		}
	}


	private void attachChest() {
		if (getChestCount() < 2) {
			setChestCount(getChestCount() + 1);
			setChanged(); // server-side persistence
		}
	}

	public void setGrowthTimer(int timer) {
		this.growthTimer = timer;
		this.entityData.set(DATA_GROWTH_TIMER, timer);
	}

	public int getGrowthTimer() {
		return this.entityData.getInt(DATA_GROWTH_TIMER);
	}


	@Override
	public boolean interact(Player player) {
		ItemStack item = player.getHeldItem();

		// Attach chest
		if (item != null &&
			(item.itemID == Blocks.CHEST_LEGACY.id() ||
				item.itemID == Blocks.CHEST_LEGACY_PAINTED.id() ||
				item.itemID == Blocks.CHEST_PLANKS_OAK.id() ||
				item.itemID == Blocks.CHEST_PLANKS_OAK_PAINTED.id())) {
			if (getChestCount() < 2) {
				attachChest();

				if (player.getGamemode().consumeBlocks()) {
					player.swingItem();
					item.stackSize--;
				}
				return true;
			}
		}

		// Open inventory while riding
		// Server-side, camel has chests
		if (!world.isClientSide && player == this.passenger && camelInventory.getContainerSize() > 0) {
			player.displayChestScreen(
				camelInventory,
				this.x,
				this.y,
				this.z
			);
			return true;
		}


		// Breeding
		if (item != null && !isBaby() && breedingCooldown <= 0 && !canBreed) {
			if (item.itemID == FunnyFaunaItems.TUMBLEWEED.id) {
				canBreed = true;
				if (player.getGamemode().consumeBlocks()) {
					player.swingItem();
					item.stackSize--;
				}
				for (int i = 0; i < 6; i++) {
					world.spawnParticle("heart",
						x + rand.nextDouble(),
						y + 0.5 + rand.nextDouble() * bbHeight,
						z + rand.nextDouble(),
						0, 0.2, 0, 0
					);
				}
				return true;
			}
		}

		// Taming / healing / saddle
		if (item != null) {
			if (!isTamed() && item.itemID == Blocks.CACTUS.id()) {
				if (!world.isClientSide) {
					chanceForTame += rand.nextInt(5) + 1;
				}
				if (player.getGamemode().consumeBlocks()) {
					player.swingItem();
					item.stackSize--;
				}
				return true;
			}

			if (isTamed()) {
				if (item.itemID == Items.SADDLE.id && !isSaddled()) {
					setSaddled(true);
					if (player.getGamemode().consumeBlocks()) {
						player.swingItem();
						item.stackSize--;
					}
					return true;
				}

				if (getHealth() < getMaxHealth() && item.itemID == Blocks.CACTUS.id()) {
					heal(4);
					if (player.getGamemode().consumeBlocks()) {
						player.swingItem();
						item.stackSize--;
					}
					return true;
				}
			}
		}

		// Mount
		if (!isBaby() && item == null && player != this.passenger) {
			if (!world.isClientSide) {
				player.startRiding(this);
			}
			return true;
		}

		return super.interact(player);
	}

	@Override
	public void updateAI() {
		// ==== Player annoyance and taming ====
		if (passenger instanceof Player && !isTamed()) {
			Player player = (Player) passenger;

			// Increase annoyance
			annoyance += 20;

			if (annoyance >= 300) {
				// Buck the rider
				if (!world.isClientSide) {
					buckRider();
				}
				annoyance = 0;
			}

			// Tame logic
			tameCounter += chanceForTame;
			if (tameCounter >= 1000) {
				setTamed(true);
				ownerName = player.username;
				chanceForTame = 0;
				annoyance = 0; // Reset annoyance when tamed

				if (!world.isClientSide) {
					for (int i = 0; i < 8; i++) {
						world.spawnParticle("heart",
							x + rand.nextDouble(),
							y + rand.nextDouble(),
							z + rand.nextDouble(),
							0, 0.2, 0, 0
						);
					}
				}
			}
		}

		// ==== Breeding & growth ====
		if (canBreed && breedingCooldown <= 0 && !isBaby()) spawnBreedingHearts();
		handleGrowth();
		handleBreedingAI();

		super.updateAI();
	}

	private void handleGrowth() {
		if (isBaby()) {
			setGrowthTimer(growthTimer - 1); // decrement via setter to sync
			if (growthTimer <= 0) {
				setBaby(false);
				setSize(1.0F, 1.8F);
			}
		}
	}


	public void setChanged() {
		if (this.world.isClientSide) return;
		// entity save will handle persistence
	}


	private void handleBreedingAI() {
		if (breedingCooldown > 0) breedingCooldown--;
		if (isBaby() || breedingCooldown > 0 || !canBreed) {
			mateTarget = null;
			return;
		}

		List<Entity> list = world.getEntitiesWithinAABBExcludingEntity(this, bb.expand(16, 16, 16));
		MobCamel closest = null;
		double closestDist = Double.MAX_VALUE;

		for (Entity e : list) {
			if (e instanceof MobCamel) {
				MobCamel camel = (MobCamel) e;
				if (camel != this && !camel.isBaby() && camel.canBreed && camel.breedingCooldown <= 0) {
					double dist = distanceToSqr(camel);
					if (dist < closestDist) {
						closestDist = dist;
						closest = camel;
					}
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
				this.yd = 0.25;
				this.isJumping = true;
			} else this.isJumping = false;

			if (dist < 2.0) {
				breedWith(mateTarget);
				mateTarget = null;
			}
		}
	}

	private void breedWith(MobCamel mate) {
		this.canBreed = false;
		mate.canBreed = false;
		this.breedingCooldown = mate.breedingCooldown = 20 * 60 * 5;

		MobCamel baby = new MobCamel(world);
		baby.setBaby(true);
		baby.setGrowthTimer(1200); // <-- use setter instead of assigning directly
		baby.setSize(0.5F, 0.9F);


		float avgJumpHeight = (this.getJumpHeight() + mate.getJumpHeight()) / 2f;
		avgJumpHeight += (rand.nextFloat() - 0.5f) * 0.15f;
		avgJumpHeight = Math.min(avgJumpHeight, MAX_JUMP);
		baby.jumpStrength = (float) Math.sqrt(2 * GRAVITY * avgJumpHeight);

		float avgSpeedBps = ((this.movementSpeed + mate.movementSpeed) / 2f) * 20f;
		avgSpeedBps += (rand.nextFloat() - 0.5f) * 0.4f;
		avgSpeedBps = Math.min(avgSpeedBps, MAX_SPEED_BPS);
		baby.movementSpeed = avgSpeedBps / 20f;

		baby.customMaxHealth = Math.max(1, (this.customMaxHealth + mate.customMaxHealth) / 2);
		baby.setHealthRaw(baby.customMaxHealth);

		baby.setSkinVariant(rand.nextBoolean() ? this.getSkinVariant() : mate.getSkinVariant());


		baby.moveTo(x, y, z, 0, 0);
		world.entityJoinedWorld(baby);
	}
	@Override
	public boolean canSpawnHere() {
		int x = MathHelper.floor(this.x);
		int y = MathHelper.floor(this.bb.minY);
		int z = MathHelper.floor(this.z);
		int id = this.world.getBlockId(x, y - 1, z);
		return id != 0 && id != 8 && id != 9 && id != 10 && id != 11;
	}

	@Override
	protected boolean canBeControlled() {
		return isSaddled() && isTamed() && !isBaby();
	}

	@Override
	protected boolean canDespawn() {
		return isTamed() ? false : super.canDespawn();
	}

	@Override
	public double getRideHeight() {
		return bbHeight * 0.85;
	}

	@Override
	public String getLivingSound() {
		return "funnyfauna:mob.camel.idle";
	}

	@Override
	protected String getHurtSound() {
		return "funnyfauna:mob.camel.hurt";
	}

	@Override
	protected String getDeathSound() {
		return "funnyfauna:mob.camel.death";
	}

	@Override
	protected void dropDeathItems() {
		if (isSaddled()) dropItem(Items.SADDLE.id, 1);
		for (int i = 0; i < getChestCount(); i++) dropItem(Blocks.CHEST_PLANKS_OAK.id(), 1);

		super.dropDeathItems();
	}

	@Override
	protected float getRideJumpStrength() {
		return jumpStrength;
	}

	@Override
	protected float getRideMovementSpeed() {
		return movementSpeed;
	}

	public float getJumpHeight() {
		return (jumpStrength * jumpStrength) / (2 * GRAVITY);
	}

	private void spawnBreedingHearts() {
		if (world.isClientSide) return;
		if (rand.nextInt(10) == 0) {
			for (int i = 0; i < 8; i++) {
				world.spawnParticle("heart",
					x + rand.nextDouble(),
					y + rand.nextDouble(),
					z + rand.nextDouble(),
					0, 0.2, 0, 0
				);
			}
		}
	}

	private void faceMovement() {
		this.yRot = (float) (Math.atan2(this.zd, this.xd) * 180.0 / Math.PI) - 90.0F;
		this.yRotO = this.yRot;
	}

	@Override
	public boolean hurt(Entity source, int amount, DamageType type) {
		if (type == DamageType.COMBAT && source == null) return false;
		return super.hurt(source, amount, type);
	}

	private void buckRider() {
		if (!(passenger instanceof Player)) return;

		Player player = (Player) passenger;

		// Server-side authoritative eject
		this.ejectRider();

		// Client-side sync
		if (EnvironmentHelper.isServerEnvironment()) {
			NetworkHandler.sendToAllAround(
				this.x, this.y, this.z, 32, this.world.dimension.id,
				new EjectRiderNetworkMessage(this) // <-- VEHICLE
			);

			MinecraftServer.getInstance().playerList.sendPacketToPlayersAroundPoint(
				x, y, z, 32, this.world.dimension.id,
				new PacketSetRiding(this, null)
			);
		}
	}


	@Override
	public void startRiding(IVehicle vehicle) {
		super.startRiding(vehicle);

		// Reset sync cooldown for immediate sync

		// Send riding packet on server
		if (EnvironmentHelper.isServerEnvironment() && this.passenger != null) {
			MinecraftServer.getInstance().playerList.sendPacketToPlayersAroundPoint(
				x, y, z, 32, this.world.dimension.id,
				new PacketSetRiding(this, this.passenger)
			);
		}
	}

	@Override
	public void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(DATA_FLAGS, (byte) 0, Byte.class);
		this.entityData.define(DATA_CHEST_COUNT, 0, Integer.class);
		this.entityData.define(DATA_SKIN_VARIANT, 0, Integer.class); // NEW
		this.entityData.define(DATA_GROWTH_TIMER, 0, Integer.class); // NEW
	}


	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putByte("Flags", getFlags());
		tag.putInt("ChanceForTame", chanceForTame);
		tag.putInt("Annoyance", annoyance);
		tag.putInt("TameCounter", tameCounter);

		tag.putInt("Growth", getGrowthTimer());


		tag.putInt("BreedCooldown", breedingCooldown);
		tag.putFloat("Jump", jumpStrength);
		tag.putFloat("Speed", movementSpeed);
		tag.putInt("MaxHealth", customMaxHealth);
		tag.putInt("Skin", getSkinVariant());

		tag.putInt("ChestCount", getChestCount()); // ✅ save from entityData
		camelInventory.writeToNBT(tag);
		if (isTamed()) tag.putString("Owner", ownerName);

	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);

		setFlags(tag.getByte("Flags"));
		chanceForTame = tag.getInteger("ChanceForTame");
		annoyance = tag.getInteger("Annoyance");
		tameCounter = tag.getInteger("TameCounter");

		setGrowthTimer(tag.getInteger("Growth"));
		breedingCooldown = tag.getInteger("BreedCooldown");
		jumpStrength = tag.getFloat("Jump");
		movementSpeed = tag.getFloat("Speed");
		customMaxHealth = tag.getInteger("MaxHealth");
		setSkinVariant(tag.getInteger("Skin"));

		int savedChests = Math.max(0, Math.min(2, tag.getInteger("ChestCount")));
		if (camelInventory == null) camelInventory = new InventoryCamel(this);
		camelInventory.updateSize(savedChests * 27); // resize first
		camelInventory.readFromNBT(tag);             // then load contents
		this.entityData.set(DATA_CHEST_COUNT, savedChests);



		if (isTamed()) ownerName = tag.getString("Owner");
	}
}

