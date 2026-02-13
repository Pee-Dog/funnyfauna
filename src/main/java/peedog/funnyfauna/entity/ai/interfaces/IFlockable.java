package peedog.funnyfauna.entity.ai.interfaces;

import net.minecraft.core.entity.Entity;

/**
 * Interface for mobs that exhibit flocking behavior.
 * Allows mobs to determine if they should flock with other entities.
 */
public interface IFlockable {
	/**
	 * Determines if this mob can flock with another entity.
	 * Examples: same species, same variant, same color, etc.
	 *
	 * @param other The entity to check
	 * @return true if this mob should flock with the other entity
	 */
	boolean canFlockWith(Entity other);

	/**
	 * Whether this mob is currently in solo flight mode (not flocking).
	 * Used to exclude solo flyers from flock coordination.
	 *
	 * @return true if flying solo, false if available for flocking
	 */
	boolean isSoloFlying();

	/**
	 * Set whether this mob is in solo flight mode.
	 *
	 * @param solo true for solo flight, false for flock flight
	 */
	void setSoloFlying(boolean solo);
}
