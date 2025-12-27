package peedog.funnyfauna.gui;

import com.mojang.nbt.tags.CompoundTag;
import com.mojang.nbt.tags.ListTag;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.player.inventory.InventorySorter;
import net.minecraft.core.player.inventory.container.Container;
import peedog.funnyfauna.item.ItemSatchel;

public class InventorySatchel implements Container {
	public final ItemStack stack;
	protected ItemStack[] satchelItemStacks;
	public InventorySatchel(final ItemStack stack){
		assert stack.getItem() instanceof ItemSatchel;
		this.stack = stack;
		final ItemSatchel itemSatchel = (ItemSatchel) stack.getItem();
		this.satchelItemStacks = new ItemStack[itemSatchel.satchelSize];
		readFromNBT(stack.getData());
	}
	@Override
	public int getContainerSize(){
		return this.satchelItemStacks.length;
	}

	@Override
	public ItemStack getItem(final int i) {
		return this.satchelItemStacks[i];
	}

	@Override
	public ItemStack removeItem(final int i, final int j) {
		if (this.satchelItemStacks[i] != null) {
			if (this.satchelItemStacks[i].stackSize <= j) {
				final ItemStack itemstack = this.satchelItemStacks[i];
				this.satchelItemStacks[i] = null;
				return itemstack;
			}
			final ItemStack itemstack1 = this.satchelItemStacks[i].splitStack(j);
			if (this.satchelItemStacks[i].stackSize <= 0) {
				this.satchelItemStacks[i] = null;
			}
			return itemstack1;
		}
		return null;
	}

	@Override
	public void setItem(final int i, final ItemStack itemStack) {
		this.satchelItemStacks[i] = itemStack;
		if (itemStack != null && itemStack.stackSize > this.getMaxStackSize()) {
			itemStack.stackSize = this.getMaxStackSize();
		}
	}

	@Override
	public String getNameTranslationKey() {
		final String name = this.stack.getCustomName();
		if (name != null){
			return name;
		}
		return this.stack.getDisplayName();
	}

	@Override
	public int getMaxStackSize() {
		return 64;
	}

	@Override
	public void setChanged() {
		writeToNBT(this.stack.getData());
	}

	public final void readFromNBT(final CompoundTag nbttagcompound) {
		final ListTag nbttaglist = nbttagcompound.getList("Items");
		this.satchelItemStacks = new ItemStack[this.getContainerSize()];
		for (int i = 0; i < nbttaglist.tagCount(); ++i) {
			final CompoundTag nbttagcompound1 = (CompoundTag)nbttaglist.tagAt(i);
			final byte byte0 = nbttagcompound1.getByte("Slot");
			if (byte0 < 0 || byte0 >= this.satchelItemStacks.length) continue;
			this.satchelItemStacks[byte0] = ItemStack.readItemStackFromNbt(nbttagcompound1);
		}
	}
	public void writeToNBT(final CompoundTag nbttagcompound) {
		final ListTag nbttaglist = new ListTag();
		for (int i = 0; i < this.satchelItemStacks.length; ++i) {
			if (this.satchelItemStacks[i] == null) continue;
			final CompoundTag nbttagcompound1 = new CompoundTag();
			nbttagcompound1.putByte("Slot", (byte)i);
			this.satchelItemStacks[i].writeToNBT(nbttagcompound1);
			nbttaglist.addTag(nbttagcompound1);
		}
		nbttagcompound.put("Items", nbttaglist);
	}
	@Override
	public boolean stillValid(final Player entityPlayer) {
		if (entityPlayer.getHeldItem() == null){
			return false;
		}
		final ItemStack heldItem = entityPlayer.getHeldItem();
		return heldItem.getItem() == this.stack.getItem() &&
			heldItem.getMetadata() == this.stack.getMetadata() &&
			heldItem.stackSize == this.stack.stackSize;
	}

	@Override
	public void sortContainer() {
		InventorySorter.sortInventory(this.satchelItemStacks);
	}
}
