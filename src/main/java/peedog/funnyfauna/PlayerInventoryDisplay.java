package peedog.funnyfauna;

import net.minecraft.core.item.ItemStack;
import peedog.funnyfauna.entity.camel.MobCamel;

public interface PlayerInventoryDisplay {

	// GUI
	void funnyfauna$displayGUISatchel(ItemStack stack);

	void funnyfauna$displayGUICamel(MobCamel camel);

	// Equipped slot API
	ItemStack funnyfauna$getEquippedSlot();

	void funnyfauna$setEquippedSlot(ItemStack stack);

	boolean funnyfauna$isEquippedEnabled();

	void funnyfauna$setEquippedEnabled(boolean enabled);
}
