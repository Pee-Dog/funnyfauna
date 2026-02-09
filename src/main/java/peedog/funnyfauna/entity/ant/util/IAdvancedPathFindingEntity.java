package peedog.funnyfauna.entity.ant.util;

/**
 * Interface for entities that use advanced pathfinding with directional path nodes.
 * Allows customization of pathing malus and obstacle handling.
 */
public interface IAdvancedPathFindingEntity {

	/**
	 * Called when the mob tries to move along the path but is obstructed
	 * @param facing The direction the mob was trying to move (0-5, Direction enum)
	 */
	default void onPathingObstructed(int facing) {
		// Override in implementing class
	}

	/**
	 * Returns how many ticks the mob can be stuck before the path is considered obstructed
	 * @return Maximum stuck check ticks (default 40)
	 */
	default int getMaxStuckCheckTicks() {
		return 40;
	}

	/**
	 * Returns the pathing malus for the given block and position.
	 * Negative values mean the node is impassable.
	 * 0.0 means highest priority/preferred.
	 * Positive values mean additional travel cost.
	 *
	 * @param blockId The block type
	 * @param x X coordinate
	 * @param y Y coordinate
	 * @param z Z coordinate
	 * @return Malus value
	 */
	default float getPathingMalus(int blockId, int x, int y, int z) {
		return 0.0f;
	}

	/**
	 * Called after the path finder has finished finding a path.
	 * Can be used to clear caches.
	 */
	default void pathFinderCleanup() {
		// Override in implementing class
	}
}
