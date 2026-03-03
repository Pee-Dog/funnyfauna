package peedog.funnyfauna.entity.moth;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.item.Items;
import net.minecraft.core.util.collection.NamespaceID;
import net.minecraft.core.util.helper.DamageType;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.util.phys.AABB;
import net.minecraft.core.world.World;
import org.jetbrains.annotations.NotNull;
import org.useless.util.AnimationState;
import peedog.funnyfauna.entity.MobTaskrunner;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.controllers.MothTask;
import peedog.funnyfauna.entity.ai.interfaces.IFlyable;
import peedog.funnyfauna.item.FunnyFaunaItems;

import java.util.List;

public class MobMoth extends MobTaskrunner implements IFlyable {

	private static final int DATA_FLAGS = 16;

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

	private WallDirection wallDirection = WallDirection.NONE;
	// Vertical landing control
	private boolean canVerticalLand = false;
	// Exact position the moth should be pinned to while wall-perched.
	// Set once at perch time so we never re-derive it from floor(x/z).
	private double perchedPinX = 0.0;
	private double perchedPinZ = 0.0;

	public MobMoth(World world) {
		super(world);
		setSize(0.4F, 0.4F); // FIXED SIZE — NEVER CHANGES
		this.textureIdentifier = NamespaceID.getPermanent("funnyfauna", "moth");
		speed = 0.18f;
	}

	@Override
	public void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(DATA_FLAGS, (byte)0, Byte.class);
	}

	@Override
	public Task<MobMoth> createTask() {
		return new MothTask(this);
	}

	// =========================
	// FLAG MANAGEMENT
	// =========================

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
	}

	public boolean isPerched() {
		return (getFlags() & 2) != 0;
	}

	public void setPerched(boolean perched) {
		byte f = getFlags();
		setFlags(perched ? (byte)(f | 2) : (byte)(f & ~2));
	}

	@Override
	public boolean isLanding() {
		return false;
	}

	@Override
	public void setLanding(boolean landing) {

	}

	// =========================
	// IFLYABLE
	// =========================

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
		// DO NOTHING — moth size never changes
	}

	// Custom addition to IFlyable usage
	public boolean canVerticalLand() {
		return canVerticalLand;
	}

	public void setVerticalLanding(boolean enabled) {
		this.canVerticalLand = enabled;
	}

	public void setPerchedPin(double pinX, double pinZ) {
		this.perchedPinX = pinX;
		this.perchedPinZ = pinZ;
	}

	public boolean shouldPerchInBrightDaylight() {
		if (!world.isDaytime()) return false;
		if (y <= 128) return false;
		int light = world.getBlockLightValue(
			MathHelper.floor(x),
			MathHelper.floor(y),
			MathHelper.floor(z)
		);
		return light >= 13;
	}


	public enum WallDirection {
		NORTH,
		SOUTH,
		WEST,
		EAST,
		NONE
	}

	public WallDirection getWallDirection() {
		return wallDirection;
	}

	private void updateWallDirection() {

		if (!canVerticalLand || !isPerched()) {
			wallDirection = WallDirection.NONE;
			return;
		}

		int bx = MathHelper.floor(x);
		int by = MathHelper.floor(y);
		int bz = MathHelper.floor(z);

		// Check 4 horizontal sides
		if (world.isBlockOpaqueCube(bx, by, bz - 1)) {
			wallDirection = WallDirection.NORTH;
		}
		else if (world.isBlockOpaqueCube(bx, by, bz + 1)) {
			wallDirection = WallDirection.SOUTH;
		}
		else if (world.isBlockOpaqueCube(bx - 1, by, bz)) {
			wallDirection = WallDirection.WEST;
		}
		else if (world.isBlockOpaqueCube(bx + 1, by, bz)) {
			wallDirection = WallDirection.EAST;
		}
		else {
			wallDirection = WallDirection.NONE;
		}
	}



	// =========================
	// TICK
	// =========================

	@Override
	public void tick() {
		if (!world.isClientSide) {
			Player nearest = world.getClosestPlayerToEntity(this, 40);
			if (nearest == null) { remove(); return; }
		}
		// Suppress velocity while wall-perched so the engine doesn't drift the moth.
		// We do this BEFORE super.tick() so the physics step sees zeroed velocity.
		if (isPerched() && canVerticalLand()) {
			xd = 0;
			yd = 0;
			zd = 0;
		}

		super.tick();

		// After physics, hard-pin x/z to the stored perch position.
		// perchedPinX/Z is set once in FlutterTask from the actual wall block coords,
		// so there's no floor() drift, no wallDirection lookup, no lerp delay.
		if (isPerched() && canVerticalLand()) {
			x = perchedPinX;
			z = perchedPinZ;
		}

		flyState.animateWhen(isFlying(), tickCount);
		updateWingAnimation();
		updateWallDirection();
	}

	@Override
	public void updateAI() {
		super.updateAI();

		boolean brightDay = shouldPerchInBrightDaylight();

		// -------------------------------------------------
		// SPOOK when sprinting player nearby (ALWAYS allowed)
		// -------------------------------------------------

		List<Player> players = world.getEntitiesWithinAABB(
			Player.class,
			AABB.getTemporaryBB(x, y, z, x + 1, y + 1, z + 1).grow(4, 3, 4)
		);

		for (Player player : players) {
			if (player.isSprinting() && !isFlying()) {
				setPerched(false);
				setFlying(true);
				setFlightTime(0);
				setGroundY(getGroundHeightAt(x, y, z));
				yd = 0.25;
				return; // IMPORTANT: exit after spook
			}
		}

		// -------------------------------------------------
		// If bright daylight and NOT flying, stay perched
		// -------------------------------------------------

		if (brightDay && !isFlying()) {
			setPerched(true);
			xd = 0;
			yd = 0;
			zd = 0;
			return;
		}

		// -------------------------------------------------
		// Take off when near light-emitting block (NOT during bright daylight)
		// -------------------------------------------------

		if (!isFlying() && !brightDay) {
			int bx = MathHelper.floor(x);
			int by = MathHelper.floor(y);
			int bz = MathHelper.floor(z);
			boolean nearLight = false;

			outer:
			for (int dx = -4; dx <= 4; dx++) {
				for (int dy = -2; dy <= 2; dy++) {
					for (int dz = -4; dz <= 4; dz++) {
						int id = world.getBlockId(bx + dx, by + dy, bz + dz);
						if (id > 0 && Blocks.lightEmission[id] >= 12) {
							nearLight = true;
							break outer;
						}
					}
				}
			}

			if (nearLight) {
				setPerched(false);
				setFlying(true);
				setFlightTime(0);
				setGroundY(getGroundHeightAt(x, y, z));
				yd = 0.25;
			}
		}
	}



	private void updateWingAnimation() {
		this.oFlap = this.flap;
		this.oFlapSpeed = this.flapSpeed;

		boolean grounded = onGround && !isFlying();

		this.flapSpeed += (!grounded ? 4 : -1) * 0.3F;
		if (this.flapSpeed < 0) this.flapSpeed = 0;
		if (this.flapSpeed > 1) this.flapSpeed = 1;

		if (!grounded && this.flapping < 1) this.flapping = 1;
		this.flap += this.flapping * 2.0F;
	}

	private double getGroundHeightAt(double px, double py, double pz) {
		int bx = MathHelper.floor(px);
		int bz = MathHelper.floor(pz);
		int by = MathHelper.floor(py);

		while (by > 0) {
			if (world.isBlockOpaqueCube(bx, by, bz)) {
				return by + 1.0;
			}
			by--;
		}
		return by + 1.0;
	}

	@Override
	protected void causeFallDamage(float f) {}

	@Override
	public int getMaxHealth() {
		return 4;
	}

	@Override
	public boolean isInWall() {
		return false;
	}

	@Override
	protected void jump() {
	}

	@Override
	public boolean collidesWith(Entity entity) {
		if (!onGround) {
			return false;
		}
		return true;
	}

	@Override
	public void playerTouch(Player player) {
		if (!onGround) return;
		if (player.y <= y + 0.05) return;

		double dx = player.x - x;
		double dz = player.z - z;
		if (dx * dx + dz * dz > 0.5) return;

		world.spawnParticle("bug_squash", x, y + 0.01, z, 0.0, 0.2, 0.0, 0);
		dropItem(FunnyFaunaItems.DUST_CHITIN.id, 1);
		if (!world.isClientSide) remove();
	}

	@Override
	public boolean interact(@NotNull Player player) {
		ItemStack held = player.inventory.getCurrentItem();
		if (held != null && held.itemID == Items.JAR.id) {
			if (!player.world.isClientSide) {
				int slot = player.inventory.getCurrentItemIndex();
				player.inventory.removeItem(slot, 1);

				ItemStack mothJar = new ItemStack(FunnyFaunaItems.JAR_MOTH);
				player.inventory.insertItem(mothJar, true);
				if (mothJar.stackSize > 0) player.dropPlayerItemWithRandomChoice(mothJar, false);

				remove();
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean hurt(Entity attacker, int damage, DamageType type) {
		if (!world.isClientSide) remove();
		if (type == DamageType.COMBAT) {
			world.playSoundAtEntity(null, this, "funnyfauna:mob.interaction.slap", 0.6F, 0.8F + random.nextFloat() * 0.2F);
		}
		dropItem(FunnyFaunaItems.DUST_CHITIN.id, 1);
		return true;
	}

	@Override
	protected boolean makeStepSound() { return false; }

	@Override
	public String getHurtSound() {
		return "";
	}

	public String getEntityTexture() {
		if (isFlying()) {
			return "/assets/funnyfauna/textures/entity/moth/1.png";
		}
		return "/assets/funnyfauna/textures/entity/moth/0.png";
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putDouble("GroundY", groundY);
		tag.putInt("FlightTime", flightTime);
		tag.putBoolean("Flying", isFlying());
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		groundY = tag.getDouble("GroundY");
		flightTime = tag.getInteger("FlightTime");
		setFlying(tag.getBoolean("Flying"));
	}
}
