package peedog.funnyfauna.entity.ai;

import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.Mob;
import net.minecraft.core.util.helper.MathHelper;
import peedog.funnyfauna.entity.MobTaskrunner;
import peedog.funnyfauna.entity.ai.i.IFleeable;

import java.util.List;

public class FleeFromMobsTask<T extends MobTaskrunner & IFleeable> extends PathTask<T> {
	public FleeFromMobsTask(T mob) {
		super(mob);
		this.moveSpeed = 2.5F;
	}

	@Override
	protected void onStart() {

	}

	@Override
	public Task onTick() {
		// 1. Scan for scary things (any Mob that isn't the same species)
		List<Entity> nearby = mob.world.getEntitiesWithinAABBExcludingEntity(mob, mob.bb.expand(6.0, 4.0, 6.0));
		Entity threat = null;

		for (Entity e : nearby) {
			if (e instanceof Mob && !(e.getClass().equals(mob.getClass()))) {
				threat = e;
				break;
			}
		}

		if (threat != null) {
			mob.setFleeTarget(threat);
			mob.setFleeTimer(40); // Short panic burst
		}

		// 2. If we are in panic mode, run!
		if (mob.getFleeTimer() > 0 && mob.getFleeTarget() != null) {
			mob.setFleeTimer(mob.getFleeTimer() - 1);
			Entity danger = mob.getFleeTarget();

			// Calculate retreat point (Same as Lizard logic)
			double dx = mob.x - danger.x;
			double dz = mob.z - danger.z;
			double dist = Math.sqrt(dx * dx + dz * dz);
			if (dist > 0) {
				int tx = MathHelper.floor(mob.x + (dx / dist) * 6.0);
				int tz = MathHelper.floor(mob.z + (dz / dist) * 6.0);
				if (this.path == null || mob.world.rand.nextInt(5) == 0) {
					this.path = mob.world.getEntityPathToXYZ(mob, tx, (int)mob.y, tz, 16.0F);
				}
			}
			super.onTick();
		} else {
			this.path = null;
		}

		return null;
	}

	@Override
	protected boolean isEqual(Task other) {
		return false;
	}
}
