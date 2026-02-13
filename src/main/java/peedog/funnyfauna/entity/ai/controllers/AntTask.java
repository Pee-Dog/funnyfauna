package peedog.funnyfauna.entity.ai.controllers;

import peedog.funnyfauna.entity.ai.*;
import peedog.funnyfauna.entity.ai.path.*;
import peedog.funnyfauna.entity.ai.path.follow.FollowPheromoneTask;
import peedog.funnyfauna.entity.ant.EntityAnt;

public class AntTask extends Task<EntityAnt> {
	private final FollowPheromoneTask pheromoneTask;
	private final WanderTask<EntityAnt> wanderTask;
	private final ReturnHomeTask<EntityAnt> homeTask;
	private final ItemCollectorTask<EntityAnt> itemCollectorTask;

	private int state = 0; // 0: Foraging, 1: Returning with item, 2: Returning without item
	private int forageTimer = 0;
	private int itemSearchTimer = 0;
	private static final int MAX_FORAGE_TIME = 200; // 10 seconds
	private static final int ITEM_SEARCH_INTERVAL = 20; // Search for items every 1 second

	public AntTask(EntityAnt mob) {
		super(mob);
		this.pheromoneTask = new FollowPheromoneTask(mob);
		this.wanderTask = new WanderTask<>(mob);
		this.homeTask = new ReturnHomeTask<>(mob);
		this.itemCollectorTask = new ItemCollectorTask<>(mob);
	}

	@Override
	protected void onStart() {
		this.state = 0;
		this.forageTimer = 0;
		this.itemSearchTimer = 0;
	}

	@Override
	public Task onTick() {
		// Check current item status
		boolean hasItem = mob.getHeldItem() != null;
		boolean hasHome = mob.hasHome();

		// Drop pheromones ONLY when:
		// 1. Returning home WITH an item (state 1)
		// 2. AND we actually have a home
		if (mob.tickCount % 10 == 0 && state == 1 && hasItem && hasHome) {
			PheromoneManager.addScent((int)mob.x, (int)mob.y, (int)mob.z);
		}

		// HOMELESS ANT BEHAVIOR
		// If ant doesn't have a home yet, just wander and collect items
		// The ant hill will set the home when the ant gets close
		if (!hasHome) {
			// If carrying an item, keep wandering with it until we find a home
			if (hasItem) {
				return wanderTask;
			}

			// Try to collect items while wandering
			if (itemCollectorTask.hasTarget()) {
				return itemCollectorTask;
			}

			// Periodically search for items
			itemSearchTimer++;
			if (itemSearchTimer >= ITEM_SEARCH_INTERVAL) {
				itemSearchTimer = 0;
				return itemCollectorTask;
			}

			// Otherwise just wander
			return wanderTask;
		}

		// HOMED ANT BEHAVIOR (normal behavior)

		// State 1 or 2: RETURNING HOME
		if (state == 1 || state == 2) {
			// Check if we're at home (within 2 blocks)
			double distanceToHomeSq = mob.getDistanceToHomeSq();
			if (distanceToHomeSq < 4.0D) { // 2^2 = 4
				// Ant reached home
				if (hasItem) {
					// Drop the item at home
					mob.dropItem(mob.getHeldItem(), 0.0F);
					mob.setHeldItem(null);
				}
				// Reset to foraging state
				state = 0;
				forageTimer = 0;
				return wanderTask; // Start wandering from home
			}
			// Still returning home
			return homeTask;
		}

		// State 0: FORAGING
		forageTimer++;
		itemSearchTimer++;

		// If we just picked up an item, switch to returning home
		if (hasItem) {
			state = 1;
			forageTimer = 0;
			return homeTask;
		}

		// Check if forage timeout reached
		if (forageTimer > MAX_FORAGE_TIME) {
			state = 2; // Return home without item
			forageTimer = 0;
			return homeTask;
		}

		// Foraging behavior priority:

		// Priority 1: Collect nearby items if the collector has a target
		if (itemCollectorTask.hasTarget()) {
			return itemCollectorTask;
		}

		// Priority 2: Follow pheromone trails to food sources
		if (pheromoneTask.hasPath()) {
			return pheromoneTask;
		}

		// Priority 3: Periodically search for items (every second)
		if (itemSearchTimer >= ITEM_SEARCH_INTERVAL) {
			itemSearchTimer = 0;
			return itemCollectorTask;
		}

		// Priority 4: Wander to explore new areas
		return wanderTask;
	}

	@Override
	protected void onStop(Task interruptTask) {
		this.forageTimer = 0;
	}

	@Override
	protected boolean isEqual(Task other) {
		return other instanceof AntTask;
	}
}
