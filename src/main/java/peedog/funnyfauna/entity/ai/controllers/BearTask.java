package peedog.funnyfauna.entity.ai.controllers;

import net.minecraft.core.entity.player.Player;
import net.minecraft.core.enums.Difficulty;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.compound.IdleTask;
import peedog.funnyfauna.entity.ai.path.ReturnHomeTask;
import peedog.funnyfauna.entity.ai.path.follow.ChaseAndAttackTask;
import peedog.funnyfauna.entity.ai.path.follow.BearThreatTask;
import peedog.funnyfauna.entity.bear.MobBear;

public class BearTask extends Task<MobBear> {

	private final ChaseAndAttackTask<MobBear> chaseTask;
	private final IdleTask<MobBear>           idleTask;
	private final ReturnHomeTask<MobBear>     returnHomeTask;
	private final BearThreatTask              threatTask;

	/** Sleeping rage-wake scan runs every 5 ticks (cheaper world query). */
	private static final int SCAN_INTERVAL = 5;
	private int scanCounter = 0;

	public BearTask(MobBear mob) {
		super(mob);
		this.chaseTask      = new ChaseAndAttackTask<>(mob);
		this.idleTask       = new IdleTask<>(mob);
		this.returnHomeTask = new ReturnHomeTask<>(mob);
		this.threatTask     = new BearThreatTask(mob);
	}

	@Override
	protected void onStart() {
		scanCounter = 0;
	}

	@Override
	protected Task onTick() {

		// -----------------------------------------------------------------------
		// 1. Has an active target — chase.
		//    Belly > 1: drop target if player walked outside the aggro radius.
		//    Belly 1 : relentless — never give up.
		// -----------------------------------------------------------------------
		if (mob.getTarget() != null && mob.getTarget().isAlive()) {
			if (mob.getBelly() > 1) {
				if (mob.distanceTo(mob.getTarget()) > mob.getAggroRange()) {
					mob.setTarget(null);
					// Fall through — re-evaluate this tick
				} else {
					mob.sleeping = false;
					return chaseTask;
				}
			} else {
				mob.sleeping = false;
				return chaseTask;
			}
		}

		// -----------------------------------------------------------------------
		// 2. Threat display + immediate attack — skip on Peaceful and while asleep.
		// -----------------------------------------------------------------------
		if (!mob.sleeping && mob.world.getDifficulty() != Difficulty.PEACEFUL) {

			// 2a. Immediate attack: belly ≤ 2 and player is within 2 blocks.
			//     Bypasses the threat display entirely.
			if (mob.getBelly() <= 2) {
				Player inYourFace = (Player) mob.world.getClosestPlayerToEntity(mob, 2.0);
				if (inYourFace != null) {
					mob.setTarget(inYourFace);
					return chaseTask;
				}
			}

			// 2b. Threat display: player within belly-scaled threat range.
			//     BearThreatTask manages escalation and will set target when ready.
			Player nearbyPlayer = (Player) mob.world.getClosestPlayerToEntity(mob, mob.getThreatRange());
			if (nearbyPlayer != null) {
				threatTask.threatTarget = nearbyPlayer;
				return threatTask;
			}
		}

		// -----------------------------------------------------------------------
		// 3. Sleeping rage-wake: scan every SCAN_INTERVAL ticks.
		// -----------------------------------------------------------------------
		if (mob.sleeping && mob.world.getDifficulty() != Difficulty.PEACEFUL) {
			if (++scanCounter >= SCAN_INTERVAL) {
				scanCounter = 0;
				Player intruder = mob.findPlayerToWake();
				if (intruder != null) {
					mob.wakeInRage(intruder);
					return chaseTask;
				}
			}
		} else {
			scanCounter = 0;
		}

		// -----------------------------------------------------------------------
		// 4. Day-wake
		// -----------------------------------------------------------------------
		if (mob.sleeping && mob.world.isDaytime()) {
			mob.sleeping = false;
		}

		// -----------------------------------------------------------------------
		// 5. Still sleeping — stay still.
		// -----------------------------------------------------------------------
		if (mob.sleeping) {
			mob.setMoveForward(0.0F);
			mob.setMoveStrafing(0.0F);
			return null;
		}

		// -----------------------------------------------------------------------
		// 6. Nighttime: head home to sleep.
		// -----------------------------------------------------------------------
		if (mob.shouldSleep()) {
			if (mob.isAtHome()) {
				mob.sleeping = true;
				mob.setMoveForward(0.0F);
				mob.setMoveStrafing(0.0F);
				return null;
			} else {
				return returnHomeTask;
			}
		}

		// -----------------------------------------------------------------------
		// 7. Daytime idle.
		// -----------------------------------------------------------------------
		idleTask.shouldWander = true;
		return idleTask;
	}

	@Override
	protected void onStop(Task interruptTask) {
		mob.setMoveForward(0.0F);
		mob.setMoveStrafing(0.0F);
	}

	@Override
	protected boolean isEqual(Task other) {
		return other instanceof BearTask;
	}
}
