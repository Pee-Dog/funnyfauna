package peedog.funnyfauna.block;

import net.minecraft.core.block.Block;
import net.minecraft.core.block.BlockLogic;
import net.minecraft.core.block.IPaintable;
import net.minecraft.core.block.material.Material;
import net.minecraft.core.util.helper.DyeColor;
import net.minecraft.core.world.World;

public class BlockLogicFur extends BlockLogic implements IPaintable {
	public BlockLogicFur(Block<?> block) {
		super(block, Material.cloth);
	}

	public void setColor(World world, int x, int y, int z, DyeColor color) {
		world.setBlock(x, y, z, FunnyFaunaBlocks.FUR_PAINTED.id());
		((BlockLogicFurPainted)FunnyFaunaBlocks.FUR_PAINTED.getLogic()).setColor(world, x, y, z, color);
	}
}
