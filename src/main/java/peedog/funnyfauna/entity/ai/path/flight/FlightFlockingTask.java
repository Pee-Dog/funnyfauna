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
 * - Solo birds maintain speed using a stronger cruise engine.
 * - Flocks turn together using Coherent Noise (shared WorldTime reference).
 * - Includes Obstacle Avoidance for walls/mountains.
 */
public class FlightFlockingTask<T extends MobTaskrunner & IFlyable> extends Task<T> {
	// Speed settings
	private static final float MAX_SPEED = 0.6F;
	private static final float MIN_SPEED = 0.35F; // Increased slightly for solo consistency
	private static final int MAX_FLIGHT_TIME = 1200; // Increased flight duration

	// Flocking parameters
	private static final double NEIGHBOR_RADIUS = 10.0;
	private static final double SEPARATION_RADIUS = 2.5;

	// Steering Weights
	private static final double WEIGHT_SEPARATION = 2.5; // High priority to prevent stacking
	private static final double WEIGHT_ALIGNMENT = 1.2;
	private static final double WEIGHT_COHESION = 0.4;

	// New/Adjusted Weights
	private static final double WEIGHT_WANDER = 0.6; // Increased to make turns noticeable
	private static final double WEIGHT_CRUISE = 1.5; // TRIPLED: Ensures solo birds have "engine power"
	private static final double WEIGHT_OBSTACLE = 5.0; // Critical priority

	// Height parameters
	private static final double MIN_FLIGHT_HEIGHT = 20.0;
	private static final double MAX_FLIGHT_HEIGHT = 30.0;

	public FlightFlockingTask(T mob) {
		super(mob);
	}

	@Override
	protected void onStart() {
	}

	@Override
	protected Task onTick() {
		mob.setGroundY(getGroundHeight());

		// Basic flight limits
		if (mob.getFlightTime() > MAX_FLIGHT_TIME) {
			mob.setFlying(false);
			mob.yd = -0.1;
			return null;
		}

		// Handle ceiling collision (Head bonk)
		if (isHeadBlocked() && mob.yd > 0) {
			mob.yd = -0.1; // Bonk down
		}

		applyFlockingBehavior();

		return null;
	}

	private void applyFlockingBehavior() {
		Vec3 steering = Vec3.getTempVec3(0, 0, 0);

		// --- 1. GATHER FLOCK NEIGHBORS ---
		List<Entity> nearbyEntities = mob.world.getEntitiesWithinAABB(
			Entity.class,
			AABB.getTemporaryBB(mob.x, mob.y, mob.z, mob.x + 1, mob.y + 1, mob.z + 1).grow(NEIGHBOR_RADIUS, 8, NEIGHBOR_RADIUS)
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

			// Separation: Push away from neighbors
			if (dist < SEPARATION_RADIUS) {
				double strength = (SEPARATION_RADIUS - dist) / SEPARATION_RADIUS;
				separation = separation.add(dx / dist * strength, 0, dz / dist * strength);
			}

			// Alignment: Match velocity
			alignment = alignment.add(entity.xd, 0, entity.zd);

			// Cohesion: Steer toward center
			cohesion = cohesion.add(entity.x, 0, entity.z);

			count++;
		}

		// --- 2. CALCULATE STEERING FORCES ---

		if (count > 0) {
			// Normalize flocking vectors
			alignment = Vec3.getTempVec3(alignment.x / count, 0, alignment.z / count).normalize();

			cohesion = Vec3.getTempVec3(cohesion.x / count, 0, cohesion.z / count);
			cohesion = cohesion.add(-mob.x, 0, -mob.z).normalize();

			steering = steering.add(separation.x * WEIGHT_SEPARATION, 0, separation.z * WEIGHT_SEPARATION);
			steering = steering.add(alignment.x * WEIGHT_ALIGNMENT, 0, alignment.z * WEIGHT_ALIGNMENT);
			steering = steering.add(cohesion.x * WEIGHT_COHESION, 0, cohesion.z * WEIGHT_COHESION);
		} else {
			// Solo Home Bias
			if (mob instanceof IHomeable) {
				IHomeable homeable = (IHomeable) mob;
				double dxHome = homeable.getHomeX() - mob.x;
				double dzHome = homeable.getHomeZ() - mob.z;
				double dist = Math.sqrt(dxHome*dxHome + dzHome*dzHome);
				if (dist > 1.0) {
					steering = steering.add((dxHome / dist) * 0.1, 0, (dzHome / dist) * 0.1);
				}
			}
		}

		// --- 3. COHERENT WANDER (FIX FOR FLOCK TURNING) ---
		// Instead of random(), use WorldTime. This creates a "global wind" that affects all birds equally.
		// Result: The entire flock turns Left/Right simultaneously.
		double time = mob.world.getWorldTime() * 0.05; // 0.05 controls how fast the "wind" changes direction

		// Use two sine waves for more organic, less robotic movement
		double wanderDirX = Math.sin(time) + Math.sin(time * 0.3) * 0.5;
		double wanderDirZ = Math.cos(time) + Math.cos(time * 0.8) * 0.5;

		steering = steering.add(wanderDirX * WEIGHT_WANDER, 0, wanderDirZ * WEIGHT_WANDER);

		// --- 4. OBSTACLE AVOIDANCE (NEW) ---
		// Cast a ray 4 blocks ahead
		Vec3 look = Vec3.getTempVec3(mob.xd, 0, mob.zd).normalize();
		if (isCollidingAhead(look, 4.0)) {
			// If blocked, turn 90 degrees relative to current look
			// We check which way is open (left or right)
			// Simple version: Just turn hard right. The coherence noise will eventually smooth it out.
			steering = steering.add(-look.z * WEIGHT_OBSTACLE, 0, look.x * WEIGHT_OBSTACLE);
		}

		// --- 5. CRUISE CONTROL (FIX FOR SLOW SOLO BIRDS) ---
		// Always apply cruise force in the direction of current movement (or desired movement)
		// This acts as the "Engine" that ensures solo birds keep up with the flock
		if (mob.xd * mob.xd + mob.zd * mob.zd > 0.001) {
			Vec3 forward = Vec3.getTempVec3(mob.xd, 0, mob.zd).normalize();
			steering = steering.add(forward.x * WEIGHT_CRUISE, 0, forward.z * WEIGHT_CRUISE);
		} else {
			// If stalled, kickstart forward
			double rad = Math.toRadians(mob.yRot);
			steering = steering.add(-Math.sin(rad) * WEIGHT_CRUISE, 0, Math.cos(rad) * WEIGHT_CRUISE);
		}

		// --- 6. APPLY AND LIMIT ---
		// Apply force
		mob.xd += steering.x * 0.05;
		mob.zd += steering.z * 0.05;

		// Speed Limiting & Normalization
		double speedSq = mob.xd * mob.xd + mob.zd * mob.zd;
		if (speedSq > 0.0001) {
			double speed = Math.sqrt(speedSq);

			// We force speed towards the target MAX_SPEED
			// This ensures solo birds speed up to match the flock
			double targetSpeed = MAX_SPEED;

			// Allow them to slow down slightly if turning hard, but clamp bottom end
			if (speed < MIN_SPEED) targetSpeed = MIN_SPEED;
			if (speed > MAX_SPEED) targetSpeed = MAX_SPEED;

			mob.xd = (mob.xd / speed) * targetSpeed;
			mob.zd = (mob.zd / speed) * targetSpeed;

			// Update rotation
			double desiredYaw = Math.toDegrees(Math.atan2(mob.zd, mob.xd)) - 90.0F;
			mob.yRot = updateRotation(mob.yRot, (float)desiredYaw, 10.0F);
		}

		// --- 7. VERTICAL FLIGHT ---
		handleHeight();
	}

	private void handleHeight() {
		double groundY = mob.getGroundY();
		double ceilingY = getCeilingHeight();

		// Default height band relative to ground
		double desiredMin = groundY + MIN_FLIGHT_HEIGHT;
		double desiredMax = groundY + MAX_FLIGHT_HEIGHT;

		// If there is a ceiling close enough to matter, constrain the band below it
		final double CEILING_CLEARANCE = 3.0;
		if (ceilingY != Double.MAX_VALUE) {
			double safeMax = ceilingY - CEILING_CLEARANCE;
			desiredMax = Math.min(desiredMax, safeMax);
			// Ensure min never exceeds max (tight cave situation)
			desiredMin = Math.min(desiredMin, desiredMax - 1.0);
		}

		// Bobbing target within the allowed band, clamped so bobbing never breaks the ceiling limit
		double rawTarget = desiredMin + Math.sin(mob.tickCount * 0.1) * 2.0;
		double targetY = MathHelper.clamp(rawTarget, desiredMin, desiredMax);

		double dy = targetY - mob.y;
		double liftStrength = (mob.y < desiredMin) ? 0.03 : 0.01;

		mob.yd += dy * liftStrength;

		// Hard push away from ceiling if somehow inside clearance zone
		if (ceilingY != Double.MAX_VALUE && mob.y > ceilingY - CEILING_CLEARANCE) {
			mob.yd -= 0.05;
		}

		mob.yd = MathHelper.clamp(mob.yd, -0.4, 0.4);

		// Friction
		mob.xd *= 0.99;
		mob.zd *= 0.99;
	}

	/**
	 * Scans upward from the mob's head to find the nearest solid ceiling within 40 blocks.
	 * Returns Double.MAX_VALUE if no ceiling is found (open sky).
	 */
	private double getCeilingHeight() {
		int bx = MathHelper.floor(mob.x);
		int bz = MathHelper.floor(mob.z);
		int startY = MathHelper.floor(mob.y + mob.bbHeight + 0.1);

		for (int by = startY; by < startY + 40; by++) {
			int id = mob.world.getBlockId(bx, by, bz);
			if (id != 0 && Blocks.blocksList[id] != null && Blocks.blocksList[id].isCubeShaped()) {
				return by; // bottom face of the ceiling block
			}
		}
		return Double.MAX_VALUE;
	}

	private boolean isCollidingAhead(Vec3 dir, double dist) {
		// Start at eye height
		double startX = mob.x;
		double startY = mob.y + mob.bbHeight * 0.5;
		double startZ = mob.z;

		// Check end point
		int endX = MathHelper.floor(startX + dir.x * dist);
		int endY = MathHelper.floor(startY);
		int endZ = MathHelper.floor(startZ + dir.z * dist);

		int id = mob.world.getBlockId(endX, endY, endZ);
		if (id != 0) {
			Block b = Blocks.blocksList[id];
			return b != null && (b.isCubeShaped() || b.getMaterial() == Material.leaves);
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

	private double getGroundHeight() {
		int bx = MathHelper.floor(mob.x);
		int bz = MathHelper.floor(mob.z);
		int by = MathHelper.floor(mob.y);
		while (by > 0) {
			int blockId = mob.world.getBlockId(bx, by, bz);
			if (blockId != 0 && Blocks.blocksList[blockId] != null) {
				Block block = Blocks.blocksList[blockId];
				if (block.isCubeShaped() || block.getMaterial() == Material.water || block.getMaterial() == Material.lava) {
					return by + 1.0;
				}
			}
			by--;
		}
		return 1.0;
	}

	private boolean isHeadBlocked() {
		int headX = MathHelper.floor(mob.x);
		int headY = MathHelper.floor(mob.y + mob.bbHeight + 0.1);
		int headZ = MathHelper.floor(mob.z);
		int id = mob.world.getBlockId(headX, headY, headZ);
		return id != 0 && Blocks.blocksList[id] != null && Blocks.blocksList[id].isCubeShaped();
	}

	@Override
	protected void onStop(Task interruptTask) {}

	@Override
	protected boolean isEqual(Task other) {
		return other instanceof FlightFlockingTask;
	}
}
