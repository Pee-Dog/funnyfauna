package peedog.funnyfauna.gui.satchel;

import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.player.inventory.container.Container;
import peedog.funnyfauna.item.ItemSatchel;

public class InventorySatchel implements Container {

	public final ItemStack stack;
	public final BundleContents contents; // Made public for Mixin access

	public InventorySatchel(final ItemStack stack) {
		assert stack.getItem() instanceof ItemSatchel;
		this.stack = stack;
		this.contents = new BundleContents();
		this.contents.readFromNBT(stack.getData());
	}

	@Override
	public int getContainerSize() {
		return Math.max(contents.getStacks().size(), 0);
	}


	@Override
	public ItemStack getItem(int i) {
		if (i < contents.getStacks().size()) {
			return contents.getStacks().get(i);
		}
		return null;
	}

	@Override
	public void setItem(int i, ItemStack stack) {
		// GUI slots are virtual – do nothing here
		setChanged();
	}


	@Override
	public ItemStack removeItem(int i, int amount) {
		if (i < 0 || i >= contents.getStacks().size()) return null;

		ItemStack itemStack = contents.getStacks().get(i);

		if (itemStack.stackSize <= amount) {
			// Remove the stack entirely
			ItemStack removed = contents.remove(i);
			setChanged(); // This will update durability
			return removed;
		} else {
			// Split off the requested amount
			ItemStack split = itemStack.splitStack(amount);

			// If original stack is now empty, remove it
			if (itemStack.stackSize <= 0) {
				contents.remove(i);
			}

			setChanged(); // This will update durability
			return split;
		}
	}


	@Override
	public int getMaxStackSize() {
		return 64;
	}

	@Override
	public void setChanged() {
		contents.writeToNBT(stack.getData());
		// Update durability bar to reflect fullness
		ItemSatchel.updateDurability(stack);
	}

	@Override
	public boolean stillValid(Player player) {
		// Allow the satchel to be accessed from any inventory slot
		// Check if player has the satchel in their inventory
		for (int i = 0; i < player.inventory.mainInventory.length; i++) {
			if (player.inventory.mainInventory[i] == stack) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String getNameTranslationKey() {
		String name = stack.getCustomName();
		return name != null ? name : stack.getDisplayName();
	}

	@Override
	public void sortContainer() {}
}
