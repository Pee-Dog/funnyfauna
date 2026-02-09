package peedog.funnyfauna.entity.ai.controllers;

import net.minecraft.core.entity.player.Player;
import peedog.funnyfauna.entity.ai.FleeFromDangerTask;
import peedog.funnyfauna.entity.ai.HuntPreyTask;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.compound.IdleTask;
import peedog.funnyfauna.entity.lizard.MobLizard;

public class LizardTask extends Task<MobLizard> {
	private final FleeFromDangerTask<MobLizard> fleeTask;
	private final HuntPreyTask huntTask;
	private final IdleTask<MobLizard> idleTask;

	public LizardTask(MobLizard mob) {
		super(mob);
		this.fleeTask = new FleeFromDangerTask<>(mob);
		this.huntTask = new HuntPreyTask(mob);
		this.idleTask = new IdleTask<>(mob);
	}

	@Override
	protected void onStart() {}

	@Override
	protected Task onTick() {
		if (mob.vehicle instanceof Player) {
			return null;
		}

		if (mob.getFleeTimer() > 0) {
			return fleeTask;
		}

		huntTask.onTick();
		if (huntTask.lookTarget() != null) {
			return huntTask;
		}
		idleTask.shouldWander = true;
		return idleTask;
	}

	@Override
	protected void onStop(Task interruptTask) {}

	@Override
	protected boolean isEqual(Task other) {
		return other instanceof LizardTask;
	}
}
