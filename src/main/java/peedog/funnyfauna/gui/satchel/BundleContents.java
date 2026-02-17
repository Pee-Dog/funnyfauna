package peedog.funnyfauna.gui.satchel;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.core.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class BundleContents {

	public static final float MAX_OCCUPANCY = 1.0f;

	private final List<ItemStack> stacks = new ArrayList<ItemStack>();
	private int selectedIndex = -1;

	/* ================= ACCESS ================= */

	public List<ItemStack> getStacks() {
		return stacks;
	}

	public boolean isEmpty() {
		return stacks.isEmpty();
	}

	// 2. Update getOccupancy to calculate dynamically
	public float getOccupancy() {
		float current = 0.0f;
		for (ItemStack s : getStacks()) {
			if (s != null) {
				current += weight(s) * s.stackSize;
			}
		}
		return current;
	}

	/* ================= LOGIC ================= */

	private static float weight(ItemStack stack) {
		return 1.0f / stack.getMaxStackSize();
	}

	// inside BundleContents.java
	// 3. Update maxInsertable to use the method
	public int maxInsertable(ItemStack stack) {
		return Math.max((int)((MAX_OCCUPANCY - getOccupancy()) / weight(stack)), 0);
	}

	/**
	 * Inserts items WITHOUT mutating the input stack.
	 * Returns how many items were inserted (merge + new slot).
	 */
	public int add(ItemStack input) {
		if (input == null || input.stackSize <= 0) return 0;

		int maxByWeight = maxInsertable(input);
		if (maxByWeight <= 0) return 0;

		int toInsert = Math.min(input.stackSize, maxByWeight);
		int inserted = 0;

		// 1️⃣ Merge into existing stacks
		for (int i = 0; i < stacks.size() && toInsert > 0; i++) {
			ItemStack existing = stacks.get(i);

			if (!existing.isItemEqual(input)) continue;

			int space = existing.getMaxStackSize() - existing.stackSize;
			if (space <= 0) continue;

			int merge = Math.min(space, toInsert);
			existing.stackSize += merge;

			inserted += merge;
			toInsert -= merge;

			// Move merged stack to front
			stacks.remove(i);
			stacks.add(0, existing);
			i--; // adjust index after removal
		}

		// 2️⃣ Create new stack if there is still room
		int allowed = Math.min(toInsert, maxInsertable(input));
		if (allowed > 0) {
			ItemStack copy = input.copy();
			copy.stackSize = allowed;
			stacks.add(0, copy);
			inserted += allowed;
			toInsert -= allowed;
		}

		return inserted;
	}

	/**
	 * 3️⃣ Always add a new stack to a brand new slot, ignoring merges
	 * Used for clicking over empty space in the satchel GUI.
	 */
	public void addNewSlot(ItemStack stack) {
		if (stack == null || stack.stackSize <= 0) return;

		ItemStack copy = stack.copy();
		stacks.add(copy);
	}

	/* ================= REMOVAL ================= */

	public ItemStack remove(int index) {
		if (stacks.isEmpty()) return null;
		if (index < 0 || index >= stacks.size()) index = 0;

		ItemStack removed = stacks.remove(index);
		selectedIndex = -1;
		return removed;
	}

	public ItemStack removeFirst() {
		return remove(0);
	}

	/* ================= NBT ================= */

	public void readFromNBT(CompoundTag tag) {
		stacks.clear();

		if (!tag.containsKey("SatchelCount")) return;

		int count = tag.getInteger("SatchelCount");
		selectedIndex = tag.getInteger("SatchelSelected");

		for (int i = 0; i < count; i++) {
			CompoundTag itemTag = tag.getCompound("SatchelItem" + i);
			ItemStack stack = ItemStack.readItemStackFromNbt(itemTag);
			if (stack != null && stack.stackSize > 0) {
				stacks.add(stack);
			}
		}
	}

	public void writeToNBT(CompoundTag tag) {
		tag.putInt("SatchelCount", stacks.size());
		tag.putInt("SatchelSelected", selectedIndex);

		for (int i = 0; i < stacks.size(); i++) {
			CompoundTag itemTag = new CompoundTag();
			stacks.get(i).writeToNBT(itemTag);
			tag.putCompound("SatchelItem" + i, itemTag);
		}
	}
}
