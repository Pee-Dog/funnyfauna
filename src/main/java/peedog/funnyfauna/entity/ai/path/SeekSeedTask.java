package peedog.funnyfauna.entity.ai.path;

import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.EntityItem;
import net.minecraft.core.item.ItemSeeds;
import net.minecraft.core.util.phys.AABB;
import peedog.funnyfauna.entity.MobTaskrunner;
import peedog.funnyfauna.entity.ai.Task;

import java.util.List;

public class SeekSeedTask<T extends MobTaskrunner> extends PathTask<T> {
	private EntityItem targetSeed;

	public SeekSeedTask(T mob) {
		super(mob);
	}

	@Override
	protected void onStart() {
		targetSeed = null;
	}

	public boolean hasTarget() {
		return targetSeed != null && !targetSeed.removed;
	}

	@Override
	public Entity lookTarget() {
		return targetSeed;
	}

	@Override
	public Task onTick() {
		// Find seeds if we don't have a target
		if (targetSeed == null || targetSeed.removed) {
			findNearestSeed();
		}

		// Check if we've reached the seed
		if (targetSeed != null && !targetSeed.removed) {
			// Pick up seed if close enough
			if (mob.bb.expand(0.5, 2.0, 0.5).intersects(targetSeed.bb)) {
				targetSeed.remove();
				onItemPickedUp(targetSeed);
				targetSeed = null;
				this.path = null;
				return null;
			}

			// Path to seed
			if (this.path == null) {
				this.path = mob.world.getEntityPathToXYZ(
					mob,
					(int) targetSeed.x,
					(int) targetSeed.y,
					(int) targetSeed.z,
					16.0F
				);
			}
		} else {
			this.path = null;
			return null;
		}

		return super.onTick();
	}

	/**
	 * Called when an item is successfully picked up.
	 * Override this in subclasses for custom behavior.
	 */
	protected void onItemPickedUp(EntityItem item) {
		// Default: do nothing
		// Subclasses or specific implementations can override
	}

	private void findNearestSeed() {
		List<EntityItem> nearbyItems = mob.world.getEntitiesWithinAABB(
			EntityItem.class,
			AABB.getTemporaryBB(mob.x, mob.y, mob.z, mob.x + 1, mob.y + 1, mob.z + 1).grow(16, 4, 16)
		);

		for (EntityItem item : nearbyItems) {
			if (item.item.getItem() instanceof ItemSeeds) {
				targetSeed = item;
				break;
			}
		}
	}

	@Override
	protected boolean isEqual(Task other) {
		return other instanceof SeekSeedTask;
	}
}
