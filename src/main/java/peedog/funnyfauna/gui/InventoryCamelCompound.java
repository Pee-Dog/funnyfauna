package peedog.funnyfauna.gui;

import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.player.inventory.container.Container;

/**
 * Wraps a single backing array as a container.
 * Works for multi-chest camels.
 */
public class InventoryCamelCompound implements Container {

	private final ItemStack[] backingArray;
	private final InventoryCamel owner;

	public InventoryCamelCompound(InventoryCamel owner, ItemStack[] backingArray) {
		this.owner = owner;
		this.backingArray = backingArray;
	}

	@Override
	public void setChanged() {
		owner.setChanged(); //  THIS is the missing link
	}

	@Override
	public int getContainerSize() {
		return backingArray.length;
	}

	@Override
	public ItemStack getItem(int index) {
		return backingArray[index];
	}

	@Override
	public ItemStack removeItem(int index, int count) {
		if (backingArray[index] != null) {
			if (backingArray[index].stackSize <= count) {
				ItemStack stack = backingArray[index];
				backingArray[index] = null;
				return stack;
			} else {
				ItemStack split = backingArray[index].splitStack(count);
				if (backingArray[index].stackSize <= 0) backingArray[index] = null;
				return split;
			}
		}
		return null;
	}

	@Override
	public void setItem(int index, ItemStack stack) {
		backingArray[index] = stack;
		if (stack != null && stack.stackSize > 64) stack.stackSize = 64;
	}

	@Override
	public String getNameTranslationKey() {
		return "Camel Inventory";
	}

	@Override
	public int getMaxStackSize() {
		return 64;
	}

	@Override
	public boolean stillValid(Player player) { return true; }

	@Override
	public void sortContainer() { }
}
