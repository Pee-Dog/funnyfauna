package peedog.funnyfauna.entity.ai.controllers;

import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.compound.IdleTask;
import peedog.funnyfauna.entity.chipmunk.MobChipmunk;
import peedog.funnyfauna.entity.ai.path.flee.FleeFromDangerTask;
import peedog.funnyfauna.entity.ai.path.flee.FleeToLogTask;
import peedog.funnyfauna.entity.ai.path.LeafLeapTask;

public class ChipmunkTask extends Task<MobChipmunk> {
	private final FleeToLogTask fleeToLogTask;
	private final FleeFromDangerTask<MobChipmunk> fleeFromDangerTask;
	private final LeafLeapTask leafLeapTask;
	private final IdleTask<MobChipmunk> idleTask;

	/**
	 * Countdown used while FleeFromDangerTask is active to periodically re-scan
	 * for a nearby log.  When the scan succeeds (foundLog becomes true) the
	 * controller immediately switches back to FleeToLogTask on the next tick.
	 */
	private int logSearchTimer = 0;

	public ChipmunkTask(MobChipmunk mob) {
		super(mob);
		this.fleeToLogTask      = new FleeToLogTask(mob);
		this.fleeFromDangerTask = new FleeFromDangerTask<>(mob);
		this.leafLeapTask       = new LeafLeapTask(mob);
		this.idleTask           = new IdleTask<>(mob);

		this.idleTask.shouldWander = true;
	}

	@Override
	protected void onStart() {}

	@Override
	protected Task onTick() {
		// Priority 1: Leaf mode — safe zone, wander/leap on leaves only.
		if (mob.isInLeafMode()) {
			return leafLeapTask;
		}

		// Priority 2: A threat has been detected — try to reach a log first.
		if (mob.getFleeTarget() != null && mob.getFleeTimer() > 0) {
			if (fleeToLogTask.foundLog) {
				return fleeToLogTask;
			} else {
				// No log known yet — flee generically, but keep scanning for a log
				// every 40 ticks so we switch to FleeToLogTask as soon as one is
				// found.  Without this, foundLog stays false forever because
				// FleeToLogTask.onTick() never runs while we're in FleeFromDanger.
				if (logSearchTimer-- <= 0) {
					logSearchTimer = 40;
					fleeToLogTask.searchForLog();
				}
				return fleeFromDangerTask;
			}
		}

		// Priority 3: Idle / wander on the ground.
		return idleTask;
	}

	@Override
	protected void onStop(Task interruptTask) {}

	@Override
	protected boolean isEqual(Task other) {
		return other instanceof ChipmunkTask;
	}
}
