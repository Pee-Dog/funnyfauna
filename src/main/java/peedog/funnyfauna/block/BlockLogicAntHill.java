package peedog.funnyfauna.block;

import com.mojang.nbt.tags.CompoundTag;
import com.mojang.nbt.tags.ListTag;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.BlockLogic;
import net.minecraft.core.block.entity.TileEntity;
import net.minecraft.core.block.material.Material;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.Mob;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.enums.EnumDropCause;
import net.minecraft.core.item.Item;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.item.Items;
import net.minecraft.core.util.helper.Side;
import net.minecraft.core.util.phys.AABB;
import net.minecraft.core.world.World;
import org.jetbrains.annotations.Nullable;
import peedog.funnyfauna.block.entity.TileEntityAntHill;
import peedog.funnyfauna.entity.ant.EntityAnt;

import java.util.List;
import java.util.Random;

public class BlockLogicAntHill extends BlockLogic {

	public BlockLogicAntHill(Block<?> block) {
		super(block, Material.sand);
		block.withEntity(TileEntityAntHill::new);
		block.setTicking(true);
	}

	// -------------------------------------------------------------------------
	// Block breaking
	// -------------------------------------------------------------------------

	/**
	 * The engine's break call order is:
	 *   1. Block set to air -> onBlockRemoved fires  (NO tool info available here)
	 *   2. harvestBlock -> dropBlockWithCause -> getBreakResult  (tool info available here)
	 *
	 * Therefore onBlockRemoved must NOT touch ants at all.
	 * All ant logic (pack or release) lives in getBreakResult, which receives
	 * the pre-captured tileEntity object whose storedAnts list is still intact.
	 *
	 * harvestBlock is only overridden to reroute the golden shovel as SILK_TOUCH.
	 */
	@Override
	public void harvestBlock(World world, Player player, int x, int y, int z, int meta, TileEntity tileEntity) {
		ItemStack heldItemStack = player.inventory.getCurrentItem();
		Item heldItem = heldItemStack != null ? Item.itemsList[heldItemStack.itemID] : null;

		if (isGoldenShovel(heldItem)) {
			// Reroute to silk touch path so getBreakResult packs the ants
			this.dropBlockWithCause(world, EnumDropCause.SILK_TOUCH, x, y, z, meta, tileEntity, player);
			return;
		}

		super.harvestBlock(world, player, x, y, z, meta, tileEntity);
	}

	/**
	 * This is where ALL ant decisions are made.
	 * By the time this runs, the block is already gone from the world, but the
	 * tileEntity parameter is the original object and still holds all ant data.
	 *
	 * SILK_TOUCH: pack ants into the dropped item's NBT — nothing is spawned.
	 * Everything else: release ants into the world, drop a plain block item.
	 */
	@Override
	public ItemStack @Nullable [] getBreakResult(World world, EnumDropCause dropCause, int meta, TileEntity tileEntity) {
		if (tileEntity instanceof TileEntityAntHill) {
			TileEntityAntHill antHill = (TileEntityAntHill) tileEntity;

			switch (dropCause) {
				case SILK_TOUCH: {
					ItemStack stack = new ItemStack(this);
					CompoundTag tag = new CompoundTag();
					ListTag antList = new ListTag();
					for (CompoundTag antData : antHill.getStoredAnts()) {
						antList.addTag(antData);
					}
					tag.put("StoredAnts", antList);
					stack.setData(tag);
					// Do NOT call releaseAnts — ants are packed into the item above
					return new ItemStack[]{stack};
				}
				case PROPER_TOOL:
				case WORLD:
				case EXPLOSION: {
					// Release ants into the world, then drop a plain block
					antHill.releaseAnts(world);
					return new ItemStack[]{new ItemStack(this)};
				}
				default:
					return null;
			}
		}
		return super.getBreakResult(world, dropCause, meta, tileEntity);
	}

	/**
	 * onBlockRemoved fires BEFORE harvestBlock/getBreakResult.
	 * It must NOT release ants — that is getBreakResult's job.
	 * It only drops the stored inventory items.
	 */
	@Override
	public void onBlockRemoved(World world, int x, int y, int z, int data) {
		if (!world.isClientSide) {
			TileEntity tileEntity = world.getTileEntity(x, y, z);
			if (tileEntity instanceof TileEntityAntHill) {
				TileEntityAntHill antHill = (TileEntityAntHill) tileEntity;
				for (int i = 0; i < antHill.getContainerSize(); i++) {
					ItemStack stack = antHill.getItem(i);
					if (stack != null) {
						world.dropItem(x, y, z, stack);
					}
				}
			}
		}
		super.onBlockRemoved(world, x, y, z, data);
	}

	// -------------------------------------------------------------------------
	// Block placement — restore stored ants into the new tile entity
	// -------------------------------------------------------------------------

	@Override
	public void onBlockPlacedByWorld(World world, int x, int y, int z) {
		if (!world.isClientSide) {
			world.scheduleBlockUpdate(x, y, z, this.id(), 20);
		}
	}

	/**
	 * When a player places an ant-hill item that has StoredAnts NBT,
	 * restore those ants into the freshly-created tile entity.
	 * The item is read here because it has not yet been decremented by the engine.
	 */
	@Override
	public void onBlockPlacedByMob(World world, int x, int y, int z, Side side, Mob mob, double xPlaced, double yPlaced) {
		super.onBlockPlacedByMob(world, x, y, z, side, mob, xPlaced, yPlaced);

		if (world.isClientSide) return;
		if (!(mob instanceof Player)) return;

		Player player = (Player) mob;
		ItemStack heldStack = player.inventory.getCurrentItem();
		if (heldStack == null || heldStack.getData() == null) return;

		CompoundTag tag = heldStack.getData();
		if (!tag.containsKey("StoredAnts")) return;

		TileEntity tileEntity = world.getTileEntity(x, y, z);
		if (!(tileEntity instanceof TileEntityAntHill)) return;

		TileEntityAntHill antHill = (TileEntityAntHill) tileEntity;
		ListTag antList = tag.getList("StoredAnts");
		for (int i = 0; i < antList.tagCount(); i++) {
			antHill.getStoredAnts().add((CompoundTag) antList.tagAt(i));
		}
		antHill.setChanged();

		world.scheduleBlockUpdate(x, y, z, this.id(), 20);
	}

	// -------------------------------------------------------------------------
	// Right-click to open inventory
	// -------------------------------------------------------------------------

	@Override
	public boolean onBlockRightClicked(World world, int x, int y, int z, Player player, Side side, double xPlaced, double yPlaced) {
		if (world.isClientSide) return true;

		TileEntity tileEntity = world.getTileEntity(x, y, z);
		if (!(tileEntity instanceof TileEntityAntHill)) return false;

		TileEntityAntHill antHill = (TileEntityAntHill) tileEntity;
		player.displayChestScreen(antHill, (double) x, (double) y, (double) z);
		return true;
	}

	// -------------------------------------------------------------------------
	// Ant detection (tick + collision)
	// -------------------------------------------------------------------------

	@Override
	public void updateTick(World world, int x, int y, int z, Random rand) {
		if (world.isClientSide) return;

		TileEntity tileEntity = world.getTileEntity(x, y, z);
		if (!(tileEntity instanceof TileEntityAntHill)) return;

		AABB searchBox = AABB.getTemporaryBB(x, y, z, x + 1, y + 1, z + 1).grow(16.0, 6.0, 16.0);
		List<EntityAnt> nearbyAnts = world.getEntitiesWithinAABB(EntityAnt.class, searchBox);
		for (EntityAnt ant : nearbyAnts) {
			handleAntNearAntHill(world, x, y, z, ant);
		}

		world.scheduleBlockUpdate(x, y, z, this.id(), 20);
	}

	@Override
	public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity) {
		if (world.isClientSide) return;
		if (!(entity instanceof EntityAnt)) return;

		EntityAnt ant = (EntityAnt) entity;
		handleAntNearAntHill(world, x, y, z, ant);
	}

	private void handleAntNearAntHill(World world, int x, int y, int z, EntityAnt ant) {
		TileEntity tileEntity = world.getTileEntity(x, y, z);
		if (!(tileEntity instanceof TileEntityAntHill)) return;

		TileEntityAntHill antHill = (TileEntityAntHill) tileEntity;
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
			if (distanceSq < 1.0) {
				antHill.storeAnt(ant);
			}
		}
	}

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

	private boolean isGoldenShovel(Item item) {
		return item != null && item == Items.TOOL_SHOVEL_GOLD;
	}
}
