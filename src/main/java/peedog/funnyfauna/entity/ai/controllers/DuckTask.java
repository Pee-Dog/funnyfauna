package peedog.funnyfauna.entity.ai.controllers;

import net.minecraft.core.entity.player.Player;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.compound.IdleTask;
import peedog.funnyfauna.entity.ai.path.flight.DuckFlightTask;
import peedog.funnyfauna.entity.duck.MobDuck;

public class DuckTask extends Task<MobDuck> {

	protected final IdleTask<MobDuck> idleTask;
	protected final DuckFlightTask flightTask;

	public DuckTask(MobDuck mob) {
		super(mob);

		this.idleTask = new IdleTask<>(mob);
		this.flightTask = new DuckFlightTask(mob);

		// Ducks walk and surface swim
		this.idleTask.shouldSwim = false;
		this.idleTask.shouldSurfaceSwim = true;
		this.idleTask.shouldHop = false;
	}

	@Override
	protected void onStart() {}

	@Override
	protected Task<?> onTick() {

		// If riding player (safety check)
		if (mob.vehicle instanceof Player) {
			return null;
		}

		// Priority 1: Flying
		if (mob.isFlying()) {
			return flightTask;
		}

		this.idleTask.shouldWander = true;
		return idleTask;
	}

	@Override
	protected void onStop(Task interruptTask) {}

	@Override
	protected boolean isEqual(Task other) {
		return other instanceof DuckTask;
	}
}
