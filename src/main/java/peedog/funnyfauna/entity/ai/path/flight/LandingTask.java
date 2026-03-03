package peedog.funnyfauna.entity.ai.path.flight;

import net.minecraft.core.util.helper.MathHelper;
import peedog.funnyfauna.block.ServerBlockPos3D;
import peedog.funnyfauna.entity.MobTaskrunner;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.interfaces.IFlyable;

/**
 * Handles the controlled landing descent when a flying mob is coming down to perch or rest.
 * Updated to provide a seamless, realistic flight-to-perch transition using 3D vector seeking.
 */
public class LandingTask<T extends MobTaskrunner & IFlyable> extends Task<T> {

	private final FlightTask<T> flightTask;

	// Tuning constants for natural flight
	private static final double APPROACH_SPEED_FAR = 0.55;  // Fast approach (swooping in)
	private static final double APPROACH_SPEED_NEAR = 0.25; // Braking phase
	private static final double APPROACH_SPEED_FINAL = 0.1; // Precision landing

	// How quickly the bird adjusts its velocity (Lower = heavy/smooth, Higher = snappy/twitchy)
	// 0.2 is a good balance for small birds
	private static final double AGILITY = 0.2;

	public LandingTask(T mob, FlightTask<T> flightTask) {
		super(mob);
		this.flightTask = flightTask;
	}

	@Override
	protected void onStart() {
		// Hitbox stays at flying size throughout descent.
		// completeLanding() calls setLandingSize(false) once the mob actually touches down.
		mob.setPos(mob.x, mob.y, mob.z);
	}

	@Override
	protected Task onTick() {
		ServerBlockPos3D landingTarget = flightTask.getLandingTarget();

		if (landingTarget == null) {
			mob.setLanding(false);
			return null;
		}

		// Calculate target coordinates (aim for the center-top of the block)
		// Note: landingTarget.y typically accounts for the block height (y + 1.0)
		double tx = landingTarget.x + 0.5;
		double ty = landingTarget.y;
		double tz = landingTarget.z + 0.5;

		// Calculate distance vector
		double dx = tx - mob.x;
		double dy = ty - mob.y;
		double dz = tz - mob.z;

		double distSq = dx * dx + dy * dy + dz * dz;
		double dist = Math.sqrt(distSq);

		// --- COMPLETION CHECK ---
		// If very close to the center target OR touching ground near the target
		// We permit a slightly larger Y tolerance because getting exact Y can be finicky with gravity
		boolean nearCenter = dist < 0.3;
		boolean landedOnTarget = mob.onGround && dist < 1.2 && Math.abs(dy) < 0.5;

		if (nearCenter || landedOnTarget) {
			completeLanding();
			return null;
		}

		// --- VELOCITY CALCULATION ---

		// 1. Determine desired speed based on distance (Braking Logic)
		double targetSpeed;
		if (dist > 3.0) {
			targetSpeed = APPROACH_SPEED_FAR;
		} else if (dist > 1.0) {
			targetSpeed = APPROACH_SPEED_NEAR;
		} else {
			targetSpeed = APPROACH_SPEED_FINAL;
		}

		// 2. Calculate ideal velocity vector (Direction * Speed)
		double idealXd = (dx / dist) * targetSpeed;
		double idealYd = (dy / dist) * targetSpeed;
		double idealZd = (dz / dist) * targetSpeed;

		// 3. Smoothly interpolate current velocity towards ideal velocity
		// This creates the "arcing" motion rather than robotic straight lines
		mob.xd = lerp(mob.xd, idealXd, AGILITY);
		mob.yd = lerp(mob.yd, idealYd, AGILITY);
		mob.zd = lerp(mob.zd, idealZd, AGILITY);

		// --- ROTATION ---
		// Update yaw to face movement direction
		if (dist > 0.1) {
			float targetYaw = (float) (Math.atan2(mob.zd, mob.xd) * 180.0 / Math.PI) - 90.0F;
			mob.yRot = smoothRotation(mob.yRot, targetYaw, 15.0F);
		}

		return null;
	}

	// Helper for linear interpolation
	private double lerp(double start, double end, double delta) {
		return start + (end - start) * delta;
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
		mob.setLandingSize(false);

		// Stop momentum instantly to prevent sliding off the perch
		mob.xd = 0;
		mob.yd = 0;
		mob.zd = 0;
		mob.setFlightTime(0);

		// Snap position to center of block if we were flying (prevents hanging off edge)
		if (!mob.onGround) {
			ServerBlockPos3D t = flightTask.getLandingTarget();
			if (t != null) mob.setPos(t.x + 0.5, t.y, t.z + 0.5);
		}

		flightTask.setLandingTarget(null);
	}

	@Override
	protected void onStop(Task interruptTask) {
		if (interruptTask != null && !(interruptTask instanceof LandingTask)) {
			mob.setLanding(false);
			flightTask.setLandingTarget(null);
		}
	}

	@Override
	protected boolean isEqual(Task other) {
		return other instanceof LandingTask;
	}
}
