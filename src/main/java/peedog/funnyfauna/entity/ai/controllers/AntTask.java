package peedog.funnyfauna.entity.ai.controllers;

import peedog.funnyfauna.entity.ai.*;
import peedog.funnyfauna.entity.ant.EntityAnt;

public class AntTask extends Task<EntityAnt> {
	private final FollowPheromoneTask pheromoneTask;
	private final WanderTask<EntityAnt> wanderTask;
	private final ReturnHomeTask<EntityAnt> homeTask;
	private final ItemCollectorTask<EntityAnt> itemCollectorTask;

	private int state = 0; // 0: Foraging, 1: Returning with item, 2: Returning without item
	private int forageTimer = 0;
	private boolean hasItem = false;
	private int stuckTimer = 0;
	private int lastX, lastY, lastZ;

	public AntTask(EntityAnt mob) {
		super(mob);
		this.pheromoneTask = new FollowPheromoneTask(mob);
		this.wanderTask = new WanderTask<>(mob);
		this.homeTask = new ReturnHomeTask<>(mob);
		this.itemCollectorTask = new ItemCollectorTask<>(mob);
	}

	@Override
	protected void onStart() {
		// Nothing to do on start
	}

	@Override
	public Task onTick() {
		// Check if stuck
		int currentX = (int)mob.x;
		int currentY = (int)mob.y;
		int currentZ = (int)mob.z;

		if (currentX == lastX && currentY == lastY && currentZ == lastZ) {
			stuckTimer++;
			if (stuckTimer > 40) { // 2 seconds stuck
				// Force state change to break out
				state = 2;
				forageTimer = 0;
				stuckTimer = 0;
			}
		} else {
			stuckTimer = 0;
		}
		lastX = currentX;
		lastY = currentY;
		lastZ = currentZ;

		// 1. Check if carrying item
		hasItem = mob.getHeldItem() != null;

		// 2. Update Scents: ONLY drop pheromones when returning home WITH an item (state 1)
		if (mob.tickCount % 10 == 0 && state == 1 && hasItem) {
			PheromoneManager.addScent((int)mob.x, (int)mob.y, (int)mob.z);
		}

		// 3. If carrying item, ALWAYS return home immediately
		if (hasItem && state != 1) {
			state = 1;
			forageTimer = 0;
			return homeTask;
		}

		// 4. State Logic
		if (state == 1 || state == 2) { // RETURNING HOME
			// Check if we're close to home (within 2 blocks)
			double distanceToHomeSq = mob.getDistanceToHomeSq();
			if (distanceToHomeSq < 4.0D) { // 2^2 = 4
				// Ant is at home
				if (hasItem) {
					// Drop item at home
					mob.dropItem(mob.getHeldItem(), 0.0F);
					mob.setHeldItem(null);
					hasItem = false;
				}
				state = 0;
				forageTimer = 0;
				return wanderTask; // Start wandering from home
			}
			return homeTask;
		}

		// 5. FORAGING LOGIC (state == 0)
		forageTimer++;

		// Force return home after timeout (10 seconds at 20 ticks/second = 200 ticks)
		if (mob.hasHome() && forageTimer > 200) {
			state = hasItem ? 1 : 2;
			return homeTask;
		}

		// 6. Try to collect items first
		if (!hasItem) {
			if (itemCollectorTask != null) {
				// Check if we actually got an item
				if (mob.getHeldItem() != null) {
					hasItem = true;
					state = 1; // Return home with item
					return homeTask;
				}
			}
		}

		// 7. Try following existing trails (only if not carrying item)
		if (!hasItem) {
			if (pheromoneTask.hasPath()) {
				return pheromoneTask;
			}
		}

		// 8. Wander to explore new areas
		return wanderTask;
	}

	@Override
	protected void onStop(Task interruptTask) {
		// Clean up when task stops
		forageTimer = 0;
		stuckTimer = 0;
	}

	@Override
	protected boolean isEqual(Task other) {
		return other instanceof AntTask;
	}
}
