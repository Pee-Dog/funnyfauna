package peedog.funnyfauna.entity.ai.controllers;

import net.minecraft.core.entity.player.Player;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.compound.IdleTask;
import peedog.funnyfauna.entity.ai.path.flee.AvoidPlayerTask;
import peedog.funnyfauna.entity.sasquatch.MobSasquatch;

public class SasquatchTask extends Task<MobSasquatch> {

	private final AvoidPlayerTask<MobSasquatch> avoidTask;
	private final IdleTask<MobSasquatch> idleTask;

	public SasquatchTask(MobSasquatch mob) {
		super(mob);
		this.avoidTask = new AvoidPlayerTask<>(mob);
		this.idleTask  = new IdleTask<>(mob);
		this.idleTask.shouldWander      = true;
		this.idleTask.shouldSwim        = true;
		this.idleTask.shouldSurfaceSwim = false;
	}

	@Override
	protected void onStart() { }

	@Override
	protected Task onTick() {
		if (mob.getFleeTarget() != null && mob.getFleeTimer() > 0) {
			// Clear the look-at-player target so IdleTask's lookAtPlayersTask
			// can't set the mob's yaw toward the player while PathTask is
			// trying to steer away — that strafing logic was fighting the flee.
			idleTask.lookAtPlayersTask.currentTarget = null;
			return avoidTask;
		}

		idleTask.shouldWander = true;
		return idleTask;
	}

	@Override
	protected void onStop(Task interruptTask) { }

	@Override
	protected boolean isEqual(Task other) {
		return other instanceof SasquatchTask;
	}
}
