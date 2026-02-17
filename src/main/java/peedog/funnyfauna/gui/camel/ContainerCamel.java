package peedog.funnyfauna.gui.camel;

import net.minecraft.core.InventoryAction;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.player.inventory.container.ContainerInventory;
import net.minecraft.core.player.inventory.slot.Slot;
import peedog.funnyfauna.entity.camel.MobCamel;

import java.util.List;

/**
 * Container for camel inventory.
 * Supports both single and compound inventories.
 */
public class ContainerCamel extends net.minecraft.core.player.inventory.menu.MenuAbstract {

	private final MobCamel camel;
	private final net.minecraft.core.player.inventory.container.Container camelContainer;

	public ContainerCamel(ContainerInventory playerInventory,
						  net.minecraft.core.player.inventory.container.Container camelContainer,
						  MobCamel camel) {
		super();
		this.camel = camel;
		this.camelContainer = camelContainer;

		int camelSlots = camelContainer.getContainerSize();
		int camelRows = (int) Math.ceil(camelSlots / 9d);

		// --- Camel inventory slots ---
		for (int row = 0; row < camelRows; row++) {
			int width = (row == camelRows - 1) ? camelSlots - row * 9 : 9;
			for (int col = 0; col < width; col++) {
				// SlotCamel works for InventoryCamel or InventoryCamelCompound
				this.addSlot(new SlotCamel(camelContainer, col + row * 9, 8 + col * 18, 18 + row * 18));
			}
		}

		// --- Player inventory slots ---
		int playerYStart = 18 + camelRows * 18 + 13;
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, playerYStart + row * 18));
			}
		}

		// --- Player hotbar ---
		int hotbarY = playerYStart + 3 * 18 + 4;
		for (int col = 0; col < 9; col++) {
			this.addSlot(new Slot(playerInventory, col, 8 + col * 18, hotbarY));
		}
	}

	@Override
	public boolean stillValid(Player player) {
		return !camel.isRemoved() && (player == camel.passenger || (camel.isTamed() && camel.ownerName.equals(player.uuid)));
	}

	@Override
	public List<Integer> getMoveSlots(InventoryAction action, Slot slot, int arg, Player player) {
		int camelSize = camelContainer.getContainerSize();
		if (slot.index >= 0 && slot.index < camelSize) return getSlots(camelSize, 36, false);
		if (slot.index >= camelSize) return getSlots(0, camelSize, false);
		return null;
	}

	@Override
	public List<Integer> getTargetSlots(InventoryAction action, Slot slot, int arg, Player player) {
		int camelSize = camelContainer.getContainerSize();
		if (slot.index < camelSize) return getSlots(camelSize, 36, true);
		return getSlots(0, camelSize, false);
	}
}
