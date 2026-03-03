package peedog.funnyfauna.entity.ai.path;

import net.minecraft.core.block.material.Material;
import net.minecraft.core.util.helper.MathHelper;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.chipmunk.MobChipmunk;

public class LeafLeapTask extends PathTask<MobChipmunk> {
	private int leapCooldown = 0;
	private int waitTimer = 0;
	private int targetX, targetY, targetZ;
	private boolean hasTarget = false;

	public LeafLeapTask(MobChipmunk mob) {
		super(mob);
		this.moveSpeed = 0.25F;
		// Leaf canopies have leaf walls everywhere. Jumping on every horizontal
		// collision would cause the chipmunk to bounce in place. All gap-crossing
		// on leaves is handled explicitly by attemptLeap() instead.
		this.shouldJumpOnCollision = false;
	}

	@Override
	protected void onStart() {
		hasTarget = false;
		waitTimer = 0;
	}

	@Override
	public Task onTick() {
		if (leapCooldown > 0) leapCooldown--;

		if (!mob.isInLeafMode()) {
			return null;
		}

		if (!hasTarget || (this.path == null && mob.onGround)) {
			if (waitTimer-- <= 0) {
				findLeafTarget();
				waitTimer = 20 + random.nextInt(40);
			}
		}

		// Suppress leaps only when we know there is a genuine non-leaf wall ahead
		// (that check is now inside attemptLeap itself).  The old !horizontalCollision
		// guard was too broad: leaf walls in the canopy always cause horizontalCollision,
		// which prevented the task from ever attempting ledge drops through leaf faces.
		if (hasTarget && mob.onGround && leapCooldown <= 0) {
			attemptLeap();
		}

		if (hasTarget && this.path == null) {
			int groundY = MathHelper.floor(mob.y) - 1;

			if (targetY < groundY) {
				// Target on a lower leaf layer — seek an exit ledge to drop off.
				steerToStepDownEdge(groundY);
			} else {
				double dx = (targetX + 0.5) - mob.x;
				double dz = (targetZ + 0.5) - mob.z;
				float yaw = (float)(Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
				mob.yRot = yaw;
				if (dx * dx + dz * dz < 1.0) hasTarget = false;
			}

			mob.setMoveForward(this.moveSpeed);
			return null;
		}

		return super.onTick();
	}

	private void steerToStepDownEdge(int groundY) {
		int mx = MathHelper.floor(mob.x);
		int mz = MathHelper.floor(mob.z);

		int[][] dirs = { {1, 0}, {-1, 0}, {0, 1}, {0, -1} };
		for (int[] d : dirs) {
			int nx = mx + d[0];
			int nz = mz + d[1];
			if (mob.world.isAirBlock(nx, groundY, nz)
				&& mob.world.getBlockMaterial(nx, groundY - 1, nz) == Material.leaves) {
				mob.yRot = (float)(Math.atan2(d[1], d[0]) * 180.0 / Math.PI) - 90.0F;
				return;
			}
		}

		double dx = (targetX + 0.5) - mob.x;
		double dz = (targetZ + 0.5) - mob.z;
		mob.yRot = (float)(Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
	}

	/**
	 * Checks ahead for a gap and either steps down (1-block drop) or leaps.
	 *
	 * Gap check uses groundY = floor(mob.y) - 1, the actual block being stood on.
	 * floor(mob.y) is the air block at foot height; using it was the old bug that
	 * triggered leaps even when there was no gap.
	 *
	 * Leaf walls: a leaf block at groundY in the forward direction is NOT treated
	 * as a solid stop — instead the code checks whether there is a droppable leaf
	 * one layer below (groundY - 1) and, if so, walks forward to let the mob fall.
	 * The bodyInLeaves push in MobChipmunk.tick() handles any remaining vertical
	 * clearance once the chipmunk steps through.
	 *
	 * For genuine air gaps, the scan validates that all intermediate columns are also
	 * air so the chipmunk doesn't leap toward a leaf that is only accessible through
	 * a wall (which caused "jumping into air for no reason").
	 * Target position is snapped to block centre so velocity maths are accurate.
	 */
	private void attemptLeap() {
		float sin = MathHelper.sin(-mob.yRot * 0.01745329F);
		float cos = MathHelper.cos(-mob.yRot * 0.01745329F);

		int ix = MathHelper.floor(mob.x + sin);
		int groundY = MathHelper.floor(mob.y) - 1;
		int iz = MathHelper.floor(mob.z + cos);

		boolean blockAhead = !mob.world.isAirBlock(ix, groundY, iz);
		boolean leafWallAhead = blockAhead
			&& mob.world.getBlockMaterial(ix, groundY, iz) == Material.leaves;

		// Hard non-leaf wall ahead (stone, dirt, etc.) — nothing to do.
		if (blockAhead && !leafWallAhead) return;

		// 1-block ledge drop: there is a leaf block one layer below the gap/wall.
		// Walk forward; gravity handles the drop.
		if (mob.world.getBlockMaterial(ix, groundY - 1, iz) == Material.leaves) {
			mob.setMoveForward(moveSpeed);
			return;
		}

		// Leaf wall with no droppable leaf below — it is just the canopy edge. Stop.
		if (leafWallAhead) return;

		// Genuine air gap — scan for a reachable leaf platform to leap onto.
		for (int dist = 2; dist <= 5; dist++) {
			int fx = MathHelper.floor(mob.x + sin * dist);
			int fz = MathHelper.floor(mob.z + cos * dist);

			// Validate that every intermediate column (dist 1 .. dist-1) is also
			// air at groundY. This prevents leaping toward leaves that are only
			// reachable through a wall, which showed up as "jumping into air."
			boolean clearPath = true;
			for (int d = 1; d < dist; d++) {
				int midX = MathHelper.floor(mob.x + sin * d);
				int midZ = MathHelper.floor(mob.z + cos * d);
				if (!mob.world.isAirBlock(midX, groundY, midZ)) {
					clearPath = false;
					break;
				}
			}
			if (!clearPath) break;

			for (int yOff : new int[]{0, 1, -1}) {
				int checkY = groundY + yOff;
				if (mob.world.getBlockMaterial(fx, checkY, fz) == Material.leaves
					&& mob.world.isAirBlock(fx, checkY + 1, fz)) {
					performLeap(fx + 0.5, fz + 0.5, checkY, groundY);
					return;
				}
			}
		}
	}

	private void findLeafTarget() {
		int r = 8;
		for (int i = 0; i < 10; i++) {
			int rx = MathHelper.floor(mob.x) + random.nextInt(r * 2) - r;
			int ry = MathHelper.floor(mob.y) + random.nextInt(3) - 1;
			int rz = MathHelper.floor(mob.z) + random.nextInt(r * 2) - r;

			if (mob.world.getBlockMaterial(rx, ry, rz) == Material.leaves
				&& mob.world.isAirBlock(rx, ry + 1, rz)) {
				targetX = rx; targetY = ry; targetZ = rz;
				hasTarget = true;
				this.path = mob.world.getEntityPathToXYZ(mob, rx, ry + 1, rz, 10.0F);
				return;
			}
		}
	}

	/**
	 * Launch the chipmunk toward the block centre (tx, tz) to land on a leaf at
	 * targetBlockY. groundY is floor(mob.y) - 1 (the block currently stood on).
	 *
	 * Horizontal velocity is physics-derived rather than a fixed constant.
	 *
	 * With Minecraft's gravity (0.08/tick downward) and y-drag (0.98/tick), the
	 * simulated air times are:
	 *   yd=0.30, same/lower target  →  ~7 ticks airborne
	 *   yd=0.45, target 1 block up  →  ~11 ticks airborne
	 *
	 * With horizontal drag ~0.91/tick, total horizontal distance = xd0 * dragSum
	 * where dragSum = sum(0.91^t, t=0..n-1) = (1 - 0.91^n) / 0.09:
	 *   7 ticks  → dragSum ≈ 5.4
	 *   11 ticks → dragSum ≈ 7.2
	 *
	 * So xd0 = hDist / dragSum, capped at 1.2 to prevent overshooting on short hops.
	 */
	private void performLeap(double tx, double tz, int targetBlockY, int groundY) {
		double dx = tx - mob.x;
		double dz = tz - mob.z;
		float hDist = MathHelper.sqrt((float)(dx * dx + dz * dz));
		if (hDist == 0) return;

		int heightDiff = targetBlockY - groundY;

		float yd;
		float dragSum;
		if (heightDiff > 0) {
			// Target is one block above — need more arc.
			yd = 0.45F;
			dragSum = 7.2F; // air time ~11 ticks
		} else {
			// Target at same level or below — shallow hop.
			yd = 0.30F;
			dragSum = 5.4F; // air time ~7 ticks
		}

		float hSpeed = Math.min(hDist / dragSum, 1.2F);

		mob.xd = (dx / hDist) * hSpeed;
		mob.zd = (dz / hDist) * hSpeed;
		mob.yd = yd;

		// Clear the pre-leap A* path and target immediately.  If we leave the path
		// alive, super.onTick() resumes following its now-stale waypoints as soon as
		// the chipmunk lands — those waypoints are behind or beside the landing spot,
		// which is what caused the "walking backward then jumping" loop post-leap.
		// Clearing hasTarget lets the wait-timer fire a fresh findLeafTarget() call
		// after the cooldown, so the chipmunk picks a new destination from its actual
		// landing position rather than chasing ghost waypoints.
		this.path = null;
		hasTarget = false;
		leapCooldown = 40;
	}

	@Override
	protected boolean isEqual(Task other) {
		return other instanceof LeafLeapTask;
	}
}
