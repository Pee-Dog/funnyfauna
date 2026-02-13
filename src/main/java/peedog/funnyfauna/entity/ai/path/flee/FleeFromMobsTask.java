package peedog.funnyfauna.entity.ai.path.flee;

import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.Mob;
import net.minecraft.core.util.helper.MathHelper;
import peedog.funnyfauna.entity.MobTaskrunner;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.path.PathTask;

import java.util.List;
import java.util.function.Supplier;

public class FleeFromMobsTask<T extends MobTaskrunner> extends PathTask<T> {

	private final Supplier<Boolean> shouldFlee;

	public FleeFromMobsTask(T mob, Supplier<Boolean> shouldFlee) {
		super(mob);
		this.shouldFlee = shouldFlee;
		this.moveSpeed = 2.5F;
	}

	@Override
	protected void onStart() {
		this.path = null;
	}

	@Override
	public Task onTick() {
		// Only active if should flee
		if (!shouldFlee.get()) {
			return null;
		}

		// Find nearest threat
		Entity threat = null;
		double closest = Double.MAX_VALUE;

		List<Entity> nearby = mob.world.getEntitiesWithinAABBExcludingEntity(
			mob, mob.bb.expand(6.0, 4.0, 6.0)
		);

		for (Entity e : nearby) {
			if ((e instanceof Mob && e.getClass() != mob.getClass())
				|| e instanceof net.minecraft.core.entity.player.Player) {

				double d = mob.distanceTo(e);
				if (d < closest) {
					closest = d;
					threat = e;
				}
			}
		}

		// No threat → stop fleeing
		if (threat == null) {
			return null;
		}

		// Move away from threat
		double dx = mob.x - threat.x;
		double dz = mob.z - threat.z;
		double dist = Math.sqrt(dx * dx + dz * dz);

		if (dist > 0 && (this.path == null || mob.world.rand.nextInt(5) == 0)) {
			int tx = MathHelper.floor(mob.x + (dx / dist) * 6.0);
			int tz = MathHelper.floor(mob.z + (dz / dist) * 6.0);
			this.path = mob.world.getEntityPathToXYZ(mob, tx, (int) mob.y, tz, 16.0F);
		}

		if (this.path != null) {
			mob.setMoveForward(this.moveSpeed);
		}

		super.onTick();
		return null; // stay in flee task
	}

	@Override
	protected boolean isEqual(Task other) {
		return other instanceof FleeFromMobsTask;
	}
}
