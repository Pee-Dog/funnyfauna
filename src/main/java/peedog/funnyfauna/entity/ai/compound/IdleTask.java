package peedog.funnyfauna.entity.ai.compound;

import net.minecraft.core.entity.Entity;
import peedog.funnyfauna.entity.MobTaskrunner;
import peedog.funnyfauna.entity.ai.LookAroundTask;
import peedog.funnyfauna.entity.ai.LookAtPlayersTask;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.path.WanderHopTask;
import peedog.funnyfauna.entity.ai.path.WanderTask;

public class IdleTask<T extends MobTaskrunner> extends Task<T> {

	public boolean shouldWander = true;
	public boolean shouldSwim = true;
	// New Toggle
	public boolean shouldHop = false;

	public final WanderTask<T> wanderTask;
	// New Task Instance
	public final WanderHopTask<T> wanderHopTask;
	public final LookAtPlayersTask<T> lookAtPlayersTask;
	public final LookAroundTask<T> lookAroundTask;

	public IdleTask(T mob) {
		super(mob);
		IdleTask<T> self = this;

		// Standard walking wander
		this.wanderTask = new WanderTask<T>(mob) {
			@Override
			public Entity lookTarget() {
				return self.lookAtPlayersTask.currentTarget;
			}
		};

		// New hopping wander
		this.wanderHopTask = new WanderHopTask<T>(mob) {
			@Override
			public Entity lookTarget() {
				return self.lookAtPlayersTask.currentTarget;
			}
		};

		this.lookAtPlayersTask = new LookAtPlayersTask<>(mob);
		this.lookAroundTask = new LookAroundTask<>(mob);
	}

	public boolean isLooking() {
		return lookAroundTask.randomYawVelocity > 0 || lookAtPlayersTask.currentTarget != null;
	}

	@Override
	protected void onStart() {
	}

	@Override
	public Task onTick() {
		if (this.shouldSwim && (this.mob.isInWater() || this.mob.isInLava())) {
			this.mob.startJumping();
		}

		// Determine which wander task to check
		WanderTask<T> activeWander = shouldHop ? wanderHopTask : wanderTask;

		if (
			this.shouldWander && (
				activeWander.path != null ||
					!this.isLooking() && this.random.nextInt(shouldHop ? 40 : 80) == 0 // Hop more often than walk
			)
		) {
			return activeWander;
		}
		else if (
			this.lookAtPlayersTask.currentTarget != null ||
				this.random.nextFloat() < 0.03F
		) {
			return this.lookAtPlayersTask;
		}
		else {
			return this.lookAroundTask;
		}
	}

	@Override
	protected void onStop(Task interruptTask) {

	}

	@Override
	protected boolean isEqual(Task other) {
		return other instanceof IdleTask;
	}
}
