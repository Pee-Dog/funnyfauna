package peedog.funnyfauna.entity.ai.path;

import net.minecraft.core.util.helper.MathHelper;
import peedog.funnyfauna.entity.MobTaskrunner;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.interfaces.IHomeable;

public class ReturnHomeTask<T extends MobTaskrunner & IHomeable> extends PathTask<T> {
	private int pathRecalcTimer = 0;
	private int pathAttempts = 0;
	private static final int MAX_PATH_ATTEMPTS = 3;

	public ReturnHomeTask(T mob) {
		super(mob);
		this.moveSpeed = 1.0F;
	}

	@Override
	protected void onStart() {
		this.pathAttempts = 0;
		this.pathRecalcTimer = 0;
		this.path = null;
	}

	@Override
	public Task onTick() {
		// If we don't have a home, stop this task
		if (!this.mob.hasHome()) {
			return null;
		}

		// Check if we're at home (within 2 blocks)
		double distanceSq = mob.getDistanceToHomeSq(mob.x, mob.y, mob.z);
		if (distanceSq < 4.0D) { // 2^2 = 4
			// We're home! Clear path and stop moving
			this.path = null;
			this.mob.setMoveForward(0.0F);
			this.mob.setMoveStrafing(0.0F);
			return null; // Task complete
		}

		// Recalculate path periodically or if we don't have one
		if (pathRecalcTimer-- <= 0 || this.path == null || this.path.isDone()) {
			pathRecalcTimer = 20; // Recalculate every 20 ticks (1 second)

			if (pathAttempts < MAX_PATH_ATTEMPTS) {
				createPathToHome();
				if (this.path == null) {
					pathAttempts++;
				} else {
					pathAttempts = 0; // Reset attempts on successful path creation
				}
			}
			// If we've failed too many times, the path will stay null and movement will handle it
		}

		// Follow the path if we have one
		if (this.path != null && !this.path.isDone()) {
			super.onTick(); // This handles the actual movement
		} else {
			// No path available, try direct movement
			moveDirectlyToHome();
		}

		// Return null to indicate we're still working (no child task)
		// DO NOT return 'this' - that would make this task its own child!
		return null;
	}

	private void createPathToHome() {
		int homeX = mob.getHomeX();
		int homeY = mob.getHomeY();
		int homeZ = mob.getHomeZ();

		// Try to create a path to home
		this.path = this.mob.world.getEntityPathToXYZ(
			this.mob,
			homeX,
			homeY,
			homeZ,
			32.0F
		);

		// If path creation failed, try a position 1 block above
		if (this.path == null) {
			this.path = this.mob.world.getEntityPathToXYZ(
				this.mob,
				homeX,
				homeY + 1,
				homeZ,
				32.0F
			);
		}
	}

	private void moveDirectlyToHome() {
		int homeX = mob.getHomeX();
		int homeY = mob.getHomeY();
		int homeZ = mob.getHomeZ();

		// Calculate direction to home
		double dx = (homeX + 0.5) - mob.x;
		double dz = (homeZ + 0.5) - mob.z;

		// Calculate yaw angle to home
		float targetYaw = (float)(Math.atan2(dz, dx) * 180.0F / Math.PI) - 90.0F;

		// Normalize the yaw difference
		float yawDiff = targetYaw - mob.yRot;
		while (yawDiff < -180.0F) yawDiff += 360.0F;
		while (yawDiff >= 180.0F) yawDiff -= 360.0F;

		// Turn towards home
		if (Math.abs(yawDiff) > 10.0F) {
			// Large turn - slow down to turn
			mob.yRot += MathHelper.clamp(yawDiff, -30.0F, 30.0F);
			mob.setMoveForward(this.moveSpeed * 0.5F);
		} else {
			// Small adjustment - move at full speed
			mob.yRot += yawDiff * 0.2F;
			mob.setMoveForward(this.moveSpeed);
		}

		// Jump if hitting a wall
		if (mob.horizontalCollision && mob.onGround) {
			mob.startJumping();
		}
	}

	@Override
	protected void onStop(Task interruptTask) {
		super.onStop(interruptTask);
		this.path = null;
		this.pathAttempts = 0;
		this.pathRecalcTimer = 0;
	}

	@Override
	protected boolean isEqual(Task other) {
		return other instanceof ReturnHomeTask;
	}
}
