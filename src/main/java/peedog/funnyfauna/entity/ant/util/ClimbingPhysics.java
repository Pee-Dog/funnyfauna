package peedog.funnyfauna.entity.ant.util;

import net.minecraft.core.entity.Entity;
import org.lwjgl.util.vector.Vector3f;

/**
 * VERSION 3 – Deterministic, non-floaty, stop–go climbing physics
 */
public class ClimbingPhysics {

	private final Entity entity;
	private final IClimberEntity climber;

	// Strong pull into surface (prevents floating)
	private static final float SURFACE_ADHESION = 0.12f;

	// Hard cutoff for tiny motion
	private static final float MIN_VELOCITY = 0.001f;

	// Whether AI requested movement this tick
	private boolean movedThisTick = false;

	public ClimbingPhysics(Entity entity, IClimberEntity climber) {
		this.entity = entity;
		this.climber = climber;
	}

	public void applyPhysics(Orientation orientation, boolean onSurface) {
		if (onSurface) {
			applyClimbingPhysics(orientation);
		} else {
			applyFallingPhysics();
		}
	}

	private void applyClimbingPhysics(Orientation orientation) {
		Vector3f n = orientation.normal;

		/* -------------------------------
		 * 1. Continuous adhesion
		 * ------------------------------- */
		entity.xd -= n.x * SURFACE_ADHESION;
		entity.yd -= n.y * SURFACE_ADHESION;
		entity.zd -= n.z * SURFACE_ADHESION;

		/* -------------------------------
		 * 2. Remove perpendicular velocity
		 * ------------------------------- */
		float perp =
			(float)(entity.xd * n.x +
				entity.yd * n.y +
				entity.zd * n.z);

		entity.xd -= perp * n.x;
		entity.yd -= perp * n.y;
		entity.zd -= perp * n.z;

		/* -------------------------------
		 * 3. Stop–go logic (NO easing)
		 * ------------------------------- */
		if (!movedThisTick) {
			entity.xd = 0;
			entity.yd = 0;
			entity.zd = 0;
		} else {
			float slipperiness = climber.getBlockSlipperiness(
				(int)Math.floor(entity.x),
				(int)Math.floor(entity.y),
				(int)Math.floor(entity.z)
			);

			entity.xd *= slipperiness;
			entity.yd *= slipperiness;
			entity.zd *= slipperiness;
		}

		/* -------------------------------
		 * 4. Hard cutoff for tiny drift
		 * ------------------------------- */
		if (Math.abs(entity.xd) < MIN_VELOCITY) entity.xd = 0;
		if (Math.abs(entity.yd) < MIN_VELOCITY) entity.yd = 0;
		if (Math.abs(entity.zd) < MIN_VELOCITY) entity.zd = 0;

		snapToSurface(orientation);
	}

	private void applyFallingPhysics() {
		entity.yd -= 0.08;
		entity.xd *= 0.98;
		entity.yd *= 0.98;
		entity.zd *= 0.98;
	}

	private void snapToSurface(Orientation orientation) {
		Vector3f n = orientation.normal;

		if (Math.abs(n.y) < 0.9f) {
			float dist =
				(float)((entity.x - Math.floor(entity.x)) * n.x +
					(entity.y - Math.floor(entity.y)) * n.y +
					(entity.z - Math.floor(entity.z)) * n.z);

			entity.x -= n.x * dist;
			entity.y -= n.y * dist;
			entity.z -= n.z * dist;
		} else if (n.y > 0) {
			entity.y = Math.floor(entity.y) + 1.0;
		}
	}

	public void addMovementForce(float localForward, float localStrafe, Orientation orientation) {
		movedThisTick = true;

		Vector3f n = orientation.normal;
		Vector3f forward;
		Vector3f right;

		if (Math.abs(n.y) < 0.7f) {
			Vector3f up = new Vector3f(0, 1, 0);
			float dot = up.x * n.x + up.y * n.y + up.z * n.z;

			forward = new Vector3f(
				up.x - dot * n.x,
				up.y - dot * n.y,
				up.z - dot * n.z
			);
			forward.normalise();

			right = Vector3f.cross(n, forward, null);
			right.normalise();
		} else {
			forward = new Vector3f(0, 0, 1);
			right = new Vector3f(1, 0, 0);
		}

		float accel = 0.08f;

		entity.xd += forward.x * localForward * accel + right.x * localStrafe * accel;
		entity.yd += forward.y * localForward * accel + right.y * localStrafe * accel;
		entity.zd += forward.z * localForward * accel + right.z * localStrafe * accel;

		// Clamp speed
		float speed = (float)Math.sqrt(entity.xd * entity.xd + entity.yd * entity.yd + entity.zd * entity.zd);
		float max = climber.getMovementSpeed();

		if (speed > max) {
			float s = max / speed;
			entity.xd *= s;
			entity.yd *= s;
			entity.zd *= s;
		}
	}

	/** MUST be called once at end of tick */
	public void endTick() {
		movedThisTick = false;
	}
}
