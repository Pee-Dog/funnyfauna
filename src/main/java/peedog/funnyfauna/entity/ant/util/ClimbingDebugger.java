package peedog.funnyfauna.entity.ant.util;

import org.lwjgl.util.vector.Vector3f;
import peedog.funnyfauna.entity.ant.util.SurfaceDetector.SurfaceInfo;

/**
 * Debugging utilities for the climbing system.
 * Use these methods to understand what your ant is detecting and doing.
 */
public class ClimbingDebugger {

	/**
	 * Print debugging information about surface detection.
	 */
	public static void debugSurfaceDetection(SurfaceInfo surface, double entityX, double entityY, double entityZ) {
		if (surface == null) {
			System.out.println("[Ant Debug] No surface detected!");
			return;
		}

		System.out.println("=== Surface Detection ===");
		System.out.println("Entity Position: (" + entityX + ", " + entityY + ", " + entityZ + ")");
		System.out.println("Surface Block: (" + surface.blockX + ", " + surface.blockY + ", " + surface.blockZ + ")");
		System.out.println("Surface Normal: (" + surface.normal.x + ", " + surface.normal.y + ", " + surface.normal.z + ")");
		System.out.println("Distance: " + surface.distance);
		System.out.println();
	}

	/**
	 * Print debugging information about orientation.
	 */
	public static void debugOrientation(Orientation orientation) {
		System.out.println("=== Orientation ===");
		System.out.println("Normal (Up): (" + orientation.normal.x + ", " + orientation.normal.y + ", " + orientation.normal.z + ")");
		System.out.println("LocalZ (Forward): (" + orientation.localZ.x + ", " + orientation.localZ.y + ", " + orientation.localZ.z + ")");
		System.out.println("LocalX (Right): (" + orientation.localX.x + ", " + orientation.localX.y + ", " + orientation.localX.z + ")");
		System.out.println("Yaw: " + orientation.yaw + "°, Pitch: " + orientation.pitch + "°");
		System.out.println();
	}

	/**
	 * Print debugging information about velocity.
	 */
	public static void debugVelocity(double xd, double yd, double zd) {
		double speed = Math.sqrt(xd * xd + yd * yd + zd * zd);
		System.out.println("=== Velocity ===");
		System.out.println("X: " + xd + ", Y: " + yd + ", Z: " + zd);
		System.out.println("Speed: " + speed);
		System.out.println();
	}

	/**
	 * Print a simple status line (good for per-tick debugging).
	 */
	public static void debugStatus(boolean onSurface, int ticksSinceSurface, double speed) {
		System.out.println("[Ant] OnSurface: " + onSurface +
			" | Ticks: " + ticksSinceSurface +
			" | Speed: " + String.format("%.3f", speed));
	}

	/**
	 * Check if orientation vectors are valid (orthogonal and normalized).
	 */
	public static boolean validateOrientation(Orientation orientation) {
		Vector3f x = orientation.localX;
		Vector3f y = orientation.localY;
		Vector3f z = orientation.localZ;

		// Check if normalized (length ≈ 1)
		float lenX = length(x);
		float lenY = length(y);
		float lenZ = length(z);

		boolean normalized =
			Math.abs(lenX - 1.0f) < 0.01f &&
				Math.abs(lenY - 1.0f) < 0.01f &&
				Math.abs(lenZ - 1.0f) < 0.01f;

		// Check if orthogonal (dot products ≈ 0)
		float dotXY = dot(x, y);
		float dotXZ = dot(x, z);
		float dotYZ = dot(y, z);

		boolean orthogonal =
			Math.abs(dotXY) < 0.01f &&
				Math.abs(dotXZ) < 0.01f &&
				Math.abs(dotYZ) < 0.01f;

		if (!normalized) {
			System.out.println("[Ant Warning] Orientation vectors not normalized!");
			System.out.println("  X length: " + lenX);
			System.out.println("  Y length: " + lenY);
			System.out.println("  Z length: " + lenZ);
		}

		if (!orthogonal) {
			System.out.println("[Ant Warning] Orientation vectors not orthogonal!");
			System.out.println("  X·Y: " + dotXY);
			System.out.println("  X·Z: " + dotXZ);
			System.out.println("  Y·Z: " + dotYZ);
		}

		return normalized && orthogonal;
	}

	/**
	 * Describe which surface the ant is on in simple terms.
	 */
	public static String describeSurface(Vector3f normal) {
		float absX = Math.abs(normal.x);
		float absY = Math.abs(normal.y);
		float absZ = Math.abs(normal.z);

		if (absY > absX && absY > absZ) {
			return normal.y > 0 ? "Floor (up)" : "Ceiling (down)";
		} else if (absX > absZ) {
			return normal.x > 0 ? "Wall (east)" : "Wall (west)";
		} else {
			return normal.z > 0 ? "Wall (south)" : "Wall (north)";
		}
	}

	// Helper methods
	private static float length(Vector3f v) {
		return (float) Math.sqrt(v.x * v.x + v.y * v.y + v.z * v.z);
	}

	private static float dot(Vector3f a, Vector3f b) {
		return a.x * b.x + a.y * b.y + a.z * b.z;
	}
}
