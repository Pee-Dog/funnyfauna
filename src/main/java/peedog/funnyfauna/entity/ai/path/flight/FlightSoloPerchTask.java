package peedog.funnyfauna.entity.ai.path.flight;

import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.block.material.Material;
import net.minecraft.core.util.helper.MathHelper;
import peedog.funnyfauna.block.ServerBlockPos3D;
import peedog.funnyfauna.entity.MobTaskrunner;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.interfaces.IFlockable;
import peedog.funnyfauna.entity.ai.interfaces.IFlyable;
import peedog.funnyfauna.entity.bird.MobBird;

/**
 * Handles solo flight for short-distance perch seeking.
 * This is a smooth, direct flight mode where the bird flies to its leap target.
 * Much faster and more natural than the old vertical-first approach.
 */
public class FlightSoloPerchTask<T extends MobTaskrunner & IFlyable> extends Task<T> {
	private static final int MAX_SOLO_FLIGHT_TIME = 100; // 5 seconds max
	private int headHitTicks = 0;
	private static final int MAX_HEAD_HIT_TICKS = 20;

	public FlightSoloPerchTask(T mob) {
		super(mob);
	}

	@Override
	protected void onStart() {
		headHitTicks = 0;

		// Initial upward boost so flight looks natural even on same-level targets
		mob.yd = 0.2;
	}

	@Override
	protected Task onTick() {
		// Safety: abort if solo flight takes too long
		if (mob.getFlightTime() > MAX_SOLO_FLIGHT_TIME) {
			abortSoloFlight();
			return null;
		}

		// Get leap target from MobBird
		ServerBlockPos3D target = null;
		if (mob instanceof MobBird) {
			target = ((MobBird) mob).getLeapTarget();
		}

		if (target == null) {
			// No target - shouldn't happen, but abort gracefully
			abortSoloFlight();
			return null;
		}

		// Smooth, direct flight to target
		flyToTarget(target);

		// Check if we've reached the target
		if (isAtTarget(target)) {
			completeLanding(target);
			return null;
		}

		return null;
	}

	private void flyToTarget(ServerBlockPos3D target) {
		double targetX = target.x + 0.5;
		double targetY = target.y + 1.2; // Slightly above the block
		double targetZ = target.z + 0.5;

		double dx = targetX - mob.x;
		double dy = targetY - mob.y;
		double dz = targetZ - mob.z;

		double dist3D = Math.sqrt(dx * dx + dy * dy + dz * dz);

		if (dist3D < 0.1) return; // Already there

		// Direct approach - fly straight toward target
		// Speed increases as we get closer for a smooth landing
		double speed = Math.min(0.25, dist3D * 0.15);

		mob.xd = (dx / dist3D) * speed;
		mob.yd = (dy / dist3D) * speed;
		mob.zd = (dz / dist3D) * speed;

		// Update rotation to face target
		double horizontalDist = Math.sqrt(dx * dx + dz * dz);
		if (horizontalDist > 0.01) {
			double desiredYaw = Math.toDegrees(Math.atan2(dz, dx)) - 90.0;
			mob.yRot = updateRotation(mob.yRot, (float)desiredYaw, 20.0F);
		}

		// Gentle damping for smooth approach
		mob.xd *= 0.95;
		mob.yd *= 0.95;
		mob.zd *= 0.95;
	}

	private boolean isAtTarget(ServerBlockPos3D target) {
		if (target == null) return false;

		double targetX = target.x + 0.5;
		double targetY = target.y + 1.0;
		double targetZ = target.z + 0.5;

		double dx = targetX - mob.x;
		double dy = targetY - mob.y;
		double dz = targetZ - mob.z;

		double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
		return dist < 0.3; // Close enough to land
	}

	private void completeLanding(ServerBlockPos3D target) {
		// Land successfully
		mob.setFlying(false);
		if (mob instanceof IFlockable) {
			((IFlockable) mob).setSoloFlying(false);
		}

		// Clear leap target
		if (mob instanceof MobBird) {
			((MobBird) mob).clearLeapTarget();
		}

		// Position precisely on target
		mob.x = target.x + 0.5;
		mob.y = target.y + 1.0;
		mob.z = target.z + 0.5;
		mob.xd = mob.yd = mob.zd = 0;

		mob.setPos(mob.x, mob.y, mob.z);
	}

	private void abortSoloFlight() {
		if (mob instanceof IFlockable) {
			((IFlockable) mob).setSoloFlying(false);
		}
		mob.setFlying(false);
		mob.yd = -0.15;

		if (mob instanceof MobBird) {
			((MobBird) mob).clearLeapTarget();
		}

		headHitTicks = 0;
	}

	private boolean isHeadBlocked() {
		int headX = MathHelper.floor(mob.x);
		int headY = MathHelper.floor(mob.y + mob.bbHeight + 0.1);
		int headZ = MathHelper.floor(mob.z);

		int id = mob.world.getBlockId(headX, headY, headZ);
		if (id == 0) return false;

		Block block = Blocks.blocksList[id];
		return block != null && block.isCubeShaped();
	}

	private boolean trySlideToAir() {
		double[][] offsets = {
			{ 0.3,  0.0},
			{-0.3,  0.0},
			{ 0.0,  0.3},
			{ 0.0, -0.3}
		};

		for (double[] o : offsets) {
			int ax = MathHelper.floor(mob.x + o[0]);
			int ay = MathHelper.floor(mob.y + mob.bbHeight + 0.1);
			int az = MathHelper.floor(mob.z + o[1]);

			if (mob.world.isAirBlock(ax, ay, az)) {
				mob.xd += o[0] * 0.15;
				mob.zd += o[1] * 0.15;
				return true;
			}
		}
		return false;
	}

	private float updateRotation(float current, float target, float maxChange) {
		float diff = target - current;
		while (diff < -180.0F) diff += 360.0F;
		while (diff >= 180.0F) diff -= 360.0F;
		if (diff > maxChange) diff = maxChange;
		if (diff < -maxChange) diff = -maxChange;
		return current + diff;
	}

	@Override
	protected void onStop(Task interruptTask) {
		if (mob instanceof MobBird) {
			((MobBird) mob).clearLeapTarget();
		}
	}

	@Override
	protected boolean isEqual(Task other) {
		return other instanceof FlightSoloPerchTask;
	}
}
