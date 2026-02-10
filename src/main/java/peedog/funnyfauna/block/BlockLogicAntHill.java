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
import peedog.funnyfauna.gui.MenuAntHill;

import java.util.List;
import java.util.Random;

public class BlockLogicAntHill extends BlockLogic {

	public BlockLogicAntHill(Block<?> block) {
		super(block, Material.sand);
		// Register the tile entity
		block.withEntity(TileEntityAntHill::new);
	}

	@Override
	public boolean onBlockRightClicked(World world, int x, int y, int z, Player player, Side side, double xPlaced, double yPlaced) {
		if (world.isClientSide) {
			return true;
		}

		TileEntity tileEntity = world.getTileEntity(x, y, z);
		if (tileEntity instanceof TileEntityAntHill) {
			TileEntityAntHill antHill = (TileEntityAntHill) tileEntity;

			// Open the chest screen with the tile entity (which implements Container)
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
	 * Periodically check for nearby ants to capture
	 * This provides an alternative to collision detection
	 */
	// Add this method to BlockLogicAntHill.java and call it from both collision and tick methods
	private void handleAntNearAntHill(World world, int x, int y, int z, EntityAnt ant) {
		// Get the tile entity
		TileEntity tileEntity = world.getTileEntity(x, y, z);
		if (!(tileEntity instanceof TileEntityAntHill)) return;

		TileEntityAntHill antHill = (TileEntityAntHill) tileEntity;

		// DEBUG: Log what's happening
		if (world.rand.nextInt(100) == 0) { // Only log occasionally to avoid spam
			System.out.println("Ant near ant hill at " + x + "," + y + "," + z);
			System.out.println("Ant has home: " + ant.hasHome());
			System.out.println("Ant hill count: " + antHill.getStoredAntCount() + "/" + antHill.getMaxAnts());
		}

		// If ant doesn't have a home, set this ant hill as its home
		if (!ant.hasHome()) {
			if (antHill.getStoredAntCount() < antHill.getMaxAnts()) {
				ant.setHome(x, y, z);
				System.out.println("Setting ant home to ant hill at " + x + "," + y + "," + z);
			}
			return; // Let the ant go out and forage first
		}

		// Only store ants that have this block as their home
		if (ant.getHomeX() == x && ant.getHomeY() == y && ant.getHomeZ() == z) {
			// Check if ant is close enough (within 1.5 blocks)
			double distanceSq = ant.getDistanceToHomeSq(x, y, z);
			if (distanceSq < 2.25) {
				// Try to store the ant
				if (antHill.storeAnt(ant)) {
					System.out.println("Stored ant in ant hill!");
				}
			}
		}
	}

	// Update onEntityCollidedWithBlock to use this method:
	@Override
	public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity) {
		// Only process on server side
		if (world.isClientSide) return;

		// Check if entity is an ant
		if (!(entity instanceof EntityAnt)) return;

		EntityAnt ant = (EntityAnt) entity;
		handleAntNearAntHill(world, x, y, z, ant);
	}

	// Update updateTick to use this method:
	@Override
	public void updateTick(World world, int x, int y, int z, Random rand) {
		if (world.isClientSide) return;

		// Get tile entity
		TileEntity tileEntity = world.getTileEntity(x, y, z);
		if (!(tileEntity instanceof TileEntityAntHill)) return;

		TileEntityAntHill antHill = (TileEntityAntHill) tileEntity;

		// If ant hill is full, don't bother checking
		if (antHill.getStoredAntCount() >= antHill.getMaxAnts()) return;

		// Search for nearby ants with a LARGER radius (8 blocks instead of 2)
		AABB searchBox = AABB.getTemporaryBB(
			x, y, z,
			x + 1, y + 1, z + 1
		).grow(8.0, 4.0, 8.0); // Increased from 2.0, 1.0, 2.0

		List<EntityAnt> nearbyAnts = world.getEntitiesWithinAABB(EntityAnt.class, searchBox);

		System.out.println("Found " + nearbyAnts.size() + " ants near ant hill at " + x + "," + y + "," + z);

		for (EntityAnt ant : nearbyAnts) {
			handleAntNearAntHill(world, x, y, z, ant);
		}
	}
}
