// BlockLogicJarAnimal.java
package peedog.funnyfauna.block;

import net.minecraft.core.block.Block;
import net.minecraft.core.block.BlockLogic;
import net.minecraft.core.block.entity.TileEntity;
import net.minecraft.core.block.material.Material;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.enums.EnumDropCause;
import net.minecraft.core.item.Item;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.util.helper.Side;
import net.minecraft.core.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public abstract class BlockLogicJarAnimal extends BlockLogic {

	protected final Supplier<Item> itemSupplier;

	protected BlockLogicJarAnimal(
		Block<?> block,
		@NotNull Supplier<Item> itemSupplier
	) {
		super(block, Material.glass);
		this.itemSupplier = itemSupplier;

		this.setBlockBounds(
			0.3125F, 0.0F, 0.3125F,
			0.6875F, 0.5F, 0.6875F
		);
	}

	/* ===================== RENDERING ===================== */
	@Override
	public boolean isSolidRender() {
		return false;
	}

	@Override
	public boolean isCubeShaped() {
		return false;
	}

	/* ===================== INTERACTION ===================== */
	@Override
	public boolean onBlockRightClicked(
		World world, int x, int y, int z,
		Player player, Side side,
		double xHit, double yHit
	) {
		world.setBlockWithNotify(x, y, z, 0);
		world.playSoundAtEntity(player, player, "item.pickup", 1.0F, 1.0F);

		if (!world.isClientSide) {
			world.dropItem(x, y, z, new ItemStack(itemSupplier.get(), 1, 0));
		}

		return true;
	}

	/* ===================== BREAKING ===================== */
	@Override
	public ItemStack[] getBreakResult(
		World world, EnumDropCause dropCause, int meta, TileEntity tileEntity
	) {
		switch (dropCause) {
			case PICK_BLOCK:
			case SILK_TOUCH:
			case WORLD:
				return new ItemStack[]{new ItemStack(itemSupplier.get())};
			default:
				return null;
		}
	}

	/* ===================== PLACEMENT / SUPPORT ===================== */
	@Override
	public boolean canBlockStay(World world, int x, int y, int z) {
		return world.canPlaceOnSurfaceOfBlock(x, y - 1, z);
	}

	@Override
	public boolean canPlaceBlockAt(World world, int x, int y, int z) {
		return world.canPlaceOnSurfaceOfBlock(x, y - 1, z);
	}

	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, int blockId) {
		if (!this.canBlockStay(world, x, y, z)) {
			this.dropBlockWithCause(
				world, EnumDropCause.WORLD, x, y, z,
				world.getBlockMetadata(x, y, z),
				null, null
			);
			world.setBlockWithNotify(x, y, z, 0);
		}
	}

	@Override
	public int getPistonPushReaction(World world, int x, int y, int z) {
		return 1;
	}

	/* ===================== EXTENSION POINT ===================== */
	/**
	 * Create the animal released when the jar breaks.
	 */
	@Override
	public void onBlockDestroyedByPlayer(
		World world, int x, int y, int z, Side side, int meta, Player player, Item item) {

		if (item == null || !item.isSilkTouch()) {
			if (!world.isClientSide) {

				// --- CAPTURE TILEENTITY BEFORE REMOVING BLOCK ---
				TileEntity te = world.getTileEntity(x, y, z);

				// Remove the block
				world.setBlockWithNotify(x, y, z, 0);

				// Spawn the cricket
				Entity entity = createReleasedEntity(world, x, y, z, te);
				if (entity != null) {
					world.entityJoinedWorld(entity);
				}

				world.playSoundAtEntity(player, player, "item.pickup", 1.0F, 1.0F);
			}
		}
	}


	/* ===================== EXTENSION POINT ===================== */
	/**
	 * Create the animal released when the jar breaks.
	 * @param te The TileEntity of the jar before destruction (may be null)
	 */
	protected abstract Entity createReleasedEntity(World world, int x, int y, int z, TileEntity te);
}
