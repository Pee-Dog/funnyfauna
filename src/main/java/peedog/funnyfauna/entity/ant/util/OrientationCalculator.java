package peedog.funnyfauna.entity.ant.util;

import org.lwjgl.util.vector.Vector3f;

/**
 * Calculates and smoothly updates entity orientation based on surface normals.
 */
public class OrientationCalculator {

	private Orientation currentOrientation;
	private Orientation targetOrientation;

	// How quickly orientation changes (0-1, higher = faster)
	private static final float SMOOTHING_FACTOR = 0.15f;

	public OrientationCalculator() {
		this.currentOrientation = Orientation.ground();
		this.targetOrientation = Orientation.ground();
	}

	/**
	 * Update target orientation based on a new surface normal.
	 *
	 * @param surfaceNormal The normal vector of the surface to walk on
	 * @param entityYaw Current yaw of the entity (for maintaining forward direction)
	 */
	public void updateTargetOrientation(Vector3f surfaceNormal, float entityYaw) {
		// Normalize the surface normal
		Vector3f normal = normalizeVector(surfaceNormal);

		// Calculate local coordinate system from the normal
		this.targetOrientation = calculateOrientationFromNormal(normal, entityYaw);
	}

	/**
	 * Get the current (smoothed) orientation.
	 */
	public Orientation getCurrentOrientation() {
		return currentOrientation;
	}

	/**
	 * Update the current orientation, smoothly interpolating toward the target.
	 */
	public void tick() {
		// Interpolate toward target orientation
		currentOrientation = interpolateOrientation(
			currentOrientation,
			targetOrientation,
			SMOOTHING_FACTOR
		);
	}

	/**
	 * Reset to ground orientation (for when falling or detached).
	 */
	public void resetToGround() {
		this.targetOrientation = Orientation.ground();
	}

	/**
	 * Force immediate orientation change (no smoothing).
	 */
	public void setImmediate(Orientation orientation) {
		this.currentOrientation = orientation;
		this.targetOrientation = orientation;
	}

	/**
	 * Calculate a full orientation from a surface normal and entity yaw.
	 */
	private Orientation calculateOrientationFromNormal(Vector3f normal, float entityYaw) {
		// The normal is our "up" vector (localY)
		Vector3f localY = new Vector3f(normal);

		// Calculate forward direction (localZ) perpendicular to normal
		// We want to maintain the entity's yaw as much as possible
		float yawRad = (float) Math.toRadians(entityYaw);
		Vector3f desiredForward = new Vector3f(
			-(float) Math.sin(yawRad),
			0,
			(float) Math.cos(yawRad)
		);

		// Project desired forward onto the surface plane
		float dot = dotProduct(desiredForward, localY);
		Vector3f localZ = new Vector3f(
			desiredForward.x - localY.x * dot,
			desiredForward.y - localY.y * dot,
			desiredForward.z - localY.z * dot
		);

		// If the projected vector is too small, use a fallback
		if (length(localZ) < 0.001f) {
			// Use world Z axis as fallback
			desiredForward = new Vector3f(0, 0, 1);
			dot = dotProduct(desiredForward, localY);
			localZ = new Vector3f(
				desiredForward.x - localY.x * dot,
				desiredForward.y - localY.y * dot,
				desiredForward.z - localY.z * dot
			);
		}

		localZ = normalizeVector(localZ);

		// Right direction is cross product of up and forward
		Vector3f localX = crossProduct(localY, localZ);
		localX = normalizeVector(localX);

		// Recalculate localZ to ensure orthogonality
		localZ = crossProduct(localX, localY);
		localZ = normalizeVector(localZ);

		// Calculate yaw and pitch from the orientation
		// (Using simplified calculation for now)
		float yaw = (float) Math.toDegrees(Math.atan2(-localZ.x, localZ.z));
		float pitch = (float) Math.toDegrees(Math.asin(localZ.y));

		return new Orientation(
			normal,
			localZ,
			localY,
			localX,
			0, 0, 0, // component values (not used in simple implementation)
			yaw,
			pitch
		);
	}

	/**
	 * Interpolate between two orientations.
	 */
	private Orientation interpolateOrientation(Orientation from, Orientation to, float factor) {
		// Interpolate each vector
		Vector3f normal = lerp(from.normal, to.normal, factor);
		Vector3f localZ = lerp(from.localZ, to.localZ, factor);
		Vector3f localY = lerp(from.localY, to.localY, factor);
		Vector3f localX = lerp(from.localX, to.localX, factor);

		// Normalize to prevent drift
		normal = normalizeVector(normal);
		localZ = normalizeVector(localZ);
		localY = normalizeVector(localY);
		localX = normalizeVector(localX);

		// Interpolate angles
		float yaw = from.yaw + (to.yaw - from.yaw) * factor;
		float pitch = from.pitch + (to.pitch - from.pitch) * factor;

		return new Orientation(normal, localZ, localY, localX, 0, 0, 0, yaw, pitch);
	}

	// ===== Vector Math Utilities =====

	private Vector3f lerp(Vector3f a, Vector3f b, float t) {
		return new Vector3f(
			a.x + (b.x - a.x) * t,
			a.y + (b.y - a.y) * t,
			a.z + (b.z - a.z) * t
		);
	}

	private Vector3f normalizeVector(Vector3f v) {
		float len = length(v);
		if (len < 0.0001f) {
			return new Vector3f(0, 1, 0); // Default to up
		}
		return new Vector3f(v.x / len, v.y / len, v.z / len);
	}

	private float length(Vector3f v) {
		return (float) Math.sqrt(v.x * v.x + v.y * v.y + v.z * v.z);
	}

	private float dotProduct(Vector3f a, Vector3f b) {
		return a.x * b.x + a.y * b.y + a.z * b.z;
	}

	private Vector3f crossProduct(Vector3f a, Vector3f b) {
		return new Vector3f(
			a.y * b.z - a.z * b.y,
			a.z * b.x - a.x * b.z,
			a.x * b.y - a.y * b.x
		);
	}
}
