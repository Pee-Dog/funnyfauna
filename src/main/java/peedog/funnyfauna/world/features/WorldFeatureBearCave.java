package peedog.funnyfauna.world.features;

import java.util.Random;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.block.material.Material;
import net.minecraft.core.world.World;
import net.minecraft.core.world.generate.feature.WorldFeature;

public class WorldFeatureBearCave extends WorldFeature {

	public boolean place(World world, Random random, int x, int y, int z) {
		// Descend to the first solid surface block
		while (y > 2 && world.isAirBlock(x, y, z)) {
			--y;
		}
		if (y <= 2) return false;

		int tunnelLength = 9 + random.nextInt(4); // slightly longer: 9-12 blocks

		// --- Pick the direction that goes most deeply under terrain ---
		// Sample 8 candidate directions. For each, score it by summing how far the
		// terrain surface is above the tunnel path at the midpoint and back end.
		// The direction with the highest score (most overhead terrain) wins.
		int numCandidates = 8;
		double bestScore = -1;
		double bestFacingX = 1;
		double bestFacingZ = 0;

		for (int i = 0; i < numCandidates; i++) {
			double angle = (i / (double) numCandidates) * Math.PI * 2.0;
			double fx = Math.cos(angle);
			double fz = Math.sin(angle);

			double score = 0;

			int midStep = tunnelLength / 2;
			int midX = (int) Math.round(x - fx * midStep);
			int midZ = (int) Math.round(z - fz * midStep);
			int midTunnelY = (int) Math.round(y - 1.0 - (midStep * 0.15));
			score += world.getHeightValue(midX, midZ) - midTunnelY;

			int bkX = (int) Math.round(x - fx * tunnelLength);
			int bkZ = (int) Math.round(z - fz * tunnelLength);
			int bkTunnelY = (int) Math.round(y - 1.0 - (tunnelLength * 0.15));
			score += world.getHeightValue(bkX, bkZ) - bkTunnelY;

			if (score > bestScore) {
				bestScore = score;
				bestFacingX = fx;
				bestFacingZ = fz;
			}
		}

		// Add a small random jitter to the chosen angle so caves don't all align to
		// the same 8 compass headings when terrain is symmetrical
		double jitter = (random.nextDouble() - 0.5) * (Math.PI * 2.0 / numCandidates);
		double jitteredAngle = Math.atan2(bestFacingZ, bestFacingX) + jitter;
		double facingX = Math.cos(jitteredAngle);
		double facingZ = Math.sin(jitteredAngle);

		// Require at least 3 blocks of overhead coverage at midpoint and back end
		// using the best direction we found
		if (bestScore < 6) return false; // score is sum of two coverage values, so 6 = avg 3 each

		int backX = (int) Math.round(x - facingX * tunnelLength);
		int backZ = (int) Math.round(z - facingZ * tunnelLength);
		int backTunnelY = (int) Math.round(y - 1.0 - (tunnelLength * 0.15));

		// Abort if the back wall isn't solid
		if (!world.getBlockMaterial(backX, backTunnelY, backZ).isSolid()) return false;

		// Explicitly check that the room itself has solid coverage on all sides —
		// the room is slightly wider than the tunnel so check a 3x3 footprint around it.
		// Every column must have terrain surface above the room ceiling, no exceptions.
		double roomCheckCY = y - 1.5 - (tunnelLength * 0.15);
		int roomCeilingY = (int) Math.ceil(roomCheckCY + 1.8) + 1; // top of the room ellipsoid + margin
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				int sampleX = backX + dx;
				int sampleZ = backZ + dz;
				if (world.getHeightValue(sampleX, sampleZ) <= roomCeilingY) {
					return false;
				}
			}
		}

		// Abort if liquid is nearby at the back
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (world.getBlockMaterial(backX + dx, backTunnelY, backZ + dz).isLiquid()) {
					return false;
				}
			}
		}

		// --- Carve entrance ---
		double entCX = x - facingX * 1.0;
		double entCY = y - 0.5;
		double entCZ = z - facingZ * 1.0;
		carveEllipsoid(world, entCX, entCY, entCZ, 2.4, 2.0, 2.4);

		// --- Carve tunnel ---
		for (int step = 2; step <= tunnelLength - 1; step++) {
			double stepX = x - facingX * step;
			double stepY = y - 1.0 - (step * 0.15);
			double stepZ = z - facingZ * step;

			double taper = (step < tunnelLength - 2) ? 1.0 : 0.85;
			carveEllipsoid(world, stepX, stepY, stepZ, 1.8 * taper, 1.6 * taper, 1.8 * taper);
		}

		// --- Carve back room ---
		double roomCX = x - facingX * (tunnelLength - 0.5);
		double roomCY = y - 1.5 - (tunnelLength * 0.15);
		double roomCZ = z - facingZ * (tunnelLength - 0.5);
		carveEllipsoid(world, roomCX, roomCY, roomCZ, 2.6, 1.8, 2.6);

		// Scatter gravel/dirt on the floor
		decorateFloor(world, random, roomCX, roomCY, roomCZ, 2.6f);
		decorateFloor(world, random, x - facingX * (tunnelLength / 2.0), y - 1.0, z - facingZ * (tunnelLength / 2.0), 1.8f);

		return true;
	}

	private void carveEllipsoid(World world, double cx, double cy, double cz,
								double rx, double ry, double rz) {
		int minX = (int) Math.floor(cx - rx) - 1;
		int maxX = (int) Math.ceil(cx + rx) + 1;
		int minY = (int) Math.floor(cy - ry) - 1;
		int maxY = (int) Math.ceil(cy + ry) + 1;
		int minZ = (int) Math.floor(cz - rz) - 1;
		int maxZ = (int) Math.ceil(cz + rz) + 1;

		for (int bx = minX; bx <= maxX; bx++) {
			for (int by = minY; by <= maxY; by++) {
				for (int bz = minZ; bz <= maxZ; bz++) {
					double nx = (bx - cx) / rx;
					double ny = (by - cy) / ry;
					double nz = (bz - cz) / rz;
					if (nx * nx + ny * ny + nz * nz > 1.0) continue;

					int blockId = world.getBlockId(bx, by, bz);
					if (blockId == Blocks.BEDROCK.id()) continue;

					Material mat = world.getBlockMaterial(bx, by, bz);
					if (mat.isLiquid()) continue;
					if (mat.isSolid()) {
						world.setBlockWithNotify(bx, by, bz, 0);
					}
				}
			}
		}
	}

	private void decorateFloor(World world, Random random, double cx, double cy, double cz, float r) {
		int minX = (int) Math.floor(cx - r);
		int maxX = (int) Math.ceil(cx + r);
		int minZ = (int) Math.floor(cz - r);
		int maxZ = (int) Math.ceil(cz + r);
		int floorY = (int) Math.floor(cy - r);

		for (int bx = minX; bx <= maxX; bx++) {
			for (int bz = minZ; bz <= maxZ; bz++) {
				double nx = (bx - cx) / r;
				double nz = (bz - cz) / r;
				if (nx * nx + nz * nz > 0.9) continue;

				for (int by = floorY; by <= floorY + 3; by++) {
					if (world.isAirBlock(bx, by, bz)
						&& world.getBlockMaterial(bx, by - 1, bz).isSolid()
						&& random.nextInt(4) == 0) {
						int placeId = random.nextInt(4) == 0 ? Blocks.DIRT.id() : Blocks.GRAVEL.id();
						world.setBlock(bx, by - 1, bz, placeId);
						break;
					}
				}
			}
		}
	}
}
