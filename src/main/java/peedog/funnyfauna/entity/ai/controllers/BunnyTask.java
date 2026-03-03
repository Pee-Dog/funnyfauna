package peedog.funnyfauna.entity.ai.controllers;

import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.player.Player;
import peedog.funnyfauna.entity.ai.path.flee.FleeFromDangerTask;
import peedog.funnyfauna.entity.ai.path.follow.FollowPlayerTask;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.compound.IdleTask;
import peedog.funnyfauna.entity.bunny.MobBunny;

public class BunnyTask extends Task<MobBunny> {
	private final FleeFromDangerTask<MobBunny> fleeTask;
	private final IdleTask<MobBunny> idleTask;
	private final FollowPlayerTask<MobBunny> followPlayerTask;

	public BunnyTask(MobBunny mob) {
		super(mob);
		this.fleeTask = new FleeFromDangerTask<>(mob);

		// Setup IdleTask with hopping enabled
		this.idleTask = new IdleTask<>(mob);
		this.idleTask.shouldWander = true;
		this.followPlayerTask = new FollowPlayerTask<>(mob);
	}

	@Override
	protected void onStart() {
	}

	@Override
	protected Task onTick() {
		// AI Disabled if riding a player
		if (mob.vehicle instanceof Player) {
			return null;
		}

		// Priority 1: Flee if hurt/danger is present
		if (mob.getFleeTarget() != null && mob.getFleeTimer() > 0) {
			return fleeTask;
		}
		idleTask.shouldWander = true;
		String owner = mob.getOwnerUUID();
		if (owner != null && !owner.isEmpty()) {

			Entity target = mob.leader();
			if (target != null) {
				if (target.distanceTo(this.mob) > 3.0F) return this.followPlayerTask;
				else idleTask.shouldWander = false;
			}
		}
		return idleTask;
	}

	@Override
	protected void onStop(Task interruptTask) {

	}

	@Override
	protected boolean isEqual(Task other) {
		return other instanceof BunnyTask;
	}
}
