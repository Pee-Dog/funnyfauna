package peedog.funnyfauna.entity.chipmunk;

import net.minecraft.core.block.Block;
import net.minecraft.core.block.material.Material;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.Mob;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.util.collection.NamespaceID;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.World;
import peedog.funnyfauna.entity.MobTaskrunner;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.controllers.ChipmunkTask;
import peedog.funnyfauna.entity.ai.interfaces.IFleeable;

import java.util.List;

public class MobChipmunk extends MobTaskrunner implements IFleeable {
	private static final int DATA_CLIMBING = 16;
	private Entity fleeTarget;
	private int fleeTimer;

	private boolean leafModeActive = false;
	private int leafModeCooldown = 0;
	private static final int LEAF_MODE_COOLDOWN_TICKS = 100;

	/**
	 * Prevents re-attaching to the log after slipping off mid-climb.
	 * Gates BOTH isAgainstLog() AND the horizontalCollision-based climbing trigger
	 * in tick(). Previously only isAgainstLog() was gated, which meant the mob
	 * could fall, hit the log face in the air (horizontalCollision=true, !onGround),
	 * immediately re-enter climbing, slip off again, and oscillate rapidly.
	 */
	private int climbCooldown = 0;
	private static final int CLIMB_COOLDOWN_TICKS = 30;

	public MobChipmunk(World world) {
		super(world);
		this.setSize(0.5F, 0.5F);
		this.moveSpeed = 0.3F;
		this.textureIdentifier = NamespaceID.getPermanent("funnyfauna", "chipmunk");
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(DATA_CLIMBING, (byte)0, Byte.class);
	}

	@Override
	public Task<MobChipmunk> createTask() {
		return new ChipmunkTask(this);
	}

	public boolean isClimbing() {
		return (this.entityData.getByte(DATA_CLIMBING) & 1) != 0;
	}

	public void setClimbing(boolean climbing) {
		byte b = this.entityData.getByte(DATA_CLIMBING);
		if (climbing) {
			this.entityData.set(DATA_CLIMBING, (byte)(b | 1));
		} else {
			this.entityData.set(DATA_CLIMBING, (byte)(b & -2));
		}
	}

	@Override
	public void tick() {
		super.tick();

		int ix = MathHelper.floor(this.x);
		int iy = MathHelper.floor(this.y);
		int iz = MathHelper.floor(this.z);
		boolean bodyInLeaves = world.getBlockMaterial(ix, iy, iz) == Material.leaves
			|| world.getBlockMaterial(ix, MathHelper.floor(this.y + 0.4F), iz) == Material.leaves;

		if (bodyInLeaves) {
			this.yd = 0.3;
		}

		if (!this.world.isClientSide) {
			boolean wasClimbing = isClimbing();

			// BUG FIX (Issue 3): The original code was:
			//   (this.horizontalCollision && !this.onGround) || isAgainstLog()
			// isAgainstLog() already checks climbCooldown, but horizontalCollision
			// did NOT. So after slipping off and setting climbCooldown, the mob
			// would fall, hit the log face in the air (horizontalCollision=true,
			// onGround=false), climbing would restart despite the cooldown, it would
			// slip again immediately, and the rapid oscillation loop was complete.
			// Fix: gate the horizontalCollision branch on climbCooldown too.
			boolean nowClimbing = (climbCooldown <= 0 && this.horizontalCollision && !this.onGround)
				|| isAgainstLog();
			setClimbing(nowClimbing);

			if (wasClimbing && !nowClimbing && !this.onGround && this.yd > 0) {
				if (isNearLeaves()) {
					// Case A — keep yd, let momentum carry mob into canopy.
				} else {
					// Case B — slipped off the side.
					this.yd = 0.0;
					climbCooldown = CLIMB_COOLDOWN_TICKS;
				}
			}

			if (leafModeCooldown > 0) leafModeCooldown--;
			if (climbCooldown > 0) climbCooldown--;
		}
	}

	@Override
	public void moveEntityWithHeading(float moveStrafing, float moveForward) {
		if (isClimbing()) {
			// BUG FIX (Issue 3, secondary): The original unconditional `yd = 0.2F`
			// overwrote the 0.3 set by bodyInLeaves in tick() on the very next call.
			// Use a floor instead so the canopy-pop push is never reduced.
			if (this.yd < 0.2F) this.yd = 0.2F;

			this.xd *= 0.5;
			this.zd *= 0.5;
			super.moveEntityWithHeading(moveStrafing, moveForward);
		} else {
			super.moveEntityWithHeading(moveStrafing, moveForward);
		}
	}

	private boolean isAgainstLog() {
		if (climbCooldown > 0) return false;
		int ix = MathHelper.floor(this.x);
		int iy = MathHelper.floor(this.y);
		int iz = MathHelper.floor(this.z);

		if (world.getBlockMaterial(ix + 1, iy, iz) == Material.wood && (ix + 1.0) - this.x <= 0.35) return true;
		if (world.getBlockMaterial(ix - 1, iy, iz) == Material.wood && this.x - ix <= 0.35) return true;
		if (world.getBlockMaterial(ix, iy, iz + 1) == Material.wood && (iz + 1.0) - this.z <= 0.35) return true;
		if (world.getBlockMaterial(ix, iy, iz - 1) == Material.wood && this.z - iz <= 0.35) return true;

		return false;
	}

	/**
	 * Returns true if any leaf block is within ±1 horizontal, dy -1..+1 vertical.
	 *
	 * BUG FIX (Issue 3): The original only checked dy=0 and dy=1. When the mob
	 * just crested the top leaf layer while climbing, it was momentarily 1 block
	 * ABOVE the leaf (iy = leafY+1), making dy=0 and dy=1 both miss the leaf below.
	 * isNearLeaves() returned false → Case B → yd killed → rapid fall → oscillation.
	 * Extending to dy=-1 catches this "just exited top of canopy" position.
	 */
	private boolean isNearLeaves() {
		int ix = MathHelper.floor(this.x);
		int iy = MathHelper.floor(this.y);
		int iz = MathHelper.floor(this.z);
		for (int dy = -1; dy <= 1; dy++) {
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					if (world.getBlockMaterial(ix + dx, iy + dy, iz + dz) == Material.leaves)
						return true;
				}
			}
		}
		return false;
	}

	@Override public Entity getFleeTarget() { return this.fleeTarget; }
	@Override public void setFleeTarget(Entity entity) { this.fleeTarget = entity; }
	@Override public int getFleeTimer() { return this.fleeTimer; }
	@Override public void setFleeTimer(int i) { this.fleeTimer = i; }

	public boolean isInLeafMode() {
		int ix = MathHelper.floor(this.x);
		int iy = MathHelper.floor(this.y - 0.1);
		int iz = MathHelper.floor(this.z);
		Material underfoot = world.getBlockMaterial(ix, iy, iz);
		boolean onLeaves = onGround && underfoot == Material.leaves;
		boolean onWood   = onGround && underfoot == Material.wood;

		boolean bodyInLeaves = world.getBlockMaterial(ix, MathHelper.floor(this.y), iz) == Material.leaves
			|| world.getBlockMaterial(ix, MathHelper.floor(this.y + 0.4F), iz) == Material.leaves;

		if (onLeaves || bodyInLeaves || onWood) {
			if (!leafModeActive) {
				this.fleeTimer = 0;
				this.fleeTarget = null;
			}
			leafModeActive = true;
		} else if (onGround) {
			if (leafModeActive) {
				leafModeCooldown = LEAF_MODE_COOLDOWN_TICKS;
			}
			leafModeActive = false;
		}
		return leafModeActive;
	}

	@Override
	public void updateAI() {
		super.updateAI();
		if (world.isClientSide) return;

		if (this.onGround && (this.getMoveForward() != 0)) {
			this.yd = 0.2;
		}

		if (!isInLeafMode() && leafModeCooldown <= 0 && this.getFleeTimer() <= 0) {
			// BUG FIX (Issue 2): Two problems in the original:
			// 1. `random.nextInt(2) == 0` — chipmunk missed 50% of threat checks.
			//    The fleeTimer guard already throttles this properly; no random gate needed.
			// 2. `instanceof Mob` — Player does not extend Mob in classic MC.
			//    Players were completely invisible to the flee detection.
			List<Entity> nearby = this.world.getEntitiesWithinAABBExcludingEntity(this, this.bb.expand(8.0, 4.0, 8.0));
			Entity closest = null;
			double closestDist = Double.MAX_VALUE;

			for (Entity e : nearby) {
				if (e == this) continue;
				if (!(e instanceof Mob) && !(e instanceof Player)) continue;
				double dist = this.distanceTo(e);
				if (dist < closestDist) {
					closestDist = dist;
					closest = e;
				}
			}

			if (closest != null) {
				this.setFleeTarget(closest);
				this.setFleeTimer(100);
			}
		}
	}

	@Override
	public boolean collidesWithBlock(Block<?> block, int metadata) {
		if (block.getMaterial() == Material.leaves) {
			if (isAgainstLog()) return false;

			int ix = MathHelper.floor(this.x);
			int iz = MathHelper.floor(this.z);
			if (world.getBlockMaterial(ix, MathHelper.floor(this.y),        iz) == Material.leaves
				|| world.getBlockMaterial(ix, MathHelper.floor(this.y + 0.4F), iz) == Material.leaves) {
				return false;
			}
		}
		return super.collidesWithBlock(block, metadata);
	}

	@Override
	public String getLivingSound() { return "funnyfauna:mob.chipmunk.idle"; }
	@Override
	protected String getHurtSound() { return "funnyfauna:mob.chipmunk.hurt"; }
	@Override
	protected String getDeathSound() { return "funnyfauna:mob.chipmunk.death"; }

	@Override
	protected void causeFallDamage(float f) {
		// Chipmunk don't take fall damage
	}
}
