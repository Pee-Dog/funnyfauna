package peedog.funnyfauna.entity.ai.controllers;

import net.minecraft.core.entity.EntityItem;
import net.minecraft.core.entity.player.Player;
import peedog.funnyfauna.entity.ai.path.SeekSeedTask;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.compound.IdleTask;
import peedog.funnyfauna.entity.ai.path.flight.ScorvidFlightTask;
import peedog.funnyfauna.entity.scorvid.MobScorvid;

public class ScorvidTask extends Task<MobScorvid> {
	protected ScorvidFlightTask<MobScorvid> flightTask;
	protected final IdleTask<MobScorvid> idleTask;

	public ScorvidTask(MobScorvid mob) {
		super(mob);
		this.flightTask = new ScorvidFlightTask<>(mob);
		this.idleTask = new IdleTask<>(mob);
	}

	@Override
	protected void onStart() {}

	@Override
	protected Task onTick() {
		if (mob.vehicle instanceof Player) {
			return null;
		}

		// Priority 1: Flying (Fleeing or ambient movement)
		if (mob.isFlying()) {
			return flightTask;
		}

		// Priority 2: Idle/wander behavior on ground
		idleTask.shouldWander = true;
		return idleTask;
	}

	@Override
	protected void onStop(Task interruptTask) {}

	@Override
	protected boolean isEqual(Task other) {
		return other instanceof ScorvidTask;
	}
}
