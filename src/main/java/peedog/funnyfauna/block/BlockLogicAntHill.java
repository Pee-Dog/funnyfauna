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
	private void handleAntNearAntHill(World world, int x, int y, int z, EntityAnt ant) {
		// Get the tile entity
		TileEntity tileEntity = world.getTileEntity(x, y, z);
		if (!(tileEntity instanceof TileEntityAntHill)) return;

		TileEntityAntHill antHill = (TileEntityAntHill) tileEntity;

		// Calculate distance
		double distanceSq = ant.getDistanceToHomeSq(x, y, z);

		// If ant doesn't have a home and is within 8 blocks, set this ant hill as its home
		if (!ant.hasHome()) {
			if (antHill.getStoredAntCount() < antHill.getMaxAnts() && distanceSq < 64.0) { // 8^2 = 64
				ant.setHome(x, y, z);
				System.out.println("Ant hill claiming homeless ant at distance " + Math.sqrt(distanceSq));
			}
			return; // Let the ant go out and forage
		}

		// Only store ants that have this block as their home
		if (ant.getHomeX() == x && ant.getHomeY() == y && ant.getHomeZ() == z) {
			// Check if ant is close enough to enter (within 2 blocks)
			if (distanceSq < 4.0) { // 2^2 = 4
				// Try to store the ant
				if (antHill.storeAnt(ant)) {
					System.out.println("Stored ant in ant hill!");
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

		// Get tile entity
		TileEntity tileEntity = world.getTileEntity(x, y, z);
		if (!(tileEntity instanceof TileEntityAntHill)) return;

		TileEntityAntHill antHill = (TileEntityAntHill) tileEntity;

		// Always search for nearby ants - we need to claim homeless ones
		// and store ants returning home

		// Large search radius to claim homeless ants
		AABB searchBox = AABB.getTemporaryBB(
			x, y, z,
			x + 1, y + 1, z + 1
		).grow(16.0, 6.0, 16.0); // Very large radius - 16 blocks

		List<EntityAnt> nearbyAnts = world.getEntitiesWithinAABB(EntityAnt.class, searchBox);

		if (nearbyAnts.size() > 0) {
			System.out.println("Ant hill tick: Found " + nearbyAnts.size() + " ants nearby");
		}

		for (EntityAnt ant : nearbyAnts) {
			handleAntNearAntHill(world, x, y, z, ant);
		}

		// Schedule next tick
		world.scheduleBlockUpdate(x, y, z, this.id(), 20); // Check every second
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
