package peedog.funnyfauna.entity.bird;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.block.material.Material;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.EntityItem;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemSeeds;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.util.collection.NamespaceID;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.util.phys.AABB;
import net.minecraft.core.world.World;
import org.useless.util.AnimationState;
import peedog.funnyfauna.block.ServerBlockPos3D;
import peedog.funnyfauna.entity.MobTaskrunner;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.controllers.BirdTask;
import peedog.funnyfauna.entity.ai.i.IFlockable;
import peedog.funnyfauna.entity.ai.i.IFlyable;
import peedog.funnyfauna.entity.ai.i.IHomeable;

import java.util.List;

public class MobBird extends MobTaskrunner implements IFlyable, IFlockable, IHomeable {

	private static final int DATA_FLAGS = 16;
	private static final int DATA_SKIN_VARIANT = 17;

	public AnimationState flyState = new AnimationState();

	// Flight state
	private int flightTime = 0;
	private double groundY = 0.0;

	// Wing animation
	public float flap = 0.0F;
	public float flapSpeed = 0.0F;
	public float oFlap = 0.0F;
	public float oFlapSpeed = 0.0F;
	public float flapping = 1.0F;

	// Perching/Landing
	private boolean soloPerchingFlight = false;
	public double homeX = 0.0;
	public double homeZ = 0.0;

	// Fed state
	public boolean isFed = false;

	// Sound
	private int ambientSoundTimer;

	public MobBird(World world) {
		super(world);
		setSize(0.5F, 0.5F);
		this.textureIdentifier = NamespaceID.getPermanent("funnyfauna", "bird");
		speed = 0.15f;
		ambientSoundTimer = random.nextInt(200);
	}

	@Override
	public void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(DATA_FLAGS, (byte) 0, Byte.class);
		this.entityData.define(DATA_SKIN_VARIANT, random.nextInt(2), Integer.class);
	}

	@Override
	public Task<MobBird> createTask() {
		return new BirdTask(this);
	}

	// ==================== FLAG MANAGEMENT ====================

	private byte getFlags() {
		return this.entityData.getByte(DATA_FLAGS);
	}

	private void setFlags(byte flags) {
		this.entityData.set(DATA_FLAGS, flags);
	}

	@Override
	public boolean isFlying() {
		return (getFlags() & 1) != 0;
	}

	@Override
	public void setFlying(boolean flying) {
		byte f = getFlags();
		setFlags(flying ? (byte)(f | 1) : (byte)(f & ~1));

		// Update size when flight state changes
		// Full size when flying (but not during solo perch seeking)
		if (flying && !isSoloFlying()) {
			setSize(1.5F, 1.5F);
		} else {
			setSize(0.5F, 0.5F);
		}
		setPos(x, y, z); // Refresh AABB
	}

	@Override
	public boolean isPerched() {
		return (getFlags() & 2) != 0;
	}

	@Override
	public void setPerched(boolean perched) {
		byte f = getFlags();
		setFlags(perched ? (byte)(f | 2) : (byte)(f & ~2));
	}

	public boolean isLanding() {
		return (getFlags() & 4) != 0;
	}

	public void setLanding(boolean landing) {
		byte f = getFlags();
		setFlags(landing ? (byte)(f | 4) : (byte)(f & ~4));
	}

	// ==================== IFLYABLE IMPLEMENTATION ====================

	@Override
	public int getFlightTime() {
		return flightTime;
	}

	@Override
	public void setFlightTime(int ticks) {
		this.flightTime = ticks;
	}

	@Override
	public double getGroundY() {
		return groundY;
	}

	@Override
	public void setGroundY(double y) {
		this.groundY = y;
	}

	@Override
	public float getFlap() {
		return flap;
	}

	@Override
	public void setFlap(float flap) {
		this.flap = flap;
	}

	@Override
	public float getFlapSpeed() {
		return flapSpeed;
	}

	@Override
	public void setFlapSpeed(float speed) {
		this.flapSpeed = speed;
	}

	@Override
	public float getFlapping() {
		return flapping;
	}

	@Override
	public void setFlapping(float flapping) {
		this.flapping = flapping;
	}

	@Override
	public void setLandingSize(boolean landing) {

	}

	// ==================== IFLOCKABLE IMPLEMENTATION ====================

	@Override
	public boolean canFlockWith(Entity other) {
		// Birds flock with other birds of the same skin variant
		if (!(other instanceof MobBird)) return false;
		MobBird otherBird = (MobBird) other;
		return otherBird.getSkinVariant() == this.getSkinVariant();
	}

	@Override
	public boolean isSoloFlying() {
		return soloPerchingFlight;
	}

	@Override
	public void setSoloFlying(boolean solo) {
		this.soloPerchingFlight = solo;
	}

	// ==================== SKIN VARIANT ====================

	public void setSkinVariant(int variant) {
		variant = Math.max(0, Math.min(1, variant));
		this.entityData.set(DATA_SKIN_VARIANT, variant);
	}

	public int getSkinVariant() {
		return this.entityData.getInt(DATA_SKIN_VARIANT);
	}

	// ==================== ADDITIONAL BEHAVIOR ====================

	@Override
	public boolean hasHome() {
		return false;
	}

	@Override
	public void setHome(int x, int y, int z) {

	}

	public int getHomeX() {
		return 0;}

	@Override
	public int getHomeY() {
		return 0;
	}

	public int getHomeZ() {return 0;}

	@Override
	public double getDistanceToHomeSq(double x, double y, double z) {
		return 0;
	}


	@Override
	public void tick() {
		super.tick();

		// Update animation state
		flyState.animateWhen(isFlying(), tickCount);

		// Update wing animation
		updateWingAnimation();

		// Handle ambient sounds
		if (!world.isClientSide) {
			if (ambientSoundTimer-- <= 0) {
				playBirdSound();
				ambientSoundTimer = 200 + random.nextInt(200);
			}
		}
	}

	@Override
	public void updateAI() {
		super.updateAI();

		// Update home position when grounded
		if ((onGround || isPerched()) && !isFlying()) {
			homeX = x;
			homeZ = z;
		}

		// Check for nearby threats (player sprinting, low health)
		checkForThreats();

		// Check if should enter perch mode (on leaves or at night)
		checkPerchConditions();
	}

	private void updateWingAnimation() {
		this.oFlap = this.flap;
		this.oFlapSpeed = this.flapSpeed;

		if (isPerched()) {
			this.flapSpeed = 0;
			this.flapping = 0;
		} else {
			boolean grounded = onGround || isPerched();
			this.flapSpeed += (!grounded ? 4 : -1) * 0.3F;
			if (this.flapSpeed < 0) this.flapSpeed = 0;
			if (this.flapSpeed > 1) this.flapSpeed = 1;
			if (!grounded && this.flapping < 1) this.flapping = 1;
			this.flap += this.flapping * 2.0F;
		}
	}

	private void checkForThreats() {
		List<Entity> nearbyLiving = world.getEntitiesWithinAABB(
			Entity.class,
			AABB.getTemporaryBB(x, y, z, x + 1, y + 1, z + 1).grow(8, 6, 8)
		);

		boolean spookedThisTick = false;

		for (Entity entity : nearbyLiving) {
			if (entity == this) continue;

			if (entity instanceof Player) {
				Player player = (Player) entity;
				if (!player.isSneaking()) {
					double dx = player.x - x;
					double dz = player.z - z;
					double distSqr = dx * dx + dz * dz;

					// Small hop away if too close
					if (distSqr <= 2.25) { // 1.5^2
						double angle = Math.atan2(dz, dx) + (random.nextDouble() - 0.5) * 0.5;
						xd = -Math.cos(angle) * 0.25;
						zd = -Math.sin(angle) * 0.25;
						yd = 0.25 + random.nextDouble() * 0.1;
					}

					// Full spook if sprinting
					if (player.isSprinting() && distSqr <= 64.0) {
						spookedThisTick = true;
					}
				}
			} else if (!(entity instanceof MobBird) && !isFed) {
				// Jump back from other entities
				double dx = entity.x - x;
				double dz = entity.z - z;
				double distSqr = dx * dx + dz * dz;

				if (distSqr <= 2.25) {
					double angle = Math.atan2(dz, dx) + (random.nextDouble() - 0.5) * 0.5;
					xd = -Math.cos(angle) * 0.25;
					zd = -Math.sin(angle) * 0.25;
					yd = 0.25 + random.nextDouble() * 0.1;
				}
			}
		}

		// Handle spook response
		if (spookedThisTick) {
			// If perched, break perch and take flight
			if (isPerched()) {
				setPerched(false);
				setFlying(true);
				setFlightTime(0);
				setGroundY(getGroundHeightAt(x, y, z));
			}
			// If on ground, take flight immediately
			else if (!isFlying()) {
				setFlying(true);
				setFlightTime(0);
				setGroundY(getGroundHeightAt(x, y, z));
				// Alert flock to take flight too
				alertFlockToFlee();
			}
			// If already flying, just continue flying (no special flee behavior needed)
		}
	}

	private void alertFlockToFlee() {
		List<MobBird> nearbyBirds = world.getEntitiesWithinAABB(
			MobBird.class,
			AABB.getTemporaryBB(x, y, z, x + 1, y + 1, z + 1).grow(8, 4, 8)
		);

		for (MobBird bird : nearbyBirds) {
			if (bird != this && bird.getSkinVariant() == this.getSkinVariant()) {
				if (!bird.isFlying()) {
					bird.setFlying(true);
					bird.setFlightTime(0);
					bird.setGroundY(getGroundHeightAt(bird.x, bird.y, bird.z));
				}
			}
		}
	}

	private void checkPerchConditions() {
		if (!isFlying()) {
			if (isStandingOnLeaves() || isNight()) {
				setPerched(true);
			}

			// Occasionally look for leaves to perch on
			if (!isPerched() && random.nextInt(200) == 0) {
				ServerBlockPos3D leaves = findNearbyLeavesAbove(8, 2);
				if (leaves != null) {
					// Start solo perch-seeking flight
					setSoloFlying(true);
					setFlying(true);
					setFlightTime(0);
					yd = 0.15; // Initial upward boost
					xd = 0;
					zd = 0;
				}
			}
		}

		// Check flock takeoff
		if (!isFlying() && !isSoloFlying() && !isNight()) {
			List<MobBird> nearbyBirds = world.getEntitiesWithinAABB(
				MobBird.class,
				AABB.getTemporaryBB(x, y, z, x + 1, y + 1, z + 1).grow(8, 4, 8)
			);

			for (MobBird bird : nearbyBirds) {
				if (bird != this
					&& bird.isFlying()
					&& !bird.isSoloFlying()
					&& bird.getSkinVariant() == this.getSkinVariant()) {

					// Join the flock!
					setFlying(true);
					setFlightTime(0);
					setGroundY(getGroundHeightAt(x, y, z));
					break;
				}
			}

			// Random takeoff chance
			if (!isFlying() && random.nextDouble() < 0.00001) {
				setFlying(true);
				setFlightTime(0);
				setGroundY(getGroundHeightAt(x, y, z));
			}
		}
	}

	private ServerBlockPos3D findNearbyLeavesAbove(int maxUp, int radius) {
		int bx = MathHelper.floor(x);
		int by = MathHelper.floor(y);
		int bz = MathHelper.floor(z);

		for (int dy = 1; dy <= maxUp; dy++) {
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					int id = world.getBlockId(bx + dx, by + dy, bz + dz);
					if (id != 0) {
						Block block = Blocks.blocksList[id];
						if (block != null && block.getMaterial() == Material.leaves) {
							return new ServerBlockPos3D(bx + dx, by + dy, bz + dz);
						}
					}
				}
			}
		}
		return null;
	}

	private boolean isStandingOnLeaves() {
		if (!onGround && !isPerched()) return false;

		int bx = MathHelper.floor(x);
		int by = MathHelper.floor(y - 0.1);
		int bz = MathHelper.floor(z);

		int id = world.getBlockId(bx, by, bz);
		if (id == 0) return false;

		Block block = Blocks.blocksList[id];
		return block != null && block.getMaterial() == Material.leaves;
	}

	private boolean isNight() {
		long time = world.getWorldTime() % 24000;
		return time >= 12500 && time <= 23500;
	}

	private double getGroundHeightAt(double px, double py, double pz) {
		int bx = MathHelper.floor(px);
		int bz = MathHelper.floor(pz);
		int by = MathHelper.floor(py);

		while (by > 0) {
			int blockId = world.getBlockId(bx, by, bz);
			if (blockId != 0 && Blocks.blocksList[blockId] != null) {
				Block block = Blocks.blocksList[blockId];
				if (block.isCubeShaped() && block.getMaterial() != Material.leaves) {
					return by + 1.0;
				}
				if (block.getMaterial() == Material.leaves) return by + 1.0;
			}
			by--;
		}
		return by + 1.0;
	}

	// ==================== OVERRIDES ====================

	@Override
	protected void causeFallDamage(float f) {
		// Birds don't take fall damage
	}

	@Override
	protected void jump() {
		yd = 0.42;
	}

	@Override
	public int getMaxHealth() {
		return 6;
	}

	public String getEntityTexture() {
		return "/assets/funnyfauna/textures/entity/bird/" + getSkinVariant() + ".png";
	}

	// ==================== NBT ====================

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putDouble("GroundY", groundY);
		tag.putInt("FlightTime", flightTime);
		tag.putBoolean("IsPerched", isPerched());
		tag.putInt("Skin", getSkinVariant());
		tag.putBoolean("SoloPerching", soloPerchingFlight);
		tag.putBoolean("IsFed", isFed);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		groundY = tag.getDouble("GroundY");
		flightTime = tag.getInteger("FlightTime");
		setPerched(tag.getBoolean("IsPerched"));
		setSkinVariant(tag.getInteger("Skin"));
		soloPerchingFlight = tag.getBoolean("SoloPerching");
		isFed = tag.getBoolean("IsFed");
	}

	private void playBirdSound() {
		String sound;
		switch (getSkinVariant()) {
			case 1:
				sound = "funnyfauna:mob.bird.robin";
				break;
			default:
				sound = "funnyfauna:mob.bird.chickadee";
				break;
		}

		float pitch = 0.9F + random.nextFloat() * 0.2F;
		world.playSoundAtEntity(null, this, sound, 7F, pitch);
	}
}
