package peedog.funnyfauna.entity.ai;

import net.minecraft.core.entity.Entity;
import net.minecraft.core.util.helper.MathHelper;
import peedog.funnyfauna.entity.MobTaskrunner;
import peedog.funnyfauna.entity.ai.i.IFlyable;
import peedog.funnyfauna.entity.lizard.MobLizard;
import peedog.funnyfauna.entity.ai.i.IFleeable;

public class FleeFromDangerTask<T extends MobTaskrunner & IFleeable> extends PathTask<T> {
	public FleeFromDangerTask(T mob) {
		super(mob);
		this.moveSpeed = 5.0F;
	}

	@Override
	protected void onStart() {
		if (mob instanceof IFlyable) {
			IFlyable flyable = (IFlyable) mob;
			if (!flyable.isFlying()) {
				flyable.setFlying(true);
				flyable.setFlightTime(0);

				// Clear any ground path
				this.path = null;
			}
		}
	}

	@Override
	public Task onTick() {
		if (mob.getFleeTimer() <= 0 || mob.getFleeTarget() == null) return null;

		mob.setFleeTimer(mob.getFleeTimer() - 1);
		Entity danger = mob.getFleeTarget();

		// Pathfinding away from the danger
		if (this.path == null || mob.world.rand.nextInt(10) == 0) {
			double dx = mob.x - danger.x;
			double dz = mob.z - danger.z;
			double dist = Math.sqrt(dx * dx + dz * dz);
			int tx = MathHelper.floor(mob.x + (dx / dist) * 8.0);
			int tz = MathHelper.floor(mob.z + (dz / dist) * 8.0);
			this.path = mob.world.getEntityPathToXYZ(mob, tx, (int)mob.y, tz, 16.0F);
		}

		if (this.path != null) {
			mob.setMoveForward(this.moveSpeed);
		}

		super.onTick();
		return null;
	}

	@Override
	protected boolean isEqual(Task other) {
		return false;
	}
}
