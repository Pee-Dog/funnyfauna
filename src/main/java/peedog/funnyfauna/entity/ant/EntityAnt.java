package peedog.funnyfauna.entity.ant;

import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.Mob;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.World;
import org.apache.commons.lang3.tuple.Pair;
import org.jspecify.annotations.Nullable;
import org.lwjgl.util.vector.Vector3f;
import peedog.funnyfauna.entity.ant.util.*;

import java.util.List;

/**
 * IMPROVED VERSION - Fixed wall crawling behavior
 *
 * Key improvements:
 * 1. Collision-aware surface detection
 * 2. Better priority logic that doesn't force ants to ground
 * 3. Stronger sticking force for wall climbing
 * 4. Smoother transitions between surfaces
 */
public class EntityAnt extends Mob implements IAdvancedPathFindingEntity, IClimberEntity {

	/* ===================== Climbing State ===================== */

	private Orientation orientation = Orientation.ground();
	private Orientation renderOrientation;
	private Vector3f attachedNormal = new Vector3f(0, 1, 0);
	private boolean isClimbing = false;

	private final SurfaceDetector surfaceDetector;
	private final ClimbingPhysics climbingPhysics;

	/* ===================== Config ===================== */

	private boolean canClimbInWater = false;
	private boolean canClimbInLava = false;
	private float collisionInclusionRange = 0.5f;
	private float collisionSmoothingRange = 0.1f;

	/* ===================== AI ===================== */

	private int wanderCooldown = 0;
	private double targetX, targetY, targetZ;
	private boolean hasTarget = false;
	private int stuckTicks = 0;

	/* ===================== Debug ===================== */

	private int ticksSinceLastSurfaceChange = 0;
	private Vector3f lastNormal = new Vector3f(0, 1, 0);

	public EntityAnt(World world) {
		super(world);
		this.setSize(0.2F, 0.2F);
		this.moveSpeed = 0.15f;
		this.heartsHalvesLife = 4;
		this.surfaceDetector = new SurfaceDetector(this, this, world);
		this.climbingPhysics = new ClimbingPhysics(this, this);
	}

	/* ===================== Core Movement ===================== */

	@Override
	public void moveEntityWithHeading(float strafe, float forward) {
		updateClimbingState();

		// 1. Add movement intent ONLY
		if (isClimbing) {
			climbingPhysics.addMovementForce(forward, strafe, orientation);
		} else {
			super.moveEntityWithHeading(strafe, forward);
			return;
		}

		// 2. Apply physics (projection, adhesion, friction, speed cap)
		climbingPhysics.applyPhysics(orientation, isClimbing);

		// 3. Move entity using resulting velocity
		move(xd, yd, zd);

		// 4. Final damping
		xd *= 0.91;
		yd *= 0.91;
		zd *= 0.91;

		fallDistance = 0;
	}

	@Override
	public void moveRelative(float strafe, float forward, float speed) {
		if (!isClimbing) {
			super.moveRelative(strafe, forward, speed);
			return;
		}

		float mag = MathHelper.sqrt_float(strafe * strafe + forward * forward);
		if (mag < 0.001f) return;

		mag = speed / Math.max(1.0f, mag);
		strafe *= mag;
		forward *= mag;

		xd += orientation.localX.x * strafe + orientation.localZ.x * forward;
		yd += orientation.localX.y * strafe + orientation.localZ.y * forward;
		zd += orientation.localX.z * strafe + orientation.localZ.z * forward;
	}

	/* ===================== Surface Detection - IMPROVED ===================== */

	/**
	 * IMPROVED VERSION - Uses collision information for better wall detection
	 */
	private void updateClimbingState() {
		// Check what we're actually touching (collision-based)
		boolean touchingWall = horizontalCollision || verticalCollision;
		boolean touchingFloor = onGround;

		// Detect nearby surfaces
		SurfaceDetector.SurfaceInfo current = surfaceDetector.detectSurface();

		// Special handling if we just collided with something

		// NEW PRIORITY SYSTEM - doesn't force back to ground when climbing

		// Priority 1: If touching a wall AND we detected a wall surface, climb it
		if (touchingWall && current != null && Math.abs(current.normal.y) < 0.9f) {
			attachTo(current.normal);
			onGround = false;
			return;
		}

		// Priority 2: If on ground AND detected ground surface below us
		if (touchingFloor && current != null && current.normal.y > 0.7f) {
			attachTo(current.normal);
			int feetBlockY = (int) Math.floor(y) - 1;
			// Only snap if close
			if (Math.abs(y - (feetBlockY + 1.0)) < 0.3) {
				y = feetBlockY + 1.0;
			}
			onGround = true;
			return;
		}

		// Priority 3: Use whatever surface we detected (may be transitioning)
		if (current != null) {
			attachTo(current.normal);
			onGround = current.normal.y > 0.7f;

			// Snap if close to a horizontal surface
			if (onGround && current.distance < 0.3f) {
				y = current.blockY + 1.0;
			}
			return;
		}

		// Priority 4: Look ahead for surfaces to climb onto
		double oldX = x, oldY = y, oldZ = z;
		float lookAhead = 0.8f; // Increased look-ahead distance
		x += orientation.localZ.x * lookAhead;
		y += orientation.localZ.y * lookAhead;
		z += orientation.localZ.z * lookAhead;
		SurfaceDetector.SurfaceInfo ahead = surfaceDetector.detectSurface();
		x = oldX; y = oldY; z = oldZ;

		if (ahead != null) {
			attachTo(ahead.normal);
			onGround = ahead.normal.y > 0.7f;

			// Snap to horizontal surfaces ahead
			if (ahead.normal.y > 0.7f && ahead.distance < 0.5f) {
				y = ahead.blockY + 1.0;
			}
			return;
		}

		// Priority 5: Check above for ceilings
		y += 0.5f;
		SurfaceDetector.SurfaceInfo above = surfaceDetector.detectSurface();
		y = oldY;

		if (above != null && above.normal.y < -0.5f) {
			attachTo(above.normal);
			onGround = false;
			return;
		}

		// Priority 6: No surface found - fall
		detach();
	}

	/* ===================== Climbing Physics ===================== */

	private void attachTo(Vector3f normal) {
		// Track if surface changed for debugging
		float normalDiff = Math.abs(normal.x - lastNormal.x) +
			Math.abs(normal.y - lastNormal.y) +
			Math.abs(normal.z - lastNormal.z);

		if (normalDiff > 0.1f) {
			ticksSinceLastSurfaceChange = 0;
			lastNormal.set(normal);
		} else {
			ticksSinceLastSurfaceChange++;
		}

		isClimbing = true;
		attachedNormal.set(normal);
		orientation = Orientation.fromNormal(normal);
	}

	private void detach() {
		isClimbing = false;
		attachedNormal.set(0, 1, 0);
		orientation = Orientation.ground();
		onGround = false;
	}

	/* ===================== AI ===================== */

	@Override
	protected void updateAI() {
		super.updateAI();

		// Pick a new target if needed
		if (!hasTarget || wanderCooldown-- <= 0) {
			targetX = x + (random.nextDouble() - 0.5) * 8;
			targetY = y + (random.nextDouble() - 0.5) * 4;
			targetZ = z + (random.nextDouble() - 0.5) * 8;
			wanderCooldown = 40 + random.nextInt(60);
			hasTarget = true;
		}

		if (!hasTarget) return;

		// Direction to target
		Vector3f toTarget = new Vector3f(
			(float)(targetX - x),
			(float)(targetY - y),
			(float)(targetZ - z)
		);
		float len = (float)Math.sqrt(toTarget.x*toTarget.x + toTarget.y*toTarget.y + toTarget.z*toTarget.z);

		if (len < 0.5f) {
			hasTarget = false;
			moveForward = 0;
			moveStrafing = 0;
			return;
		}

		toTarget.x /= len;
		toTarget.y /= len;
		toTarget.z /= len;

		// Compute movement axes based on current surface
		Vector3f normal = attachedNormal;
		Vector3f forward;
		Vector3f right;

		if (Math.abs(normal.y) < 0.7f) {
			// WALL: Forward = up the wall
			Vector3f worldUp = new Vector3f(0, 1, 0);
			float dot = worldUp.x * normal.x + worldUp.y * normal.y + worldUp.z * normal.z;
			forward = new Vector3f(
				worldUp.x - dot * normal.x,
				worldUp.y - dot * normal.y,
				worldUp.z - dot * normal.z
			);
			if (forward.length() < 0.001f) {
				forward = new Vector3f(0, 1, 0);
			}
			forward.normalise();

			right = Vector3f.cross(normal, forward, null);
			right.normalise();
		} else {
			// GROUND: Forward = horizontal forward
			Vector3f worldForward = new Vector3f(0, 0, 1);
			float dot = worldForward.x * normal.x + worldForward.y * normal.y + worldForward.z * normal.z;
			forward = new Vector3f(
				worldForward.x - dot * normal.x,
				worldForward.y - dot * normal.y,
				worldForward.z - dot * normal.z
			);
			if (forward.length() < 0.001f) {
				forward = new Vector3f(1, 0, 0);
			}
			forward.normalise();

			right = Vector3f.cross(normal, forward, null);
			right.normalise();
		}

		// Project target direction onto surface plane
		float fwd = toTarget.x*forward.x + toTarget.y*forward.y + toTarget.z*forward.z;
		float str = toTarget.x*right.x   + toTarget.y*right.y   + toTarget.z*right.z;

		moveForward  = MathHelper.clamp(fwd, -1.0f, 1.0f);
		moveStrafing = MathHelper.clamp(str, -1.0f, 1.0f);
	}

	/* ===================== Interfaces ===================== */

	@Override public Orientation getOrientation() { return orientation; }
	@Override public void setRenderOrientation(Orientation o) { renderOrientation = o; }
	@Nullable @Override public Orientation getRenderOrientation() { return renderOrientation; }
	@Override public float getMovementSpeed() { return 0.15f; }

	@Override
	public Pair<Integer, Vector3f> getGroundDirection() {
		Vector3f n = attachedNormal;
		if (Math.abs(n.y) > Math.abs(n.x) && Math.abs(n.y) > Math.abs(n.z))
			return Pair.of(n.y > 0 ? 1 : 0, new Vector3f());
		if (Math.abs(n.z) > Math.abs(n.x))
			return Pair.of(n.z > 0 ? 3 : 2, new Vector3f());
		return Pair.of(n.x > 0 ? 5 : 4, new Vector3f());
	}

	@Override public boolean canClimbOnBlock(int id, int x, int y, int z) { return id != 0; }

	@Override
	public float getBlockSlipperiness(int x, int y, int z) {
		// IMPROVED: Less friction when climbing walls for better movement
		return isClimbing && Math.abs(attachedNormal.y) < 0.7f ? 0.94f : 0.87f;
	}

	@Override public boolean canClimberTriggerWalking() { return true; }
	@Override public boolean canClimbInWater() { return canClimbInWater; }
	@Override public boolean canClimbInLava() { return canClimbInLava; }
	@Override public float getCollisionsInclusionRange() { return collisionInclusionRange; }
	@Override public float getCollisionsSmoothingRange() { return collisionSmoothingRange; }

	@Override public int getMaxHealth() { return 4; }
	@Override public int getAmbientSoundInterval() { return 160; }
	@Override public boolean collidesWith(Entity entity) {return false;}

	public int getAnimFrame() {
		double motion = Math.abs(this.xd) + Math.abs(this.yd) + Math.abs(this.zd);
		return motion > 0.002 ? (this.tickCount / 6) & 1 : 0;
	}

	@Override
	public String getEntityTexture() {
		return getAnimFrame() == 0
			? "funnyfauna:entity/ant/bug1"
			: "funnyfauna:entity/ant/bug2";
	}

	/**
	 * Debug method - check if ant is stuck
	 */
	public boolean isStableOnSurface() {
		return ticksSinceLastSurfaceChange > 10;
	}
}
