package peedog.funnyfauna.inventory;

import net.minecraft.core.InventoryAction;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.player.inventory.container.Container;
import net.minecraft.core.player.inventory.menu.MenuAbstract;
import net.minecraft.core.player.inventory.slot.Slot;

import java.util.ArrayList;
import java.util.List;

public class MenuSatchel extends MenuAbstract {

	private final Container satchel;

	private final int satchelStart;
	private final int satchelEnd;
	private final int playerStart;
	private final int playerEnd;

	public MenuSatchel(Container satchel, Player player) {
		this.satchel = satchel;

		// =========================
		// Satchel inventory (3×9)
		// =========================
		satchelStart = this.slots.size();
		for (int y = 0; y < 3; y++) {
			for (int x = 0; x < 9; x++) {
				this.addSlot(new Slot(
					satchel,
					x + y * 9,
					8 + x * 18,
					18 + y * 18
				));
			}
		}
		satchelEnd = this.slots.size() - 1;

		// =========================
		// Player inventory (3×9)
		// =========================
		playerStart = this.slots.size();
		for (int y = 0; y < 3; y++) {
			for (int x = 0; x < 9; x++) {
				this.addSlot(new Slot(
					player.inventory,
					x + y * 9 + 9,
					8 + x * 18,
					84 + y * 18
				));
			}
		}

		// =========================
		// Hotbar (1×9)
		// =========================
		for (int x = 0; x < 9; x++) {
			this.addSlot(new Slot(
				player.inventory,
				x,
				8 + x * 18,
				142
			));
		}
		playerEnd = this.slots.size() - 1;
	}

	@Override
	public boolean stillValid(Player player) {
		return satchel.stillValid(player);
	}

	// =========================
	// Shift-click logic
	// =========================

	@Override
	public List<Integer> getMoveSlots(InventoryAction action, Slot slot, int target, Player player) {
		List<Integer> list = new ArrayList<>();
		int index = slot.index;

		if (index >= satchelStart && index <= satchelEnd) {
			for (int i = satchelStart; i <= satchelEnd; i++) list.add(i);
		} else {
			for (int i = playerStart; i <= playerEnd; i++) list.add(i);
		}

		return list;
	}

	@Override
	public List<Integer> getTargetSlots(InventoryAction action, Slot slot, int target, Player player) {
		List<Integer> list = new ArrayList<>();
		int index = slot.index;

		if (index >= satchelStart && index <= satchelEnd) {
			for (int i = playerStart; i <= playerEnd; i++) list.add(i);
		} else {
			for (int i = satchelStart; i <= satchelEnd; i++) list.add(i);
		}

		return list;
	}
}
