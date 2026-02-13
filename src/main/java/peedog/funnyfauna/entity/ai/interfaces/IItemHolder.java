package peedog.funnyfauna.entity.ai.interfaces;

import net.minecraft.core.item.ItemStack;

public interface IItemHolder {

	 ItemStack getHeldItem();
	 void setHeldItem(ItemStack stack);
}
