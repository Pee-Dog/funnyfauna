package peedog.funnyfauna.entity.ai.path.flee;

import net.minecraft.core.entity.Entity;
import net.minecraft.core.util.helper.MathHelper;
import peedog.funnyfauna.entity.MobTaskrunner;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.interfaces.IFleeable;
import peedog.funnyfauna.entity.ai.path.PathTask;

/**
 * Flees from whatever entity MobSasquatch.onLivingUpdate() stored in
 * IFleeable.  Mirrors FleeFromDangerTask exactly — the mob owns the
 * flee state; this task just consumes it.  Always returns null so the
 * task system doesn't stack-overflow.
 */
public class AvoidPlayerTask<T extends MobTaskrunner & IFleeable> extends PathTask<T> {

	public AvoidPlayerTask(T mob) {
		super(mob);
		this.moveSpeed = 5F;
	}

	@Override
	protected void onStart() {
		this.path = null;
	}

	@Override
	public Task onTick() {
		if (mob.getFleeTimer() <= 0 || mob.getFleeTarget() == null) {
			return null;
		}

		mob.setFleeTimer(mob.getFleeTimer() - 1);
		Entity danger = mob.getFleeTarget();

		// Recalculate path every ~10 ticks so it keeps course-correcting
		if (this.path == null || mob.world.rand.nextInt(10) == 0) {
			double dx = mob.x - danger.x;
			double dz = mob.z - danger.z;
			double dist = Math.sqrt(dx * dx + dz * dz);

			if (dist < 0.01) {
				// Exactly on top — pick a random direction
				double angle = mob.world.rand.nextDouble() * Math.PI * 2.0;
				dx = Math.cos(angle);
				dz = Math.sin(angle);
				dist = 1.0;
			}

			int tx = MathHelper.floor(mob.x + (dx / dist) * 16.0);
			int tz = MathHelper.floor(mob.z + (dz / dist) * 16.0);
			this.path = mob.world.getEntityPathToXYZ(mob, tx, (int) mob.y, tz, 32.0F);
		}

		super.onTick();
		return null;
	}

	@Override
	protected boolean isEqual(Task other) {
		return other instanceof AvoidPlayerTask;
	}
}
