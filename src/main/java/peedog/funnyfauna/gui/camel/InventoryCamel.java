package peedog.funnyfauna.gui.camel;

import com.mojang.nbt.tags.CompoundTag;
import com.mojang.nbt.tags.ListTag;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.player.inventory.container.Container;
import peedog.funnyfauna.entity.camel.MobCamel;

public class InventoryCamel implements Container {

	private final MobCamel camel;
	ItemStack[] camelItemStacks; // remove final so we can resize

	// Main constructor
	public InventoryCamel(MobCamel camel) {
		this.camel = camel;
		this.camelItemStacks = new ItemStack[camel.getChestCount() * 27];
	}

	// Sub-inventory constructor (points to existing backing array)
	private InventoryCamel(MobCamel camel, ItemStack[] backingArray) {
		this.camel = camel;
		this.camelItemStacks = backingArray;
	}

	/** Resize inventory dynamically (used when attaching chests) */
	public void updateSize(int newSize) {
		if (newSize <= camelItemStacks.length) return; // only grow
		ItemStack[] newStacks = new ItemStack[newSize];
		System.arraycopy(camelItemStacks, 0, newStacks, 0, camelItemStacks.length);
		camelItemStacks = newStacks; // replace the old array — no crash
		setChanged();
	}


	@Override
	public int getContainerSize() {
		return camelItemStacks.length;
	}

	@Override
	public ItemStack getItem(int index) {
		return index >= 0 && index < camelItemStacks.length ? camelItemStacks[index] : null;
	}


	@Override
	public ItemStack removeItem(int index, int count) {
		if (camelItemStacks[index] != null) {
			if (camelItemStacks[index].stackSize <= count) {
				ItemStack stack = camelItemStacks[index];
				camelItemStacks[index] = null;
				setChanged();
				return stack;
			}
			ItemStack split = camelItemStacks[index].splitStack(count);
			if (camelItemStacks[index].stackSize <= 0) camelItemStacks[index] = null;
			setChanged();
			return split;
		}
		return null;
	}

	@Override
	public void setItem(int index, ItemStack stack) {
		camelItemStacks[index] = stack;
		if (stack != null && stack.stackSize > getMaxStackSize()) {
			stack.stackSize = getMaxStackSize();
		}
		setChanged();
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
	public void setChanged() {
		if (!camel.world.isClientSide) {
			camel.setChanged(); // <-- tell the entity to save its NBT
		}
	}

	@Override
	public boolean stillValid(Player player) {
		return camel.isTamed() && camel.ownerName != null && camel.ownerName.equals(player.username);
	}


	@Override
	public void sortContainer() { }

	/** Load from NBT */
	public void readFromNBT(CompoundTag tag) {
		ListTag items = tag.getList("CamelItems");
		for (int i = 0; i < items.tagCount(); i++) {
			CompoundTag itemTag = (CompoundTag) items.tagAt(i);
			int slot = itemTag.getByte("Slot");
			if (slot >= 0 && slot < camelItemStacks.length) {
				camelItemStacks[slot] = ItemStack.readItemStackFromNbt(itemTag);
			}
		}
	}

	/** Save to NBT */
	public void writeToNBT(CompoundTag tag) {
		ListTag items = new ListTag();
		for (int i = 0; i < camelItemStacks.length; i++) {
			if (camelItemStacks[i] != null) {
				CompoundTag itemTag = new CompoundTag();
				itemTag.putByte("Slot", (byte) i);
				camelItemStacks[i].writeToNBT(itemTag);
				items.addTag(itemTag);
			}
		}
		tag.put("CamelItems", items);
	}

	/** Double chest support: return a compound container pointing to the original array */
	public Container getContainerForGui() {
		if (camel.getChestCount() <= 1) return this;
		return new InventoryCamelCompound(this, camelItemStacks);
	}


}
