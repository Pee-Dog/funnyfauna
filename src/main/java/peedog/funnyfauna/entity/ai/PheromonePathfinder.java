package peedog.funnyfauna.entity.ai;

import net.minecraft.core.world.World;
import net.minecraft.core.world.pathfinder.Node;
import peedog.funnyfauna.entity.ai.PheromoneManager;

import java.util.*;

public class PheromonePathfinder {
	private final World world;

	public PheromonePathfinder(World world) {
		this.world = world;
	}

	public List<ScentNode> findScentTrail(int startX, int startY, int startZ, int range) {
		PriorityQueue<ScentNode> openSet = new PriorityQueue<>();
		Map<Integer, Double> gScore = new HashMap<>();
		Map<Integer, ScentNode> cameFrom = new HashMap<>();

		ScentNode start = new ScentNode(startX, startY, startZ);
		openSet.add(start);
		gScore.put(start.hashCode(), 0.0);

		ScentNode bestEndNode = null;
		double highestUtility = -1.0;
		int iterations = 0;
		while (!openSet.isEmpty()) {
			iterations++;
			if (iterations > 100) break; // Safety cap to prevent StackOverflow/Hanging

			ScentNode current = openSet.poll();

			// Search neighbors
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					if (dx == 0 && dz == 0) continue;

					int nx = current.x + dx;
					int nz = current.z + dz;
					int ny = getValidY(nx, current.y, nz);
					if (ny == -1) continue;

					int strength = PheromoneManager.getStrength(nx, ny, nz);
					if (strength <= 0) continue;

					// A* Cost calculation
					// We want: High Strength = Low Cost
					double moveCost = 1.0 + (200.0 / (double)strength);
					double tentativeG = gScore.getOrDefault(current.hashCode(), Double.MAX_VALUE) + moveCost;

					ScentNode neighbor = new ScentNode(nx, ny, nz);
					if (tentativeG < gScore.getOrDefault(neighbor.hashCode(), Double.MAX_VALUE)) {
						cameFrom.put(neighbor.hashCode(), new ScentNode(current.x, current.y, current.z));
						gScore.put(neighbor.hashCode(), tentativeG);
						neighbor.fScore = tentativeG;
						openSet.add(neighbor);

						// Utility: We want a node that is FAR AWAY and STRENGTHY
						double dist = Math.sqrt(Math.pow(nx-startX, 2) + Math.pow(nz-startZ, 2));
						double utility = dist * strength;

						if (utility > highestUtility && dist > 2.0) { // Must be at least 2 blocks away to prevent spinning
							highestUtility = utility;
							bestEndNode = neighbor;
						}
					}
				}
			}
			// Optimization: Stop if we've searched too far
			if (gScore.size() > 200) break;
		}

		if (bestEndNode == null) return null;

		// Reconstruct path
		List<ScentNode> path = new ArrayList<>();
		ScentNode curr = new ScentNode(bestEndNode.x, bestEndNode.y, bestEndNode.z);
		while (curr != null) {
			path.add(0, curr);
			curr = cameFrom.get(PheromonePathfinder.createHash(curr.x, curr.y, curr.z));
		}
		return path;
	}

	private int getValidY(int x, int y, int z) {
		if (world.isBlockNormalCube(x, y, z)) return y + 1;
		if (!world.isBlockNormalCube(x, y - 1, z)) return y - 1;
		return y;
	}

	public static int createHash(int x, int y, int z) {
		// Use the standard Minecraft hashing to prevent collisions
		int l = y & 255 | (x & 32767) << 8 | (z & 32767) << 24;
		if (x < 0) l |= Integer.MIN_VALUE;
		if (z < 0) l |= 32768;
		return l;
	}

	static class ScentNode implements Comparable<ScentNode> {
		int x, y, z;
		double fScore;
		ScentNode(int x, int y, int z) { this.x = x; this.y = y; this.z = z; }
		@Override public int compareTo(ScentNode o) { return Double.compare(this.fScore, o.fScore); }
		@Override public int hashCode() { return PheromonePathfinder.createHash(x, y, z); }
	}
}
