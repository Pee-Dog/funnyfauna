package peedog.funnyfauna.gui.camel;

import net.minecraft.core.item.ItemStack;
import net.minecraft.core.player.inventory.container.Container;
import net.minecraft.core.player.inventory.slot.Slot;

/**
 * Camel slot triggers inventory save on change.
 * Works for single or compound camel inventories.
 */
public class SlotCamel extends Slot {

	public SlotCamel(Container inventory, int index, int x, int y) {
		super(inventory, index, x, y);
	}

	@Override
	public void set(ItemStack stack) {
		super.set(stack);

		this.container.setChanged();

	}

	@Override
	public boolean mayPlace(ItemStack stack) {
		return true; // allow any items
	}
}
