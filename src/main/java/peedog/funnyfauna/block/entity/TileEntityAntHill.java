package peedog.funnyfauna.block.entity;

import com.mojang.nbt.tags.CompoundTag;
import com.mojang.nbt.tags.ListTag;
import net.minecraft.core.block.entity.TileEntity;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.player.inventory.container.Container;
import net.minecraft.core.util.phys.AABB;
import net.minecraft.core.world.World;
import net.minecraft.core.world.season.SeasonManager;
import net.minecraft.core.world.season.SeasonWinter;
import net.minecraft.core.world.season.Seasons;
import net.minecraft.core.world.type.WorldTypes;
import org.jetbrains.annotations.Nullable;
import peedog.funnyfauna.entity.ant.EntityAnt;

import java.util.ArrayList;
import java.util.List;

public class TileEntityAntHill extends TileEntity implements Container {

	private ItemStack[] storedItems = new ItemStack[27];
	private List<CompoundTag> storedAnts = new ArrayList<>();

	private static final int MAX_ANTS = 20;

	// How long a released ant must wait before it can re-enter (in ticks)
	private static final int EXIT_COOLDOWN_TICKS = 200;

	private int checkTimer = 0;

	// Add this field
	public boolean silkTouchPickup = false;

	// Add this getter
	public List<CompoundTag> getStoredAnts() {
		return storedAnts;
	}

	// Inside TileEntityAntHill.java

	@Override
	public void tick() {
		super.tick();
		if (this.worldObj == null || this.worldObj.isClientSide) return;

		// Check for nearby ants once per second
		checkTimer++;
		if (checkTimer >= 20) {
			checkTimer = 0;
			checkForNearbyAnts();
		}

		// MODIFIED: Added environmental checks
		if (getStoredAntCount() > 0 && canReleaseAnts() && worldObj.rand.nextInt(100) == 0) {
			releaseAnts(worldObj, 1);
		}
	}

	private boolean canReleaseAnts() {
		// 1. Check for Winter (BTA specific WorldType check)
		if (worldObj.getSeasonManager().getCurrentSeason() == Seasons.OVERWORLD_WINTER) {
			return false;
		}

		// 2. Check Light Level (Too dark)
		// 8 is usually the threshold where hostile mobs spawn; good for "daylight" creatures
		if (worldObj.getBlockLightValue(x, y + 1, z) < 8) {
			return false;
		}

		// 3. Check Rain + Sky access
		// If it's raining AND the block can see the sky, it's getting wet.
		if (worldObj.canBlockBeRainedOn(x, y + 1, z)) {
			return false;
		}

		return true;
	}

	private void checkForNearbyAnts() {
		AABB searchBox = AABB.getTemporaryBB(
			this.x, this.y, this.z,
			this.x + 1, this.y + 1, this.z + 1
		).grow(16.0, 6.0, 16.0);

		List<EntityAnt> nearbyAnts = worldObj.getEntitiesWithinAABB(EntityAnt.class, searchBox);
		for (EntityAnt ant : nearbyAnts) {
			handleNearbyAnt(ant);
		}
	}

	private void handleNearbyAnt(EntityAnt ant) {
		double dx = (this.x + 0.5) - ant.x;
		double dy = (this.y + 0.5) - ant.y;
		double dz = (this.z + 0.5) - ant.z;
		double distanceSq = dx * dx + dy * dy + dz * dz;

		// Claim homeless ants that wander close enough
		if (!ant.hasHome()) {
			if (this.getStoredAntCount() < MAX_ANTS && distanceSq < 64.0) {
				ant.setHome(this.x, this.y, this.z);
			}
			return;
		}

		// Only absorb ants whose home is this hill
		if (ant.getHomeX() != this.x || ant.getHomeY() != this.y || ant.getHomeZ() != this.z) return;

		// Must be close AND past the exit cooldown
		if (distanceSq < 1.0) {
			storeAnt(ant);
		}
	}

	// --- Ant Storage ---

	/**
	 * Attempts to store an ant. Returns false if the ant is still in its exit cooldown
	 * or the hill is full.
	 */
	public boolean storeAnt(EntityAnt ant) {
		if (storedAnts.size() >= MAX_ANTS) return false;

		ItemStack heldItem = ant.getHeldItem();
		// Don't absorb ants that were just released
		if (ant.exitCooldown > 0 && heldItem == null) return false;

		// Deposit any carried item into the hill's inventory
		if (heldItem != null && heldItem.stackSize > 0) {
			addItemToInventory(heldItem.copy());
			ant.setHeldItem(null);
		}

		CompoundTag antData = new CompoundTag();
		ant.addAdditionalSaveData(antData);
		storedAnts.add(antData);

		ant.removed = true;
		this.setChanged();
		return true;
	}

	/** Releases all stored ants. */
	public void releaseAnts(World world) {
		if (world.isClientSide) return;

		for (CompoundTag antData : storedAnts) {
			spawnAnt(world, antData);
		}
		storedAnts.clear();
		this.setChanged();
	}

	/** Releases up to {@code count} stored ants. */
	public void releaseAnts(World world, int count) {
		if (world.isClientSide) return;

		int toRelease = Math.min(count, storedAnts.size());
		for (int i = 0; i < toRelease; i++) {
			spawnAnt(world, storedAnts.remove(0));
		}
		this.setChanged();
	}

	private void spawnAnt(World world, CompoundTag antData) {
		EntityAnt ant = new EntityAnt(world);
		ant.readAdditionalSaveData(antData);

		double spawnX = this.x + 0.5;
		double spawnY = this.y + 1.0;
		double spawnZ = this.z + 0.5;

		ant.setPos(spawnX, spawnY, spawnZ);
		ant.setHome(this.x, this.y, this.z);

		// Give the ant time to walk away before it can re-enter
		ant.exitCooldown = EXIT_COOLDOWN_TICKS;

		world.entityJoinedWorld(ant);
	}

	public void populateWithDefaultAnts(int count) {
		if (this.worldObj == null) return;

		for (int i = 0; i < count; i++) {
			if (this.storedAnts.size() >= MAX_ANTS) break; // Respect the limit

			// Create a temporary ant to generate the default NBT data
			EntityAnt tempAnt = new EntityAnt(this.worldObj);
			CompoundTag antData = new CompoundTag();
			tempAnt.addAdditionalSaveData(antData);

			// Add to storage and mark tile as changed for saving
			this.storedAnts.add(antData);
		}
		this.setChanged();
	}

	// --- Item Helpers ---

	private void addItemToInventory(ItemStack stack) {
		if (stack == null || stack.stackSize <= 0) return;

		// Try to merge with existing stacks first
		for (int i = 0; i < storedItems.length && stack.stackSize > 0; i++) {
			if (storedItems[i] != null && storedItems[i].canStackWith(stack)) {
				int space = Math.min(getMaxStackSize() - storedItems[i].stackSize, stack.stackSize);
				storedItems[i].stackSize += space;
				stack.stackSize -= space;
			}
		}

		// Fill empty slots
		for (int i = 0; i < storedItems.length && stack.stackSize > 0; i++) {
			if (storedItems[i] == null) {
				storedItems[i] = stack.copy();
				stack.stackSize = 0;
				break;
			}
		}

		// Drop overflow
		if (stack.stackSize > 0) {
			worldObj.dropItem(x, y + 1, z, stack);
		}
	}

	// --- Container Interface ---

	@Override public int getContainerSize() { return storedItems.length; }
	@Override public @Nullable ItemStack getItem(int index) { return storedItems[index]; }

	@Override
	public @Nullable ItemStack removeItem(int index, int takeAmount) {
		if (storedItems[index] == null) return null;

		if (storedItems[index].stackSize <= takeAmount) {
			ItemStack result = storedItems[index];
			storedItems[index] = null;
			this.setChanged();
			return result;
		}

		ItemStack result = storedItems[index].splitStack(takeAmount);
		if (storedItems[index].stackSize <= 0) storedItems[index] = null;
		this.setChanged();
		return result;
	}

	@Override
	public void setItem(int index, @Nullable ItemStack itemstack) {
		storedItems[index] = itemstack;
		if (itemstack != null && itemstack.stackSize > this.getMaxStackSize()) {
			itemstack.stackSize = this.getMaxStackSize();
		}
		this.setChanged();
	}

	@Override public String getNameTranslationKey() { return "container.anthill.name"; }
	@Override public int getMaxStackSize() { return 64; }

	@Override
	public boolean stillValid(Player player) {
		if (this.worldObj != null && this.worldObj.getTileEntity(this.x, this.y, this.z) == this) {
			return player.distanceToSqr(this.x + 0.5, this.y + 0.5, this.z + 0.5) <= 64.0;
		}
		return false;
	}

	@Override public void sortContainer() {}

	public int getStoredAntCount() { return storedAnts.size(); }
	public int getMaxAnts() { return MAX_ANTS; }

	// --- NBT ---

	@Override
	public void readFromNBT(CompoundTag tag) {
		super.readFromNBT(tag);

		ListTag itemList = tag.getList("Items");
		storedItems = new ItemStack[getContainerSize()];
		for (int i = 0; i < itemList.tagCount(); i++) {
			CompoundTag itemTag = (CompoundTag) itemList.tagAt(i);
			int slot = itemTag.getByte("Slot") & 255;
			if (slot < storedItems.length) {
				storedItems[slot] = ItemStack.readItemStackFromNbt(itemTag);
			}
		}

		ListTag antList = tag.getList("Ants");
		storedAnts.clear();
		for (int i = 0; i < antList.tagCount(); i++) {
			storedAnts.add((CompoundTag) antList.tagAt(i));
		}
	}

	@Override
	public void writeToNBT(CompoundTag tag) {
		super.writeToNBT(tag);

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

		ListTag antList = new ListTag();
		for (CompoundTag antTag : storedAnts) {
			antList.addTag(antTag);
		}
		tag.put("Ants", antList);
	}
}
