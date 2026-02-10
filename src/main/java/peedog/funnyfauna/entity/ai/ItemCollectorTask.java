package peedog.funnyfauna.entity.ai;

import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.EntityItem;
import net.minecraft.core.world.World;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.util.phys.AABB;
import peedog.funnyfauna.entity.MobTaskrunner;
import peedog.funnyfauna.entity.ai.i.IItemHolder;

import java.util.List;

public class ItemCollectorTask<T extends MobTaskrunner & IItemHolder> extends PathTask<T> {
	private EntityItem targetItem = null;
	private int searchCooldown = 0;
	private int pickupCooldown = 0;

	public ItemCollectorTask(T mob) {
		super(mob);
	}

	@Override
	protected void onStart() {
		// Nothing to do
	}

	@Override
	public Task onTick() {
		// If already carrying an item, task is done
		if (mob.getHeldItem() != null) {
			return null;
		}

		// Handle pickup cooldown
		if (pickupCooldown > 0) {
			pickupCooldown--;
		}

		// Find new item to collect
		if (targetItem == null || targetItem.removed || searchCooldown <= 0) {
			targetItem = findNearbyItem();
			searchCooldown = 20;
		}

		if (targetItem != null && pickupCooldown == 0) {
			// Create path to item
			if (this.path == null || this.path.isDone()) {
				this.path = mob.world.getEntityPathToXYZ(
					mob,
					MathHelper.floor(targetItem.x),
					MathHelper.floor(targetItem.y),
					MathHelper.floor(targetItem.z),
					12.0F
				);
			}

			// Follow path
			Task result = super.onTick();

			// Check if close enough to pick up (like wolf does)
			double distance = targetItem.distanceTo(mob);
			if (distance < 1.5F) {
				// Wolf-style pickup: check if item is valid
				if (!targetItem.removed && targetItem.item != null && targetItem.item.stackSize > 0 && targetItem.pickupDelay == 0) {
					mob.setHeldItem(targetItem.item.copy());
					targetItem.item.stackSize = 0;
					targetItem.removed = true;
					targetItem = null;
					this.path = null;
					pickupCooldown = 10; // Prevent immediate repickup

					// Force immediate return to home
					return null; // Let AntTask handle the state change
				}
			}

			// If we can't reach the item after a while, give up
			if (this.path != null && this.path.isDone() && distance > 3.0F) {
				targetItem = null;
				this.path = null;
			}

			return result;
		}

		return null;
	}

	private EntityItem findNearbyItem() {
		World world = mob.world;
		AABB searchBox = AABB.getTemporaryBB(
			mob.x, mob.y, mob.z,
			mob.x + 1.0F, mob.y + 1.0F, mob.z + 1.0F
		).grow(10.0F, 4.0F, 10.0F); // Increased range

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
	protected boolean isEqual(Task other) {
		return other instanceof ItemCollectorTask;
	}
}
