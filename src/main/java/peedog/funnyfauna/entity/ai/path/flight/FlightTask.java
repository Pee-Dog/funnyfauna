package peedog.funnyfauna.entity.ai.path.flight;

import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.block.material.Material;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.util.phys.AABB;
import peedog.funnyfauna.block.ServerBlockPos3D;
import peedog.funnyfauna.entity.MobTaskrunner;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.interfaces.IFlockable;
import peedog.funnyfauna.entity.ai.interfaces.IFlyable;

import java.util.List;

/**
 * Compound task that manages all flight behaviors for flying mobs.
 * Delegates to subtasks: Flocking, Solo Perch Seeking, and Landing.
 */
public class FlightTask<T extends MobTaskrunner & IFlyable> extends Task<T> {
	private final FlightFlockingTask<T> flockingTask;
	private final FlightSoloPerchTask<T> soloPerchTask;
	private final LandingTask<T> landingTask;

	private ServerBlockPos3D landingTarget;

	public FlightTask(T mob) {
		super(mob);
		this.flockingTask = new FlightFlockingTask<>(mob);
		this.soloPerchTask = new FlightSoloPerchTask<>(mob);
		this.landingTask = new LandingTask<>(mob, this);
	}

	public ServerBlockPos3D getLandingTarget() {
		return landingTarget;
	}

	public void setLandingTarget(ServerBlockPos3D target) {
		this.landingTarget = target;
	}

	@Override
	protected void onStart() {
		// Initialize flight
		if (!mob.isFlying()) {
			mob.setFlying(true);
			mob.setFlightTime(0);
			mob.setGroundY(getGroundHeight());
		}
	}

	@Override
	protected Task onTick() {
		// Increment flight time
		mob.setFlightTime(mob.getFlightTime() + 1);

		// Fix Issue 1: If landing flag is set but no target, create one
		if (mob.isLanding() && landingTarget == null) {
			// Try to find a landing spot using the same logic
			ServerBlockPos3D target = findSafeLandingSpot(mob.getFlightTime());
			if (target != null) {
				landingTarget = target;
			} else {
				// Can't find landing spot, abort landing
				mob.setLanding(false);
			}
		}

		// Priority 1: Landing
		if (mob.isLanding()) {
			return landingTask;
		}

		// Priority 2: Solo perch seeking (if mob is IFlockable and flying solo)
		if (mob instanceof IFlockable && ((IFlockable) mob).isSoloFlying()) {
			return soloPerchTask;
		}

		// Priority 3: Check if we should start landing
		if (shouldAttemptLanding()) {
			if (tryInitiateLanding()) {
				return landingTask;
			}
		}

		// Priority 4: Flocking flight (default flying behavior)
		return flockingTask;
	}

	private boolean shouldAttemptLanding() {
		int flightTime = mob.getFlightTime();

		// Don't land immediately
		if (flightTime < 200) return false;

		// NEW: Don't even try to land if currently over water
		if (isOverWater()) return false;

		// Random chance to attempt landing
		return this.random.nextInt(100) == 0;
	}

	// Check if the bird is currently flying over water
	private boolean isOverWater() {
		int bx = MathHelper.floor(mob.x);
		int bz = MathHelper.floor(mob.z);

		// Scan downward from current position
		for (int by = MathHelper.floor(mob.y) - 1; by >= Math.max(0, MathHelper.floor(mob.y) - 30); by--) {
			int id = mob.world.getBlockId(bx, by, bz);
			if (id != 0) {
				Block block = Blocks.blocksList[id];
				if (block != null) {
					// Found water or lava - we're over it
					if (block.getMaterial() == Material.water || block.getMaterial() == Material.lava) {
						return true;
					}
					// Found solid ground or leaves - we're NOT over water
					if (block.isCubeShaped() || block.getMaterial() == Material.leaves) {
						return false;
					}
				}
			}
		}
		// Default to false if nothing found below
		return false;
	}

	private boolean tryInitiateLanding() {
		int flightTime = mob.getFlightTime();

		// First, try to find a valid landing spot (checking current and nearby positions)
		ServerBlockPos3D target = findSafeLandingSpot(flightTime);

		if (target == null) return false;

		// Set landing target on the task
		this.landingTarget = target;
		mob.setLanding(true);

		// Signal nearby flockmates to land too (if this mob is flockable)
		if (mob instanceof IFlockable) {
			alertFlockToLand(target);
		}

		return true;
	}

	// New method to find safe landing spot, checking multiple positions
	private ServerBlockPos3D findSafeLandingSpot(int flightTime) {
		// Try current position first
		int bx = MathHelper.floor(mob.x);
		int bz = MathHelper.floor(mob.z);

		double landingY = -1;

		// After 10-20 seconds: prefer leaves
		if (flightTime > 200 && flightTime <= 400) {
			landingY = getLandingHeightOnLeavesAt(bx, bz);
		}
		// After 20+ seconds: any ground (but not water)
		else if (flightTime > 400) {
			landingY = getLandingHeightAt(bx, bz);
		}

		if (landingY != -1) {
			return new ServerBlockPos3D(bx, (int) landingY, bz);
		}

		// Current position is bad (probably water), try nearby positions
		int[][] offsets = {
			{1, 0}, {-1, 0}, {0, 1}, {0, -1},
			{1, 1}, {-1, -1}, {1, -1}, {-1, 1},
			{2, 0}, {-2, 0}, {0, 2}, {0, -2}
		};

		for (int[] offset : offsets) {
			int checkX = bx + offset[0];
			int checkZ = bz + offset[1];

			if (flightTime > 200 && flightTime <= 400) {
				landingY = getLandingHeightOnLeavesAt(checkX, checkZ);
			} else if (flightTime > 400) {
				landingY = getLandingHeightAt(checkX, checkZ);
			}

			if (landingY != -1) {
				return new ServerBlockPos3D(checkX, (int) landingY, checkZ);
			}
		}

		// No valid landing spot found
		return null;
	}

	// Fix Issue 1: Properly notify flock members to land with targets
	private void alertFlockToLand(ServerBlockPos3D target) {
		IFlockable thisFlockable = (IFlockable) mob;

		List<Entity> nearbyEntities = mob.world.getEntitiesWithinAABB(
			Entity.class,
			AABB.getTemporaryBB(mob.x, mob.y, mob.z, mob.x + 1, mob.y + 1, mob.z + 1).grow(8, 6, 8)
		);

		for (Entity entity : nearbyEntities) {
			if (entity == mob) continue;
			if (!(entity instanceof IFlyable)) continue;
			if (!(entity instanceof IFlockable)) continue;

			IFlyable flyable = (IFlyable) entity;
			IFlockable flockable = (IFlockable) entity;

			// Check if this entity can flock with us
			if (!thisFlockable.canFlockWith(entity)) continue;

			// Check if they're flying and not solo and not already landing
			if (flyable.isFlying() && !flockable.isSoloFlying() && !flyable.isLanding()) {
				// Set their landing flag - they'll create their own target in their next tick
				flyable.setLanding(true);
			}
		}
	}

	private double getGroundHeight() {
		int bx = MathHelper.floor(mob.x);
		int bz = MathHelper.floor(mob.z);
		int by = MathHelper.floor(mob.y);

		while (by > 0) {
			int blockId = mob.world.getBlockId(bx, by, bz);
			if (blockId != 0 && Blocks.blocksList[blockId] != null) {
				Block block = Blocks.blocksList[blockId];
				if (block.isCubeShaped() && block.getMaterial() != Material.leaves) {
					return by + 1.0;
				}
				if (block.getMaterial() == Material.leaves) return by + 1.0;
			}
			by--;
		}
		return by + 1.0;
	}

	// Fix Issue 2: Avoid landing on water - now accepts position parameters
	private double getLandingHeightAt(int bx, int bz) {
		for (int by = MathHelper.floor(mob.y) - 1; by >= 0; by--) {
			int id = mob.world.getBlockId(bx, by, bz);
			if (id != 0) {
				Block block = Blocks.blocksList[id];
				if (block != null) {
					// Skip water and lava - don't land on them
					if (block.getMaterial() == Material.water || block.getMaterial() == Material.lava) {
						continue;
					}
					if (block.getMaterial() == Material.leaves) return by + 1.0;
					if (block.isCubeShaped()) return by + 1.0;
				}
			}
		}
		return -1;
	}

	// Fix Issue 2: Avoid landing on water - now accepts position parameters
	private double getLandingHeightOnLeavesAt(int bx, int bz) {
		for (int by = MathHelper.floor(mob.y) - 1; by >= 0; by--) {
			int id = mob.world.getBlockId(bx, by, bz);
			if (id != 0) {
				Block block = Blocks.blocksList[id];
				if (block != null) {
					// Skip water and lava
					if (block.getMaterial() == Material.water || block.getMaterial() == Material.lava) {
						continue;
					}
					if (block.getMaterial() == Material.leaves) {
						return by + 1.0;
					}
				}
			}
		}
		return -1;
	}

	@Override
	protected void onStop(Task interruptTask) {
		// Clean up flight state if interrupted
		if (interruptTask != null && !(interruptTask instanceof FlightTask)) {
			mob.setFlying(false);
			mob.setFlightTime(0);
		}
	}

	@Override
	protected boolean isEqual(Task other) {
		return other instanceof FlightTask;
	}
}
