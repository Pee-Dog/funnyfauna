package peedog.funnyfauna.entity.ai.i;

import net.minecraft.core.item.ItemStack;

public interface IItemHolder {

	 ItemStack getHeldItem();
	 void setHeldItem(ItemStack stack);
}
