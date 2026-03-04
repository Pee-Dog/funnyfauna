package peedog.funnyfauna.entity.ai.controllers;

import peedog.funnyfauna.entity.ai.path.follow.FollowLeaderTask;
import peedog.funnyfauna.entity.ai.path.follow.FoxDistractionTask;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.compound.IdleTask;
import peedog.funnyfauna.entity.fox.MobFox;

public class FoxTask extends Task<MobFox> {

	private final FollowLeaderTask followTask;
	private final FoxDistractionTask distractionTask;
	private final IdleTask<MobFox> idleTask;

	public FoxTask(MobFox mob) {
		super(mob);
		this.followTask = new FollowLeaderTask(mob);
		this.distractionTask = new FoxDistractionTask(mob);
		this.idleTask = new IdleTask<>(mob);
	}

	@Override
	protected Task onTick() {

		if (mob.isFoxSitting()) {
			mob.stopJumping();
			mob.setPathToEntity(null);
			mob.setMoveForward(0);
			idleTask.shouldWander = false;
			return idleTask;
		}

		// Highest priority: distraction
		if (mob.isDistracting()) {
			return distractionTask;
		}

		// Follow owner if tamed
		if (mob.isFoxTamed()) {
			Task t = followTask.onTick();
			if (t != null) {
				return followTask;
			}
		}

		idleTask.shouldWander = true;
		return idleTask;
	}

	@Override protected void onStart() {}
	@Override protected void onStop(Task interruptTask) {}
	@Override protected boolean isEqual(Task other) {
		return other instanceof FoxTask;
	}
}
