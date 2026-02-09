package peedog.funnyfauna.entity.ai.controllers;

import net.minecraft.core.entity.EntityItem;
import net.minecraft.core.entity.player.Player;
import peedog.funnyfauna.entity.ai.FleeFromDangerTask;
import peedog.funnyfauna.entity.ai.SeekSeedTask;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.compound.IdleTask;
import peedog.funnyfauna.entity.ai.flight.FlightTask;
import peedog.funnyfauna.entity.ai.flight.PerchTask;
import peedog.funnyfauna.entity.bird.MobBird;

public class BirdTask extends Task<MobBird> {
	private final PerchTask<MobBird> perchTask;
	private final FleeFromDangerTask<MobBird> fleeTask;
	private final SeekSeedTask<MobBird> seekSeedTask;
	private final FlightTask<MobBird> flightTask;
	private final IdleTask<MobBird> idleTask;

	public BirdTask(MobBird mob) {
		super(mob);
		this.perchTask = new PerchTask<>(mob);
		this.fleeTask = new FleeFromDangerTask<>(mob);
		// Custom SeekSeedTask that marks bird as fed
		this.seekSeedTask = new SeekSeedTask<MobBird>(mob) {
			@Override
			protected void onItemPickedUp(EntityItem item) {
				mob.isFed = true;
			}
		};
		this.flightTask = new FlightTask<>(mob);
		this.idleTask = new IdleTask<>(mob);
	}

	@Override
	protected void onStart() {}

	@Override
	protected Task onTick() {
		// If riding a player (shouldn't happen for birds, but just in case)
		if (mob.vehicle instanceof Player) {
			return null;
		}

		// Priority 1: Perching (frozen state)
		if (mob.isPerched()) {
			return perchTask;
		}

		// Priority 2: Fleeing from danger
		if (mob.getFleeTimer() > 0) {
			return fleeTask;
		}

		// Priority 3: Flying
		if (mob.isFlying()) {
			return flightTask;
		}

		// Priority 4: Seeking seeds
		seekSeedTask.onTick();
		if (seekSeedTask.hasTarget()) {
			return seekSeedTask;
		}

		// Priority 5: Idle behavior (wandering, looking around)
		idleTask.shouldWander = true;
		return idleTask;
	}

	@Override
	protected void onStop(Task interruptTask) {}

	@Override
	protected boolean isEqual(Task other) {
		return other instanceof BirdTask;
	}
}
