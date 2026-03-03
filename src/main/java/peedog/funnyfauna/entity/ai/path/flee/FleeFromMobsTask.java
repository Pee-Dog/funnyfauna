package peedog.funnyfauna.entity.ai.path.flee;

import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.Mob;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.util.helper.MathHelper;
import peedog.funnyfauna.entity.MobTaskrunner;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.path.PathTask;
import peedog.funnyfauna.entity.lizard.MobLizard;

import java.util.List;
import java.util.function.Supplier;

public class FleeFromMobsTask<T extends MobTaskrunner> extends PathTask<T> {

	private final Supplier<Boolean> shouldFlee;

	public FleeFromMobsTask(T mob, Supplier<Boolean> shouldFlee) {
		super(mob);
		this.shouldFlee = shouldFlee;
		this.moveSpeed = 1.5F; // 5.0F is too fast and often breaks pathfinding
	}

	@Override
	protected void onStart() {
		this.path = null;
	}

	/**
	 * Helper to find the nearest threat.
	 */
	public Entity findThreat() {
		Entity threat = null;
		double closest = Double.MAX_VALUE;

		// Increased range to 12.0 so players don't instantly reach the lizard
		List<Entity> nearby = mob.world.getEntitiesWithinAABBExcludingEntity(
			mob, mob.bb.expand(12.0, 4.0, 12.0)
		);

		for (Entity e : nearby) {
			// Check for Mobs or Players
			if ((e instanceof Mob && e.getClass() != mob.getClass()) || e instanceof Player) {

				// Don't flee from the owner if tamed
				if (mob instanceof MobLizard && e instanceof Player) {
					MobLizard lizard = (MobLizard) mob;
					if (lizard.isTamed() && ((Player) e).uuid.toString().equals(lizard.getOwnerUUID())) {
						continue;
					}
				}

				double d = mob.distanceTo(e);
				if (d < closest) {
					closest = d;
					threat = e;
				}
			}
		}
		return threat;
	}

	@Override
	public Task onTick() {
		if (!shouldFlee.get()) {
			return null;
		}

		Entity threat = findThreat();

		if (threat == null) {
			this.path = null;
			return null;
		}

		// Calculate escape vector
		double dx = mob.x - threat.x;
		double dz = mob.z - threat.z;
		double dist = Math.sqrt(dx * dx + dz * dz);

		// Update path away from threat
		if (dist > 0 && (this.path == null || mob.world.rand.nextInt(10) == 0)) {
			int tx = MathHelper.floor(mob.x + (dx / dist) * 8.0);
			int tz = MathHelper.floor(mob.z + (dz / dist) * 8.0);
			this.path = mob.world.getEntityPathToXYZ(mob, tx, (int) mob.y, tz, 16.0F);
		}

		if (this.path != null) {
			mob.setMoveForward(this.moveSpeed);
			super.onTick(); // Move along the path
		}

		// MUST return null to avoid StackOverflowError in recursive AI engines
		return null;
	}

	@Override
	protected boolean isEqual(Task other) {
		return other instanceof FleeFromMobsTask;
	}
}
