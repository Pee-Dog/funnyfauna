package peedog.funnyfauna.entity.ai.controllers;

import net.minecraft.core.block.entity.TileEntity;
import peedog.funnyfauna.block.entity.TileEntityAntHill;
import peedog.funnyfauna.entity.ai.*;
import peedog.funnyfauna.entity.ai.path.*;
import peedog.funnyfauna.entity.ant.EntityAnt;

public class AntTask extends Task<EntityAnt> {

	private final WanderTask<EntityAnt> wanderTask;
	private final ReturnHomeTask<EntityAnt> homeTask;
	private final ItemCollectorTask<EntityAnt> itemCollectorTask;
	private final WanderNearHomeTask<EntityAnt> wanderNearHomeTask;

	// 0 = Forage, 1 = Return with item, 2 = Return empty
	private int state = 0;
	private int forageTimer = 0;
	private int maxForageTime = 200; // randomized on start

	public AntTask(EntityAnt mob) {
		super(mob);
		this.wanderTask = new WanderTask<>(mob);
		this.homeTask = new ReturnHomeTask<>(mob);
		this.itemCollectorTask = new ItemCollectorTask<>(mob);
		this.wanderNearHomeTask = new WanderNearHomeTask<>(mob, 12); // 12 block radius

	}

	@Override
	protected void onStart() {
		state = 0;
		forageTimer = 0;
		// Randomize how long this ant forages before heading home (200–400 ticks)
		maxForageTime = 200 + mob.world.rand.nextInt(201);
	}

	@Override
	public Task onTick() {
		boolean hasItem = mob.getHeldItem() != null;
		boolean hasHome = mob.hasHome();

		// --- 1. Hill entry check (only when in return state and cooldown is expired) ---
		if (hasHome && (state == 1 || state == 2) && mob.exitCooldown <= 0) {
			if (mob.getDistanceToHomeSq() < 2.5) {
				TileEntity te = mob.world.getTileEntity(mob.getHomeX(), mob.getHomeY(), mob.getHomeZ());
				if (te instanceof TileEntityAntHill) {
					TileEntityAntHill hill = (TileEntityAntHill) te;
					if (hill.storeAnt(mob)) {
						return null; // Ant removed from world; task ends
					}
				}
				// Hill full or missing — reset to foraging so the ant doesn't get stuck
				state = 0;
				forageTimer = 0;
			}
		}

		// --- 2. State transitions ---
		if (hasHome) {
			if (state == 0) {
				forageTimer++;
				if (hasItem) {
					state = 1; // Picked something up, head home
					forageTimer = 0;
				} else if (forageTimer > maxForageTime) {
					state = 2; // Timed out, head home empty-handed
					forageTimer = 0;
					// Pick a fresh forage time for the next trip
					maxForageTime = 200 + mob.world.rand.nextInt(201);
				}
			}
		} else {
			// Homeless ants always forage
			state = 0;
		}

		// --- 3. Task selection ---

		// Returning home takes priority
		if (hasHome && (state == 1 || state == 2)) {
			return homeTask;
		}

		// Collect nearby items while foraging
		if (state == 0 && !hasItem) {
			itemCollectorTask.onTick();
			if (itemCollectorTask.hasTarget()) {
				return itemCollectorTask;
			}
		}

		// Default: wander (near home if we have one)
		if (hasHome) {
			return wanderNearHomeTask;
		}

		return wanderTask;

	}

	@Override
	protected void onStop(Task interruptTask) {
		forageTimer = 0;
	}

	@Override
	protected boolean isEqual(Task other) {
		return other instanceof AntTask;
	}
}
