package peedog.funnyfauna.gui.satchel;

import net.minecraft.core.item.ItemStack;
import net.minecraft.core.player.inventory.slot.Slot;
import peedog.funnyfauna.item.ItemSatchel;

public class SlotSatchel extends Slot {
	private final InventorySatchel inventorySatchel;

	public SlotSatchel(final InventorySatchel inventory, final int id, final int x, final int y) {
		super(inventory, id, x, y);
		this.inventorySatchel = inventory;
	}

	@Override
	public boolean mayPlace(final ItemStack itemstack) {
		// Prevent nesting satchels
		if (itemstack.getItem() instanceof ItemSatchel) return false;

		// Check if there is room in the bundle
		// We use a copy of size 1 to check if even ONE item can fit
		ItemStack check = itemstack.copy();
		check.stackSize = 1;
		return inventorySatchel.contents.maxInsertable(check) > 0;
	}

	public int getSatchelIndex() {
		return super.slot; // protected access works here
	}


}
