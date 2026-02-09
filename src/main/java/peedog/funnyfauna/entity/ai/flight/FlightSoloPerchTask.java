package peedog.funnyfauna.entity.ai.flight;

import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.block.material.Material;
import net.minecraft.core.util.helper.MathHelper;
import peedog.funnyfauna.block.ServerBlockPos3D;
import peedog.funnyfauna.entity.MobTaskrunner;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.i.IFlockable;
import peedog.funnyfauna.entity.ai.i.IFlyable;

/**
 * Handles solo flight when a mob is specifically looking for a perch.
 * This is a focused flight mode where the mob searches for leaves or suitable perching spots.
 */
public class FlightSoloPerchTask<T extends MobTaskrunner & IFlyable> extends Task<T> {
	private static final float PERCH_SEARCH_SPEED = 0.5F;
	private static final int SEARCH_RADIUS = 8;
	private static final int SEARCH_HEIGHT = 12;

	private ServerBlockPos3D perchTarget = null;
	private int searchCooldown = 0;

	public FlightSoloPerchTask(T mob) {
		super(mob);
	}

	@Override
	protected void onStart() {
		perchTarget = null;
		searchCooldown = 0;
	}

	@Override
	protected Task onTick() {
		// Search for perch periodically
		if (--searchCooldown <= 0) {
			searchCooldown = 40; // Search every 2 seconds
			perchTarget = findNearbyPerch();
		}

		if (perchTarget != null) {
			// Move toward perch
			moveTowardPerch();

			// Check if we've reached the perch
			if (isAtPerch()) {
				// Successfully reached perch - land here
				mob.setFlying(false);
				if (mob instanceof IFlockable) {
					((IFlockable) mob).setSoloFlying(false);
				}
				mob.setPerched(true);
				mob.xd = mob.yd = mob.zd = 0;
				return null;
			}
		} else {
			// No perch found - just fly around looking
			wanderWhileSearching();
		}

		return null;
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
							// Found leaves! Check if there's air above to perch
							if (mob.world.isAirBlock(checkX, checkY + 1, checkZ)) {
								return new ServerBlockPos3D(checkX, checkY + 1, checkZ);
							}
						}
					}
				}
			}
		}

		// No leaves found - try any solid ground
		for (int dy = -5; dy <= 5; dy++) {
			int checkY = by + dy;
			int id = mob.world.getBlockId(bx, checkY, bz);
			if (id != 0) {
				Block block = Blocks.blocksList[id];
				if (block != null && block.isCubeShaped()) {
					if (mob.world.isAirBlock(bx, checkY + 1, bz)) {
						return new ServerBlockPos3D(bx, checkY + 1, bz);
					}
				}
			}
		}

		return null;
	}

	private void moveTowardPerch() {
		double dx = perchTarget.x + 0.5 - mob.x;
		double dy = perchTarget.y - mob.y;
		double dz = perchTarget.z + 0.5 - mob.z;

		double horizontalDist = Math.sqrt(dx * dx + dz * dz);

		if (horizontalDist > 0.01) {
			// Move horizontally toward perch
			double speed = PERCH_SEARCH_SPEED * 0.1;
			mob.xd = (dx / horizontalDist) * speed;
			mob.zd = (dz / horizontalDist) * speed;
		}

		// Move vertically toward perch
		if (Math.abs(dy) > 0.5) {
			mob.yd = MathHelper.clamp(dy * 0.1, -0.2, 0.2);
		} else {
			mob.yd = dy * 0.05;
		}

		// Face movement direction
		mob.yRot = (float) (Math.atan2(mob.zd, mob.xd) * 180.0 / Math.PI) - 90.0F;
	}

	private void wanderWhileSearching() {
		// Random wandering while searching for perch
		mob.xd += (random.nextDouble() - 0.5) * 0.05;
		mob.yd += (random.nextDouble() - 0.5) * 0.03;
		mob.zd += (random.nextDouble() - 0.5) * 0.05;

		// Damping
		mob.xd *= 0.95;
		mob.yd *= 0.95;
		mob.zd *= 0.95;

		// Update yaw
		if (Math.abs(mob.xd) > 0.01 || Math.abs(mob.zd) > 0.01) {
			mob.yRot = (float) (Math.atan2(mob.zd, mob.xd) * 180.0 / Math.PI) - 90.0F;
		}
	}

	private boolean isAtPerch() {
		if (perchTarget == null) return false;

		double dx = perchTarget.x + 0.5 - mob.x;
		double dy = perchTarget.y - mob.y;
		double dz = perchTarget.z + 0.5 - mob.z;

		return Math.abs(dx) < 0.3 && Math.abs(dy) < 0.3 && Math.abs(dz) < 0.3;
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
