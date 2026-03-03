package peedog.funnyfauna.entity.ai.controllers;

import net.minecraft.core.entity.EntityItem;
import net.minecraft.core.entity.player.Player;
import peedog.funnyfauna.entity.ai.path.SeekSeedTask;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.compound.IdleTask;
import peedog.funnyfauna.entity.ai.path.flight.FlightTask;
import peedog.funnyfauna.entity.bird.MobBird;

public class BirdTask extends Task<MobBird> {
	protected final SeekSeedTask<MobBird> seekSeedTask;
	protected FlightTask<MobBird> flightTask;
	protected final IdleTask<MobBird> idleTask;

	public BirdTask(MobBird mob) {
		super(mob);
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

		// Priority 1: Perching
		if (mob.isPerched()) {
			idleTask.shouldWander = false;
			return idleTask;
		}

		// Priority 2: Flying (birds flee by flying, so flight handles all aerial behavior)
		if (mob.isFlying()) {
			return flightTask;
		}

		// Priority 3: Seeking seeds
		seekSeedTask.onTick();
		if (seekSeedTask.hasTarget()) {
			return seekSeedTask;
		}

		// Priority 4: Idle behavior (wandering, looking around)
		idleTask.shouldWander = true;
		return idleTask;
	}

	private boolean isNight() {
		long time = mob.world.getWorldTime() % 24000;
		return time >= 12500 && time <= 23500;
	}

	@Override
	protected void onStop(Task interruptTask) {}

	@Override
	protected boolean isEqual(Task other) {
		return other instanceof BirdTask;
	}
}
