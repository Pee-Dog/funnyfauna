package peedog.funnyfauna.entity.ai.path;

import peedog.funnyfauna.entity.MobTaskrunner;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.interfaces.IHomeable;

public class ReturnHomeTask<T extends MobTaskrunner & IHomeable> extends PathTask<T> {

	private int pathRecalcTimer = 0;

	public ReturnHomeTask(T mob) {
		super(mob);
		this.moveSpeed = 1.0F;
	}

	@Override
	protected void onStart() {
		pathRecalcTimer = 0;
	}

	@Override
	public Task onTick() {
		if (!this.mob.hasHome()) return null;

		// Recalculate path every 2 seconds, or immediately if the current one is exhausted
		if (pathRecalcTimer-- <= 0 || this.path == null || this.path.isDone()) {
			pathRecalcTimer = 40;
			this.path = this.mob.world.getEntityPathToXYZ(
				this.mob,
				mob.getHomeX(), mob.getHomeY(), mob.getHomeZ(),
				20.0F
			);
		}

		// Delegate movement to PathTask's A* follower
		super.onTick();

		return null;
	}

	@Override
	protected boolean isEqual(Task other) {
		return other instanceof ReturnHomeTask;
	}
}
