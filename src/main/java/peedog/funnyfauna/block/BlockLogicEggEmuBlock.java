package peedog.funnyfauna.block;

import net.minecraft.core.block.Block;
import net.minecraft.core.block.BlockLogic;
import net.minecraft.core.block.entity.TileEntity;
import net.minecraft.core.block.material.Material;
import net.minecraft.core.enums.EnumDropCause;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.world.World;
import net.minecraft.core.world.WorldSource;
import peedog.funnyfauna.block.entity.TileEntityEmuEgg;

public class BlockLogicEggEmuBlock extends BlockLogic {

	public BlockLogicEggEmuBlock(Block<?> block) {
		super(block, Material.stone);
		block.withEntity(TileEntityEmuEgg::new);
	}

	@Override
	public void initializeBlock() {
		// Egg-sized bounds
		this.setBlockBounds(
			0.25, 0.0, 0.25,
			0.75, 0.6, 0.75
		);
	}

	/* =========================
	   Rendering
	   ========================= */

	@Override
	public boolean isCubeShaped() {
		return false;
	}

	@Override
	public boolean renderAsNormalBlockOnCondition(WorldSource world, int x, int y, int z) {
		return false;
	}

	@Override
	public boolean isSolidRender() {
		return false;
	}

	@Override
	public boolean blocksLight() {
		return false;
	}

	/* =========================
	   IMPORTANT: THIS MAKES IT TICK
	   ========================= */

	@Override
	public boolean isSignalSource() {
		// REQUIRED so the TileEntity ticks in BTA
		return true;
	}

	/* =========================
	   Drops
	   ========================= */

	@Override
	public ItemStack[] getBreakResult(World world, EnumDropCause dropCause, int meta, TileEntity tileEntity) {
		switch (dropCause) {
			case PICK_BLOCK:
			case SILK_TOUCH:
				return new ItemStack[]{ new ItemStack(this.block) };
			default:
				return null;
		}
	}
}
