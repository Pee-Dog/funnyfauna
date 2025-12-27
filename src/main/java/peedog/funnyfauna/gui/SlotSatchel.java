package peedog.funnyfauna.gui;

import net.minecraft.core.item.ItemStack;
import net.minecraft.core.player.inventory.container.Container;
import net.minecraft.core.player.inventory.slot.Slot;
import peedog.funnyfauna.item.ItemSatchel;

public class SlotSatchel extends Slot {
	public SlotSatchel(final Container inventory, final int id, final int x, final int y) {
	super(inventory, id, x, y);
}
	@Override
	public boolean mayPlace(final ItemStack itemstack) {
		return !(itemstack.getItem() instanceof ItemSatchel);
	}
}
