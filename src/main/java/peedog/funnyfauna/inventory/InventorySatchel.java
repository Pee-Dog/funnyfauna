package peedog.funnyfauna.inventory;

import com.mojang.nbt.tags.CompoundTag;
import com.mojang.nbt.tags.ListTag;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.player.inventory.InventorySorter;
import net.minecraft.core.player.inventory.container.Container;

public class InventorySatchel implements Container {

	private final ItemStack satchel;
	private final ItemStack[] items = new ItemStack[27];

	public InventorySatchel(ItemStack satchel) {
		this.satchel = satchel;
		readFromNBT();
	}

	@Override
	public int getContainerSize() {
		return 27;
	}

	@Override
	public ItemStack getItem(int index) {
		return items[index];
	}

	@Override
	public ItemStack removeItem(int index, int amount) {
		if (items[index] == null) return null;

		ItemStack result;
		if (items[index].stackSize <= amount) {
			result = items[index];
			items[index] = null;
		} else {
			result = items[index].splitStack(amount);
		}
		setChanged();
		return result;
	}

	@Override
	public void setItem(int index, ItemStack stack) {
		items[index] = stack;
		setChanged();
	}

	@Override
	public String getNameTranslationKey() {
		return "container.satchel";
	}

	@Override
	public int getMaxStackSize() {
		return 64;
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	@Override
	public void sortContainer() {
		InventorySorter.sortInventory(items);
	}

	@Override
	public void setChanged() {
		writeToNBT();
	}

	private void readFromNBT() {
		CompoundTag tag = satchel.getData();
		ListTag list = tag.getList("Items");

		for (int i = 0; i < list.tagCount(); i++) {
			CompoundTag entry = (CompoundTag) list.tagAt(i);
			int slot = entry.getByte("Slot") & 255;
			if (slot >= 0 && slot < items.length) {
				items[slot] = ItemStack.readItemStackFromNbt(entry);
			}
		}
	}

	private void writeToNBT() {
		ListTag list = new ListTag();

		for (int i = 0; i < items.length; i++) {
			if (items[i] != null) {
				CompoundTag entry = new CompoundTag();
				entry.putByte("Slot", (byte) i);
				items[i].writeToNBT(entry);
				list.addTag(entry);
			}
		}

		satchel.getData().put("Items", list);
	}
}
