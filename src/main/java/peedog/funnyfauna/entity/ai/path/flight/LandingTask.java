package peedog.funnyfauna.entity.ai.path.flight;

import net.minecraft.core.util.helper.MathHelper;
import peedog.funnyfauna.block.ServerBlockPos3D;
import peedog.funnyfauna.entity.MobTaskrunner;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.interfaces.IFlyable;

/**
 * Handles the controlled landing descent when a flying mob is coming down to perch or rest.
 * Fix Issue 4: Smooth descent with gradual transition and reduced jittering.
 */
public class LandingTask<T extends MobTaskrunner & IFlyable> extends Task<T> {
	private static final double HORIZONTAL_SPEED = 0.15;
	private static final double MAX_DESCENT_SPEED = 0.25;
	private static final double MIN_DESCENT_SPEED = 0.05;

	// Transition phase for smooth entry into landing
	private static final int TRANSITION_TICKS = 20; // 1 second transition
	private int landingTicks = 0;

	private final FlightTask<T> flightTask;

	public LandingTask(T mob, FlightTask<T> flightTask) {
		super(mob);
		this.flightTask = flightTask;
	}

	@Override
	protected void onStart() {
		// Reduce size for landing
		mob.setLandingSize(true);
		mob.setPos(mob.x, mob.y, mob.z); // Refresh AABB

		landingTicks = 0;
	}

	@Override
	protected Task onTick() {
		ServerBlockPos3D landingTarget = flightTask.getLandingTarget();

		if (landingTarget == null) {
			// Lost landing target - abort landing
			mob.setLanding(false);
			return null;
		}

		landingTicks++;

		double dx = landingTarget.x + 0.5 - mob.x;
		double dz = landingTarget.z + 0.5 - mob.z;
		double dy = landingTarget.y - mob.y;

		// NEW: Instant landing when touching ground, but ONLY if close to target
		// This prevents premature landing far from the target
		double horizontalDist = Math.sqrt(dx * dx + dz * dz);
		if (mob.onGround && horizontalDist < 2.0 && Math.abs(dy) < 1.5) {
			completeLanding();
			return null;
		}

		// Calculate transition progress (0.0 to 1.0)
		double transitionProgress = Math.min(1.0, landingTicks / (double) TRANSITION_TICKS);

		// Fix Issue 4: Gradual trajectory change

		if (horizontalDist > 0.01) {
			// Smoothly reduce horizontal speed during transition
			double currentHorizontalSpeed = HORIZONTAL_SPEED * (1.5 - 0.5 * transitionProgress);

			// Fix Issue 4: Smooth horizontal movement (reduce jittering)
			// Use exponential smoothing instead of direct assignment
			double targetXd = dx / horizontalDist * currentHorizontalSpeed;
			double targetZd = dz / horizontalDist * currentHorizontalSpeed;

			mob.xd = mob.xd * 0.7 + targetXd * 0.3; // Smooth interpolation
			mob.zd = mob.zd * 0.7 + targetZd * 0.3;
		} else {
			// Near target horizontally, dampen movement
			mob.xd *= 0.8;
			mob.zd *= 0.8;
		}

		// Fix Issue 4: Gradual descent with smooth speed curve
		// Start with slow descent, increase during transition
		double descentSpeed;
		if (transitionProgress < 1.0) {
			// Smooth acceleration during transition
			descentSpeed = MIN_DESCENT_SPEED + (MAX_DESCENT_SPEED - MIN_DESCENT_SPEED) * transitionProgress;
		} else {
			// Full descent speed after transition
			descentSpeed = MAX_DESCENT_SPEED;
		}

		// Apply descent, but reduce if getting close to target
		if (Math.abs(dy) > 0.5) {
			mob.yd = MathHelper.clamp(dy * 0.1, -descentSpeed, 0.05);
		} else {
			// Close to ground - very gentle final approach
			mob.yd = MathHelper.clamp(dy * 0.2, -MIN_DESCENT_SPEED, 0.02);
		}

		// Update yaw to face landing direction (smooth rotation)
		if (horizontalDist > 0.01) {
			float targetYaw = (float) (Math.atan2(mob.zd, mob.xd) * 180.0 / Math.PI) - 90.0F;
			mob.yRot = smoothRotation(mob.yRot, targetYaw, 8.0F);
		}

		// Check if we've reached the landing spot
		if (Math.abs(dx) < 0.25 && Math.abs(dz) < 0.25 && Math.abs(dy) < 0.15) {
			completeLanding();
			return null;
		}

		return null;
	}

	private float smoothRotation(float current, float target, float maxChange) {
		float diff = target - current;
		while (diff < -180.0F) diff += 360.0F;
		while (diff >= 180.0F) diff -= 360.0F;
		if (diff > maxChange) diff = maxChange;
		if (diff < -maxChange) diff = -maxChange;
		return current + diff;
	}

	private void completeLanding() {
		// Successfully landed
		mob.setFlying(false);
		mob.setLanding(false);

		// Revert to normal size
		mob.setLandingSize(false);

		// DON'T auto-perch - let MobBird decide based on ground type and time

		// Ensure clean stop
		mob.xd = 0;
		mob.yd = 0;
		mob.zd = 0;
		mob.setFlightTime(0);

		// Clear landing target
		flightTask.setLandingTarget(null);

		landingTicks = 0;
	}

	@Override
	protected void onStop(Task interruptTask) {
		// If landing is interrupted, cancel it
		if (interruptTask != null && !(interruptTask instanceof LandingTask)) {
			mob.setLanding(false);
			flightTask.setLandingTarget(null);
			landingTicks = 0;
		}
	}

	@Override
	protected boolean isEqual(Task other) {
		return other instanceof LandingTask;
	}
}
