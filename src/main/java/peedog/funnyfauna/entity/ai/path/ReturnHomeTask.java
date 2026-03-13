package peedog.funnyfauna.entity.ai.path;

import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.util.phys.Vec3;
import peedog.funnyfauna.entity.MobTaskrunner;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.interfaces.IHomeable;

public class ReturnHomeTask<T extends MobTaskrunner & IHomeable> extends PathTask<T> {

	private int pathRecalcTimer = 0;

	// -----------------------------------------------------------------------
	// Stuck detection — count consecutive ticks where the mob has horizontal
	// collision while actively moving forward. That's the direct signal that
	// it is pressing against a wall and not getting through.
	// After STUCK_PATIENCE ticks of continuous collision, strafe for
	// STRAFE_DURATION ticks then stop.
	// -----------------------------------------------------------------------
	private static final int   STUCK_PATIENCE  = 20;  // ticks of wall contact before strafing (1 second)
	private static final int   STRAFE_DURATION = 40;  // ticks to strafe (2 seconds)

	private int   collisionTicks = 0;
	private int   strafeTicks    = 0;
	/** +moveSpeed = strafe right, -moveSpeed = strafe left */
	private float strafeDir      = 0.0F;

	public ReturnHomeTask(T mob) {
		super(mob);
		this.moveSpeed = 1.0F;
	}

	@Override
	protected void onStart() {
		pathRecalcTimer = 0;
		collisionTicks  = 0;
		strafeTicks     = 0;
		strafeDir       = 0.0F;
		this.path = null;
	}

	@Override
	public Task onTick() {
		if (!this.mob.hasHome()) return null;

		// Recalculate path every 2 seconds, or immediately when exhausted / missing.
		// Same 1/20 jitter refresh that MobPathfinder uses.
		if (this.path == null || this.path.isDone() || pathRecalcTimer-- <= 0 || this.random.nextInt(20) == 0) {
			pathRecalcTimer = 40;
			this.path = this.mob.world.getEntityPathToXYZ(
				this.mob,
				mob.getHomeX(), mob.getHomeY(), mob.getHomeZ(),
				32.0F
			);
		}

		// -----------------------------------------------------------------------
		// Stuck detection — horizontalCollision is true every tick the mob is
		// pressing against a wall while moveForward > 0. Count those ticks
		// consecutively; if they pile up past STUCK_PATIENCE, start strafing.
		// Clear the collision counter whenever the mob is moving freely again.
		// -----------------------------------------------------------------------
		if (strafeTicks > 0) {
			// Currently strafing — count down, don't re-accumulate collision ticks
			strafeTicks--;
			if (strafeTicks == 0) {
				strafeDir      = 0.0F;
				collisionTicks = 0;
			}
		} else if (mob.horizontalCollision) {
			// Pressing against a wall
			if (++collisionTicks >= STUCK_PATIENCE) {
				collisionTicks = 0;
				strafeTicks    = STRAFE_DURATION;
				strafeDir      = this.random.nextBoolean() ? this.moveSpeed : -this.moveSpeed;
				this.path      = null; // force a fresh path request
			}
		} else {
			// Moving freely — reset
			collisionTicks = 0;
		}

		// -----------------------------------------------------------------------
		// Movement — same inline loop as ChaseAndAttackTask (ported from
		// MobPathfinder). We do NOT call super.onTick() because PathTask zeros
		// moveForward unconditionally at its top, causing the mob to stop for
		// one tick every time a waypoint chain finishes.
		// -----------------------------------------------------------------------
		int floorY = MathHelper.floor(mob.bb.minY + 0.5F);
		boolean inWater = mob.isInWater();
		boolean inLava  = mob.isInLava();
		mob.xRot = 0.0F;

		if (this.path != null && this.random.nextInt(100) != 0) {
			Vec3 next = this.path.getPos(mob);
			double d = (double)(mob.bbWidth * 2.0F);

			// Advance past waypoints already reached
			while (next != null && next.distanceToSquared(mob.x, next.y, mob.z) < d * d) {
				this.path.next();
				if (this.path.isDone()) {
					next = null;
					this.path = null;
				} else {
					next = this.path.getPos(mob);
				}
			}

			mob.stopJumping();

			if (next != null) {
				double dx = next.x - mob.x;
				double dz = next.z - mob.z;
				double dy = next.y - (double) floorY;

				float targetYaw = (float)(Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
				float yawDelta  = targetYaw - mob.yRot;

				for (; yawDelta < -180.0F; yawDelta += 360.0F) {}
				while (yawDelta >= 180.0F) yawDelta -= 360.0F;
				if (yawDelta >  30.0F) yawDelta =  30.0F;
				if (yawDelta < -30.0F) yawDelta = -30.0F;

				mob.yRot += yawDelta;
				mob.setMoveForward(this.moveSpeed);
				mob.setMoveStrafing(strafeTicks > 0 ? strafeDir * this.moveSpeed : 0.0F);

				if (dy > 0.0) mob.startJumping();
			}

			if (mob.horizontalCollision) mob.startJumping();
			if (this.random.nextFloat() < 0.8F && (inWater || inLava)) mob.startJumping();

		} else {
			// Path is null or the 1% jitter tick — keep walking toward home directly.
			this.path = null;
			double dX = mob.getHomeX() + 0.5 - mob.x;
			double dZ = mob.getHomeZ() + 0.5 - mob.z;
			mob.yRot = (float)(Math.atan2(dZ, dX) * 180.0 / Math.PI) - 90.0F;
			mob.setMoveForward(this.moveSpeed);
			mob.setMoveStrafing(strafeTicks > 0 ? strafeDir * this.moveSpeed : 0.0F);
			if (mob.horizontalCollision) mob.startJumping();
			if (this.random.nextFloat() < 0.8F && (inWater || inLava)) mob.startJumping();
		}

		return null;
	}

	@Override
	protected void onStop(Task interruptTask) {
		this.path      = null;
		strafeTicks    = 0;
		strafeDir      = 0.0F;
		collisionTicks = 0;
		mob.setMoveForward(0.0F);
		mob.setMoveStrafing(0.0F);
	}

	@Override
	protected boolean isEqual(Task other) {
		return other instanceof ReturnHomeTask;
	}
}
