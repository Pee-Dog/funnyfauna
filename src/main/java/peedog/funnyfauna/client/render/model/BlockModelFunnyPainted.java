// BlockModelPainted.java
package peedog.funnyfauna.client.render.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.block.model.BlockModelStandard;
import net.minecraft.client.render.texture.stitcher.IconCoordinate;
import net.minecraft.client.render.texture.stitcher.TextureRegistry;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.BlockLogic;
import net.minecraft.core.util.helper.DyeColor;
import net.minecraft.core.util.helper.Side;

@Environment(EnvType.CLIENT)
public class BlockModelFunnyPainted<T extends BlockLogic> extends BlockModelStandard<T> {
	public final IconCoordinate[] texCoords;

	public BlockModelFunnyPainted(Block<T> block, IconCoordinate[] texCoords) {
		super(block);
		this.texCoords = texCoords;
	}

	/** Build a 16-slot coords array from a texture prefix, e.g. "funnyfauna:block/scales/" */
	public static IconCoordinate[] buildTexCoords(String prefix) {
		IconCoordinate[] coords = new IconCoordinate[16];
		for (DyeColor c : DyeColor.blockOrderedColors()) {
			coords[c.blockMeta] = TextureRegistry.getTexture(prefix + c.colorID);
		}
		return coords;
	}

	@Override
	public IconCoordinate getBlockTextureFromSideAndMetadata(Side side, int meta) {
		return texCoords[meta & 15];
	}
}
