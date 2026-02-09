package peedog.funnyfauna.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.core.InventoryAction;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.player.inventory.container.ContainerInventory;
import net.minecraft.core.player.inventory.menu.MenuAbstract;
import net.minecraft.core.player.inventory.slot.Slot;
import peedog.funnyfauna.item.ItemSatchel;

import java.util.List;

public class ContainerSatchel extends MenuAbstract {
	public InventorySatchel inventorySatchel;

	public ContainerSatchel(ContainerInventory playerInv, ItemStack satchel) {
		super();
		this.inventorySatchel = new InventorySatchel(satchel);
		this.refreshSlots();
	}

	public void refreshSlots() {
		this.slots.clear();
		this.lastSlots.clear();

		int slotsNum = this.inventorySatchel.getContainerSize();
		if (slotsNum > 0) { // only add satchel slots if any items exist
			int rows = (int) Math.ceil(slotsNum / 9d);

			for (int i = 0; i < rows; ++i) {
				int width = (i == rows - 1) ? slotsNum - 9 * i : 9;
				for (int k = 0; k < width; ++k) {
					this.addSlot(new SlotSatchel(
						this.inventorySatchel,
						k + i * 9,
						8 + k * 18,
						18 + 18 * i
					));
				}
			}
		}

		// Always add player inventory slots
		ContainerInventory playerInv = Minecraft.getMinecraft().thePlayer.inventory;
		for (int i = 0; i < 3; ++i) {
			for (int k = 0; k < 9; ++k) {
				this.addSlot(new Slot(playerInv, k + i * 9 + 9, 8 + k * 18, 84 + i * 18));
			}
		}
		for (int j = 0; j < 9; ++j) {
			this.addSlot(new Slot(playerInv, j, 8 + j * 18, 142));
		}

		// Reassign GUI indices
		for (int i = 0; i < this.slots.size(); i++) {
			this.slots.get(i).index = i;
		}
	}


	@Override
	public void handleItemMove(InventoryAction action, Slot slot, int target, Player player) {
		ItemStack cursorStack = player.inventory.getHeldItemStack();

		// Calculate moveAmount at the start so it's available everywhere
		int moveAmount = (action == InventoryAction.CLICK_RIGHT || action == InventoryAction.MOVE_SINGLE_ITEM) ? 1 :
			(cursorStack != null ? cursorStack.stackSize : 0);

		// 1. CLICKING THE EMPTY BACKGROUND AREA (The "Click Anywhere" feature)
		if (slot == null) {
			if (cursorStack == null || cursorStack.getItem() instanceof ItemSatchel) return;

			ItemStack toAdd = cursorStack.copy();
			toAdd.stackSize = moveAmount;

			int added = inventorySatchel.contents.add(toAdd);
			if (added > 0) {
				cursorStack.stackSize -= added;
				if (cursorStack.stackSize <= 0) player.inventory.setHeldItemStack(null);

				// CRITICAL: Call forceSync instead of just setChanged
				this.forceSync();
			}
			return;
		}

		// 2. CLICKING AN EXISTING ITEM IN THE SATCHEL
		if (slot instanceof SlotSatchel) {
			int satchelIndex = ((SlotSatchel) slot).getSatchelIndex();
			ItemStack stackInSlot = inventorySatchel.getItem(satchelIndex);
			if (stackInSlot == null) return;

			// SHIFT-CLICK (Move item out of satchel into player inventory)
			if (action == InventoryAction.MOVE_STACK || action == InventoryAction.MOVE_ALL) {
				ItemStack toMove = stackInSlot.copy();
				int playerInvStart = inventorySatchel.getContainerSize();

				// BTA mergeItems returns void. We check if it worked by comparing sizes.
				int originalSize = toMove.stackSize;
				this.mergeItems(toMove, playerInvStart, playerInvStart + 36, false);

				int movedCount = originalSize - toMove.stackSize;
				if (movedCount > 0) {
					inventorySatchel.removeItem(satchelIndex, movedCount);
				}
			}
			// NORMAL CLICK
			else {
				if (cursorStack == null) {
					// Right click = take half (rounded up), Left click = take all
					int amountToTake = (action == InventoryAction.CLICK_RIGHT) ? (stackInSlot.stackSize + 1) / 2 : stackInSlot.stackSize;
					ItemStack removed = inventorySatchel.removeItem(satchelIndex, amountToTake);
					player.inventory.setHeldItemStack(removed);
					inventorySatchel.setChanged();
					refreshSlots();
				} else {
					// FIX: If holding an item, ALWAYS add it to the satchel (don't pick up slot item)
					if (cursorStack.getItem() instanceof ItemSatchel) return;

					int added = inventorySatchel.contents.add(cursorStack);
					if (added > 0) {
						cursorStack.stackSize -= added;
						if (cursorStack.stackSize <= 0) player.inventory.setHeldItemStack(null);
						inventorySatchel.setChanged();
						refreshSlots();
					}
				}
			}
			return;
		}

		// 3. SHIFT-CLICKING FROM PLAYER INVENTORY INTO SATCHEL
		if (action == InventoryAction.MOVE_STACK && !(slot instanceof SlotSatchel)) {
			ItemStack toMove = slot.getItemStack();
			if (toMove != null && !(toMove.getItem() instanceof ItemSatchel)) {
				int added = inventorySatchel.contents.add(toMove);
				if (added > 0) {
					toMove.stackSize -= added;
					if (toMove.stackSize <= 0) slot.set(null);
					else slot.setChanged();

					inventorySatchel.setChanged();
					refreshSlots();
				}
			}
		}
	}


	/**
	 * Helper to get only the player inventory slot indices (0-35)
	 */
	@Override
	public List<Integer> getMoveSlots(InventoryAction action, Slot slot, int target, Player player) {
		int satchelSize = this.inventorySatchel.getContainerSize();
		int playerInvSize = 36;

		if (slot != null && slot.getContainer() instanceof InventorySatchel) {
			return this.getSlots(satchelSize, playerInvSize, false);
		} else {
			return this.getSlots(0, satchelSize, false);
		}
	}



	@Override
	public List<Integer> getTargetSlots(InventoryAction action, Slot slot, int target, Player player) {
		return this.getMoveSlots(action, slot, target, player);
	}

	@Override
	public boolean stillValid(Player player) {
		return this.inventorySatchel.stillValid(player);
	}

	public void forceSync() {
		// This tells the container to rebuild its slot list based on the new inventory size
		this.refreshSlots();

		// This marks the container as changed so it sends packet updates to the client
		this.inventorySatchel.setChanged();
	}
}
