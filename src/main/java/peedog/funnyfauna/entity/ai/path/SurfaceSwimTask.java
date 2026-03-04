package peedog.funnyfauna.entity.ai.path;

import net.minecraft.core.block.material.Material;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.util.phys.Vec3;
import peedog.funnyfauna.entity.MobTaskrunner;
import peedog.funnyfauna.entity.ai.LookAroundTask;
import peedog.funnyfauna.entity.ai.LookAtPlayersTask;
import peedog.funnyfauna.entity.ai.Task;

public class SurfaceSwimTask<T extends MobTaskrunner> extends PathTask<T> {

	public final LookAtPlayersTask<T> lookAtPlayersTask;
	public final LookAroundTask<T> lookAroundTask;

	// Tracks which idle sub-task is currently active so we can tick it
	private Task<T> activeSubTask = null;

	public SurfaceSwimTask(T mob) {
		super(mob);
		this.moveSpeed = 0.5F;
		this.shouldJumpOnCollision = false;

		this.lookAtPlayersTask = new LookAtPlayersTask<>(mob);
		this.lookAroundTask = new LookAroundTask<>(mob);
	}

	@Override
	protected void onStart() {
		activeSubTask = null;
	}

	@Override
	public Entity lookTarget() {
		// PathTask uses this so the duck faces its look-target while swimming
		return lookAtPlayersTask.currentTarget;
	}

	// ── helpers ────────────────────────────────────────────────────────────────

	private boolean isLooking() {
		return lookAroundTask.randomYawVelocity > 0 || lookAtPlayersTask.currentTarget != null;
	}

	/**
	 * Switch to a new sub-task, calling onStop on the old one first.
	 */
	private void switchSubTask(Task<T> next) {
		if (activeSubTask != null && activeSubTask != next) {
			activeSubTask.stop(next);
		}
		if (activeSubTask != next) {
			activeSubTask = next;
			if (activeSubTask != null) activeSubTask.tick();
		}
	}

	// ── main tick ──────────────────────────────────────────────────────────────

	@Override
	public Task onTick() {
		if (!mob.isInWater()) {
			// Left the water — stop everything cleanly
			switchSubTask(null);
			return null;
		}

		// ── SURFACE LOCK & BUOYANCY ──────────────────────────────────────────
		int bx = MathHelper.floor(mob.x);
		int by = MathHelper.floor(mob.y + 0.1);
		int bz = MathHelper.floor(mob.z);

		if (mob.world.getBlockMaterial(bx, by, bz) == Material.water) {
			int surfaceY = by;
			while (mob.world.getBlockMaterial(bx, surfaceY + 1, bz) == Material.water) {
				surfaceY++;
			}
			double targetY = (double) surfaceY + 0.85;
			double diff = targetY - mob.y;

			if (Math.abs(diff) < 0.1) {
				mob.yd *= 0.5; // damping deadzone — kills bobbing oscillation
			} else {
				double buoyantForce = diff * 0.1;
				mob.yd = Math.max(-0.15, Math.min(0.15, buoyantForce));
			}
		}

		// ── ROTATION (no sideways tilt) ──────────────────────────────────────
		if (Math.abs(mob.xd) > 0.01 || Math.abs(mob.zd) > 0.01) {
			float targetAngle = (float) (Math.atan2(mob.zd, mob.xd) * 180.0 / Math.PI) - 90.0F;
			float angleDiff = targetAngle - mob.yRot;
			while (angleDiff <= -180.0F) angleDiff += 360.0F;
			while (angleDiff > 180.0F) angleDiff -= 360.0F;
			mob.yRot += angleDiff * 0.2F;
		}

		// ── DECIDE: WANDER OR IDLE ───────────────────────────────────────────
		boolean wantsToWander = this.path != null
			|| (!isLooking() && this.random.nextInt(80) == 0);

		if (wantsToWander) {
			// Refresh path occasionally
			if (this.path == null || this.random.nextInt(40) == 0) {
				Vec3 target = findWaterTarget();
				if (target != null) {
					this.path = mob.world.getEntityPathToXYZ(mob,
						MathHelper.floor(target.x),
						MathHelper.floor(target.y),
						MathHelper.floor(target.z), 16.0F);
				}
			}
			// Stop any idle sub-task so PathTask can steer cleanly
			switchSubTask(null);
			super.onTick(); // handles movement along path

		} else if (lookAtPlayersTask.currentTarget != null
			|| this.random.nextFloat() < 0.03F) {
			// Look at a nearby player
			switchSubTask(lookAtPlayersTask);
			lookAtPlayersTask.tick();

		} else {
			// Gently look around while bobbing
			switchSubTask(lookAroundTask);
			lookAroundTask.tick();
		}

		return null; // stay in SurfaceSwimTask; IdleTask re-enters us next tick
	}

	@Override
	protected void onStop(Task interruptTask) {
		super.onStop(interruptTask);
		switchSubTask(null);
	}

	@Override
	protected boolean isEqual(Task other) {
		return other instanceof SurfaceSwimTask;
	}

	// ── path target helper ────────────────────────────────────────────────────

	private Vec3 findWaterTarget() {
		for (int i = 0; i < 10; i++) {
			int tx = MathHelper.floor(mob.x + random.nextInt(11) - 5);
			int ty = MathHelper.floor(mob.y + random.nextInt(3) - 1);
			int tz = MathHelper.floor(mob.z + random.nextInt(11) - 5);
			if (mob.world.getBlockId(tx, ty, tz) != 0
				&& mob.world.getBlockMaterial(tx, ty, tz) == Material.water) {
				return Vec3.getTempVec3(tx, ty, tz);
			}
		}
		return null;
	}
}
