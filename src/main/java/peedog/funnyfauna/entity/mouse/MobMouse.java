package peedog.funnyfauna.entity.mouse;

import java.util.List;
import net.minecraft.core.entity.MobPathfinder;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.util.collection.NamespaceID;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.util.phys.Vec3;
import net.minecraft.core.world.World;
import org.jetbrains.annotations.Nullable;

public class MobMouse extends MobPathfinder {

	private MobMouse leader;
	private MobMouse follower;
	private static final double FOLLOW_DISTANCE = 2.0D;
	private static final int MAX_CARAVAN_LENGTH = 8;
	private int fleeTimer = 0;

	public MobMouse(@Nullable World world) {
		super(world);
		this.setSize(0.5F, 0.5F);
		this.textureIdentifier = NamespaceID.getPermanent("funnyfauna", "mouse");
		this.moveSpeed = 0.5F;
	}

	public boolean isFollowing() {
		return this.leader != null;
	}

	public boolean hasFollower() {
		return this.follower != null;
	}

	public void stopFollowing() {
		if (this.leader != null) {
			this.leader.follower = null;
		}
		this.leader = null;
		this.pathToEntity = null;
	}

	public void follow(MobMouse leader) {
		this.leader = leader;
		leader.follower = this;
	}

	public boolean canFollowChain(MobMouse mouse, int length) {
		if (length >= MAX_CARAVAN_LENGTH) return false;
		if (mouse.isFollowing()) {
			return canFollowChain(mouse.leader, length + 1);
		}
		return true;
	}

	@Override
	protected void updateAI() {
		if (world == null) return;

		if (bb == null) {
			setSize(0.5F, 0.5F);
			setPos(x, y, z);
		}

		this.isJumping = false;
		this.moveForward = 0.0F;
		this.moveStrafing = 0.0F;

		// 1. FLEE LOGIC
		Player nearbyPlayer = world.getClosestPlayer(x, y, z, 6.0);
		if (nearbyPlayer != null || fleeTimer > 0) {
			if (nearbyPlayer != null && !nearbyPlayer.isSneaking()) {
				fleeTimer = 60;
				if (isFollowing()) stopFollowing();
			}

			if (fleeTimer > 0) {
				fleeTimer--;
				if (nearbyPlayer != null) {
					double dx = x - nearbyPlayer.x;
					double dz = z - nearbyPlayer.z;
					double dist = Math.sqrt(dx*dx + dz*dz);
					if (dist > 0) {
						int targetX = MathHelper.floor(x + (dx / dist) * 5.0);
						int targetZ = MathHelper.floor(z + (dz / dist) * 5.0);
						if (this.pathToEntity == null || random.nextInt(10) == 0) {
							this.pathToEntity = world.getEntityPathToXYZ(this, targetX, MathHelper.floor(y), targetZ, 16.0F);
						}
					}
				}
			}
		}

		// 2. CARAVAN LOGIC
		if (fleeTimer == 0 && !isFollowing()) {
			List<MobMouse> nearbyMice = world.getEntitiesWithinAABB(MobMouse.class, bb.grow(8, 4, 8));
			MobMouse potentialLeader = null;
			double closest = Double.MAX_VALUE;

			for (MobMouse mouse : nearbyMice) {
				if (mouse != this && !mouse.hasFollower() && (mouse.isFollowing() || random.nextInt(50) == 0)) {
					if (canFollowChain(mouse, 1)) {
						double d = this.distanceTo(mouse);
						if (d < closest) {
							closest = d;
							potentialLeader = mouse;
						}
					}
				}
			}
			if (potentialLeader != null && closest < 10.0 && closest > FOLLOW_DISTANCE) {
				follow(potentialLeader);
			}
		}

		float currentSpeed = this.moveSpeed;

		if (fleeTimer == 0 && isFollowing() && leader != null) {
			if (!leader.isAlive()) {
				stopFollowing();
			} else {
				double dist = this.distanceTo(leader);
				if (dist > 12.0) {
					stopFollowing();
				} else if (dist > FOLLOW_DISTANCE) {
					if (dist > 5.0) currentSpeed = 1.2F;

					if (this.pathToEntity == null || random.nextInt(15) == 0) {
						this.pathToEntity = world.getPathToEntity(this, leader, 16.0F);
					}
				} else {
					this.pathToEntity = null;
				}
			}
		}

		// 3. RANDOM ROAM
		if (fleeTimer == 0 && !isFollowing() && this.pathToEntity == null && random.nextInt(80) == 0) {
			int tx = MathHelper.floor(x) + random.nextInt(11) - 5;
			int ty = MathHelper.floor(y) + random.nextInt(5) - 2;
			int tz = MathHelper.floor(z) + random.nextInt(11) - 5;
			this.pathToEntity = world.getEntityPathToXYZ(this, tx, ty, tz, 10.0F);
		}

		// 4. MOVEMENT EXECUTION (Fixed Crash)
		if (this.pathToEntity != null) {
			Vec3 nextPos = this.pathToEntity.getPos(this);
			double pathThreshold = this.bbWidth * 2.0F;

			// Replaced the 'if' with the 'while' loop from MobPathfinderRef
			// This ensures we check isDone() immediately after calling next()
			while (nextPos != null && nextPos.distanceToSquared(this.x, nextPos.y, this.z) < pathThreshold * pathThreshold) {
				this.pathToEntity.next();
				if (this.pathToEntity.isDone()) {
					nextPos = null;
					this.pathToEntity = null;
				} else {
					nextPos = this.pathToEntity.getPos(this);
				}
			}

			if (nextPos != null) {
				double dx = nextPos.x - this.x;
				double dz = nextPos.z - this.z;
				double dy = nextPos.y - MathHelper.floor(this.bb.minY + 0.5);

				float targetYaw = (float)(Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
				float yawDiff = targetYaw - this.yRot;
				while (yawDiff < -180.0F) yawDiff += 360.0F;
				while (yawDiff >= 180.0F) yawDiff -= 360.0F;
				this.yRot += MathHelper.clamp(yawDiff, -30.0F, 30.0F);

				this.moveForward = currentSpeed;
				if (fleeTimer > 0) this.moveForward = 3F;

				if (dy > 0.0) this.isJumping = true;
			}
		}

		if (this.horizontalCollision && this.pathToEntity != null) this.isJumping = true;
		if (random.nextFloat() < 0.8F && (isInWater() || isInLava())) this.isJumping = true;
	}
}
