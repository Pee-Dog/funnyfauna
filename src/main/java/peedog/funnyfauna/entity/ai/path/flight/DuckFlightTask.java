package peedog.funnyfauna.entity.ai.path.flight;

import net.minecraft.core.block.material.Material;
import net.minecraft.core.util.helper.MathHelper;
import peedog.funnyfauna.block.ServerBlockPos3D;
import peedog.funnyfauna.entity.bird.MobBird;
import peedog.funnyfauna.entity.duck.MobDuck;

public class DuckFlightTask extends FlightTask<MobDuck> {

	public DuckFlightTask(MobDuck mob) {
		super(mob);
	}

	@Override
	protected boolean shouldAttemptLanding() {
		int flightTime = mob.getFlightTime();
		if (flightTime < 200) return false;
		// Ducks can land over water, so we skip the isOverWater() check.
		return this.random.nextInt(100) == 0;
	}

	@Override
	protected ServerBlockPos3D findSafeLandingSpot(int flightTime) {
		int bx = MathHelper.floor(mob.x);
		int bz = MathHelper.floor(mob.z);

		// First, try current position
		double landingY = getLandingHeightAt(bx, bz, flightTime);
		if (landingY != -1) {
			return new ServerBlockPos3D(bx, (int) landingY, bz);
		}

		// Try nearby positions
		int[][] offsets = {
			{1, 0}, {-1, 0}, {0, 1}, {0, -1},
			{1, 1}, {-1, -1}, {1, -1}, {-1, 1},
			{2, 0}, {-2, 0}, {0, 2}, {0, -2}
		};

		for (int[] offset : offsets) {
			int checkX = bx + offset[0];
			int checkZ = bz + offset[1];
			landingY = getLandingHeightAt(checkX, checkZ, flightTime);
			if (landingY != -1) {
				return new ServerBlockPos3D(checkX, (int) landingY, checkZ);
			}
		}
		return null;
	}

	// Unified method that returns Y of a valid landing block, or -1 if none.
	private double getLandingHeightAt(int bx, int bz, int flightTime) {
		// Search downward from current Y
		for (int by = MathHelper.floor(mob.y) - 1; by >= 0; by--) {
			int id = mob.world.getBlockId(bx, by, bz);
			if (id == 0) continue;

			net.minecraft.core.block.Block block = net.minecraft.core.block.Blocks.blocksList[id];
			if (block == null) continue;

			Material material = block.getMaterial();

			// Ducks prefer water
			if (material == Material.water) {
				// Allow landing on liquids (duck will float)
				return by + 1.0;
			}

			// For non‑liquid blocks, only land on solid ground
			if (block.isCubeShaped()) {
				// If flightTime < 400, prefer ground
				if (flightTime <= 400) {
					continue; // not leaves, skip for early landing
				}
				return by + 1.0;
			}
		}
		return -1;
	}
}
