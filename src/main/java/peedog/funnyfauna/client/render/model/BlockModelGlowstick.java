package peedog.funnyfauna.client.render.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.LightmapHelper;
import net.minecraft.client.render.block.model.BlockModelTorch;
import net.minecraft.client.render.tessellator.Tessellator;
import net.minecraft.client.render.texture.stitcher.IconCoordinate;
import net.minecraft.client.render.texture.stitcher.TextureRegistry;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.BlockLogic;
import net.minecraft.core.util.helper.DyeColor;
import net.minecraft.core.util.helper.Side;

@Environment(EnvType.CLIENT)
public class BlockModelGlowstick<T extends BlockLogic> extends BlockModelTorch<T> {

	/**
	 * One texture slot per dye color (indices 0-15).
	 * Textures are expected at: funnyfauna:block/glowstick/<colorID>
	 * e.g. funnyfauna:block/glowstick/white, funnyfauna:block/glowstick/red, …
	 */
	public static final IconCoordinate[] texCoords = new IconCoordinate[16];

	/**
	 * Cached during render() so that renderTorchAtAngle() — which has no
	 * metadata parameter — can still look up the correct color texture.
	 */
	private int currentColorMeta = 0;

	public BlockModelGlowstick(Block<T> block) {
		super(block);
	}

	/**
	 * Override the full render method so that:
	 *  1. The direction is ALWAYS the floor case (topOffsetX/Z = 0,0), regardless
	 *     of what value is stored in metadata.
	 *  2. The real metadata is cached into currentColorMeta before
	 *     renderTorchAtAngle is called, so the texture lookup gets the right color.
	 *
	 * In the vanilla torch renderer, metadata drives both direction AND texture
	 * (via a hardcoded 0 passed to getBlockTextureFromSideAndMetadata). By
	 * overriding here we break that coupling.
	 */
	@Override
	public boolean render(Tessellator tessellator, int x, int y, int z) {
		float brightness = 1.0F;
		if (LightmapHelper.isLightmapEnabled()) {
			tessellator.setLightmapCoord(this.block.getLightmapCoord(renderBlocks.blockAccess, x, y, z));
		} else {
			brightness = this.getBlockBrightness(renderBlocks.blockAccess, x, y, z);
			if (this.block.emission > 0) {
				brightness = 1.0F;
			}
		}
		tessellator.setColorOpaque_F(brightness, brightness, brightness);

		// Cache the color metadata so renderTorchAtAngle can use it.
		currentColorMeta = renderBlocks.blockAccess.getBlockMetadata(x, y, z) & 15;

		// Always render as floor — offsets of 0,0 = straight up.
		this.renderTorchAtAngle(tessellator, (double) x, (double) y, (double) z, 0.0, 0.0);
		return true;
	}

	/**
	 * Route the cached color metadata to the correct texture.
	 * The 'meta' parameter here would normally be 0 (hardcoded by the parent's
	 * renderTorchAtAngle call), so we ignore it in favour of currentColorMeta.
	 */
	@Override
	public IconCoordinate getBlockTextureFromSideAndMetadata(Side side, int meta) {
		return texCoords[currentColorMeta];
	}

	static {
		for (DyeColor c : DyeColor.blockOrderedColors()) {
			texCoords[c.blockMeta] = TextureRegistry.getTexture(
				"funnyfauna:block/glowstick/" + c.colorID
			);
		}
	}
}
