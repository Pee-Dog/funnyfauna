package peedog.funnyfauna.entity.ai.path.flight;

import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.block.material.Material;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.util.phys.AABB;
import net.minecraft.core.util.phys.Vec3;
import peedog.funnyfauna.entity.MobTaskrunner;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.interfaces.IFlockable;
import peedog.funnyfauna.entity.ai.interfaces.IFlyable;
import peedog.funnyfauna.entity.ai.interfaces.IHomeable;

import java.util.List;

/**
 * Handles flocking flight behavior.
 * - Solo birds maintain cruising speed.
 * - Water is treated as ground for height checks.
 */
public class FlightFlockingTask<T extends MobTaskrunner & IFlyable> extends Task<T> {
	// Increased MIN_SPEED to match flocking speed
	private static final float MAX_SPEED = 0.6F;
	private static final float MIN_SPEED = 0.3F; // Raised from 0.15 to prevent sluggish solo flight
	private static final int MAX_FLIGHT_TIME = 600;

	// Flocking parameters
	private static final double NEIGHBOR_RADIUS = 8.0;
	private static final double SEPARATION_RADIUS = 3.0;

	// Weights
	private static final double WEIGHT_SEPARATION = 1.8;
	private static final double WEIGHT_ALIGNMENT = 1.0;
	private static final double WEIGHT_COHESION = 0.5;
	private static final double WEIGHT_WANDER = 0.3;
	private static final double WEIGHT_CRUISE = 0.5; // New force for solo speed

	// Height parameters
	private static final double MIN_FLIGHT_HEIGHT = 20.0; // Target ~20 blocks up
	private static final double MAX_FLIGHT_HEIGHT = 30.0;

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
		// Update ground reference (Includes water now)
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
			if (mob.yd > 0) mob.yd = 0;
			boolean escaped = trySlideToAir();

			if (!escaped || headHitTicks > MAX_HEAD_HIT_TICKS) {
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

		applyFlockingBehavior();

		return null;
	}

	private void applyFlockingBehavior() {
		// 1. GATHER NEIGHBORS
		List<Entity> nearbyEntities = mob.world.getEntitiesWithinAABB(
			Entity.class,
			AABB.getTemporaryBB(mob.x, mob.y, mob.z, mob.x + 1, mob.y + 1, mob.z + 1).grow(NEIGHBOR_RADIUS, 6, NEIGHBOR_RADIUS)
		);

		Vec3 separation = Vec3.getTempVec3(0, 0, 0);
		Vec3 alignment = Vec3.getTempVec3(0, 0, 0);
		Vec3 cohesion = Vec3.getTempVec3(0, 0, 0);

		int count = 0;
		double neighborRadiusSq = NEIGHBOR_RADIUS * NEIGHBOR_RADIUS;
		boolean canFlock = mob instanceof IFlockable;

		for (Entity entity : nearbyEntities) {
			if (entity == mob) continue;
			if (!(entity instanceof IFlyable)) continue;
			IFlyable flyable = (IFlyable) entity;
			if (!flyable.isFlying()) continue;

			if (canFlock) {
				if (!(entity instanceof IFlockable)) continue;
				IFlockable flockable = (IFlockable) entity;
				if (flockable.isSoloFlying()) continue;
				if (!((IFlockable) mob).canFlockWith(entity)) continue;
			}

			double dx = mob.x - entity.x;
			double dz = mob.z - entity.z;
			double distSq = dx * dx + dz * dz;

			if (distSq > neighborRadiusSq || distSq == 0) continue;
			double dist = Math.sqrt(distSq);

			// Separation
			if (dist < SEPARATION_RADIUS) {
				double strength = (SEPARATION_RADIUS - dist) / SEPARATION_RADIUS;
				separation = separation.add(dx / dist * strength, 0, dz / dist * strength);
			}

			// Alignment
			alignment = alignment.add(entity.xd, 0, entity.zd);

			// Cohesion
			cohesion = cohesion.add(entity.x, 0, entity.z);

			count++;
		}

		// 2. CALCULATE STEERING FORCES
		Vec3 steering = Vec3.getTempVec3(0, 0, 0);

		if (count > 0) {
			alignment = Vec3.getTempVec3(alignment.x / count, 0, alignment.z / count).normalize();

			cohesion = Vec3.getTempVec3(cohesion.x / count, 0, cohesion.z / count);
			cohesion = cohesion.add(-mob.x, 0, -mob.z).normalize();

			steering = steering.add(separation.x * WEIGHT_SEPARATION, 0, separation.z * WEIGHT_SEPARATION);
			steering = steering.add(alignment.x * WEIGHT_ALIGNMENT, 0, alignment.z * WEIGHT_ALIGNMENT);
			steering = steering.add(cohesion.x * WEIGHT_COHESION, 0, cohesion.z * WEIGHT_COHESION);
		} else {
			// Solo behavior: Home bias
			if (mob instanceof IHomeable) {
				IHomeable homeable = (IHomeable) mob;
				double dxHome = homeable.getHomeX() - mob.x;
				double dzHome = homeable.getHomeZ() - mob.z;
				double dist = Math.sqrt(dxHome*dxHome + dzHome*dzHome);
				if (dist > 1.0) {
					steering = steering.add((dxHome / dist) * 0.05, 0, (dzHome / dist) * 0.05);
				}
			}
		}

		// CRUISE CONTROL (Fix for slow solo birds)
		// If the bird has velocity, add a force in that direction to maintain momentum.
		// If it has no velocity (stopped), push it forward based on rotation.
		double currentSpeed = Math.sqrt(mob.xd * mob.xd + mob.zd * mob.zd);
		if (currentSpeed > 0.01) {
			steering = steering.add((mob.xd / currentSpeed) * WEIGHT_CRUISE, 0, (mob.zd / currentSpeed) * WEIGHT_CRUISE);
		} else {
			double rad = Math.toRadians(mob.yRot);
			steering = steering.add(-Math.sin(rad) * WEIGHT_CRUISE, 0, Math.cos(rad) * WEIGHT_CRUISE);
		}

		// WANDER
		double timeScale = mob.tickCount * 0.1;
		double wanderX = Math.sin(timeScale) * 0.5 + (random.nextDouble() - 0.5);
		double wanderZ = Math.cos(timeScale) * 0.5 + (random.nextDouble() - 0.5);
		steering = steering.add(wanderX * WEIGHT_WANDER, 0, wanderZ * WEIGHT_WANDER);

		// 3. APPLY FORCES
		mob.xd += steering.x * 0.05;
		mob.zd += steering.z * 0.05;

		// 4. LIMIT AND NORMALIZE SPEED
		double speedSq = mob.xd * mob.xd + mob.zd * mob.zd;
		if (speedSq > 0.0001) {
			double speed = Math.sqrt(speedSq);

			// Force speed into the Goldilocks zone (not too fast, not too slow)
			double targetSpeed = speed;
			if (speed > MAX_SPEED) targetSpeed = MAX_SPEED;
			if (speed < MIN_SPEED) targetSpeed = MIN_SPEED; // This ensures solo birds don't stall

			mob.xd = (mob.xd / speed) * targetSpeed;
			mob.zd = (mob.zd / speed) * targetSpeed;

			// Update rotation to face velocity
			double desiredYaw = Math.toDegrees(Math.atan2(mob.zd, mob.xd)) - 90.0F;
			mob.yRot = updateRotation(mob.yRot, (float)desiredYaw, 10.0F);
		}

		// VERTICAL FLIGHT
		double groundY = mob.getGroundY();
		double targetY = groundY + MIN_FLIGHT_HEIGHT + random.nextDouble() * (MAX_FLIGHT_HEIGHT - MIN_FLIGHT_HEIGHT);

		double dy = targetY - mob.y;

		// Stronger lift if below minimum height
		double liftStrength = (mob.y < groundY + MIN_FLIGHT_HEIGHT) ? 0.02 : 0.005;

		mob.yd += dy * liftStrength;

		if (mob.yd > 0.45) mob.yd = 0.45;
		if (mob.yd < -0.45) mob.yd = -0.45;

		mob.xd *= 0.99;
		mob.zd *= 0.99;
	}

	private float updateRotation(float current, float target, float maxChange) {
		float diff = target - current;
		while (diff < -180.0F) diff += 360.0F;
		while (diff >= 180.0F) diff -= 360.0F;
		if (diff > maxChange) diff = maxChange;
		if (diff < -maxChange) diff = -maxChange;
		return current + diff;
	}

	private double getGroundHeight() {
		int bx = MathHelper.floor(mob.x);
		int bz = MathHelper.floor(mob.z);
		int by = MathHelper.floor(mob.y);

		// Scan downwards
		while (by > 0) {
			int blockId = mob.world.getBlockId(bx, by, bz);
			if (blockId != 0) {
				Block block = Blocks.blocksList[blockId];
				if (block != null) {
					// Check for Solids OR Liquid
					// This prevents birds from diving into water
					if (block.isCubeShaped() || block.getMaterial() == Material.water || block.getMaterial() == Material.lava) {
						return by + 1.0;
					}
				}
			}
			by--;
		}
		return 1.0; // Default to bottom if void
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
			{ 0.4,  0.0}, {-0.4,  0.0}, { 0.0,  0.4}, { 0.0, -0.4},
			{ 0.4,  0.4}, {-0.4, -0.4}
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
