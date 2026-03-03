package peedog.funnyfauna.entity.ai.path.flee;

import net.minecraft.core.block.material.Material;
import net.minecraft.core.util.helper.MathHelper;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.path.PathTask;
import peedog.funnyfauna.entity.chipmunk.MobChipmunk;

public class FleeToLogTask extends PathTask<MobChipmunk> {
	private int targetX, targetY, targetZ;
	private boolean hasTarget = false;
	private int pathRecalcTimer = 0;

	/**
	 * Set to false if the last log search came up empty.
	 * ChipmunkTask reads this to decide whether to fall back to FleeFromDangerTask.
	 * Defaults to true so the task is given at least one chance to search before
	 * the fallback triggers.
	 */
	public boolean foundLog = true;

	public FleeToLogTask(MobChipmunk mob) {
		super(mob);
		this.moveSpeed = 0.5F;
		// Jump over terrain obstacles encountered while pathfinding to the log.
		// Without this the chipmunk plants its face into any 1-block bump along
		// the way and never reaches the tree.
		this.shouldJumpOnCollision = true;
	}

	@Override
	protected void onStart() {
		hasTarget = false;
		foundLog = true;
		pathRecalcTimer = 0;
	}

	@Override
	public Task onTick() {
		if (mob.isInLeafMode()) {
			mob.setFleeTimer(0);
			mob.setFleeTarget(null);
			return null;
		}

		// Search for a log target if we don't have one yet.
		// Only re-search on a timer or when the path is explicitly finished — NOT
		// just because path is null.  When the pathfinder returns null (mob already
		// adjacent to the log, no navigable path to a solid block), we must fall
		// through to the distSq manual-push below instead of recalculating every
		// tick in an infinite loop that never reaches the close-range steering.
		if (!hasTarget) {
			searchForLog();
		} else if (pathRecalcTimer-- <= 0 || (this.path != null && this.path.isDone())) {
			pathRecalcTimer = 40;
			searchForLog();
		}

		if (!foundLog) {
			return null;
		}

		if (hasTarget) {
			double dx = (targetX + 0.5) - mob.x;
			double dz = (targetZ + 0.5) - mob.z;
			double distSq = dx * dx + dz * dz;

			// Switch to manual push at 1.5 blocks (distSq < 2.25).
			// The old threshold of 2.0 blocks was too generous and caused the mob
			// to start "climbing" while still too far from the log face to actually
			// trigger horizontalCollision. At 1.5 blocks the mob is close enough
			// that the push will bring it flush against the face within 1-2 ticks.
			if (distSq < 2.25 || mob.isClimbing()) {
				float yaw = (float)(Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
				mob.yRot = yaw;
				mob.setMoveForward(this.moveSpeed);
				// Jump if there is a non-log obstacle directly ahead while in the
				// close-range push phase. The isClimbing() branch never needs this
				// (climbing is already working), but when just pressing toward the log
				// face any 1-block bump will otherwise stall the approach completely.
				if (mob.horizontalCollision && mob.onGround && !mob.isClimbing()) {
					mob.yd = 0.4F;
				}
				return null;
			}
		}

		return super.onTick();
	}

	/**
	 * Scans nearby blocks for the closest log and paths toward it.
	 * Public so ChipmunkTask can call this periodically even while
	 * FleeFromDangerTask is running, allowing a timely switch back to
	 * this task once a log comes within range.
	 */
	public void searchForLog() {
		int r = 16;
		double bestDist = Double.MAX_VALUE;
		int bx = 0, by = 0, bz = 0;
		boolean found = false;

		for (int dx = -r; dx <= r; dx++) {
			for (int dy = -4; dy <= 4; dy++) {
				for (int dz = -r; dz <= r; dz++) {
					int x = MathHelper.floor(mob.x) + dx;
					int y = MathHelper.floor(mob.y) + dy;
					int z = MathHelper.floor(mob.z) + dz;

					if (mob.world.getBlockMaterial(x, y, z) == Material.wood) {
						double d = mob.distanceToSqr(x, y, z);
						if (d < bestDist) {
							bestDist = d;
							bx = x; by = y; bz = z;
							found = true;
						}
					}
				}
			}
		}

		foundLog = found;

		if (found) {
			targetX = bx; targetY = by; targetZ = bz;
			hasTarget = true;
			this.path = mob.world.getEntityPathToXYZ(mob, bx, by, bz, 20.0F);
		}
	}

	@Override
	protected boolean isEqual(Task other) {
		return other instanceof FleeToLogTask;
	}
}
