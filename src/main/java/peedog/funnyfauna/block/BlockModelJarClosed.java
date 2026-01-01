package peedog.funnyfauna.block;

import net.minecraft.client.render.block.model.BlockModelJar;
import net.minecraft.client.render.block.model.BlockModelLantern;
import net.minecraft.client.render.texture.stitcher.IconCoordinate;
import net.minecraft.client.render.texture.stitcher.TextureRegistry;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.BlockLogic;
import net.minecraft.core.util.helper.Side;

public class BlockModelJarClosed<T extends BlockLogic> extends BlockModelLantern<T> {

	private final IconCoordinate jarClosed = TextureRegistry.getTexture("funnyfauna:block/jar_closed");

	public BlockModelJarClosed(Block<T> block) {
		super (block);
	}

	@Override
	public IconCoordinate getBlockTextureFromSideAndMetadata(Side side, int meta) {
		return jarClosed;
	}
}
