package peedog.funnyfauna.entity.ai.path.follow;

import net.minecraft.core.entity.player.Player;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.util.phys.Vec3;
import net.minecraft.core.world.pathfinder.Path;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.bear.MobBear;
import peedog.funnyfauna.entity.util.LookUtil;

/**
 * Pre-attack threat display for bears. Activated by BearTask when a player
 * enters the bear's threat range. Runs a weighted set of intimidation
 * sub-behaviors (staring, slow approach, swipe display, head sway, mock charge)
 * and escalates to a real attack once the escalation timer expires.
 *
 * Escalation timer:
 *   belly 4 → 40–50 s   (800–1000 ticks)
 *   belly 3 → 25–40 s   (500–800 ticks)
 *   belly 2 → 10–25 s   (200–500 ticks)
 *   belly 1 →  5–10 s   (100–200 ticks)
 *
 * Timer resets on onStop() (player left range, bear interrupted, etc.).
 * Immediate attack triggered inside this task when belly ≤ 2 and dist ≤ 2 blocks.
 */
public class BearThreatTask extends Task<MobBear> {

	// -------------------------------------------------------------------------
	// Sub-behavior enum
	// -------------------------------------------------------------------------
	private enum Behavior {
		STARE,          // Stand still, face player
		SLOW_APPROACH,  // Walk slowly toward player, stop ~5 blocks away
		SWIPE_DISPLAY,  // Trigger swipe animation (no damage), stay put
		HEAD_SWAY,      // Stand still, oscillate head on Z axis (renderer reads headSwayTick)
		MOCK_CHARGE     // Sprint at player, brake hard 3 blocks away
	}

	// -------------------------------------------------------------------------
	// Constants
	// -------------------------------------------------------------------------
	private static final float SLOW_APPROACH_STOP_DIST = 5.0F;
	private static final float MOCK_CHARGE_STOP_DIST   = 3.0F;
	private static final float SLOW_APPROACH_SPEED     = 0.6F;
	private static final float MOCK_CHARGE_SPEED       = 20.0F;

	// -------------------------------------------------------------------------
	// State — all reset in onStart / onStop
	// -------------------------------------------------------------------------
	/** Set by BearTask before returning this task each tick. */
	public Player threatTarget;

	private int escalationTimer  = 0;
	private Behavior currentBehavior = Behavior.STARE;
	private int behaviorTimer    = 0;
	private int soundTimer       = 0;
	private boolean mockChargeStopped = false;

	// Inline path for approach/charge (not extending PathTask to avoid its zero-moveForward issue)
	private Path path            = null;
	private int pathRecalcTimer  = 0;

	// -------------------------------------------------------------------------

	public BearThreatTask(MobBear mob) {
		super(mob);
	}

	// =========================================================================
	// Task lifecycle
	// =========================================================================

	@Override
	protected void onStart() {
		escalationTimer   = pickEscalationMax();
		currentBehavior   = Behavior.STARE;
		behaviorTimer     = pickBehaviorDuration(Behavior.STARE);
		soundTimer        = pickSoundInterval();
		mockChargeStopped = false;
		path              = null;
		mob.headSwayTarget = 0.0F;
	}

	@Override
	protected void onStop(Task interruptTask) {
		// Signal MobBear.tick()'s lerp to fade out — do NOT zero headSwayAmount
		// or the renderer hard-cuts. The lerp in tick() handles the smooth fade.
		mob.headSwayTarget = 0.0F;
		mob.setMoveForward(0.0F);
		mob.setMoveStrafing(0.0F);
		escalationTimer = 0;
		path = null;
	}

	// =========================================================================
	// Main tick
	// =========================================================================

	@Override
	public Task onTick() {
		if (threatTarget == null || !threatTarget.isAlive()) {
			cleanup();
			return null;
		}

		float dist        = mob.distanceTo(threatTarget);
		float threatRange = mob.getThreatRange();

		// Player walked out of range — abandon threat display, timer resets via onStop
		if (dist > threatRange) {
			cleanup();
			return null;
		}

		// Immediate attack: belly ≤ 2 and player is dangerously close
		if (mob.getBelly() <= 2 && dist <= 2.0F) {
			mob.setTarget(threatTarget);
			cleanup();
			return null;
		}

		// Escalation — tick down; 0 means the bear has had enough
		if (--escalationTimer <= 0) {
			mob.setTarget(threatTarget);
			cleanup();
			return null;
		}

		// -----------------------------------------------------------------------
		// Sound
		// -----------------------------------------------------------------------
		if (--soundTimer <= 0) {
			soundTimer = pickSoundInterval();
			playThreatSound();
		}

		// -----------------------------------------------------------------------
		// Head sway — set the target; MobBear.tick() does the actual lerp so
		// the fade always completes even if this task is interrupted.
		// -----------------------------------------------------------------------
		mob.headSwayTarget = (currentBehavior == Behavior.HEAD_SWAY) ? 1.0F : 0.0F;

		// -----------------------------------------------------------------------
		// Behavior timer — pick next when expired
		// -----------------------------------------------------------------------
		if (--behaviorTimer <= 0) {
			endCurrentBehavior();
			currentBehavior   = pickNextBehavior();
			behaviorTimer     = pickBehaviorDuration(currentBehavior);
			path              = null;
			mockChargeStopped = false;
		}

		// -----------------------------------------------------------------------
		// Run sub-behavior
		// -----------------------------------------------------------------------
		runBehavior(dist);

		return null;
	}

	// =========================================================================
	// Sub-behavior execution
	// =========================================================================

	private void runBehavior(float dist) {
		switch (currentBehavior) {

			case STARE:
				faceTarget();
				mob.setMoveForward(0.0F);
				mob.setMoveStrafing(0.0F);
				break;

			case SLOW_APPROACH:
				if (dist > SLOW_APPROACH_STOP_DIST) {
					pathTowardTarget(SLOW_APPROACH_SPEED);
				} else {
					faceTarget();
					mob.setMoveForward(0.0F);
					mob.setMoveStrafing(0.0F);
				}
				break;

			case SWIPE_DISPLAY:
				faceTarget();
				mob.setMoveForward(0.0F);
				mob.setMoveStrafing(0.0F);
				// Trigger swipe animation without dealing damage — just the visual
				if (mob.attackAnimTime <= 0 && random.nextInt(18) == 0) {
					mob.attackAnimTime   = 10;
					mob.attackSwipeRight = random.nextBoolean();
				}
				break;

			case HEAD_SWAY:
				faceTarget();
				mob.setMoveForward(0.0F);
				mob.setMoveStrafing(0.0F);
				break;

			case MOCK_CHARGE:
				if (mockChargeStopped) {
					// Bear already braked — hold position and face player
					faceTarget();
					mob.setMoveForward(0.0F);
					mob.setMoveStrafing(0.0F);
				} else if (dist <= MOCK_CHARGE_STOP_DIST) {
					// Close enough — brake
					mockChargeStopped = true;
					mob.setMoveForward(0.0F);
					mob.setMoveStrafing(0.0F);
					faceTarget();
				} else {
					pathTowardTarget(MOCK_CHARGE_SPEED);
				}
				break;
		}
	}

	// =========================================================================
	// Movement helpers
	// =========================================================================

	private void faceTarget() {
		// LookUtil.lookAt turns the mob smoothly (capped per-tick) — never snaps yRot directly.
		LookUtil.lookAt(mob, threatTarget, 10.0F, 10.0F);
	}

	/**
	 * Paths the bear toward threatTarget at the given speed.
	 * Mirrors ChaseAndAttackTask's movement loop exactly — does NOT call
	 * PathTask.onTick() to avoid the zero-moveForward per-waypoint freeze.
	 */
	private void pathTowardTarget(float speed) {
		if (path == null || --pathRecalcTimer <= 0 || random.nextInt(20) == 0) {
			pathRecalcTimer = 40;
			path = mob.world.getPathToEntity(mob, threatTarget, 32.0F);
		}

		int floorY     = MathHelper.floor(mob.bb.minY + 0.5F);
		boolean inWater = mob.isInWater();
		boolean inLava  = mob.isInLava();

		if (path != null && random.nextInt(100) != 0) {
			Vec3 next = path.getPos(mob);
			double d  = (double)(mob.bbWidth * 2.0F);

			while (next != null && next.distanceToSquared(mob.x, next.y, mob.z) < d * d) {
				path.next();
				if (path.isDone()) {
					next = null;
					path = null;
				} else {
					next = path.getPos(mob);
				}
			}

			mob.stopJumping();

			if (next != null) {
				double dx = next.x - mob.x;
				double dz = next.z - mob.z;
				double dy = next.y - (double) floorY;

				float targetYaw = (float)(Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
				float yawDelta  = targetYaw - mob.yRot;
				for (; yawDelta < -180.0F; yawDelta += 360.0F) {}
				while (yawDelta >= 180.0F) yawDelta -= 360.0F;
				if (yawDelta >  30.0F) yawDelta =  30.0F;
				if (yawDelta < -30.0F) yawDelta = -30.0F;

				mob.yRot += yawDelta;
				mob.setMoveForward(speed);
				mob.setMoveStrafing(0.0F);
				if (dy > 0.0) mob.startJumping();
			}

			LookUtil.lookAt(mob, threatTarget, 10.0F, 10.0F);
			if (mob.horizontalCollision) mob.startJumping();
			if (random.nextFloat() < 0.8F && (inWater || inLava)) mob.startJumping();

		} else {
			path = null;
			faceTarget();
			mob.setMoveForward(speed);
			mob.setMoveStrafing(0.0F);
			if (mob.horizontalCollision) mob.startJumping();
		}
	}

	// =========================================================================
	// Behavior selection
	// =========================================================================

	/**
	 * Weighted random behavior pick. Hungrier bears (belly 1) favour active
	 * behaviors; well-fed bears (belly 4) mostly just stand and stare.
	 *
	 * Weights: [STARE, SLOW_APPROACH, SWIPE_DISPLAY, HEAD_SWAY, MOCK_CHARGE]
	 */
	private Behavior pickNextBehavior() {
		int[] weights;
		switch (mob.getBelly()) {
			case 4:  weights = new int[]{55, 15, 10, 15,  5}; break;
			case 3:  weights = new int[]{40, 20, 15, 18,  7}; break;
			case 2:  weights = new int[]{30, 20, 20, 20, 10}; break;
			default: weights = new int[]{20, 25, 25, 20, 10}; break; // belly 1
		}
		int total = 0;
		for (int w : weights) total += w;
		int roll = random.nextInt(total);
		int cumulative = 0;
		Behavior[] values = Behavior.values();
		for (int i = 0; i < values.length; i++) {
			cumulative += weights[i];
			if (roll < cumulative) return values[i];
		}
		return Behavior.STARE;
	}

	/** How many ticks the next sub-behavior should run before re-rolling. */
	private int pickBehaviorDuration(Behavior behavior) {
		int belly = mob.getBelly();
		switch (behavior) {
			case STARE:
				return belly == 1 ? 20 + random.nextInt(30) : 40 + random.nextInt(40);
			case SLOW_APPROACH:
				return belly == 1 ? 30 + random.nextInt(20) : 40 + random.nextInt(40);
			case SWIPE_DISPLAY:
				return 20 + random.nextInt(20);
			case HEAD_SWAY:
				return belly == 1 ? 30 + random.nextInt(30) : 40 + random.nextInt(60);
			case MOCK_CHARGE:
				// Upper-bounded; usually ends early when the bear reaches MOCK_CHARGE_STOP_DIST
				return 60 + random.nextInt(40);
			default:
				return 40;
		}
	}

	/** Escalation max in ticks — how long before the bear gives up threatening and attacks. */
	private int pickEscalationMax() {
		switch (mob.getBelly()) {
			case 4: return 800  + random.nextInt(200); // 40–50 s
			case 3: return 500  + random.nextInt(300); // 25–40 s
			case 2: return 400  + random.nextInt(100); // 20–25 s
			default: return 300 + random.nextInt(100); //  15-20 s
		}
	}

	/** Ticks between threat sounds. Belly 1 = more frequent, belly 4 = more relaxed. */
	private int pickSoundInterval() {
		int base = mob.getBelly() == 1 ? 40
			: mob.getBelly() == 2 ? 55
			: mob.getBelly() == 3 ? 70
			: 90;
		return base + random.nextInt(80);
	}

	private void playThreatSound() {
		if (random.nextBoolean()) {
			mob.playHuffSound();
		} else {
			mob.playJawClackSound();
		}
	}

	/** Called when exiting a behavior or cleaning up. */
	private void endCurrentBehavior() {
		if (currentBehavior == Behavior.MOCK_CHARGE) {
			mob.setMoveForward(0.0F);
		}
		// headSwayTarget drives the lerp in MobBear.tick() — let that handle fading
	}

	private void cleanup() {
		mob.headSwayTarget = 0.0F; // MobBear.tick() will lerp headSwayAmount to 0
		mob.setMoveForward(0.0F);
		mob.setMoveStrafing(0.0F);
		path = null;
	}

	@Override
	protected boolean isEqual(Task other) {
		return other instanceof BearThreatTask;
	}
}
