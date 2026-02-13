package peedog.funnyfauna.entity.ai.controllers;

import net.minecraft.core.entity.player.Player;
import peedog.funnyfauna.entity.ai.path.flee.FleeFromMobsTask;
import peedog.funnyfauna.entity.ai.path.follow.HuntPreyTask;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.compound.IdleTask;
import peedog.funnyfauna.entity.lizard.MobLizard;

public class LizardTask extends Task<MobLizard> {
	private final FleeFromMobsTask<MobLizard> fleeTask;
	private final HuntPreyTask huntTask;
	private final IdleTask<MobLizard> idleTask;

	public LizardTask(MobLizard mob) {
		super(mob);
		this.fleeTask = new FleeFromMobsTask<>(mob, () -> !mob.hasTail());
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

		// If missing tail → attempt flee
		if (!mob.hasTail()) {
			Task fleeing = fleeTask.onTick();
			if (fleeing != null) {
				return fleeTask;
			}

			idleTask.shouldWander = true;
			return idleTask;
		}

		// Normal behavior
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
