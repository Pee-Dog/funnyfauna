package peedog.funnyfauna.world.features;

import java.util.Random;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.BlockLogicLeavesBase;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.world.World;
import net.minecraft.core.block.entity.TileEntity;
import net.minecraft.core.world.generate.feature.WorldFeature;
import peedog.funnyfauna.block.entity.TileEntityAntHill;

public class WorldFeatureAntHill extends WorldFeature {
	private final int blockId;

	public WorldFeatureAntHill(int blockId) {
		this.blockId = blockId;
	}

	@Override
	public boolean place(World world, Random random, int x, int y, int z) {
		int l;

		// 1. Move down through air or leaves to find the ground
		while(((l = world.getBlockId(x, y, z)) == 0 || Block.hasLogicClass(Blocks.blocksList[l], BlockLogicLeavesBase.class)) && y > 0) {
			--y;
		}

		// 2. CHECK: Is the ground block Sand? (Replacing targetY logic)
		// We use 'y' directly to replace the ground block instead of placing above it.
		int groundBlockId = world.getBlockId(x, y, z);

		if (groundBlockId == Blocks.SAND.id()) {
			// 3. Replace the sand block with the Ant Hill
			world.setBlock(x, y, z, this.blockId);

			// 4. Populate with 10 ants immediately
			TileEntity te = world.getTileEntity(x, y, z);
			if (te instanceof TileEntityAntHill) {
				((TileEntityAntHill) te).populateWithDefaultAnts(10);
			}

			return true;
		}

		return false;
	}
}
