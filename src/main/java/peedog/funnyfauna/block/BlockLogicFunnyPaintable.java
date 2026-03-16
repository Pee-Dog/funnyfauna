// BlockLogicPaintable.java
package peedog.funnyfauna.block;

import net.minecraft.core.block.Block;
import net.minecraft.core.block.BlockLogic;
import net.minecraft.core.block.IPaintable;
import net.minecraft.core.block.IPainted;
import net.minecraft.core.block.material.Material;
import net.minecraft.core.util.helper.DyeColor;
import net.minecraft.core.world.World;
import java.util.function.Supplier;

public class BlockLogicFunnyPaintable extends BlockLogic implements IPaintable {
	private final Supplier<Block<?>> paintedVariant;

	public BlockLogicFunnyPaintable(Block<?> block, Material material, Supplier<Block<?>> paintedVariant) {
		super(block, material);
		this.paintedVariant = paintedVariant;
	}

	@Override
	public void setColor(World world, int x, int y, int z, DyeColor color) {
		world.setBlock(x, y, z, paintedVariant.get().id());
		((IPainted) paintedVariant.get().getLogic()).setColor(world, x, y, z, color);
	}
}
