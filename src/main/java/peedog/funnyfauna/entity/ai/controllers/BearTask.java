package peedog.funnyfauna.entity.ai.controllers;

import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.compound.IdleTask;
import peedog.funnyfauna.entity.ai.path.follow.ChaseAndAttackTask;
import peedog.funnyfauna.entity.bear.MobBear;

public class BearTask extends Task<MobBear> {

	private final ChaseAndAttackTask<MobBear> chaseTask;
	private final IdleTask<MobBear> idleTask;

	public BearTask(MobBear mob) {
		super(mob);
		this.chaseTask = new ChaseAndAttackTask<>(mob);
		this.idleTask = new IdleTask<>(mob);
	}

	@Override
	protected void onStart() {

	}

	@Override
	protected Task onTick() {
		// If the bear has a live target, chase and attack
		if (mob.getTarget() != null && mob.getTarget().isAlive()) {
			return chaseTask;
		}

		// Otherwise wander lazily
		idleTask.shouldWander = true;
		return idleTask;
	}

	@Override
	protected void onStop(Task interruptTask) {

	}

	@Override
	protected boolean isEqual(Task other) {
		return other instanceof BearTask;
	}
}
