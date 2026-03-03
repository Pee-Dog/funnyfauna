package peedog.funnyfauna.entity.ai.controllers;

import net.minecraft.core.entity.player.Player;
import peedog.funnyfauna.entity.ai.path.flee.FleeFromDangerTask;
import peedog.funnyfauna.entity.ai.path.follow.HuntPreyTask;
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
	protected void onStart() {

	}

	@Override
	protected Task onTick() {
		if (mob.vehicle instanceof Player) {
			return null;
		}

		// Priority 1: If missing tail and there's a flee target, run away
		if (!mob.hasTail()) {
			if (mob.getFleeTarget() != null && mob.getFleeTimer() > 0) {
				return fleeTask;
			}
			// No active flee target — wander while waiting for tail to regrow
			idleTask.shouldWander = true;
			return idleTask;
		}

		// Priority 2: Normal behavior (tail is present)
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
