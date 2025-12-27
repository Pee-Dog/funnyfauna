package peedog.funnyfauna.item;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.Item;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.util.collection.NamespaceID;
import net.minecraft.core.world.World;

public class ItemArmExtension extends Item {

	public static final String TAG_EXTENDED = "Extended";
	private World world;

	public ItemArmExtension(String translationKey, String namespace, int id) {
		super(translationKey, namespace, id);
		this.setMaxStackSize(1);
		this.setHasSubtypes(true);
	}

    /* ----------------------------
       NBT helpers
       ---------------------------- */

	public static boolean isExtended(ItemStack stack) {
		if (stack == null || !(stack.getItem() instanceof ItemArmExtension)) {
			return false;
		}
		return stack.getData().getBoolean(TAG_EXTENDED);
	}

	public static void setExtended(ItemStack stack, boolean extended) {
		CompoundTag tag = stack.getData();
		tag.putBoolean(TAG_EXTENDED, extended);
		stack.setMetadata(extended ? 1 : 0); // 0 = off, 1 = on (texture)
	}

    /* ----------------------------
       Right click toggle
       ---------------------------- */

	@Override
	public ItemStack onUseItem(ItemStack stack, World world, Player player) {

		boolean extended = isExtended(stack);
		setExtended(stack, !extended);
		if (extended) {
			world.playSoundAtEntity(player, player, "tile.piston.in", 0.3F, 1.2F / (itemRand.nextFloat() * 0.4F + 0.8F));
		} else {
			world.playSoundAtEntity(player, player, "tile.piston.out", 0.3F, 1.2F / (itemRand.nextFloat() * 0.4F + 0.8F));
		}
		return stack;
	}
}
