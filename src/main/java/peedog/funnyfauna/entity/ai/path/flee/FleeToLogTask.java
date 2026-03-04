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
	 * BUG FIX (Issue 2): Was `true` as field default AND reset to `true` in
	 * onStart(). This meant ChipmunkTask always jumped into FleeToLogTask (slow,
	 * 0.5 speed) on the very first flee tick, instead of the fast FleeFromDangerTask.
	 * The chipmunk appeared to "leisurely stroll" because it was using the wrong task.
	 *
	 * Now defaults false. ChipmunkTask runs FleeFromDangerTask immediately,
	 * while logSearchTimer scans in the background. Once a log is found, this
	 * switches to true and ChipmunkTask redirects to FleeToLogTask.
	 */
	public boolean foundLog = false;

	public FleeToLogTask(MobChipmunk mob) {
		super(mob);
		this.moveSpeed = 1.5F;
		this.shouldJumpOnCollision = true;
	}

	@Override
	protected void onStart() {
		hasTarget = false;
		foundLog = false; // was `true` — see field comment above
		pathRecalcTimer = 0;
	}

	@Override
	public Task onTick() {
		if (mob.isInLeafMode()) {
			mob.setFleeTimer(0);
			mob.setFleeTarget(null);
			return null;
		}

		if (hasTarget) {
			double dx = (targetX + 0.5) - mob.x;
			double dz = (targetZ + 0.5) - mob.z;
			double distSq = dx * dx + dz * dz;

			// Manual push phase: take over from A* when close to the log.
			// Threshold is 2 blocks (distSq < 4.0). We also clear this.path so
			// the isDone() check in the recalc condition can never fire while pushing,
			// which was the cause of the "freeze" — searchForLog() was being called
			// every tick once the A* path completed, continuously re-issuing a path
			// to a solid block and fighting the manual steering.
			if (distSq < 4.0 || mob.isClimbing()) {
				this.path = null; // stop A* recalc timer from interfering
				float yaw = (float)(Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
				mob.yRot = yaw;
				mob.setMoveForward(this.moveSpeed);
				if (mob.horizontalCollision && mob.onGround && !mob.isClimbing()) {
					mob.yd = 0.4F;
				}
				return null;
			}
		}

		// Search for a log if we don't have one, or refresh every 40 ticks.
		// NOT conditioned on path.isDone() — that fired every tick when the
		// pathfinder returned immediately-done paths to solid log blocks.
		if (!hasTarget) {
			searchForLog();
		} else if (pathRecalcTimer-- <= 0) {
			pathRecalcTimer = 40;
			searchForLog();
		}

		if (!foundLog) {
			return null;
		}

		return super.onTick();
	}

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
