package peedog.funnyfauna.item;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.Item;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.world.World;

public class ItemSturdyShell extends Item {

	public static final String TAG_ENABLED = "Enabled";

	// Total durability of the shell
	private static final int MAX_DURABILITY = 200;

	public ItemSturdyShell(String translationKey, String namespaceId, int id) {
		super(translationKey, namespaceId, id);
		this.setMaxStackSize(1);
		this.setMaxDamage(MAX_DURABILITY);
		this.setHasSubtypes(true); // needed for texture switching
		// DO NOT touch metadata at all
	}

    /* ----------------------------
       NBT helpers
       ---------------------------- */

	public static boolean isEnabled(ItemStack stack) {
		if (stack == null || !(stack.getItem() instanceof ItemSturdyShell)) return false;
		return stack.getData().getBoolean(TAG_ENABLED);
	}

	public static void setEnabled(ItemStack stack, boolean enabled) {
		if (stack == null) return;
		stack.getData().putBoolean(TAG_ENABLED, enabled);
		// No metadata changes — we rely on NBT for icon switching
	}

    /* ----------------------------
       Right-click toggle
       ---------------------------- */

	@Override
	public ItemStack onUseItem(ItemStack stack, World world, Player player) {
		boolean enabled = isEnabled(stack);
		setEnabled(stack, !enabled);

		if (!world.isClientSide) {
			world.playSoundAtEntity(
				player,
				player,
				enabled ? "tile.piston.in" : "tile.piston.out",
				0.4F,
				1.1F
			);
		}

		return stack;
	}
}
