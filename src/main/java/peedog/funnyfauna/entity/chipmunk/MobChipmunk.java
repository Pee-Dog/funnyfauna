package peedog.funnyfauna.entity.chipmunk;

import net.minecraft.core.block.Block;
import net.minecraft.core.block.material.Material;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.Mob;
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

	/**
	 * Persists leaf-mode state across airborne ticks (e.g. mid-leap between canopies,
	 * or passing upward through a leaf canopy from a log climb).
	 * Set true the moment leaves are detected at or around the chipmunk's body;
	 * cleared only when it touches down on something that is NOT leaves.
	 */
	private boolean leafModeActive = false;

	/**
	 * Grace-period countdown after leaving leaf mode.
	 * While > 0, flee-target acquisition in updateAI() is suppressed, so a chipmunk
	 * that just descended from a tree won't immediately panic again.
	 * Set to LEAF_MODE_COOLDOWN_TICKS whenever leafModeActive transitions to false.
	 */
	private int leafModeCooldown = 0;
	private static final int LEAF_MODE_COOLDOWN_TICKS = 100;

	/**
	 * Prevents the chipmunk from immediately re-attaching to a log after
	 * slipping off mid-climb. Without this, the mob's inertia carries it back
	 * against the log face within a tick or two, climbing re-starts, it slips
	 * again, and the cycle repeats as a rapid oscillation.
	 *
	 * Set to CLIMB_COOLDOWN_TICKS whenever climbing ends WITHOUT reaching the
	 * canopy (i.e. isNearLeaves() is false at the moment climbing stops).
	 * isAgainstLog() returns false while the cooldown is active.
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

		// Feature: Canopy Popping and Particles
		int ix = MathHelper.floor(this.x);
		int iy = MathHelper.floor(this.y);
		int iz = MathHelper.floor(this.z);
		boolean bodyInLeaves = world.getBlockMaterial(ix, iy, iz) == Material.leaves
			|| world.getBlockMaterial(ix, MathHelper.floor(this.y + 0.4F), iz) == Material.leaves;

		// Push upward whenever the body is overlapping a leaf block, regardless of
		// climbing state. This covers three cases:
		//   • Transitioning from log climb into the canopy (was: && isClimbing()).
		//   • Still inside leaves after climbing has stopped but before onGround is set.
		//   • Already in LeafLeap mode but clipped into a leaf block (e.g. on a log
		//     top surrounded by leaf walls) — keeps jumping until the body is clear.
		if (bodyInLeaves) {
			this.yd = 0.3;
		}

		if (!this.world.isClientSide) {
			boolean wasClimbing = isClimbing();
			// isAgainstLog() alone can initiate climbing even while still on the ground.
			// The old condition required !onGround, which meant the flee task would push
			// the chipmunk against the log face (onGround == true) indefinitely without
			// ever triggering climbing. Once isAgainstLog() fires and yd is set to 0.2
			// in moveEntityWithHeading, the mob lifts off within a tick and
			// horizontalCollision && !onGround takes over naturally.
			boolean nowClimbing = (this.horizontalCollision && !this.onGround) || isAgainstLog();
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
			// Automatically climb upwards, regardless of what the AI task is doing.
			this.yd = 0.2F;

			// Dampen horizontal drift to prevent sliding sideways, but STILL
			// pass moveForward below so it actively presses its body into the log.
			this.xd *= 0.5;
			this.zd *= 0.5;
			super.moveEntityWithHeading(moveStrafing, moveForward);
		} else {
			super.moveEntityWithHeading(moveStrafing, moveForward);
		}
	}

	/**
	 * Returns true if there is a log block within striking distance in any of the
	 * four cardinal directions AND the mob is physically close to that face.
	 *
	 * Cardinal-only (not diagonals): this keeps the mob flush against a flat face
	 * rather than triggering at a corner where it would be too far to climb cleanly.
	 *
	 * Distance threshold 0.6: the mob must be within 0.6 blocks of the log face.
	 * Previously the block-adjacency check alone allowed the mob to be anywhere in
	 * the adjacent block (up to ~1.4 blocks away), which caused the "climbing too
	 * far from the tree" visual and meant the mob might miss the narrow climb path.
	 */
	private boolean isAgainstLog() {
		if (climbCooldown > 0) return false;
		int ix = MathHelper.floor(this.x);
		int iy = MathHelper.floor(this.y);
		int iz = MathHelper.floor(this.z);

		// Distance threshold reduced from 0.6 to 0.35 to keep the model flush against the bark.
		if (world.getBlockMaterial(ix + 1, iy, iz) == Material.wood && (ix + 1.0) - this.x <= 0.35) return true;
		if (world.getBlockMaterial(ix - 1, iy, iz) == Material.wood && this.x - ix <= 0.35) return true;
		if (world.getBlockMaterial(ix, iy, iz + 1) == Material.wood && (iz + 1.0) - this.z <= 0.35) return true;
		if (world.getBlockMaterial(ix, iy, iz - 1) == Material.wood && this.z - iz <= 0.35) return true;

		return false;
	}

	/**
	 * Returns true if any leaf block exists within ±1 block horizontally or 0-1
	 * blocks above the mob's current position.
	 *
	 * Used to distinguish "reached the canopy top" from "slipped off the log side"
	 * when climbing stops mid-air. If leaves are nearby the upward velocity should
	 * be preserved so the mob pops into the canopy; if not it should be killed to
	 * prevent floating.
	 */
	private boolean isNearLeaves() {
		int ix = MathHelper.floor(this.x);
		int iy = MathHelper.floor(this.y);
		int iz = MathHelper.floor(this.z);
		for (int dy = 0; dy <= 1; dy++) {
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

	/**
	 * True when the chipmunk is on, inside, or recently transitioning through
	 * leaves, OR when it is standing on top of a log.
	 *
	 * Cases that set leafModeActive = true:
	 *  1. Standing on a leaf block (normal leaf-canopy mode).
	 *  2. Body overlapping a leaf block while airborne — catches the upward
	 *     transition when the mob pops through the canopy from a log climb.
	 *  3. Standing on top of a log block — prevents the flee task from restarting
	 *     after the chipmunk reaches the top of the log it just climbed, which was
	 *     causing it to immediately pathfind back into the log it was already on.
	 *
	 * When leafModeActive first becomes true the active flee state is cleared
	 * immediately so both ChipmunkTask and FleeToLogTask switch on the same tick.
	 *
	 * leafModeActive is only cleared when the mob touches down on something that
	 * is neither leaves nor wood.  While airborne the flag is unchanged, keeping
	 * LeafLeapTask active mid-leap.
	 */
	public boolean isInLeafMode() {
		int ix = MathHelper.floor(this.x);
		int iy = MathHelper.floor(this.y - 0.1);
		int iz = MathHelper.floor(this.z);
		Material underfoot = world.getBlockMaterial(ix, iy, iz);
		boolean onLeaves = onGround && underfoot == Material.leaves;
		// Treat standing on a log top the same as leaf mode: the chipmunk has
		// reached its climb destination and should switch to LeafLeapTask rather
		// than continuing to pathfind toward the same log.
		boolean onWood   = onGround && underfoot == Material.wood;

		// Detect the climbing-into-canopy transition: body is passing through leaf
		// blocks from below while still airborne (onGround not yet true).
		boolean bodyInLeaves = world.getBlockMaterial(ix, MathHelper.floor(this.y), iz) == Material.leaves
			|| world.getBlockMaterial(ix, MathHelper.floor(this.y + 0.4F), iz) == Material.leaves;

		if (onLeaves || bodyInLeaves || onWood) {
			if (!leafModeActive) {
				// Just entered leaf/tree mode — cancel any active flee immediately.
				this.fleeTimer = 0;
				this.fleeTarget = null;
			}
			leafModeActive = true;
		} else if (onGround) {
			// Landed on something other than leaves or wood — leaf mode is over.
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
		// isInLeafMode() covers the active case; leafModeCooldown covers the
		// "recently on leaves" grace period after descending.
		if (!isInLeafMode() && leafModeCooldown <= 0)
			if (this.random.nextInt(2) == 0 && this.getFleeTimer() <= 0) {
				List<Entity> nearby = this.world.getEntitiesWithinAABBExcludingEntity(this, this.bb.expand(8.0, 4.0, 8.0));
				Entity closest = null;
				double closestDist = Double.MAX_VALUE;

				for (Entity e : nearby) {
					if (!(e instanceof Mob) || e == this) continue;
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
			// Pass through while climbing a log so the chipmunk can pop into the canopy.
			if (isAgainstLog()) return false;

			// Also pass through when the body is already overlapping a leaf block.
			// Without this, the collision system resolves the chipmunk OUT of the leaf
			// during super.tick() before the bodyInLeaves push in tick() can fire, so
			// the push always acts on air and the chipmunk never bounces clear.
			// Checking both foot-level and mid-body covers the full 0.5-block tall hitbox.
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
	public String getLivingSound() { return null; }
	@Override
	protected String getHurtSound() { return "funnyfauna:mob.chipmunk.hurt"; }
	@Override
	protected String getDeathSound() { return "funnyfauna:mob.chipmunk.death"; }
}
