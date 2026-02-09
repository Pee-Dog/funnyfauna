package peedog.funnyfauna.entity.ai.flight;

import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.block.material.Material;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.util.phys.AABB;
import peedog.funnyfauna.block.ServerBlockPos3D;
import peedog.funnyfauna.entity.MobTaskrunner;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.i.IFlockable;
import peedog.funnyfauna.entity.ai.i.IFlyable;

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

		// Random chance to attempt landing
		return this.random.nextInt(100) == 0;
	}

	private boolean tryInitiateLanding() {
		int flightTime = mob.getFlightTime();
		double landingY = -1;

		// After 10-20 seconds: prefer leaves
		if (flightTime > 200 && flightTime <= 400) {
			landingY = getLandingHeightOnLeaves();
		}
		// After 20+ seconds: any ground
		else if (flightTime > 400) {
			landingY = getLandingHeight();
		}

		if (landingY == -1) return false;

		// Set landing target on the task
		ServerBlockPos3D target = new ServerBlockPos3D(
			MathHelper.floor(mob.x),
			(int) landingY,
			MathHelper.floor(mob.z)
		);

		this.landingTarget = target;
		mob.setLanding(true);

		// Signal nearby flockmates to land too (if this mob is flockable)
		if (mob instanceof IFlockable) {
			alertFlockToLand(target);
		}

		return true;
	}

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
				// Find landing spot near this mob
				double nearbyY = getLandingHeightNear(entity);
				if (nearbyY != -1) {
					// If the entity has a FlightTask, set its landing target
					// Otherwise, just set the landing flag and hope for the best
					flyable.setLanding(true);

					// We can't directly access their FlightTask, so they'll need to
					// create their own landing target when they detect the landing flag
				}
			}
		}
	}

	private double getLandingHeightNear(Entity entity) {
		int bx = MathHelper.floor(entity.x);
		int bz = MathHelper.floor(entity.z);

		for (int by = MathHelper.floor(entity.y) - 1; by >= 0; by--) {
			int id = entity.world.getBlockId(bx, by, bz);
			if (id != 0) {
				Block block = Blocks.blocksList[id];
				if (block != null) {
					if (block.getMaterial() == Material.leaves || block.isCubeShaped()) {
						return by + 1.0;
					}
				}
			}
		}
		return -1;
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

	private double getLandingHeight() {
		int bx = MathHelper.floor(mob.x);
		int bz = MathHelper.floor(mob.z);

		for (int by = MathHelper.floor(mob.y) - 1; by >= 0; by--) {
			int id = mob.world.getBlockId(bx, by, bz);
			if (id != 0) {
				Block block = Blocks.blocksList[id];
				if (block != null) {
					if (block.getMaterial() == Material.leaves) return by + 1.0;
					if (block.isCubeShaped()) return by + 1.0;
				}
			}
		}
		return -1;
	}

	private double getLandingHeightOnLeaves() {
		int bx = MathHelper.floor(mob.x);
		int bz = MathHelper.floor(mob.z);

		for (int by = MathHelper.floor(mob.y) - 1; by >= 0; by--) {
			int id = mob.world.getBlockId(bx, by, bz);
			if (id != 0) {
				Block block = Blocks.blocksList[id];
				if (block != null && block.getMaterial() == Material.leaves) {
					return by + 1.0;
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
