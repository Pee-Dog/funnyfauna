package peedog.funnyfauna.entity.ai.controllers;

import peedog.funnyfauna.entity.ai.path.flee.CowerTask;
import peedog.funnyfauna.entity.ai.path.follow.HuntPreyTask;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.compound.IdleTask;
import peedog.funnyfauna.entity.armadillo.MobArmadillo;

public class ArmadilloTask extends Task<MobArmadillo> {
	private final CowerTask cowerTask;
	private final HuntPreyTask huntTask;
	private final IdleTask<MobArmadillo> idleTask;

	public ArmadilloTask(MobArmadillo mob) {
		super(mob);
		this.cowerTask = new CowerTask(mob);
		this.huntTask = new HuntPreyTask(mob);
		this.idleTask = new IdleTask<>(mob);
	}

	@Override
	protected void onStart() {

	}

	@Override
	protected Task onTick() {
		// 1. If hurt, cower and back away
		if (mob.getFleeTimer() > 0) {
			return cowerTask;
		}

		// 2. Otherwise, look for food
		Task hunting = huntTask.onTick();
		if (hunting != null) {
			return huntTask;
		}

		// 3. Just chill
		idleTask.shouldWander = true;
		return idleTask;
	}

	@Override
	protected void onStop(Task interruptTask) {

	}

	@Override
	protected boolean isEqual(Task other) {
		return other instanceof ArmadilloTask;
	}
}
