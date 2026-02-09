package peedog.funnyfauna.entity.ai.flight;

import net.minecraft.core.util.helper.MathHelper;
import peedog.funnyfauna.block.ServerBlockPos3D;
import peedog.funnyfauna.entity.MobTaskrunner;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.i.IFlyable;

/**
 * Handles the controlled landing descent when a flying mob is coming down to perch or rest.
 */
public class LandingTask<T extends MobTaskrunner & IFlyable> extends Task<T> {
	private static final double HORIZONTAL_SPEED = 0.1;
	private static final double MAX_DESCENT_SPEED = 0.2;

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
	}

	@Override
	protected Task onTick() {
		ServerBlockPos3D landingTarget = flightTask.getLandingTarget();

		if (landingTarget == null) {
			// Lost landing target - abort landing
			mob.setLanding(false);
			return null;
		}

		double dx = landingTarget.x + 0.5 - mob.x;
		double dz = landingTarget.z + 0.5 - mob.z;
		double dy = landingTarget.y - mob.y;

		// Horizontal motion: move toward target
		double horizontalDist = Math.sqrt(dx * dx + dz * dz);
		if (horizontalDist > 0.01) {
			mob.xd = dx / horizontalDist * HORIZONTAL_SPEED;
			mob.zd = dz / horizontalDist * HORIZONTAL_SPEED;
		}

		// Vertical motion: constant descent speed
		mob.yd = MathHelper.clamp(dy, -MAX_DESCENT_SPEED, 0.05);

		// Update yaw to face landing direction
		if (horizontalDist > 0.01) {
			mob.yRot = (float) (Math.atan2(mob.zd, mob.xd) * 180.0 / Math.PI) - 90.0F;
		}

		// Check if we've reached the landing spot
		if (Math.abs(dx) < 0.2 && Math.abs(dz) < 0.2 && Math.abs(dy) < 0.1) {
			completeLanding();
			return null;
		}

		return null;
	}

	private void completeLanding() {
		// Successfully landed
		mob.setFlying(false);
		mob.setLanding(false);
		mob.setPerched(true);
		mob.xd = mob.yd = mob.zd = 0;
		mob.setFlightTime(0);

		// Clear landing target
		flightTask.setLandingTarget(null);
	}

	@Override
	protected void onStop(Task interruptTask) {
		// If landing is interrupted, cancel it
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
