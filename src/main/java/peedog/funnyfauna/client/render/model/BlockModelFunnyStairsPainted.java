//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package peedog.funnyfauna.client.render.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.block.model.BlockModelStairs;
import net.minecraft.client.render.texture.stitcher.IconCoordinate;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.BlockLogicStairs;
import net.minecraft.core.util.helper.Side;
import net.minecraft.core.world.WorldSource;

// BlockModelStairsPainted.java  (identical structure to slab version)
@Environment(EnvType.CLIENT)
public class BlockModelFunnyStairsPainted<T extends BlockLogicStairs> extends BlockModelStairs<T> {
	private final IconCoordinate[] texCoords;

	public BlockModelFunnyStairsPainted(Block<T> block, IconCoordinate[] texCoords) {
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
