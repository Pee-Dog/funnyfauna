package peedog.funnyfauna.entity.ant.util;

import net.minecraft.core.util.helper.MathHelper;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.util.vector.Vector3f;

/**
 * Represents the orientation of a climber entity on a surface.
 * Provides local coordinate system (localX, localY, localZ) relative to the surface normal.
 */
public class Orientation {
	public final Vector3f normal;      // Surface normal (direction perpendicular to surface)
	public final Vector3f localZ;      // Forward direction relative to surface
	public final Vector3f localY;      // Up direction relative to surface (same as normal)
	public final Vector3f localX;      // Right direction relative to surface

	public final float componentZ;  // Z component of rotation
	public final float componentY;  // Y component of rotation
	public final float componentX;  // X component of rotation
	public final float yaw;         // Yaw angle
	public final float pitch;       // Pitch angle

	public Orientation(Vector3f normal, Vector3f localZ, Vector3f localY, Vector3f localX,
					   float componentZ, float componentY, float componentX, float yaw, float pitch) {
		this.normal = normal;
		this.localZ = localZ;
		this.localY = localY;
		this.localX = localX;
		this.componentZ = componentZ;
		this.componentY = componentY;
		this.componentX = componentX;
		this.yaw = yaw;
		this.pitch = pitch;
	}

	/**
	 * Create a ground orientation (entity standing on ground normally)
	 */
	public static Orientation ground() {
		return new Orientation(
			new Vector3f(0, 1, 0),      // normal points up
			new Vector3f(0, 0, 1),      // localZ forward
			new Vector3f(0, 1, 0),      // localY up
			new Vector3f(1, 0, 0),      // localX right
			0, 0, 0,                 // components
			0, 0                     // yaw, pitch
		);
	}

	/**
	 * Create an orientation from just a surface normal.
	 * This generates appropriate forward and right vectors.
	 */
	public static Orientation fromNormal(Vector3f surfaceNormal) {
		// Normalize the normal
		Vector3f normal = normalize(new Vector3f(surfaceNormal));

		// localY (up) = surface normal
		Vector3f localY = new Vector3f(normal);

		// Pick a reference forward vector
		Vector3f ref;
		if (Math.abs(normal.y) < 0.9f) {
			ref = new Vector3f(0, 1, 0); // use world up for walls
		} else {
			ref = new Vector3f(0, 0, 1); // use world forward for ground/ceiling
		}


		// localX (right) = cross(worldForward, normal)
		Vector3f localX = crossProduct(ref, normal);
		localX = normalize(localX);

		// localZ (forward) = cross(normal, localX)
		Vector3f localZ = crossProduct(normal, localX);
		localZ = normalize(localZ);

		return new Orientation(normal, localZ, localY, localX, 0, 0, 0, 0, 0);
	}

	/**
	 * Convert local coordinates to global coordinates
	 */
	public Vector3f getGlobal(Vector3f local) {
		Vector3f result = new Vector3f();

		// result = localX * local.x + localY * local.y + localZ * local.z
		result.x = this.localX.x * local.x + this.localY.x * local.y + this.localZ.x * local.z;
		result.y = this.localX.y * local.x + this.localY.y * local.y + this.localZ.y * local.z;
		result.z = this.localX.z * local.x + this.localY.z * local.y + this.localZ.z * local.z;

		return result;
	}

	/**
	 * Convert rotation angles to global direction vector
	 */
	public Vector3f getGlobal(float yaw, float pitch) {
		float cy = MathHelper.cos(yaw * 0.017453292F);
		float sy = MathHelper.sin(yaw * 0.017453292F);
		float cp = -MathHelper.cos(-pitch * 0.017453292F);
		float sp = MathHelper.sin(-pitch * 0.017453292F);

		Vector3f result = new Vector3f();
		result.x = sy * cp * this.localX.x + sp * this.localY.x + cy * cp * this.localZ.x;
		result.y = sy * cp * this.localX.y + sp * this.localY.y + cy * cp * this.localZ.y;
		result.z = sy * cp * this.localX.z + sp * this.localY.z + cy * cp * this.localZ.z;
		return result;
	}

	/**
	 * Convert global coordinates to local coordinates
	 */
	public Vector3f getLocal(Vector3f global) {
		Vector3f result = new Vector3f();
		// Dot product with local basis vectors
		result.x = this.localX.x * global.x + this.localX.y * global.y + this.localX.z * global.z;
		result.y = this.localY.x * global.x + this.localY.y * global.y + this.localY.z * global.z;
		result.z = this.localZ.x * global.x + this.localZ.y * global.y + this.localZ.z * global.z;
		return result;
	}

	/**
	 * Get local rotation angles (yaw, pitch) for a global direction
	 */
	public Pair<Float, Float> getLocalRotation(Vector3f global) {
		Vector3f local = this.getLocal(global);

		float yaw = (float) Math.toDegrees(Math.atan2(local.x, local.z)) + 180.0f;
		float pitch = (float) -Math.toDegrees(Math.atan2(local.y,
			(float)Math.sqrt(local.x * local.x + local.z * local.z)));

		return Pair.of(yaw, pitch);
	}

	// ===== Vector Math Utilities =====

	private static Vector3f normalize(Vector3f v) {
		float len = (float) Math.sqrt(v.x * v.x + v.y * v.y + v.z * v.z);
		if (len < 0.0001f) {
			return new Vector3f(0, 1, 0);
		}
		return new Vector3f(v.x / len, v.y / len, v.z / len);
	}

	private static float dotProduct(Vector3f a, Vector3f b) {
		return a.x * b.x + a.y * b.y + a.z * b.z;
	}

	private static Vector3f crossProduct(Vector3f a, Vector3f b) {
		return new Vector3f(
			a.y * b.z - a.z * b.y,
			a.z * b.x - a.x * b.z,
			a.x * b.y - a.y * b.x
		);
	}
}
