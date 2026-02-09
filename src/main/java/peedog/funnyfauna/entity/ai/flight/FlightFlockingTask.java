package peedog.funnyfauna.entity.ai.flight;

import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.util.phys.AABB;
import peedog.funnyfauna.entity.MobTaskrunner;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.i.IFlockable;
import peedog.funnyfauna.entity.ai.i.IFlyable;

import java.util.List;

/**
 * Handles flocking flight behavior - mobs fly together in coordinated groups.
 * Works with any mob that implements IFlyable and optionally IFlockable.
 */
public class FlightFlockingTask<T extends MobTaskrunner & IFlyable> extends Task<T> {
	private static final float BASE_FLIGHT_SPEED = 0.7F;
	private static final double MAX_FLIGHT_HEIGHT = 20.0;

	private int courseChangeCooldown = 0;
	private int courseCooldown = 200;
	private int headHitTicks = 0;
	private static final int MAX_HEAD_HIT_TICKS = 40;

	public FlightFlockingTask(T mob) {
		super(mob);
	}

	@Override
	protected void onStart() {}

	@Override
	protected Task onTick() {
		// Check ceiling collision
		if (isHeadBlocked()) {
			headHitTicks++;
			if (headHitTicks >= MAX_HEAD_HIT_TICKS) {
				// Give up and try to slide sideways
				if (!trySlideToAir()) {
					// Force downward if stuck
					mob.yd = -0.15;
				}
				headHitTicks = 0;
			}
		} else {
			headHitTicks = 0;
		}

		// Update course
		if (--courseChangeCooldown <= 0) {
			courseChangeCooldown = courseCooldown + random.nextInt(100);
			updateFlightCourse();
		}

		// Apply flight movement
		applyFlockingMovement();

		return null;
	}

	private void updateFlightCourse() {
		// Only attempt flocking if this mob is flockable
		if (!(mob instanceof IFlockable)) {
			// Just do solo flight
			soloFlight();
			return;
		}

		IFlockable thisFlockable = (IFlockable) mob;

		// Get nearby entities for flock coordination
		List<Entity> nearbyEntities = mob.world.getEntitiesWithinAABB(
			Entity.class,
			AABB.getTemporaryBB(mob.x, mob.y, mob.z, mob.x + 1, mob.y + 1, mob.z + 1).grow(8, 4, 8)
		);

		double flockCenterX = 0;
		double flockCenterY = 0;
		double flockCenterZ = 0;
		int flockCount = 0;

		for (Entity entity : nearbyEntities) {
			if (entity == mob) continue;
			if (!(entity instanceof IFlyable)) continue;
			if (!(entity instanceof IFlockable)) continue;

			IFlyable flyable = (IFlyable) entity;
			IFlockable flockable = (IFlockable) entity;

			// Check if we can flock with this entity
			if (!thisFlockable.canFlockWith(entity)) continue;

			// Only flock with others who are also flocking (not solo flying)
			if (!flyable.isFlying() || flockable.isSoloFlying()) continue;

			flockCenterX += entity.x;
			flockCenterY += entity.y;
			flockCenterZ += entity.z;
			flockCount++;
		}

		if (flockCount > 0) {
			// Move toward flock center
			flockCenterX /= flockCount;
			flockCenterY /= flockCount;
			flockCenterZ /= flockCount;

			double dx = flockCenterX - mob.x;
			double dy = flockCenterY - mob.y;
			double dz = flockCenterZ - mob.z;

			// Gentle steering toward flock
			mob.xd += dx * 0.01;
			mob.yd += dy * 0.01;
			mob.zd += dz * 0.01;
		} else {
			// Solo flight - random wandering
			soloFlight();
		}
	}

	private void soloFlight() {
		mob.xd += (random.nextDouble() - 0.5) * 0.1;
		mob.zd += (random.nextDouble() - 0.5) * 0.1;
	}

	private void applyFlockingMovement() {
		double currentY = mob.y;
		double groundY = mob.getGroundY();
		double heightAboveGround = currentY - groundY;

		// Height maintenance
		if (heightAboveGround > MAX_FLIGHT_HEIGHT) {
			mob.yd = -0.1; // Descend if too high
		} else if (heightAboveGround < 3.0 && mob.getFlightTime() > 20) {
			mob.yd = 0.15; // Climb if too low
		} else {
			// Gentle bobbing
			mob.yd += (random.nextDouble() - 0.5) * 0.02;
		}

		// Apply horizontal movement
		double speed = BASE_FLIGHT_SPEED * 0.1;
		mob.xd += (random.nextDouble() - 0.5) * speed;
		mob.zd += (random.nextDouble() - 0.5) * speed;

		// Damping to prevent excessive speed
		mob.xd *= 0.95;
		mob.yd *= 0.95;
		mob.zd *= 0.95;

		// Update yaw to face movement direction
		if (Math.abs(mob.xd) > 0.01 || Math.abs(mob.zd) > 0.01) {
			mob.yRot = (float) (Math.atan2(mob.zd, mob.xd) * 180.0 / Math.PI) - 90.0F;
		}
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
			{ 0.4,  0.0},
			{-0.4,  0.0},
			{ 0.0,  0.4},
			{ 0.0, -0.4},
			{ 0.4,  0.4},
			{-0.4, -0.4}
		};

		for (double[] o : offsets) {
			int ax = MathHelper.floor(mob.x + o[0]);
			int ay = MathHelper.floor(mob.y + mob.bbHeight + 0.1);
			int az = MathHelper.floor(mob.z + o[1]);

			if (mob.world.isAirBlock(ax, ay, az)) {
				mob.xd += o[0] * 0.2;
				mob.zd += o[1] * 0.2;
				return true;
			}
		}
		return false;
	}

	@Override
	protected void onStop(Task interruptTask) {}

	@Override
	protected boolean isEqual(Task other) {
		return other instanceof FlightFlockingTask;
	}
}
