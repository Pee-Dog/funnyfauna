package peedog.funnyfauna.entity.ant.util;

import net.minecraft.core.entity.Entity;
import net.minecraft.core.world.World;
import org.lwjgl.util.vector.Vector3f;

/**
 * Detects climbable surfaces near an entity by checking blocks in all directions.
 * Returns the surface normal (perpendicular vector to the surface).
 */
public class SurfaceDetector {

	private final IClimberEntity climber;
	private final Entity entity;
	private final World world;

	// Check distance from entity center
	private static final float CHECK_DISTANCE = 0.2f;

	// Directions to check (6 cardinal directions)
	private static final Vector3f[] DIRECTIONS = {
		new Vector3f(0, -1, 0),  // DOWN
		new Vector3f(0, 1, 0),   // UP
		new Vector3f(0, 0, -1),  // NORTH
		new Vector3f(0, 0, 1),   // SOUTH
		new Vector3f(-1, 0, 0),  // WEST
		new Vector3f(1, 0, 0)    // EAST
	};

	public SurfaceDetector(IClimberEntity climber, Entity entity, World world) {
		this.climber = climber;
		this.entity = entity;
		this.world = world;
	}

	/**
	 * Finds the nearest climbable surface and returns its normal vector.
	 * Returns null if no surface is found.
	 */
	public SurfaceInfo detectSurface() {
		Vector3f bestNormal = null;
		float bestDistance = Float.MAX_VALUE;
		int bestBlockX = 0, bestBlockY = 0, bestBlockZ = 0;

		// Entity center positions
		double ex = entity.x;
		double ey = entity.y + entity.bbHeight * 0.5;
		double ez = entity.z;

		// Offsets to check (center, slightly lower, slightly higher)
		double[] yOffsets = { 0, -0.25, 0.25 };

		// Directions to check (6 cardinal + diagonals for climbing up)
		Vector3f[] directions = {
			new Vector3f(0, -1, 0), // DOWN
			new Vector3f(0, 1, 0),  // UP
			new Vector3f(0, 0, -1), // NORTH
			new Vector3f(0, 0, 1),  // SOUTH
			new Vector3f(-1, 0, 0), // WEST
			new Vector3f(1, 0, 0),  // EAST
			new Vector3f(0.5f, 1, 0),  // diagonal forward+up
			new Vector3f(-0.5f, 1, 0),
			new Vector3f(0, 1, 0.5f),
			new Vector3f(0, 1, -0.5f)
		};

		for (double yOffset : yOffsets) {
			for (Vector3f dir : directions) {
				double checkX = ex + dir.x * CHECK_DISTANCE;
				double checkY = ey + yOffset + dir.y * CHECK_DISTANCE;
				double checkZ = ez + dir.z * CHECK_DISTANCE;

				int bx = (int) Math.floor(checkX);
				int by = (int) Math.floor(checkY);
				int bz = (int) Math.floor(checkZ);

				int blockId = world.getBlockId(bx, by, bz);

				if (!climber.canClimbOnBlock(blockId, bx, by, bz)) continue;

				float distance = getDistanceToBlockSurface(
					(float) ex, (float) ey, (float) ez, bx, by, bz, dir
				);

				if (distance < bestDistance) {
					bestDistance = distance;
					Vector3f n = new Vector3f(dir);
					n.normalise();
					bestNormal = new Vector3f(-n.x, -n.y, -n.z);

					bestBlockX = bx;
					bestBlockY = by;
					bestBlockZ = bz;
				}
			}
		}

		if (bestNormal != null) {
			return new SurfaceInfo(bestNormal, bestDistance, bestBlockX, bestBlockY, bestBlockZ);
		}
		return null;
	}

	/**
	 * Calculate distance from entity to block surface in a given direction.
	 */
	private float getDistanceToBlockSurface(float ex, float ey, float ez,
											int bx, int by, int bz,
											Vector3f direction) {
		// Calculate distance to block face
		float dx, dy, dz;

		if (direction.x > 0) {
			dx = bx - ex;
		} else if (direction.x < 0) {
			dx = ex - (bx + 1);
		} else {
			dx = 0;
		}

		if (direction.y > 0) {
			dy = by - ey;
		} else if (direction.y < 0) {
			dy = ey - (by + 1);
		} else {
			dy = 0;
		}

		if (direction.z > 0) {
			dz = bz - ez;
		} else if (direction.z < 0) {
			dz = ez - (bz + 1);
		} else {
			dz = 0;
		}

		return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	/**
	 * Holds information about a detected surface.
	 */
	public static class SurfaceInfo {
		public final Vector3f normal;
		public final float distance;
		public final int blockX, blockY, blockZ;

		public SurfaceInfo(Vector3f normal, float distance, int blockX, int blockY, int blockZ) {
			this.normal = normal;
			this.distance = distance;
			this.blockX = blockX;
			this.blockY = blockY;
			this.blockZ = blockZ;
		}
	}
}
