package peedog.funnyfauna.entity.ai.path.follow;

import net.minecraft.core.entity.Entity;
import net.minecraft.core.util.phys.AABB;
import peedog.funnyfauna.entity.MobTaskrunner;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.path.PathTask;
import peedog.funnyfauna.entity.cricket.EntityCricket;
import peedog.funnyfauna.entity.worm.EntityWorm;

import java.util.ArrayList;
import java.util.List;

public class HuntPreyTask<T extends MobTaskrunner> extends PathTask<T> {
	private Entity preyTarget = null;

	public HuntPreyTask(T mob) {
		super(mob);
	}

	@Override
	protected void onStart() {
		this.preyTarget = null;
	}

	@Override
	public Entity lookTarget() {
		return preyTarget;
	}

	@Override
	public Task onTick() {
		// 1. Validate existing target
		if (preyTarget != null && (preyTarget.isRemoved() || !preyTarget.isAlive())) {
			preyTarget = null;
			this.path = null;
		}

		// 2. Search for new prey if we don't have one
		if (preyTarget == null) {
			// Use the Class name AABB instead of the instance mob.bb
			List<EntityCricket> nearbyCrickets = mob.world.getEntitiesWithinAABB(
				EntityCricket.class,
				AABB.getTemporaryBB(mob.x, mob.y, mob.z, mob.x + 1, mob.y + 1, mob.z + 1).grow(10, 5, 10)
			);

			List<EntityWorm> nearbyWorms = mob.world.getEntitiesWithinAABB(
				EntityWorm.class,
				AABB.getTemporaryBB(mob.x, mob.y, mob.z, mob.x + 1, mob.y + 1, mob.z + 1).grow(10, 5, 10)
			);

			List<Entity> allPrey = new ArrayList<>();
			allPrey.addAll(nearbyCrickets);
			allPrey.addAll(nearbyWorms);

			if (!allPrey.isEmpty()) {
				preyTarget = allPrey.get(random.nextInt(allPrey.size()));
			}
		}

		// 3. Handle interaction and pathing
		if (preyTarget != null) {
			// Eat the prey if touching
			if (mob.bb.expand(0.5, 0.5, 0.5).intersects(preyTarget.bb)) {
				preyTarget.outOfWorld();
				preyTarget = null;
				return null;
			}

			// Direct Movement Logic (Armadillo style)
			double dx = preyTarget.x - mob.x;
			double dz = preyTarget.z - mob.z;

			// Face the prey
			mob.yRot = (float)(Math.atan2(dz, dx) * 180 / Math.PI) - 90.0F;

			// Move toward them
			mob.setMoveForward(0.5F); // Adjust speed as needed

			return null;
		}

		return null;
	}

	@Override
	protected boolean isEqual(Task other) {
		return other instanceof HuntPreyTask;
	}
}
