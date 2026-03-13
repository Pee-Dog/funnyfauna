package peedog.funnyfauna.entity.ai.path.follow;

import net.minecraft.core.entity.Entity;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.util.phys.Vec3;
import peedog.funnyfauna.entity.MobTaskrunner;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.interfaces.IAttacker;
import peedog.funnyfauna.entity.ai.path.PathTask;

public class ChaseAndAttackTask<T extends MobTaskrunner> extends PathTask<T> {

	private final float sightRadius = 16.0F;

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
			mob.setMoveForward(0.0F);
			mob.setMoveStrafing(0.0F);
			return null;
		}

		// -----------------------------------------------------------------------
		// Re-pathing — mirrors MobPathfinder exactly:
		// always re-path when path is null; 1/20 chance to refresh an existing one.
		// -----------------------------------------------------------------------
		if (this.path == null || this.random.nextInt(20) == 0) {
			this.path = mob.world.getPathToEntity(mob, target, sightRadius);
		}

		// -----------------------------------------------------------------------
		// Attack check — BB vertical overlap (same as MobMonster.attackEntity).
		// Avoids canEntityBeSeen ray-cast which self-occludes on large hitboxes.
		// -----------------------------------------------------------------------
		if (mob instanceof IAttacker) {
			IAttacker attacker = (IAttacker) mob;
			float distance = mob.distanceTo(target);
			boolean verticalOverlap = target.bb.maxY > mob.bb.minY && target.bb.minY < mob.bb.maxY;
			if (distance <= attacker.getAttackRange() && verticalOverlap && attacker.getAttackCooldown() <= 0) {
				attacker.startAttack(target);
			}
		}

		// -----------------------------------------------------------------------
		// Movement — ported directly from MobPathfinder.updateAI() so behaviour
		// is identical to zombies. We do NOT delegate to super.onTick() (PathTask)
		// because PathTask zeros moveForward unconditionally at the top of every
		// tick. MobPathfinder never zeros it, so momentum carries through the tick
		// where the path finishes (isDone) and coordsForNextPath goes null.
		// That missing carry-over was the freeze — one dead tick per waypoint
		// finish, multiplied by how often the target moves and forces re-paths.
		// -----------------------------------------------------------------------
		int floorY = MathHelper.floor(mob.bb.minY + 0.5F);
		boolean inWater = mob.isInWater();
		boolean inLava  = mob.isInLava();
		mob.xRot = 0.0F;

		if (this.path != null && this.random.nextInt(100) != 0) {
			Vec3 coordsForNextPath = this.path.getPos(mob);
			double d = (double)(mob.bbWidth * 2.0F);

			// Advance past waypoints the mob has already reached
			while (coordsForNextPath != null
				&& coordsForNextPath.distanceToSquared(mob.x, coordsForNextPath.y, mob.z) < d * d) {
				this.path.next();
				if (this.path.isDone()) {
					coordsForNextPath = null;
					this.path = null;
				} else {
					coordsForNextPath = this.path.getPos(mob);
				}
			}

			mob.stopJumping();

			if (coordsForNextPath != null) {
				double dx = coordsForNextPath.x - mob.x;
				double dz = coordsForNextPath.z - mob.z;
				double dy = coordsForNextPath.y - (double) floorY;

				float targetYaw = (float)(Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
				float yawDelta = targetYaw - mob.yRot;

				// Normalise to [-180, 180] and clamp to ±30° — identical to MobPathfinder
				for (; yawDelta < -180.0F; yawDelta += 360.0F) {}
				while (yawDelta >= 180.0F) yawDelta -= 360.0F;
				if (yawDelta >  30.0F) yawDelta =  30.0F;
				if (yawDelta < -30.0F) yawDelta = -30.0F;

				mob.yRot += yawDelta;

				// moveForward is set here, never zeroed at the top — this is the
				// critical difference from PathTask.
				mob.setMoveForward(this.moveSpeed);
				mob.setMoveStrafing(0.0F);

				if (dy > 0.0) {
					mob.startJumping();
				}
			}

			mob.lookAt(target, 30.0F, 30.0F);

			if (mob.horizontalCollision) {
				mob.startJumping();
			}
			if (this.random.nextFloat() < 0.8F && (inWater || inLava)) {
				mob.startJumping();
			}

		} else {
			// Path is null or the 1% random jitter tick.
			// MobPathfinder calls super.updateAI() here — it never just stops.
			// We face the target and keep walking forward, which is what makes
			// zombies walk directly into the player at close range.
			this.path = null;
			double dX = target.x - mob.x;
			double dZ = target.z - mob.z;
			mob.yRot = (float)(Math.atan2(dZ, dX) * 180.0 / Math.PI) - 90.0F;
			mob.lookAt(target, 30.0F, 30.0F);
			mob.setMoveForward(this.moveSpeed);
			mob.setMoveStrafing(0.0F);
			if (mob.horizontalCollision) {
				mob.startJumping();
			}
			if (this.random.nextFloat() < 0.8F && (inWater || inLava)) {
				mob.startJumping();
			}
		}

		return null;
	}

	@Override
	protected void onStop(Task interruptTask) {
		this.path = null;
		mob.setMoveForward(0.0F);
		mob.setMoveStrafing(0.0F);
	}

	@Override
	protected boolean isEqual(Task other) {
		return other instanceof ChaseAndAttackTask;
	}
}
