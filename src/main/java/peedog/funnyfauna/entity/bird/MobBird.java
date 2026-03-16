package peedog.funnyfauna.entity.bird;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.block.material.Material;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.util.collection.NamespaceID;
import net.minecraft.core.util.helper.DamageType;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.util.phys.AABB;
import net.minecraft.core.world.World;
import peedog.funnyfauna.block.ServerBlockPos3D;
import peedog.funnyfauna.entity.MobFlying;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.controllers.BirdTask;
import peedog.funnyfauna.entity.ai.interfaces.IFlockable;
import peedog.funnyfauna.entity.ai.interfaces.IHomeable;

import java.util.List;

public class MobBird extends MobFlying implements IFlockable {

	private static final int DATA_SKIN_VARIANT = 17;
	private static final int FLAG_PERCHED = 2;

	// Perching / flock state
	private boolean soloPerchingFlight = false;
	public double homeX = 0.0;
	public double homeZ = 0.0;

	// Fed state
	public boolean isFed = false;

	// HoppingF
	private int hopCooldown = 0;
	private ServerBlockPos3D leapTarget = null;

	// Sound
	private int ambientSoundTimer;

	public MobBird(World world) {
		super(world);
		this.textureIdentifier = NamespaceID.getPermanent("funnyfauna", "bird");
		speed = 0.15f;
		ambientSoundTimer = random.nextInt(200);
	}

	@Override
	public void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(DATA_SKIN_VARIANT, random.nextInt(4), Integer.class);
	}

	@Override
	public Task<MobBird> createTask() {
		return new BirdTask(this);
	}

	// ================= PERCHING =================

	protected byte getFlags() {
		return this.entityData.getByte(16);
	}

	protected void setFlags(byte flags) {
		this.entityData.set(16, flags);
	}

	@Override
	public boolean isPerched() {
		return (getFlags() & FLAG_PERCHED) != 0;
	}

	@Override
	public void setPerched(boolean perched) {
		byte f = getFlags();
		setFlags(perched ? (byte)(f | FLAG_PERCHED) : (byte)(f & ~FLAG_PERCHED));
	}

	public ServerBlockPos3D getLeapTarget() {
		return leapTarget;
	}

	public void clearLeapTarget() {
		leapTarget = null;
	}

	private void leapToward(ServerBlockPos3D target) {
		setSoloFlying(true);
		setFlying(true);
		setFlightTime(0);
		leapTarget = target;
	}

	// ================= SOLO FLIGHT =================

	@Override
	public boolean isSoloFlying() {
		return soloPerchingFlight;
	}

	@Override
	public void setSoloFlying(boolean solo) {
		this.soloPerchingFlight = solo;
	}

	// Override to adjust size logic for solo perch flights
	@Override
	public void setFlying(boolean flying) {
		super.setFlying(flying);

		if (flying && isSoloFlying()) {
			setSize(0.5F, 0.5F);
			setPos(x, y, z);
		}
	}

	// ================= SKIN VARIANT =================

	public void setSkinVariant(int variant) {
		variant = Math.max(0, Math.min(3, variant));
		this.entityData.set(DATA_SKIN_VARIANT, variant);
	}

	public int getSkinVariant() {
		return this.entityData.getInt(DATA_SKIN_VARIANT);
	}

	// ================= FLOCKING =================

	@Override
	public boolean canFlockWith(Entity other) {
		if (!(other instanceof MobBird)) return false;
		return ((MobBird) other).getSkinVariant() == this.getSkinVariant();
	}

	protected void alertFlockToFlee() {
		List<MobBird> nearbyBirds = world.getEntitiesWithinAABB(
			MobBird.class,
			AABB.getTemporaryBB(x, y, z, x + 1, y + 1, z + 1).grow(8, 4, 8)
		);

		for (MobBird bird : nearbyBirds) {
			if (bird != this && bird.getSkinVariant() == this.getSkinVariant()) {
				if (!bird.isFlying()) {
					bird.setFlying(true);
					bird.setFlightTime(0);
					bird.setGroundY(bird.getGroundHeightAt(bird.x, bird.y, bird.z));
				}
			}
		}
	}

	// ================= THREAT RESPONSE =================

	protected void spookBird() {
		if (isPerched()) {
			setPerched(false);
		}

		if (!isFlying()) {
			setFlying(true);
			setFlightTime(0);
			setGroundY(getGroundHeightAt(x, y, z));
			alertFlockToFlee();
		}
	}

	@Override
	public boolean hurt(Entity attacker, int damage, DamageType damageType) {
		boolean wasHurt = super.hurt(attacker, damage, damageType);
		if (wasHurt && !world.isClientSide) {
			spookBird();
		}
		return wasHurt;
	}

	// ================= AI =================

	@Override
	public void updateAI() {
		super.updateAI();

		// 1. Friction when grounded
		if (!isFlying() && onGround) {
			if (Math.abs(yd) < 0.05) {
				xd *= 0.3;
				zd *= 0.3;
			}
		}

		// 2. Home Position
		if ((onGround || isPerched()) && !isFlying()) {
			homeX = x;
			homeZ = z;
		}

		// 3. Hopping and Perching logic
		if (!isFlying() && !isPerched()) {
			handleHopping();
		}

		if (this.onGround && (this.getMoveForward() != 0)) {
			this.yd = 0.2;
		}

		checkPerchConditions(); // Re-add this call
		checkForThreats();
	}

	private void checkPerchConditions() {
		if (!isFlying()) {
			if (isStandingOnLeaves() || isNight()) setPerched(true);
			if (isPerched() && !isNight() && !isStandingOnLeaves()) setPerched(false);
		} else if (!isSoloFlying() && !isNight()) {
			// Flock joining logic
			List<MobBird> nearby = world.getEntitiesWithinAABB(MobBird.class, bb.grow(8, 4, 8));
			for (MobBird bird : nearby) {
				if (bird != this && bird.isFlying() && !bird.isSoloFlying() && bird.getSkinVariant() == this.getSkinVariant()) {
					setFlying(true);
					setFlightTime(0);
					setGroundY(getGroundHeightAt(x, y, z));
					break;
				}
			}
		}
	}

	private void handleHopping() {
		if (!onGround) return;

		hopCooldown--;

		if (isStandingOnLeaves()) {
			if (hopCooldown <= 0) {
				ServerBlockPos3D newLeaf = findNearbyDifferentLeaf();
				if (newLeaf != null) {
					leapToward(newLeaf);
					hopCooldown = 40 + random.nextInt(40);
				}
			}
			return;
		}

		if (hopCooldown <= 0) {
			ServerBlockPos3D target = findLeapTarget();
			if (target != null) {
				leapToward(target);
				hopCooldown = 20 + random.nextInt(20);
			}
		}
	}

	protected void checkForThreats() {
		List<Entity> nearby = world.getEntitiesWithinAABB(
			Entity.class,
			AABB.getTemporaryBB(x, y, z, x + 1, y + 1, z + 1).grow(8, 6, 8)
		);

		for (Entity entity : nearby) {
			if (entity == this) continue;

			if (entity instanceof Player) {
				Player player = (Player) entity;
				if (!player.isSneaking() && player.isSprinting()) {
					spookBird();
				}
			}
		}
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

	private ServerBlockPos3D findNearbyDifferentLeaf() {
		int bx = MathHelper.floor(x);
		int by = MathHelper.floor(y - 0.1);
		int bz = MathHelper.floor(z);

		for (int dx = -3; dx <= 3; dx++) {
			for (int dz = -3; dz <= 3; dz++) {
				for (int dy = -1; dy <= 3; dy++) {
					int cx = bx + dx; int cy = by + dy; int cz = bz + dz;
					if (cx == bx && cy == by && cz == bz) continue;
					int id = world.getBlockId(cx, cy, cz);
					if (id > 0 && Blocks.blocksList[id] != null && Blocks.blocksList[id].getMaterial() == Material.leaves) {
						if (world.isAirBlock(cx, cy + 1, cz)) return new ServerBlockPos3D(cx, cy, cz);
					}
				}
			}
		}
		return null;
	}

	private ServerBlockPos3D findLeapTarget() {
		int bx = MathHelper.floor(x);
		int by = MathHelper.floor(y);
		int bz = MathHelper.floor(z);
		ServerBlockPos3D leavesTarget = null;
		ServerBlockPos3D groundTarget = null;

		for (int dx = -6; dx <= 6; dx++) {
			for (int dz = -6; dz <= 6; dz++) {
				if (dx == 0 && dz == 0) continue;
				for (int dy = 0; dy <= 8; dy++) {
					int cx = bx + dx; int cy = by + dy; int cz = bz + dz;
					int id = world.getBlockId(cx, cy, cz);
					if (id != 0 && Blocks.blocksList[id] != null) {
						Block block = Blocks.blocksList[id];
						if (world.isAirBlock(cx, cy + 1, cz)) {
							if (block.getMaterial() == Material.leaves) {
								if (leavesTarget == null || random.nextBoolean()) leavesTarget = new ServerBlockPos3D(cx, cy, cz);
							} else if (block.isCubeShaped()) {
								if (groundTarget == null || random.nextBoolean()) groundTarget = new ServerBlockPos3D(cx, cy, cz);
							}
						}
					}
				}
			}
		}
		return leavesTarget != null ? leavesTarget : groundTarget;
	}

	@Override
	public boolean collidesWithBlock(Block<?> block, int metadata) {
		// If doing solo perch flight, ignore leaf collisions
		if (this.isSoloFlying() && block.getMaterial() == Material.leaves) {
			return false;
		}

		return super.collidesWithBlock(block, metadata);
	}
	// ================= SOUND =================

	@Override
	public void tick() {
		super.tick();

		if (!world.isClientSide) {
			if (ambientSoundTimer-- <= 0) {
				if (!isNight()) {
					playBirdSound();
				}
				ambientSoundTimer = 300 + random.nextInt(200);
			}
		}
	}

	private void playBirdSound() {
		String sound;
		switch (getSkinVariant()) {
			case 1:
				sound = "funnyfauna:mob.bird.robin";
				break;
			case 2:
				sound = "funnyfauna:mob.bird.bluejay";
				break;
			case 3:
				sound = "funnyfauna:mob.bird.cardinal";
				break;
			default:
				sound = "funnyfauna:mob.bird.chickadee";
				break;
		}

		float pitch = 0.9F + random.nextFloat() * 0.2F;
		world.playSoundAtEntity(null, this, sound, 2.0F, pitch);
	}

	// ================= TEXTURE =================

	public String getEntityTexture() {
		return "/assets/funnyfauna/textures/entity/bird/" + getSkinVariant() + ".png";
	}

	// ================= NBT =================

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putBoolean("IsPerched", isPerched());
		tag.putInt("Skin", getSkinVariant());
		tag.putBoolean("SoloPerching", soloPerchingFlight);
		tag.putBoolean("IsFed", isFed);
		tag.putInt("HopCooldown", hopCooldown);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		setPerched(tag.getBoolean("IsPerched"));
		setSkinVariant(tag.getInteger("Skin"));
		soloPerchingFlight = tag.getBoolean("SoloPerching");
		isFed = tag.getBoolean("IsFed");
		hopCooldown = tag.getInteger("HopCooldown");
	}

	@Override
	public int getMaxHealth() {
		return 6;
	}

	@Override
	protected void jump() {
		yd = 0.42;
	}

	@Override
	public String getHurtSound() {
		return "";
	}

	@Override
	protected boolean makeStepSound() { return false; }
}
