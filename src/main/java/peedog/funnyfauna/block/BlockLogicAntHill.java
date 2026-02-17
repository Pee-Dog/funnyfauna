package peedog.funnyfauna.block;

import net.minecraft.core.block.Block;
import net.minecraft.core.block.BlockLogic;
import net.minecraft.core.block.entity.TileEntity;
import net.minecraft.core.block.material.Material;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.util.helper.Side;
import net.minecraft.core.util.phys.AABB;
import net.minecraft.core.world.World;
import peedog.funnyfauna.block.entity.TileEntityAntHill;
import peedog.funnyfauna.entity.ant.EntityAnt;

import java.util.List;
import java.util.Random;

public class BlockLogicAntHill extends BlockLogic {

	public BlockLogicAntHill(Block<?> block) {
		super(block, Material.sand);
		// Register the tile entity
		block.withEntity(TileEntityAntHill::new);
		// Schedule random ticks for ant detection
		block.setTicking(true);
	}

	@Override
	public boolean onBlockRightClicked(World world, int x, int y, int z, Player player, Side side, double xPlaced, double yPlaced) {
		if (world.isClientSide) {
			return true;
		}

		TileEntity tileEntity = world.getTileEntity(x, y, z);
		if (tileEntity instanceof TileEntityAntHill) {
			TileEntityAntHill antHill = (TileEntityAntHill) tileEntity;
			player.displayChestScreen(antHill, (double)x, (double)y, (double)z);
			return true;
		}

		return false;
	}

	/**
	 * Called when the block is broken - release all ants and drop items
	 */
	@Override
	public void onBlockRemoved(World world, int x, int y, int z, int data) {
		if (!world.isClientSide) {
			TileEntity tileEntity = world.getTileEntity(x, y, z);
			if (tileEntity instanceof TileEntityAntHill) {
				TileEntityAntHill antHill = (TileEntityAntHill) tileEntity;

				// Release all ants
				antHill.releaseAnts(world);

				// Drop all items
				for (int i = 0; i < antHill.getContainerSize(); i++) {
					if (antHill.getItem(i) != null) {
						world.dropItem(x, y, z, antHill.getItem(i));
					}
				}
			}
		}

		super.onBlockRemoved(world, x, y, z, data);
	}

	/**
	 * Handle an ant that is near the ant hill
	 */
	// Inside BlockLogicAntHill.java
	private void handleAntNearAntHill(World world, int x, int y, int z, EntityAnt ant) {
		TileEntity tileEntity = world.getTileEntity(x, y, z);
		if (!(tileEntity instanceof TileEntityAntHill)) return;

		TileEntityAntHill antHill = (TileEntityAntHill) tileEntity;

		// FIX: Physical distance to the block
		double dx = (x + 0.5) - ant.x;
		double dy = (y + 0.5) - ant.y;
		double dz = (z + 0.5) - ant.z;
		double distanceSq = dx * dx + dy * dy + dz * dz;

		if (!ant.hasHome()) {
			if (antHill.getStoredAntCount() < antHill.getMaxAnts() && distanceSq < 64.0) {
				ant.setHome(x, y, z);
			}
			return;
		}

		if (ant.getHomeX() == x && ant.getHomeY() == y && ant.getHomeZ() == z) {
			// ADD THIS LOG
			if (distanceSq < 10.0) { // Log when they get close
				System.out.println("[Hill Debug] Home-bound ant is nearby. Distance: " + Math.sqrt(distanceSq));
			}

			if (distanceSq < 4.0) {
				if (antHill.storeAnt(ant)) {
					System.out.println("[Hill Debug] SUCCESS: Ant entered the hill!");
				}
			}
		}
	}
	/**
	 * Called when an entity collides with this block
	 */
	@Override
	public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity) {
		// Only process on server side
		if (world.isClientSide) return;

		// Check if entity is an ant
		if (!(entity instanceof EntityAnt)) return;

		EntityAnt ant = (EntityAnt) entity;
		handleAntNearAntHill(world, x, y, z, ant);
	}

	/**
	 * Called periodically (random tick)
	 * This is CRITICAL for detecting nearby ants
	 */
	@Override
	public void updateTick(World world, int x, int y, int z, Random rand) {
		if (world.isClientSide) return;

		TileEntity tileEntity = world.getTileEntity(x, y, z);
		if (!(tileEntity instanceof TileEntityAntHill)) return;

		// Search radius of 16 blocks to find homeless ants
		AABB searchBox = AABB.getTemporaryBB(x, y, z, x + 1, y + 1, z + 1).grow(16.0, 6.0, 16.0);
		List<EntityAnt> nearbyAnts = world.getEntitiesWithinAABB(EntityAnt.class, searchBox);

		for (EntityAnt ant : nearbyAnts) {
			handleAntNearAntHill(world, x, y, z, ant);
		}

		// RECURSIVE TICK: Keep the hill "alive" every second
		world.scheduleBlockUpdate(x, y, z, this.id(), 20);
	}

	/**
	 * Called when the block is placed
	 */
	@Override
	public void onBlockPlacedByWorld(World world, int x, int y, int z) {
		if (!world.isClientSide) {
			// Start the tick updates
			world.scheduleBlockUpdate(x, y, z, this.id(), 20);
			System.out.println("Ant hill placed at " + x + "," + y + "," + z + " - starting ant detection");
		}
	}
}
