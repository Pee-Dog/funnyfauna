package peedog.funnyfauna.entity.ai;

import net.minecraft.core.util.helper.MathHelper;
import peedog.funnyfauna.entity.MobTaskrunner;
import peedog.funnyfauna.entity.ai.i.IHomeable;

public class ReturnHomeTask<T extends MobTaskrunner & IHomeable> extends PathTask<T> {
	private int pathAttempts = 0;
	private static final int MAX_PATH_ATTEMPTS = 3;

	public ReturnHomeTask(T mob) {
		super(mob);
		this.moveSpeed = 2.3F; // Move faster when going home
	}

	@Override
	protected void onStart() {
		this.pathAttempts = 0;
		// Immediately try to create a path when task starts
		createPathToHome();
	}

	@Override
	public Task onTick() {
		// If we don't have a home, this task is invalid.
		if (!this.mob.hasHome()) {
			return null;
		}

		// Get the ant's home coordinates
		int homeX = mob.getHomeX();
		int homeY = mob.getHomeY();
		int homeZ = mob.getHomeZ();

		// Check if we're close enough to home (within 1.5 blocks)
		double distanceSq = mob.getDistanceToHomeSq(mob.x, mob.y, mob.z);
		if (distanceSq < 2.25D) { // 1.5^2 = 2.25
			this.path = null;
			this.mob.setMoveForward(0.0F);
			this.mob.setMoveStrafing(0.0F);
			return null; // Task complete - ant is at home
		}

		// If we don't have a path or path is stuck, create one to the home block
		if (this.path == null || this.path.isDone()) {
			createPathToHome();
		}

		// If still no path after attempts, fail the task
		if (this.path == null && pathAttempts >= MAX_PATH_ATTEMPTS) {
			return null;
		}

		// Follow the path - super.onTick() returns THIS if following path, null if done
		Task result = super.onTick();

		// If path following returned null (path completed), check if we're at home
		if (result == null) {
			double newDistanceSq = mob.getDistanceToHomeSq(mob.x, mob.y, mob.z);
			if (newDistanceSq < 2.25D) {
				return null; // We're home!
			} else {
				// Path ended but we're not home, try again
				this.path = null;
				pathAttempts++;
				return this; // Keep trying
			}
		}

		return result; // Return whatever super.onTick() returned
	}

	private void createPathToHome() {
		if (pathAttempts >= MAX_PATH_ATTEMPTS) return;

		int homeX = mob.getHomeX();
		int homeY = mob.getHomeY();
		int homeZ = mob.getHomeZ();

		// Create path to home
		this.path = this.mob.world.getEntityPathToXYZ(
			this.mob,
			homeX,
			homeY,
			homeZ,
			16.0F
		);

		pathAttempts++;

		// If path creation failed, try to find a nearby walkable position
		if (this.path == null) {
			// Try positions around home
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					if (dx == 0 && dz == 0) continue;

					this.path = this.mob.world.getEntityPathToXYZ(
						this.mob,
						homeX + dx,
						homeY,
						homeZ + dz,
						12.0F
					);
					if (this.path != null) break;
				}
				if (this.path != null) break;
			}
		}
	}

	@Override
	protected boolean isEqual(Task other) {
		return other instanceof ReturnHomeTask;
	}
}
