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
 *
 * Subclasses can override the hook methods to customise landing behaviour:
 *   {@link #isValidLandingBlock(Block)} - which blocks are valid surfaces
 *   {@link #prefersLeaves()}            - whether early flight targets leaf blocks
 *   {@link #canUseSoloPerch()}          - whether solo perch seeking is permitted
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

	// ================= TASK LIFECYCLE =================

	@Override
	protected void onStart() {
		if (!mob.isFlying()) {
			mob.setFlying(true);
			mob.setFlightTime(0);
			mob.setGroundY(getGroundHeight());
		}
	}

	@Override
	protected Task onTick() {
		mob.setFlightTime(mob.getFlightTime() + 1);

		// If landing flag is set but no target, try to create one
		if (mob.isLanding() && landingTarget == null) {
			ServerBlockPos3D target = findSafeLandingSpot(mob.getFlightTime());
			if (target != null) {
				landingTarget = target;
			} else {
				mob.setLanding(false);
			}
		}

		// Priority 1: Landing
		if (mob.isLanding()) {
			return landingTask;
		}

		// Priority 2: Solo perch seeking (opt-in per subclass)
		if (canUseSoloPerch() && mob instanceof IFlockable && ((IFlockable) mob).isSoloFlying()) {
			return soloPerchTask;
		}

		// Priority 3: Check if we should start landing
		if (shouldAttemptLanding()) {
			if (tryInitiateLanding()) {
				return landingTask;
			}
		}

		// Priority 4: Flocking flight (default)
		return flockingTask;
	}

	@Override
	protected void onStop(Task interruptTask) {
		// Don't cancel flight when handing off to LandingTask — the mob is still physically
		// airborne and needs to keep its flying hitbox until completeLanding() fires.
		if (interruptTask != null
			&& !(interruptTask instanceof FlightTask)
			&& !(interruptTask instanceof LandingTask)) {
			mob.setFlying(false);
			mob.setFlightTime(0);
		}
	}

	@Override
	protected boolean isEqual(Task other) {
		return other instanceof FlightTask;
	}

	// ================= LANDING LOGIC =================

	protected boolean shouldAttemptLanding() {
		int flightTime = mob.getFlightTime();
		if (flightTime < 200) return false;
		if (isOverInvalidGround()) return false;
		return this.random.nextInt(100) == 0;
	}

	private boolean tryInitiateLanding() {
		ServerBlockPos3D target = findSafeLandingSpot(mob.getFlightTime());
		if (target == null) return false;

		this.landingTarget = target;
		mob.setLanding(true);

		if (mob instanceof IFlockable) {
			alertFlockToLand(target);
		}

		return true;
	}

	protected ServerBlockPos3D findSafeLandingSpot(int flightTime) {
		int bx = MathHelper.floor(mob.x);
		int bz = MathHelper.floor(mob.z);

		double landingY = findLandingHeightAt(bx, bz, flightTime);
		if (landingY != -1) {
			return new ServerBlockPos3D(bx, (int) landingY, bz);
		}

		int[][] offsets = {
			{1, 0}, {-1, 0}, {0, 1}, {0, -1},
			{1, 1}, {-1, -1}, {1, -1}, {-1, 1},
			{2, 0}, {-2, 0}, {0, 2}, {0, -2}
		};

		for (int[] offset : offsets) {
			landingY = findLandingHeightAt(bx + offset[0], bz + offset[1], flightTime);
			if (landingY != -1) {
				return new ServerBlockPos3D(bx + offset[0], (int) landingY, bz + offset[1]);
			}
		}

		return null;
	}

	private double findLandingHeightAt(int bx, int bz, int flightTime) {
		if (prefersLeaves() && flightTime > 200 && flightTime <= 400) {
			return getLandingHeightOnLeavesAt(bx, bz);
		}
		if (flightTime > 200) {
			return getLandingHeightAt(bx, bz);
		}
		return -1;
	}

	private double getLandingHeightAt(int bx, int bz) {
		for (int by = MathHelper.floor(mob.y) - 1; by >= 0; by--) {
			int id = mob.world.getBlockId(bx, by, bz);
			if (id != 0) {
				Block block = Blocks.blocksList[id];
				if (block != null && isValidLandingBlock(block)) {
					return by + 1.0;
				}
			}
		}
		return -1;
	}

	private double getLandingHeightOnLeavesAt(int bx, int bz) {
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

	private double getGroundHeight() {
		int bx = MathHelper.floor(mob.x);
		int bz = MathHelper.floor(mob.z);
		int by = MathHelper.floor(mob.y);

		while (by > 0) {
			int blockId = mob.world.getBlockId(bx, by, bz);
			if (blockId != 0 && Blocks.blocksList[blockId] != null) {
				Block block = Blocks.blocksList[blockId];
				if (block.isCubeShaped() || block.getMaterial() == Material.leaves) {
					return by + 1.0;
				}
			}
			by--;
		}
		return by + 1.0;
	}

	/** Scans downward to check if the mob is currently over an invalid surface (water or lava). */
	private boolean isOverInvalidGround() {
		int bx = MathHelper.floor(mob.x);
		int bz = MathHelper.floor(mob.z);

		for (int by = MathHelper.floor(mob.y) - 1; by >= Math.max(0, MathHelper.floor(mob.y) - 30); by--) {
			int id = mob.world.getBlockId(bx, by, bz);
			if (id != 0) {
				Block block = Blocks.blocksList[id];
				if (block != null) {
					if (block.getMaterial() == Material.water || block.getMaterial() == Material.lava) {
						return true;
					}
					if (block.isCubeShaped() || block.getMaterial() == Material.leaves) {
						return false;
					}
				}
			}
		}
		return false;
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

			if (!thisFlockable.canFlockWith(entity)) continue;

			if (flyable.isFlying() && !flockable.isSoloFlying() && !flyable.isLanding()) {
				flyable.setLanding(true);
			}
		}
	}

	// ================= HOOKS FOR SUBCLASSES =================

	/**
	 * Whether this block is a valid surface to land on.
	 * Default: any solid cube or leaf block that isn't water or lava.
	 */
	protected boolean isValidLandingBlock(Block block) {
		if (block.getMaterial() == Material.water || block.getMaterial() == Material.lava) return false;
		return block.isCubeShaped() || block.getMaterial() == Material.leaves;
	}

	/**
	 * Whether this task should prefer landing on leaves during early flight (200–400 ticks).
	 * Default: true (standard bird behaviour).
	 */
	protected boolean prefersLeaves() {
		return true;
	}

	/**
	 * Whether this task may delegate to {@link FlightSoloPerchTask}.
	 * Default: true.
	 */
	protected boolean canUseSoloPerch() {
		return true;
	}
}
