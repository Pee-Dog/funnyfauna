package peedog.funnyfauna.entity.ai.path.follow;

import net.minecraft.core.entity.Entity;
import net.minecraft.core.util.helper.DamageType;
import peedog.funnyfauna.entity.MobTaskrunner;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.interfaces.IAttacker;
import peedog.funnyfauna.entity.ai.path.PathTask;

public class ChaseAndAttackTask<T extends MobTaskrunner> extends PathTask<T> {

	private final float sightRadius = 16.0F;
	private final float attackRange = 2.5F;
	private int attackCooldown = 0;

	public ChaseAndAttackTask(T mob) {
		super(mob);
		this.moveSpeed = 20.0F;
	}

	@Override
	protected void onStart() {

	}

	@Override
	public Entity lookTarget() {
		return mob.getTarget();
	}

	@Override
	public Task onTick() {
		Entity target = lookTarget();

		if (target == null || !target.isAlive()) {
			this.path = null;
			return null;
		}

		// Pathfind toward target
		this.path = mob.world.getPathToEntity(mob, target, sightRadius);


		// If mob implements IAttacker → attack logic
		if (mob instanceof IAttacker) {
			IAttacker attacker = (IAttacker) mob;
			float distance = mob.distanceTo(target);

			// Only start attack if not already attacking
			if (distance <= attacker.getAttackRange() && mob.canEntityBeSeen(target) && attacker.getAttackCooldown() <= 0) {
				attacker.startAttack(target);
			}

		}



		return super.onTick();
	}



	@Override
	protected boolean isEqual(Task other) {
		return other instanceof ChaseAndAttackTask;
	}
}
