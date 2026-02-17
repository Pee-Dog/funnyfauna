package peedog.funnyfauna.item;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.Item;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.player.inventory.slot.Slot;
import peedog.funnyfauna.gui.satchel.BundleContents;

public class ItemSatchel extends Item {

	public final int satchelSize;

	public ItemSatchel(String translationKey, String namespaceId, int id) {
		this(translationKey, namespaceId, id, 9);
	}

	public ItemSatchel(String translationKey, String namespaceId, int id, int satchelSize) {
		super(translationKey, namespaceId, id);
		this.maxStackSize = 1;
		this.satchelSize = satchelSize;
		this.setMaxDamage(64); // Set max damage for durability bar
	}


	/* ================= CONTENT ACCESS ================= */

	public static BundleContents getContents(ItemStack stack) {
		BundleContents contents = new BundleContents();
		CompoundTag tag = stack.getData();
		if (tag != null) {
			contents.readFromNBT(tag);
		}
		return contents;
	}

	public static void saveContents(ItemStack stack, BundleContents contents) {
		CompoundTag tag = stack.getData();
		if (tag == null) {
			tag = new CompoundTag();
			stack.setData(tag);
		}
		contents.writeToNBT(tag);
	}

	/* ================= DURABILITY BAR ================= */

	@Override
	public boolean showFullDurability() {
		return true; // Always show the durability bar
	}

	@Override
	public int getMaxDamageForStack(ItemStack itemStack) {
		return 64; // Max occupancy represented as 64 "damage" points
	}

	/**
	 * Update the durability bar to represent fullness.
	 * When empty, metadata = 0 (full bar).
	 * When full, metadata = 64 (no bar).
	 */
	public static void updateDurability(ItemStack stack) {
		BundleContents contents = getContents(stack);
		float occupancy = contents.getOccupancy();
		int damage = (int)(occupancy * 64);
		stack.setMetadata(damage);
	}

	/* ================= INVENTORY INTERACTION ================= */

	@Override
	public boolean hasInventoryInteraction() {
		return true;
	}

	@Override
	public ItemStack onInventoryInteract(Player player, Slot slot, ItemStack stackInSlot, boolean isItemGrabbed) {
		// This allows right-clicking the satchel anywhere in inventory to open it
		// The actual GUI opening is handled in ScreenContainerAbstractMixin
		return stackInSlot;
	}
}
