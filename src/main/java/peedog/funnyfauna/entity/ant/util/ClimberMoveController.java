package peedog.funnyfauna.entity.ant.util;

import net.minecraft.core.entity.Mob;
import net.minecraft.core.util.helper.MathHelper;
import org.jspecify.annotations.Nullable;
import org.lwjgl.util.vector.Vector3f;

/**
 * Movement controller for climbing entities.
 *
 * NOTE: This is optional and not used by EntityAnt_WORKING.java
 * The working ant implementation handles movement directly in moveEntityWithHeading().
 *
 * This controller could be used for more complex pathfinding in the future.
 */
public class ClimberMoveController {
	protected final Mob entity;
	protected final IClimberEntity climber;

	@Nullable
	protected int[] blockPos;

	@Nullable
	protected Integer side;

	protected double wantedX;
	protected double wantedY;
	protected double wantedZ;
	protected double speedModifier = 1.0;

	protected boolean hasMovementTarget = false;

	public ClimberMoveController(Mob entity) {
		this.entity = entity;
		this.climber = (IClimberEntity) entity;
	}

	/**
	 * Set the wanted position for the entity to move to
	 */
	public void setWantedPosition(double x, double y, double z, double speedIn) {
		this.setMoveTo(x, y, z, null, null, speedIn);
	}

	/**
	 * Set movement target with optional block and side information
	 */
	public void setMoveTo(double x, double y, double z, @Nullable int[] block, @Nullable Integer side, double speedIn) {
		this.wantedX = x;
		this.wantedY = y;
		this.wantedZ = z;
		this.speedModifier = speedIn;
		this.blockPos = block;
		this.side = side;
		this.hasMovementTarget = true;
	}

	/**
	 * Stop current movement
	 */
	public void stop() {
		this.hasMovementTarget = false;
	}

	/**
	 * Check if we have a movement target
	 */
	public boolean hasTarget() {
		return this.hasMovementTarget;
	}

	/**
	 * Get the target position
	 */
	public double[] getTarget() {
		return new double[] { this.wantedX, this.wantedY, this.wantedZ };
	}

	/**
	 * Calculate the movement direction toward the target.
	 *
	 * This version sets moveForward/moveStrafing instead of modifying velocity directly.
	 * This works with the new moveEntityWithHeading() override.
	 */
	public void tick() {
		if (!this.hasMovementTarget) {
			// No target, stop movement
			if (this.entity instanceof Mob) {
				try {
					setMoveForward(0);
					setMoveStrafing(0);
				} catch (Exception e) {
					// Ignore reflection errors
				}
			}
			return;
		}

		// Compute vector toward target
		double dx = this.wantedX - this.entity.x;
		double dy = this.wantedY - this.entity.y;
		double dz = this.wantedZ - this.entity.z;

		double distSq = dx * dx + dy * dy + dz * dz;

		// Stop if we're close enough
		if (distSq < 0.25) {
			this.hasMovementTarget = false;
			try {
				setMoveForward(0);
				setMoveStrafing(0);
			} catch (Exception e) {
				// Ignore
			}
			return;
		}

		// Get current orientation
		Orientation orientation = this.climber.getOrientation();
		Vector3f normal = orientation.normal;

		// Flatten movement along the surface
		Vector3f desiredDir = new Vector3f((float) dx, (float) dy, (float) dz);
		float perpComponent = desiredDir.x * normal.x + desiredDir.y * normal.y + desiredDir.z * normal.z;

		desiredDir.x -= perpComponent * normal.x;
		desiredDir.y -= perpComponent * normal.y;
		desiredDir.z -= perpComponent * normal.z;

		// Normalize
		float len = (float) Math.sqrt(desiredDir.x * desiredDir.x + desiredDir.y * desiredDir.y + desiredDir.z * desiredDir.z);

		if (len < 0.001f) {
			// Target is perpendicular to surface
			return;
		}

		desiredDir.x /= len;
		desiredDir.y /= len;
		desiredDir.z /= len;

		// Convert to local space (relative to entity orientation)
		Vector3f local = orientation.getLocal(desiredDir);

		// Update yaw to face target
		float targetYaw = (float) Math.toDegrees(Math.atan2(local.x, local.z));
		float yawDiff = targetYaw - this.entity.yRot;

		// Normalize angle difference
		while (yawDiff > 180) yawDiff -= 360;
		while (yawDiff < -180) yawDiff += 360;

		// Smoothly turn toward target
		this.entity.yRot += yawDiff * 0.1f;

		// Set movement (forward movement toward target)
		float speed = (float) (this.climber.getMovementSpeed() * this.speedModifier);

		try {
			setMoveForward(speed);
			setMoveStrafing(0);
		} catch (Exception e) {
			// If reflection fails, fall back to direct field access
			// This shouldn't happen but just in case
		}
	}

	/**
	 * Set moveForward field using reflection (since it's protected in Mob)
	 */
	private void setMoveForward(float value) throws Exception {
		java.lang.reflect.Field field = Mob.class.getDeclaredField("moveForward");
		field.setAccessible(true);
		field.setFloat(this.entity, value);
	}

	/**
	 * Set moveStrafing field using reflection (since it's protected in Mob)
	 */
	private void setMoveStrafing(float value) throws Exception {
		java.lang.reflect.Field field = Mob.class.getDeclaredField("moveStrafing");
		field.setAccessible(true);
		field.setFloat(this.entity, value);
	}
}
