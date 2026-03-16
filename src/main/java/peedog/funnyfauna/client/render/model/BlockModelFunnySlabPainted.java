// BlockModelSlabPainted.java
package peedog.funnyfauna.client.render.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.block.model.BlockModelSlab;
import net.minecraft.client.render.texture.stitcher.IconCoordinate;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.BlockLogicSlab;
import net.minecraft.core.util.helper.Side;
import net.minecraft.core.world.WorldSource;

@Environment(EnvType.CLIENT)
public class BlockModelFunnySlabPainted<T extends BlockLogicSlab> extends BlockModelSlab<T> {
	private final IconCoordinate[] texCoords;

	public BlockModelFunnySlabPainted(Block<T> block, IconCoordinate[] texCoords) {
		super(block);
		this.texCoords = texCoords;
	}

	@Override
	public IconCoordinate getBlockTextureFromSideAndMetadata(Side side, int meta) {
		return texCoords[(meta >> 4) & 15];
	}

	@Override
	public IconCoordinate getBlockTexture(WorldSource world, int x, int y, int z, Side side) {
		return getBlockTextureFromSideAndMetadata(side, world.getBlockMetadata(x, y, z));
	}
}
