package peedog.funnyfauna.entity.ai.flight;

import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.util.phys.AABB;
import net.minecraft.core.util.phys.Vec3;
import peedog.funnyfauna.entity.MobTaskrunner;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.i.IFlockable;
import peedog.funnyfauna.entity.ai.i.IFlyable;
import peedog.funnyfauna.entity.ai.i.IHomeable;

import java.util.List;

/**
 * Handles flocking flight behavior - mobs fly together using separation, cohesion, and alignment.
 * Based on classic boids flocking algorithm.
 */
public class FlightFlockingTask<T extends MobTaskrunner & IFlyable> extends Task<T> {
	private static final float BASE_FLIGHT_SPEED = 0.7F;
	private static final int MAX_FLIGHT_TIME = 600;

	// Flocking parameters
	private static final double NEIGHBOR_RADIUS = 8.0;
	private static final double SEPARATION_RADIUS = 3.0;

	// Height parameters
	private static final double MIN_FLIGHT_HEIGHT = 15.0;
	private static final double MAX_FLIGHT_HEIGHT = 25.0;

	// Collision handling
	private int headHitTicks = 0;
	private static final int MAX_HEAD_HIT_TICKS = 40;

	public FlightFlockingTask(T mob) {
		super(mob);
	}

	@Override
	protected void onStart() {
	}

	@Override
	protected Task onTick() {
		// Update ground reference
		mob.setGroundY(getGroundHeight());

		// Check for max flight time
		if (mob.getFlightTime() > MAX_FLIGHT_TIME) {
			mob.setFlying(false);
			mob.yd = -0.1;
			return null;
		}

		// Handle ceiling collision
		if (isHeadBlocked()) {
			headHitTicks++;

			// Kill upward motion
			if (mob.yd > 0) mob.yd = 0;

			// Try sliding sideways
			boolean escaped = trySlideToAir();

			if (!escaped || headHitTicks > MAX_HEAD_HIT_TICKS) {
				// Give up and land
				mob.setFlying(false);
				mob.yd = -0.15;
				mob.xd *= 0.3;
				mob.zd *= 0.3;
				headHitTicks = 0;
				return null;
			}
		} else {
			headHitTicks = 0;
		}

		// Apply flocking behavior
		applyFlockingBehavior();

		return null;
	}

	private void applyFlockingBehavior() {
		// Get nearby flockmates
		List<Entity> nearbyEntities = mob.world.getEntitiesWithinAABB(
			Entity.class,
			AABB.getTemporaryBB(mob.x, mob.y, mob.z, mob.x + 1, mob.y + 1, mob.z + 1).grow(8, 6, 8)
		);

		// Boids algorithm vectors
		Vec3 flockVec = Vec3.getTempVec3(0, 0, 0);
		Vec3 cohesionVec = Vec3.getTempVec3(0, 0, 0);
		double alignmentSin = 0;
		double alignmentCos = 0;
		int neighborCount = 0;

		double neighborRadiusSq = NEIGHBOR_RADIUS * NEIGHBOR_RADIUS;

		// Check if this mob can flock
		boolean canFlock = mob instanceof IFlockable;

		for (Entity entity : nearbyEntities) {
			if (entity == mob) continue;
			if (!(entity instanceof IFlyable)) continue;

			IFlyable flyable = (IFlyable) entity;

			// Only flock with flying entities
			if (!flyable.isFlying()) continue;

			// If this mob is flockable, check if it can flock with the other entity
			if (canFlock) {
				if (!(entity instanceof IFlockable)) continue;
				IFlockable flockable = (IFlockable) entity;
				if (flockable.isSoloFlying()) continue;
				if (!((IFlockable) mob).canFlockWith(entity)) continue;
			}

			double dx = entity.x - mob.x;
			double dz = entity.z - mob.z;
			double distSq = dx * dx + dz * dz;

			if (distSq > neighborRadiusSq) continue;

			double dist = Math.sqrt(distSq);

			// SEPARATION: Move away from very close neighbors
			if (dist < SEPARATION_RADIUS) {
				double strength = (SEPARATION_RADIUS - dist) / SEPARATION_RADIUS;
				strength *= 0.08;
				flockVec = flockVec.add(-dx * strength, 0, -dz * strength);
			}

			// COHESION: Move toward the center of the flock
			cohesionVec = cohesionVec.add(dx, 0, dz);

			// ALIGNMENT: Match the heading of neighbors
			double rad = Math.toRadians(entity.yRot);
			alignmentSin += Math.sin(rad);
			alignmentCos += Math.cos(rad);

			neighborCount++;
		}

		// Apply cohesion
		if (neighborCount > 0) {
			cohesionVec = cohesionVec.scale(0.05 / neighborCount);
			flockVec = flockVec.add(cohesionVec.x, cohesionVec.y, cohesionVec.z);

			// Apply alignment - turn toward average heading
			double avgYaw = Math.atan2(alignmentSin / neighborCount, alignmentCos / neighborCount);
			float yawDiff = (float) Math.toDegrees(avgYaw) - mob.yRot;
			while (yawDiff < -180) yawDiff += 360;
			while (yawDiff > 180) yawDiff -= 360;
			mob.yRot += yawDiff * 0.1;
		}

		// HOME BIAS: If no neighbors, gently pull toward home position
		if (neighborCount == 0 && mob instanceof IHomeable) {
			IHomeable homeable = (IHomeable) mob;
			double dxHome = homeable.getHomeX() - mob.x;
			double dzHome = homeable.getHomeZ() - mob.z;
			flockVec = flockVec.add(dxHome * 0.002, 0, dzHome * 0.002);
		}

		// RANDOM WANDERING: Add some randomness
		if (random.nextInt(15) == 0) {
			double angle = Math.toRadians(random.nextDouble() * 360);
			flockVec = flockVec.add(Math.cos(angle) * 0.005, 0, Math.sin(angle) * 0.005);
		}

		// Compute velocity based on yaw + flocking vectors
		double radYaw = Math.toRadians(mob.yRot);
		double vx = Math.cos(radYaw) * BASE_FLIGHT_SPEED + flockVec.x;
		double vz = Math.sin(radYaw) * BASE_FLIGHT_SPEED + flockVec.z;

		// Clamp speed to max
		double speed = Math.sqrt(vx * vx + vz * vz);
		if (speed > BASE_FLIGHT_SPEED) {
			double scale = BASE_FLIGHT_SPEED / speed;
			vx *= scale;
			vz *= scale;
		}

		// Smooth velocity changes (damping)
		mob.xd = mob.xd * 0.7 + vx * 0.3;
		mob.zd = mob.zd * 0.7 + vz * 0.3;

		// Set forward movement
		mob.setMoveForward(BASE_FLIGHT_SPEED * 5);

		// VERTICAL FLIGHT: Maintain height in range
		double groundY = mob.getGroundY();
		double targetY = groundY + MIN_FLIGHT_HEIGHT + random.nextDouble() * (MAX_FLIGHT_HEIGHT - MIN_FLIGHT_HEIGHT);
		mob.yd += (targetY - mob.y) * 0.01;

		// Occasional upward boost
		if (mob.y < groundY + 20.0 && random.nextInt(6) == 0) {
			mob.yd += 0.25;
		}

		// Clamp vertical speed
		if (mob.yd > 0.45) mob.yd = 0.45;
		if (mob.yd < 0) mob.yd *= 0.6;
	}

	private double getGroundHeight() {
		int bx = MathHelper.floor(mob.x);
		int bz = MathHelper.floor(mob.z);
		int by = MathHelper.floor(mob.y);

		while (by > 0) {
			int blockId = mob.world.getBlockId(bx, by, bz);
			if (blockId != 0 && Blocks.blocksList[blockId] != null) {
				Block block = Blocks.blocksList[blockId];
				if (block.isCubeShaped()) {
					return by + 1.0;
				}
			}
			by--;
		}
		return by + 1.0;
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
	protected void onStop(Task interruptTask) {
		mob.setMoveForward(0);
	}

	@Override
	protected boolean isEqual(Task other) {
		return other instanceof FlightFlockingTask;
	}
}
