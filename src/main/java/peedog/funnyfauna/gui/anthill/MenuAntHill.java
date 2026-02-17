package peedog.funnyfauna.gui.anthill;

import net.minecraft.core.InventoryAction;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.player.inventory.container.ContainerInventory;
import net.minecraft.core.player.inventory.menu.MenuAbstract;
import net.minecraft.core.player.inventory.slot.Slot;
import peedog.funnyfauna.block.entity.TileEntityAntHill;

import java.util.Collections;
import java.util.List;

public class MenuAntHill extends MenuAbstract {

	private final TileEntityAntHill antHill;
	private final Player player;

	public MenuAntHill(Player player, TileEntityAntHill antHill) {
		this.player = player;
		this.antHill = antHill;

		// Add ant hill inventory slots (3 rows of 9 = 27 slots)
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				this.addSlot(new Slot(antHill, col + row * 9, 8 + col * 18, 18 + row * 18));
			}
		}

		// Add player inventory slots
		ContainerInventory playerInventory = player.inventory;

		// Player main inventory (3 rows)
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
			}
		}

		// Player hotbar
		for (int col = 0; col < 9; col++) {
			this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
		}
	}

	@Override
	public List<Integer> getMoveSlots(InventoryAction inventoryAction, Slot slot, int i, Player player) {
		return Collections.emptyList();
	}

	@Override
	public List<Integer> getTargetSlots(InventoryAction inventoryAction, Slot slot, int i, Player player) {
		return Collections.emptyList();
	}

	@Override
	public boolean stillValid(Player player) {
		return this.antHill.stillValid(player);
	}

	/**
	 * Get the number of ants currently stored
	 */
	public int getStoredAntCount() {
		return this.antHill.getStoredAntCount();
	}

	/**
	 * Get the maximum number of ants that can be stored
	 */
	public int getMaxAnts() {
		return this.antHill.getMaxAnts();
	}

	/**
	 * Release all ants from the ant hill
	 */
	public void releaseAllAnts() {
		if (!player.world.isClientSide) {
			this.antHill.releaseAnts(player.world);
		}
	}

	/**
	 * Release a specific number of ants
	 */
	public void releaseAnts(int count) {
		if (!player.world.isClientSide) {
			this.antHill.releaseAnts(player.world, count);
		}
	}
}
