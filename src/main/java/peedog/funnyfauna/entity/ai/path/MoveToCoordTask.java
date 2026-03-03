package peedog.funnyfauna.entity.ai.path;

import peedog.funnyfauna.entity.MobTaskrunner;
import peedog.funnyfauna.entity.ai.Task;

public class MoveToCoordTask<T extends MobTaskrunner> extends PathTask<T> {
	private int tx, ty, tz;

	public MoveToCoordTask(T mob) {
		super(mob);
	}

	@Override
	protected void onStart() {

	}

	public void setTarget(int x, int y, int z) {
		if (tx != x || ty != y || tz != z) {
			this.tx = x;
			this.ty = y;
			this.tz = z;
			this.path = null; // Reset path to force recalculation for the new spot
		}
	}

	@Override
	public Task onTick() {
		// If we don't have a path to the spot yet, calculate it
		if (this.path == null && mob.world != null) {
			this.path = mob.world.getEntityPathToXYZ(mob, tx, ty, tz, 16.0F);
		}

		// Use the path-following logic from the parent PathTask
		return super.onTick();
	}

	@Override
	protected boolean isEqual(Task other) {
		return other instanceof MoveToCoordTask;
	}
}
