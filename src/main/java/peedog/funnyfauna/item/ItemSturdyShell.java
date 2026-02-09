package peedog.funnyfauna.item;

import net.minecraft.core.item.ItemStack;

public class ItemSturdyShell extends ItemToggleable {

	private static final int MAX_DURABILITY = 200;

	public ItemSturdyShell(String translationKey, String namespaceId, int id) {
		super(translationKey, namespaceId, id);
		this.setMaxDamage(MAX_DURABILITY);
	}

	@Override
	public int getMaxDamage() {
		return MAX_DURABILITY;
	}

	/* ----------------------------
	   Compatibility helpers
	   ---------------------------- */

	public static boolean isEnabled(ItemStack stack) {
		return isToggled(stack);
	}
}
