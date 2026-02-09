package peedog.funnyfauna.entity.ant.util;

import net.minecraft.core.block.Block;
import org.apache.commons.lang3.tuple.Pair;
import org.jspecify.annotations.Nullable;
import org.lwjgl.util.vector.Vector3f;

import java.util.List;

/**
 * Interface for entities that can climb on surfaces.
 * Extends IAdvancedPathFindingEntity to provide climbing-specific pathfinding and movement.
 */
public interface IClimberEntity extends IAdvancedPathFindingEntity {

	/**
	 * Gets the attachment offset for the given axis
	 * @param axis The axis (0=X, 1=Y, 2=Z)
	 * @param partialTicks Partial ticks for interpolation
	 * @return Offset value
	 */
	default float getAttachmentOffset(int axis, float partialTicks) {
		return 0.0f;
	}

	/**
	 * Gets the vertical offset for rendering
	 * @param partialTicks Partial ticks for interpolation
	 * @return Offset value
	 */
	default float getVerticalOffset(float partialTicks) {
		return 0.0f;
	}

	/**
	 * Gets the current orientation of the climber
	 * @return Current orientation
	 */
	Orientation getOrientation();

	/**
	 * Calculates the orientation based on partial ticks
	 * @param partialTicks Partial ticks for interpolation
	 * @return Calculated orientation
	 */
	default Orientation calculateOrientation(float partialTicks) {
		return getOrientation();
	}

	/**
	 * Sets the rendering orientation (for client-side rendering)
	 * @param orientation The orientation to set
	 */
	default void setRenderOrientation(Orientation orientation) {
		// Override in implementing class
	}

	/**
	 * Gets the rendering orientation
	 * @return Rendering orientation, or null if none set
	 */
	@Nullable
	default Orientation getRenderOrientation() {
		return null;
	}

	/**
	 * Gets the movement speed of the climber
	 * @return Movement speed (0.0 - 1.0)
	 */
	default float getMovementSpeed() {
		return 0.1f;
	}

	/**
	 * Gets the ground direction and offset
	 * @return Pair of (direction, offset) where direction is 0-5 (DOWN, UP, NORTH, SOUTH, WEST, EAST)
	 */
	default Pair<Integer, Vector3f> getGroundDirection() {
		return Pair.of(0, new Vector3f(0, 0, 0)); // DOWN by default
	}

	/**
	 * Whether this climber should track pathing targets for debugging
	 * @return true if targets should be tracked
	 */
	default boolean shouldTrackPathingTargets() {
		return false;
	}

	/**
	 * Gets the tracked movement target (for debugging)
	 * @return Movement target or null
	 */
	@Nullable
	default Vector3f getTrackedMovementTarget() {
		return null;
	}

	/**
	 * Gets the list of tracked pathing targets (for debugging)
	 * @return List of pathing targets or null
	 */
	@Nullable
	default List<?> getTrackedPathingTargets() {
		return null;
	}

	/**
	 * Checks if the climber can climb on the given block
	 * @param blockId The block ID to check
	 * @param x X coordinate
	 * @param y Y coordinate
	 * @param z Z coordinate
	 * @return true if climbable
	 */
	boolean canClimbOnBlock(int blockId, int x, int y, int z);

	/**
	 * Gets the slipperiness of a block for climbing
	 * @param x X coordinate
	 * @param y Y coordinate
	 * @param z Z coordinate
	 * @return Slipperiness value (0.0 - 1.0)
	 */
	default float getBlockSlipperiness(int x, int y, int z) {
		return 0.6f;
	}

	/**
	 * Whether this climber can trigger walking animations/sounds
	 * @return true if walking should trigger events
	 */
	default boolean canClimberTriggerWalking() {
		return true;
	}

	/**
	 * Whether this climber can climb in water
	 * @return true if can climb in water
	 */
	default boolean canClimbInWater() {
		return false;
	}

	/**
	 * Sets whether the climber can climb in water
	 * @param value true to allow climbing in water
	 */
	default void setCanClimbInWater(boolean value) {
		// Override in implementing class
	}

	/**
	 * Whether this climber can climb in lava
	 * @return true if can climb in lava
	 */
	default boolean canClimbInLava() {
		return false;
	}

	/**
	 * Sets whether the climber can climb in lava
	 * @param value true to allow climbing in lava
	 */
	default void setCanClimbInLava(boolean value) {
		// Override in implementing class
	}

	/**
	 * Gets the collision inclusion range for obstacle detection
	 * @return Range in blocks
	 */
	default float getCollisionsInclusionRange() {
		return 0.5f;
	}

	/**
	 * Sets the collision inclusion range for obstacle detection
	 * @param range Range in blocks
	 */
	default void setCollisionsInclusionRange(float range) {
		// Override in implementing class
	}

	/**
	 * Gets the collision smoothing range
	 * @return Smoothing range
	 */
	default float getCollisionsSmoothingRange() {
		return 0.1f;
	}

	/**
	 * Sets the collision smoothing range
	 * @param range Smoothing range
	 */
	default void setCollisionsSmoothingRange(float range) {
		// Override in implementing class
	}

	/**
	 * Sets the jump direction for the climber
	 * @param dir Direction to jump, or null to clear
	 */
	default void setJumpDirection(@Nullable Vector3f dir) {
		// Override in implementing class
	}
}
