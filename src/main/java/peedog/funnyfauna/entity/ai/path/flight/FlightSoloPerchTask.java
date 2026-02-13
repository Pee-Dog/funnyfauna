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

/**
 * Handles solo flight when a mob is specifically looking for a perch.
 * This is a focused flight mode where the mob searches for leaves or suitable perching spots.
 * Uses vertical-first approach: rise to safe height, then move horizontally.
 */
public class FlightSoloPerchTask<T extends MobTaskrunner & IFlyable> extends Task<T> {
	private static final int SEARCH_RADIUS = 2;
	private static final int SEARCH_HEIGHT = 8;
	private static final double BIRD_HEIGHT = 0.5;
	private static final double BUFFER = 0.2;

	private ServerBlockPos3D perchTarget = null;
	private int searchCooldown = 0;
	private int headHitTicks = 0;
	private static final int MAX_HEAD_HIT_TICKS = 40;

	public FlightSoloPerchTask(T mob) {
		super(mob);
	}

	@Override
	protected void onStart() {
		perchTarget = null;
		searchCooldown = 0;
		headHitTicks = 0;
	}

	@Override
	protected Task onTick() {
		// Handle ceiling collision
		if (isHeadBlocked()) {
			headHitTicks++;

			if (mob.yd > 0) mob.yd = 0;

			if (!trySlideToAir() || headHitTicks > MAX_HEAD_HIT_TICKS) {
				// Abort perch attempt and land
				abortPerchSeek();
				return null;
			}
		} else {
			headHitTicks = 0;
		}

		// Search for perch periodically
		if (--searchCooldown <= 0) {
			searchCooldown = 40; // Search every 2 seconds
			if (perchTarget == null) {
				perchTarget = findNearbyPerch();
			}
		}

		if (perchTarget != null) {
			// Move toward perch using vertical-first approach
			moveTowardPerch();

			// Check if we've reached the perch
			if (isAtPerch()) {
				// Successfully reached perch - land here
				completeLanding();
				return null;
			}
		} else {
			// No perch found - wander while searching
			wanderWhileSearching();
		}

		return null;
	}

	private void moveTowardPerch() {
		double targetX = perchTarget.x + 0.5;
		double targetZ = perchTarget.z + 0.5;

		// Determine safe hover height: must be above any blocks above the perch
		int perchX = MathHelper.floor(targetX);
		int perchY = perchTarget.y;
		int perchZ = MathHelper.floor(targetZ);

		int safeY = perchY + 1; // base hover 1 block above leaf
		while (!mob.world.isAirBlock(perchX, safeY, perchZ) && safeY < mob.world.getHeightBlocks()) {
			safeY++; // rise until air
		}

		double targetY = safeY + BUFFER;

		// VERTICAL-FIRST APPROACH:
		// First, rise to targetY if not high enough
		if (mob.y < targetY) {
			mob.yd = Math.min(0.15, targetY - mob.y); // controlled ascent
			mob.xd = 0;
			mob.zd = 0;
		} else {
			// Horizontal motion once safely above perch
			double dx = targetX - mob.x;
			double dz = targetZ - mob.z;

			mob.xd = dx * 0.1;
			mob.zd = dz * 0.1;

			// Clamp horizontal speed
			double horizontalSpeed = Math.sqrt(mob.xd * mob.xd + mob.zd * mob.zd);
			double maxSpeed = 0.1;
			if (horizontalSpeed > maxSpeed) {
				mob.xd = mob.xd / horizontalSpeed * maxSpeed;
				mob.zd = mob.zd / horizontalSpeed * maxSpeed;
			}

			// Minor vertical adjustment
			double dy = targetY - mob.y;
			mob.yd = MathHelper.clamp(dy * 0.05, -0.05, 0.05);
		}
	}

	private boolean isAtPerch() {
		if (perchTarget == null) return false;

		double targetX = perchTarget.x + 0.5;
		double targetZ = perchTarget.z + 0.5;

		int safeY = perchTarget.y + 1;
		while (!mob.world.isAirBlock(perchTarget.x, safeY, perchTarget.z) && safeY < mob.world.getHeightBlocks()) {
			safeY++;
		}
		double targetY = safeY + BUFFER;

		double distXZ = Math.sqrt(Math.pow(targetX - mob.x, 2) + Math.pow(targetZ - mob.z, 2));
		double distY = Math.abs(targetY - mob.y);

		return distXZ < 0.15 && distY < 0.05;
	}

	private void completeLanding() {
		mob.setFlying(false);
		if (mob instanceof IFlockable) {
			((IFlockable) mob).setSoloFlying(false);
		}
		mob.xd = mob.yd = mob.zd = 0;
		perchTarget = null;
	}

	private void abortPerchSeek() {
		if (mob instanceof IFlockable) {
			((IFlockable) mob).setSoloFlying(false);
		}
		mob.setFlying(false);
		mob.yd = -0.15;
		headHitTicks = 0;
		perchTarget = null;
	}

	private ServerBlockPos3D findNearbyPerch() {
		int bx = MathHelper.floor(mob.x);
		int by = MathHelper.floor(mob.y);
		int bz = MathHelper.floor(mob.z);

		// Search for leaves above current position
		for (int dy = 1; dy <= SEARCH_HEIGHT; dy++) {
			for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
				for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
					int checkY = by + dy;
					int checkX = bx + dx;
					int checkZ = bz + dz;

					int id = mob.world.getBlockId(checkX, checkY, checkZ);
					if (id != 0) {
						Block block = Blocks.blocksList[id];
						if (block != null && block.getMaterial() == Material.leaves) {
							// Found leaves - return position on top
							return new ServerBlockPos3D(checkX, checkY, checkZ);
						}
					}
				}
			}
		}

		return null;
	}

	private void wanderWhileSearching() {
		// Gentle wandering while searching
		mob.xd += (random.nextDouble() - 0.5) * 0.02;
		mob.yd += (random.nextDouble() - 0.5) * 0.01;
		mob.zd += (random.nextDouble() - 0.5) * 0.02;

		// Damping
		mob.xd *= 0.95;
		mob.yd *= 0.95;
		mob.zd *= 0.95;
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
			{ 0.0, -0.4}
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
		perchTarget = null;
	}

	@Override
	protected boolean isEqual(Task other) {
		return other instanceof FlightSoloPerchTask;
	}
}
