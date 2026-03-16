// BlockLogicSlabPainted.java
package peedog.funnyfauna.block;

import net.minecraft.core.block.Block;
import net.minecraft.core.block.BlockLogicSlab;
import net.minecraft.core.block.IPainted;
import net.minecraft.core.util.helper.DyeColor;
import net.minecraft.core.world.World;
import java.util.function.Supplier;

public class BlockLogicFunnySlabPainted extends BlockLogicSlab implements IPainted {
	private final Supplier<Block<?>> unpaintedVariant;

	public BlockLogicFunnySlabPainted(Block<?> block, Block<?> modelBlock, Supplier<Block<?>> unpaintedVariant) {
		super(block, modelBlock);
		this.unpaintedVariant = unpaintedVariant;
	}

	@Override public DyeColor fromMetadata(int meta) { return DyeColor.colorFromBlockMeta((meta & 240) >> 4); }
	@Override public int toMetadata(DyeColor color)  { return color.blockMeta << 4; }
	@Override public int stripColorFromMetadata(int meta) { return meta & 15; }

	@Override
	public void removeDye(World world, int x, int y, int z) {
		int meta = world.getBlockMetadata(x, y, z);
		world.setBlockAndMetadataWithNotify(x, y, z, unpaintedVariant.get().id(), stripColorFromMetadata(meta));
	}
}
