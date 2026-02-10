package peedog.funnyfauna.block.entity;

import com.mojang.nbt.tags.CompoundTag;
import com.mojang.nbt.tags.ListTag;
import net.minecraft.core.block.entity.TileEntity;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.player.inventory.container.Container;
import net.minecraft.core.world.World;
import org.jetbrains.annotations.Nullable;
import peedog.funnyfauna.entity.ant.EntityAnt;

import java.util.ArrayList;
import java.util.List;

public class TileEntityAntHill extends TileEntity implements Container {

	// Storage for items collected by ants
	private ItemStack[] storedItems = new ItemStack[27]; // 3 rows like a chest

	// Storage for ant data (NBT tags)
	private List<CompoundTag> storedAnts = new ArrayList<>();

	// Maximum ants that can be stored
	private static final int MAX_ANTS = 20;

	// --- Container Interface for Items ---

	@Override
	public int getContainerSize() {
		return storedItems.length;
	}

	@Override
	public @Nullable ItemStack getItem(int index) {
		return storedItems[index];
	}

	@Override
	public @Nullable ItemStack removeItem(int index, int takeAmount) {
		if (storedItems[index] != null) {
			if (storedItems[index].stackSize <= takeAmount) {
				ItemStack itemstack = storedItems[index];
				storedItems[index] = null;
				this.setChanged();
				return itemstack;
			} else {
				ItemStack itemstack1 = storedItems[index].splitStack(takeAmount);
				if (storedItems[index].stackSize <= 0) {
					storedItems[index] = null;
				}
				this.setChanged();
				return itemstack1;
			}
		}
		return null;
	}

	@Override
	public void setItem(int index, @Nullable ItemStack itemstack) {
		storedItems[index] = itemstack;
		if (itemstack != null && itemstack.stackSize > this.getMaxStackSize()) {
			itemstack.stackSize = this.getMaxStackSize();
		}
		this.setChanged();
	}

	@Override
	public String getNameTranslationKey() {
		return "container.anthill.name";
	}

	@Override
	public int getMaxStackSize() {
		return 64;
	}

	@Override
	public boolean stillValid(Player player) {
		if (this.worldObj != null && this.worldObj.getTileEntity(this.x, this.y, this.z) == this) {
			return player.distanceToSqr(
				(double)this.x + 0.5,
				(double)this.y + 0.5,
				(double)this.z + 0.5
			) <= 64.0;
		}
		return false;
	}

	@Override
	public void sortContainer() {
		// Sort inventory if needed
	}

	// --- Ant Storage Methods ---

	/**
	 * Attempts to store an ant in the ant hill.
	 * @param ant The ant to store
	 * @return true if successfully stored, false if ant hill is full
	 */
	public boolean storeAnt(EntityAnt ant) {
		if (storedAnts.size() >= MAX_ANTS) {
			return false; // Ant hill is full
		}

		// Save ant data to NBT
		CompoundTag antData = new CompoundTag();
		ant.addAdditionalSaveData(antData);

		// Store the item the ant was carrying (if any)
		ItemStack heldItem = ant.getHeldItem();
		if (heldItem != null && heldItem.stackSize > 0) {
			// Try to add to inventory
			addItemToInventory(heldItem.copy());
			ant.setHeldItem(null); // Clear the ant's held item
		}

		// Add ant to storage
		storedAnts.add(antData);

		// Remove ant from world
		ant.removed = true;

		this.setChanged();
		return true;
	}

	/**
	 * Releases all stored ants back into the world
	 */
	public void releaseAnts(World world) {
		if (world.isClientSide) return;

		for (CompoundTag antData : storedAnts) {
			// Create new ant entity
			EntityAnt ant = new EntityAnt(world);

			// Restore ant data
			ant.readAdditionalSaveData(antData);

			// Spawn near the ant hill with slight randomness
			double spawnX = this.x + 0.5 + (world.rand.nextDouble() - 0.5) * 2.0;
			double spawnY = this.y + 1.0;
			double spawnZ = this.z + 0.5 + (world.rand.nextDouble() - 0.5) * 2.0;

			ant.setPos(spawnX, spawnY, spawnZ);

			// Make sure home is still set to this ant hill
			ant.setHome(this.x, this.y, this.z);

			world.entityJoinedWorld(ant);
		}

		storedAnts.clear();
		this.setChanged();
	}

	/**
	 * Releases a specific number of ants
	 */
	public void releaseAnts(World world, int count) {
		if (world.isClientSide) return;

		int toRelease = Math.min(count, storedAnts.size());

		for (int i = 0; i < toRelease; i++) {
			CompoundTag antData = storedAnts.remove(0);

			EntityAnt ant = new EntityAnt(world);
			ant.readAdditionalSaveData(antData);

			double spawnX = this.x + 0.5 + (world.rand.nextDouble() - 0.5) * 2.0;
			double spawnY = this.y + 1.0;
			double spawnZ = this.z + 0.5 + (world.rand.nextDouble() - 0.5) * 2.0;

			ant.setPos(spawnX, spawnY, spawnZ);
			ant.setHome(this.x, this.y, this.z);

			world.entityJoinedWorld(ant);
		}

		this.setChanged();
	}

	/**
	 * Helper method to add an item to the first available slot
	 */
	private void addItemToInventory(ItemStack stack) {
		if (stack == null || stack.stackSize <= 0) return;

		// First pass: Try to stack with existing items
		for (int i = 0; i < storedItems.length && stack.stackSize > 0; i++) {
			if (storedItems[i] != null && storedItems[i].canStackWith(stack)) {
				int space = Math.min(
					getMaxStackSize() - storedItems[i].stackSize,
					stack.stackSize
				);
				storedItems[i].stackSize += space;
				stack.stackSize -= space;
			}
		}

		// Second pass: Fill empty slots
		for (int i = 0; i < storedItems.length && stack.stackSize > 0; i++) {
			if (storedItems[i] == null) {
				storedItems[i] = stack.copy();
				stack.stackSize = 0;
				break;
			}
		}

		// If there's still items left, drop them in the world
		if (stack.stackSize > 0) {
			worldObj.dropItem(x, y + 1, z, stack);
		}
	}

	public int getStoredAntCount() {
		return storedAnts.size();
	}

	public int getMaxAnts() {
		return MAX_ANTS;
	}

	// --- NBT Save/Load ---

	@Override
	public void readFromNBT(CompoundTag tag) {
		super.readFromNBT(tag);

		// Load items
		ListTag itemList = tag.getList("Items");
		storedItems = new ItemStack[getContainerSize()];

		for (int i = 0; i < itemList.tagCount(); i++) {
			CompoundTag itemTag = (CompoundTag) itemList.tagAt(i);
			int slot = itemTag.getByte("Slot") & 255;
			if (slot >= 0 && slot < storedItems.length) {
				storedItems[slot] = ItemStack.readItemStackFromNbt(itemTag);
			}
		}

		// Load ants
		ListTag antList = tag.getList("Ants");
		storedAnts.clear();

		for (int i = 0; i < antList.tagCount(); i++) {
			CompoundTag antTag = (CompoundTag) antList.tagAt(i);
			storedAnts.add(antTag);
		}
	}

	@Override
	public void writeToNBT(CompoundTag tag) {
		super.writeToNBT(tag);

		// Save items
		ListTag itemList = new ListTag();
		for (int i = 0; i < storedItems.length; i++) {
			if (storedItems[i] != null) {
				CompoundTag itemTag = new CompoundTag();
				itemTag.putByte("Slot", (byte) i);
				storedItems[i].writeToNBT(itemTag);
				itemList.addTag(itemTag);
			}
		}
		tag.put("Items", itemList);

		// Save ants
		ListTag antList = new ListTag();
		for (CompoundTag antTag : storedAnts) {
			antList.addTag(antTag);
		}
		tag.put("Ants", antList);
	}
}
