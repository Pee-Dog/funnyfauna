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

		if (hasTarget && mob.onGround && leapCooldown <= 0) {
			attemptLeap();
		}

		if (hasTarget && this.path == null) {
			int groundY = MathHelper.floor(mob.y) - 1;

			if (targetY < groundY) {
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

	private void attemptLeap() {
		float sin = MathHelper.sin(-mob.yRot * 0.01745329F);
		float cos = MathHelper.cos(-mob.yRot * 0.01745329F);

		int ix = MathHelper.floor(mob.x + sin);
		int groundY = MathHelper.floor(mob.y) - 1;
		int iz = MathHelper.floor(mob.z + cos);

		boolean blockAhead = !mob.world.isAirBlock(ix, groundY, iz);
		boolean leafWallAhead = blockAhead
			&& mob.world.getBlockMaterial(ix, groundY, iz) == Material.leaves;

		if (blockAhead && !leafWallAhead) return;

		if (mob.world.getBlockMaterial(ix, groundY - 1, iz) == Material.leaves) {
			mob.setMoveForward(moveSpeed);
			return;
		}

		if (leafWallAhead) return;

		// Genuine air gap — scan for a reachable leaf platform.
		for (int dist = 2; dist <= 5; dist++) {
			int fx = MathHelper.floor(mob.x + sin * dist);
			int fz = MathHelper.floor(mob.z + cos * dist);

			// BUG FIX 1: The original code used `break` on the outer loop when any
			// intermediate column was non-air. This aborted ALL further distance
			// checks — meaning a leaf block floating at groundY between the chipmunk
			// and a distant platform killed the scan at dist=2 and dist=3,4,5 were
			// never tried. Changed to `continue` so only this specific distance is
			// skipped, not the rest.
			//
			// BUG FIX 2: The original clearPath treated leaf blocks as solid walls.
			// Leaves are passable for the chipmunk, so only truly solid non-leaf
			// blocks should block the trajectory.
			boolean clearPath = true;
			for (int d = 1; d < dist; d++) {
				int midX = MathHelper.floor(mob.x + sin * d);
				int midZ = MathHelper.floor(mob.z + cos * d);
				Material midMat = mob.world.getBlockMaterial(midX, groundY, midZ);
				if (!mob.world.isAirBlock(midX, groundY, midZ) && midMat != Material.leaves) {
					clearPath = false;
					break;
				}
			}
			if (!clearPath) continue; // was `break` — wrong, see above

			// Scan at same level, one below, one above, and two above.
			// Flat/downward checked first so the chipmunk prefers low-arc hops.
			for (int yOff : new int[]{0, -1, 1, 2}) {
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
			// Vertical range -1..+3 (nextInt(5)-1) to discover targets 2 blocks up.
			int ry = MathHelper.floor(mob.y) + random.nextInt(5) - 1;
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
	 * Physics (gravity 0.08/tick, horizontal drag ~0.91/tick):
	 *   yd=0.30, same/lower → ~7 ticks   → dragSum ≈ 5.4
	 *   yd=0.45, +1 block   → ~11 ticks  → dragSum ≈ 7.2
	 *   yd=0.62, +2 blocks  → ~15 ticks  → dragSum ≈ 8.8
	 */
	private void performLeap(double tx, double tz, int targetBlockY, int groundY) {
		double dx = tx - mob.x;
		double dz = tz - mob.z;
		float hDist = MathHelper.sqrt((float)(dx * dx + dz * dz));
		if (hDist == 0) return;

		int heightDiff = targetBlockY - groundY;

		float yd;
		float dragSum;
		if (heightDiff >= 2) {
			yd = 0.62F;
			dragSum = 8.8F;
		} else if (heightDiff > 0) {
			yd = 0.45F;
			dragSum = 7.2F;
		} else {
			yd = 0.30F;
			dragSum = 5.4F;
		}

		float hSpeed = Math.min(hDist / dragSum, 1.2F);

		mob.xd = (dx / hDist) * hSpeed;
		mob.zd = (dz / hDist) * hSpeed;
		mob.yd = yd;

		this.path = null;
		hasTarget = false;
		leapCooldown = 40;
	}

	@Override
	protected boolean isEqual(Task other) {
		return other instanceof LeafLeapTask;
	}
}
