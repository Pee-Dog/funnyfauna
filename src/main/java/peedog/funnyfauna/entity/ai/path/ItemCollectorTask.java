package peedog.funnyfauna.entity.ai.path;

import net.minecraft.core.entity.EntityItem;
import net.minecraft.core.world.World;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.util.phys.AABB;
import peedog.funnyfauna.entity.MobTaskrunner;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.interfaces.IItemHolder;

import java.util.List;

public class ItemCollectorTask<T extends MobTaskrunner & IItemHolder> extends PathTask<T> {
	private EntityItem targetItem = null;
	private int searchCooldown = 0;
	private int pickupCooldown = 0;
	private int giveUpTimer = 0;
	private static final int MAX_GIVE_UP_TIME = 100; // 5 seconds

	public ItemCollectorTask(T mob) {
		super(mob);
		this.moveSpeed = 0.8F;
	}

	@Override
	protected void onStart() {
		this.targetItem = null;
		this.searchCooldown = 0;
		this.pickupCooldown = 0;
		this.giveUpTimer = 0;
	}

	@Override
	public Task onTick() {
		// If already carrying an item, task is complete
		if (mob.getHeldItem() != null) {
			return null; // Task complete - successfully picked up item
		}

		// Handle pickup cooldown
		if (pickupCooldown > 0) {
			pickupCooldown--;
		}

		// Handle search cooldown
		if (searchCooldown > 0) {
			searchCooldown--;
		}

		// Find new item to collect if needed
		if (targetItem == null || targetItem.removed || searchCooldown <= 0) {
			targetItem = findNearbyItem();
			searchCooldown = 20; // Search again in 1 second
		}

		// If no item found, give up
		if (targetItem == null) {
			return null; // No items to collect
		}

		// We have a target item
		giveUpTimer++;
		if (giveUpTimer > MAX_GIVE_UP_TIME) {
			// Took too long, give up on this item
			targetItem = null;
			this.path = null;
			giveUpTimer = 0;
			return null; // Give up
		}

		// Create or update path to item
		if (pickupCooldown == 0) {
			if (this.path == null || this.path.isDone()) {
				this.path = mob.world.getEntityPathToXYZ(
					mob,
					MathHelper.floor(targetItem.x),
					MathHelper.floor(targetItem.y),
					MathHelper.floor(targetItem.z),
					16.0F
				);
			}

			// Follow path if we have one
			if (this.path != null && !this.path.isDone()) {
				super.onTick();
			}

			// Check if close enough to pick up
			double distance = targetItem.distanceTo(mob);
			if (distance < 1.5) {
				// Try to pick up the item
				if (!targetItem.removed && targetItem.item != null &&
					targetItem.item.stackSize > 0 && targetItem.pickupDelay == 0) {

					// Pick up the item
					mob.setHeldItem(targetItem.item.copy());
					targetItem.item.stackSize = 0;
					targetItem.removed = true;

					// Task complete
					targetItem = null;
					this.path = null;
					return null; // Successfully picked up item
				}
			}

			// If path failed and we're not close, give up on this item
			if (this.path != null && this.path.isDone() && distance > 3.0) {
				targetItem = null;
				this.path = null;
				giveUpTimer = 0;
			}
		}

		// Continue working (no child task)
		return null;
	}

	private EntityItem findNearbyItem() {
		World world = mob.world;
		AABB searchBox = AABB.getTemporaryBB(
			mob.x, mob.y, mob.z,
			mob.x + 1.0, mob.y + 1.0, mob.z + 1.0
		).grow(12.0, 4.0, 12.0); // Large search radius

		List<EntityItem> items = world.getEntitiesWithinAABB(EntityItem.class, searchBox);
		EntityItem closestItem = null;
		double closestDistance = Double.MAX_VALUE;

		for (EntityItem item : items) {
			if (!item.removed && item.onGround &&
				!item.isInWater() && !item.isInLava() &&
				item.pickupDelay == 0 && item.item != null && item.item.stackSize > 0) {

				double distance = item.distanceToSqr(mob);
				if (distance < closestDistance) {
					closestDistance = distance;
					closestItem = item;
				}
			}
		}
		return closestItem;
	}

	@Override
	protected void onStop(Task interruptTask) {
		super.onStop(interruptTask);
		this.targetItem = null;
		this.giveUpTimer = 0;
	}

	@Override
	protected boolean isEqual(Task other) {
		return other instanceof ItemCollectorTask;
	}

	public boolean hasTarget() {
		return this.targetItem != null && !this.targetItem.removed;
	}
}
