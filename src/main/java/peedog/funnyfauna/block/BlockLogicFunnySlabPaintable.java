// BlockLogicSlabPaintable.java
package peedog.funnyfauna.block;

import net.minecraft.core.block.Block;
import net.minecraft.core.block.BlockLogicSlab;
import net.minecraft.core.block.IPaintable;
import net.minecraft.core.block.IPainted;
import net.minecraft.core.util.helper.DyeColor;
import net.minecraft.core.world.World;
import java.util.function.Supplier;

public class BlockLogicFunnySlabPaintable extends BlockLogicSlab implements IPaintable {
	private final Supplier<Block<?>> paintedVariant;

	public BlockLogicFunnySlabPaintable(Block<?> block, Block<?> modelBlock, Supplier<Block<?>> paintedVariant) {
		super(block, modelBlock);
		this.paintedVariant = paintedVariant;
	}

	@Override
	public void setColor(World world, int x, int y, int z, DyeColor color) {
		int meta = world.getBlockMetadata(x, y, z);
		world.setBlockAndMetadata(x, y, z, paintedVariant.get().id(), meta);
		((IPainted) paintedVariant.get().getLogic()).setColor(world, x, y, z, color);
	}
}
