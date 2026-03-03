package peedog.funnyfauna.entity.ai.path;

import net.minecraft.core.block.material.Material;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.util.phys.Vec3;
import peedog.funnyfauna.entity.MobTaskrunner;
import peedog.funnyfauna.entity.ai.Task;

public class SurfaceSwimTask<T extends MobTaskrunner> extends PathTask<T> {

	public SurfaceSwimTask(T mob) {
		super(mob);
		this.moveSpeed = 0.5F;
		this.shouldJumpOnCollision = false;
	}

	@Override
	protected void onStart() {

	}

	@Override
	public Task onTick() {
		if (!mob.isInWater()) return null;

		// ================== SURFACE LOCK & BUOYANCY ==================
		int bx = MathHelper.floor(mob.x);
		int by = MathHelper.floor(mob.y + 0.1);
		int bz = MathHelper.floor(mob.z);

		if (mob.world.getBlockMaterial(bx, by, bz) == Material.water) {
			int surfaceY = by;
			while (mob.world.getBlockMaterial(bx, surfaceY + 1, bz) == Material.water) {
				surfaceY++;
			}

			double targetY  = (double)surfaceY + 0.85;
			double diff = targetY - mob.y;

			// THE FIX: The Damping Deadzone
			if (Math.abs(diff) < 0.1) {
				// We are practically at the surface. Kill the momentum to stop the bobbing oscillation.
				mob.yd *= 0.5;
			} else {
				// We are deep underwater or too high. Float up or sink smoothly.
				double buoyantForce = diff * 0.1;
				mob.yd = Math.max(-0.15, Math.min(0.15, buoyantForce));
			}
		}

		// ================== ROTATION FIX (No Sideways) ==================
		if (Math.abs(mob.xd) > 0.01 || Math.abs(mob.zd) > 0.01) {
			float targetAngle = (float)(Math.atan2(mob.zd, mob.xd) * 180.0 / Math.PI) - 90.0F;
			float angleDiff = targetAngle - mob.yRot;

			// Manual wrapDegrees
			while (angleDiff <= -180.0F) angleDiff += 360.0F;
			while (angleDiff > 180.0F) angleDiff -= 360.0F;

			mob.yRot += angleDiff * 0.2F;

		}
		// ================== PATHING ==================
		if (this.path == null || this.random.nextInt(40) == 0) {
			Vec3 target = findWaterTarget();
			if (target != null) {
				this.path = mob.world.getEntityPathToXYZ(mob,
					MathHelper.floor(target.x),
					MathHelper.floor(target.y),
					MathHelper.floor(target.z), 16.0F);
			}
		}

		super.onTick();

		// FIX 3: RETURN NULL TO PREVENT STACK OVERFLOW
		// Returning null tells the runner "I'm done for this tick."
		// IdleTask will trigger this again on the next tick automatically.
		return null;
	}

	@Override
	protected boolean isEqual(Task other) {
		// FIX: Must return true for other SurfaceSwimTasks to prevent task flickering
		return other instanceof SurfaceSwimTask;
	}

	private Vec3 findWaterTarget() {
		for (int i = 0; i < 10; i++) {
			int tx = MathHelper.floor(mob.x + random.nextInt(11) - 5);
			int ty = MathHelper.floor(mob.y + random.nextInt(3) - 1);
			int tz = MathHelper.floor(mob.z + random.nextInt(11) - 5);

			int id = mob.world.getBlockId(tx, ty, tz);
			if (id != 0 && mob.world.getBlockMaterial(tx, ty, tz) == Material.water) {
				return Vec3.getTempVec3(tx, ty, tz);
			}
		}
		return null;
	}
}
