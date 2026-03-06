package peedog.funnyfauna.entity.ai.controllers;

import net.minecraft.core.entity.animal.MobPig;
import net.minecraft.core.entity.player.Player;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.compound.IdleTask;
import peedog.funnyfauna.entity.ai.path.flight.DuckFlightTask;
import peedog.funnyfauna.entity.ai.path.flight.PigeonFlightTask;
import peedog.funnyfauna.entity.duck.MobDuck;
import peedog.funnyfauna.entity.pigeon.MobPigeon;

public class PigeonTask extends Task<MobPigeon> {

	protected final IdleTask<MobPigeon> idleTask;
	protected final PigeonFlightTask flightTask;

	public PigeonTask(MobPigeon mob) {
		super(mob);

		this.idleTask = new IdleTask<>(mob);
		this.flightTask = new PigeonFlightTask(mob);
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
