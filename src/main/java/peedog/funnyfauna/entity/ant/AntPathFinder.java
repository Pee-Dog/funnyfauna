package peedog.funnyfauna.entity.ant;

import net.minecraft.core.world.World;

import java.util.*;

public class AntPathFinder {

	private final World world;

	public AntPathFinder(World world) {
		this.world = world;
	}

	/* ===================== Public API ===================== */

	public List<AntNode> findPath(int sx, int sy, int sz, AntNode.Face sFace,
								  int tx, int ty, int tz) {

		AntNode start = new AntNode(sx, sy, sz, sFace);
		AntNode goal = findSurfaceNode(tx, ty, tz);

		if (goal == null) return null;
		if (!isValidNode(start) || !isValidNode(goal)) return null;
		if (start.equals(goal)) return null;

		return aStar(start, goal);
	}

	/* ===================== A* ===================== */

	private List<AntNode> aStar(AntNode start, AntNode goal) {
		PriorityQueue<NodeRecord> open =
			new PriorityQueue<>(Comparator.comparingDouble(n -> n.f));

		Map<AntNode, NodeRecord> records = new HashMap<>();
		Set<AntNode> closed = new HashSet<>();

		NodeRecord startRec = new NodeRecord(start, null, 0, heuristic(start, goal));
		open.add(startRec);
		records.put(start, startRec);

		while (!open.isEmpty()) {
			NodeRecord current = open.poll();

			if (current.node.equals(goal))
				return reconstruct(current);

			closed.add(current.node);

			for (AntNode neighbor : getNeighbors(current.node)) {
				if (!isValidNode(neighbor)) continue;
				if (closed.contains(neighbor)) continue;

				double g = current.g + 1.0;
				NodeRecord rec = records.get(neighbor);

				if (rec == null || g < rec.g) {
					double h = heuristic(neighbor, goal);
					NodeRecord next = new NodeRecord(neighbor, current, g, g + h);
					records.put(neighbor, next);
					open.add(next);
				}
			}
		}
		return null;
	}

	/* ===================== Neighbor Generation ===================== */

	public List<AntNode> getNeighbors(AntNode n) {
		List<AntNode> out = new ArrayList<>();
		int x = n.x, y = n.y, z = n.z;

		switch (n.face) {

			/* ===== UP FACE ===== */
			case UP: {
				// move horizontally on top
				add(out, new AntNode(x + 1, y, z, AntNode.Face.UP));
				add(out, new AntNode(x - 1, y, z, AntNode.Face.UP));
				add(out, new AntNode(x, y, z + 1, AntNode.Face.UP));
				add(out, new AntNode(x, y, z - 1, AntNode.Face.UP));

				// step down onto side faces (tower edge case)
				int by = y - 1;
				if (isSolid(x, by, z)) {
					add(out, new AntNode(x + 1, by, z, AntNode.Face.EAST));
					add(out, new AntNode(x - 1, by, z, AntNode.Face.WEST));
					add(out, new AntNode(x, by, z + 1, AntNode.Face.SOUTH));
					add(out, new AntNode(x, by, z - 1, AntNode.Face.NORTH));
				}
				break;
			}

			/* ===== WALL FACES ===== */
			case EAST:
			case WEST:
			case NORTH:
			case SOUTH: {
				// climb up
				add(out, new AntNode(x, y + 1, z, n.face));

				// climb down ONLY if still supported
				add(out, new AntNode(x, y - 1, z, n.face));

				// transition to UP if possible
				add(out, new AntNode(x, y, z, AntNode.Face.UP));

				break;
			}

			/* ===== DOWN FACE (rare but supported) ===== */
			case DOWN: {
				add(out, new AntNode(x + 1, y, z, AntNode.Face.DOWN));
				add(out, new AntNode(x - 1, y, z, AntNode.Face.DOWN));
				add(out, new AntNode(x, y, z + 1, AntNode.Face.DOWN));
				add(out, new AntNode(x, y, z - 1, AntNode.Face.DOWN));
				break;
			}
		}

		return out;
	}

	private void add(List<AntNode> list, AntNode n) {
		if (isValidNode(n)) list.add(n);
	}

	/* ===================== Surface Validity ===================== */

	/**
	 * Enforces the physical rule:
	 * Every node must be attached to an actual solid block face.
	 */
	private boolean isValidNode(AntNode n) {
		switch (n.face) {
			case UP:
				return isSolid(n.x, n.y, n.z);

			case DOWN:
				return isSolid(n.x, n.y, n.z);

			case NORTH:
				return isSolid(n.x, n.y, n.z + 1);

			case SOUTH:
				return isSolid(n.x, n.y, n.z - 1);

			case EAST:
				return isSolid(n.x - 1, n.y, n.z);

			case WEST:
				return isSolid(n.x + 1, n.y, n.z);
		}
		return false;
	}

	/* ===================== Goal Discovery ===================== */

	public AntNode findSurfaceNode(int x, int y, int z) {
		for (int dy = 2; dy >= -4; dy--) {
			int ny = y + dy;
			if (isSolid(x, ny, z))
				return new AntNode(x, ny, z, AntNode.Face.UP);
		}
		return null;
	}

	/* ===================== Utils ===================== */

	private boolean isSolid(int x, int y, int z) {
		return world.getBlockMaterial(x, y, z).isSolidBlocking();
	}

	private double heuristic(AntNode a, AntNode b) {
		int dx = a.x - b.x;
		int dy = a.y - b.y;
		int dz = a.z - b.z;
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	private List<AntNode> reconstruct(NodeRecord end) {
		List<AntNode> path = new ArrayList<>();
		for (NodeRecord n = end; n != null; n = n.parent)
			path.add(n.node);
		Collections.reverse(path);
		return path.size() > 1 ? path : null;
	}

	private static class NodeRecord {
		final AntNode node;
		final NodeRecord parent;
		final double g, f;

		NodeRecord(AntNode node, NodeRecord parent, double g, double f) {
			this.node = node;
			this.parent = parent;
			this.g = g;
			this.f = f;
		}
	}
}
