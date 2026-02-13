package peedog.funnyfauna.entity.ai.path;

import peedog.funnyfauna.entity.MobTaskrunner;
import peedog.funnyfauna.entity.ai.Task;

public class WanderHopTask<T extends MobTaskrunner> extends WanderTask<T> {

	public WanderHopTask(T mob) {
		super(mob);
	}

	@Override
	public Task onTick() {
		// Run standard pathfinding logic
		Task result = super.onTick();

		// If we are currently moving along a path
		if (this.path != null && !this.path.isDone()) {
			// Aerbunny/Rabbit style movement: Jump if on ground
			if (this.mob.onGround) {
				this.mob.yd = 0.3;
			}
		}
		return result;
	}
}
