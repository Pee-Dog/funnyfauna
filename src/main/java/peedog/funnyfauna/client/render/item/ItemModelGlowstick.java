package peedog.funnyfauna.client.render.item;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.item.model.ItemModelStandard;
import net.minecraft.client.render.texture.stitcher.IconCoordinate;
import net.minecraft.client.render.texture.stitcher.TextureRegistry;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.item.Item;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.util.helper.DyeColor;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public class ItemModelGlowstick extends ItemModelStandard {

	/**
	 * One icon slot per dye color (indices 0-15), keyed by item metadata.
	 * Textures expected at: funnyfauna:item/glowstick/<colorID>
	 * e.g. funnyfauna:item/glowstick/white, funnyfauna:item/glowstick/red, …
	 */
	public static final IconCoordinate[] icons = new IconCoordinate[16];

	public ItemModelGlowstick(Item item) {
		super(item, null);
	}

	@Override
	public @NotNull IconCoordinate getIcon(Entity entity, ItemStack itemStack) {
		// itemMeta is the DyeColor ordering used for item stacks (same as
		// blockMeta for most colors, but use itemMeta to be safe, matching
		// DyeColor.colorFromItemMeta as seen in ItemDoorPainted.getLanguageKey).
		int meta = itemStack.getMetadata() & 15;
		return icons[meta];
	}

	static {
		for (DyeColor c : DyeColor.blockOrderedColors()) {
			icons[c.blockMeta] = TextureRegistry.getTexture(
				"funnyfauna:item/glowstick/" + c.colorID
			);
		}
	}
}
