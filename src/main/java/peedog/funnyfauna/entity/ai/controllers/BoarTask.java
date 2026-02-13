package peedog.funnyfauna.entity.ai.controllers;

import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.compound.IdleTask;
import peedog.funnyfauna.entity.ai.path.follow.ChaseAndAttackTask;
import peedog.funnyfauna.entity.boar.MobBoar;

public class BoarTask extends Task<MobBoar> {

	private final ChaseAndAttackTask<MobBoar> chaseTask;
	private final IdleTask<MobBoar> idleTask;

	public BoarTask(MobBoar mob) {
		super(mob);
		this.chaseTask = new ChaseAndAttackTask<>(mob);
		this.idleTask = new IdleTask<>(mob);
	}

	@Override
	protected void onStart() {

	}

	@Override
	protected Task onTick() {

		// If angry or has target → chase
		if (mob.getTarget() != null && mob.getTarget().isAlive()) {
			return chaseTask;
		}

		// Otherwise idle
		idleTask.shouldWander = true;
		return idleTask;
	}

	@Override
	protected void onStop(Task interruptTask) {

	}

	@Override
	protected boolean isEqual(Task other) {
		return other instanceof BoarTask;
	}
}
