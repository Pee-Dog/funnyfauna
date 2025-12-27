package peedog.funnyfauna.entity.ant;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.util.phys.Vec3;
import net.minecraft.core.world.World;

import java.util.List;

public class EntityAnt extends Entity {

	private static final double WANDER_SPEED = 0.03;
	private static final double NODE_REACH_DIST = 0.25;

	private double homeX, homeY, homeZ;
	private boolean homeInitialized = false;

	private double targetXd, targetYd, targetZd;
	private int wanderCooldown = 0;

	private int animFrame = 0;
	private int animTimer = 0;
	private int variant;

	private AntPathFinder pathFinder;
	private AntNode[] currentPath;
	private int pathIndex = 0;

	private double offsetX, offsetZ;

	public EntityAnt(World world) {
		super(world);
		this.setSize(0.2F, 0.2F);
		this.pathFinder = new AntPathFinder(world);
		this.offsetX = (random.nextDouble() - 0.5) * 0.3;
		this.offsetZ = (random.nextDouble() - 0.5) * 0.3;

	}

	@Override
	protected void defineSynchedData() {}

	@Override
	public void tick() {
		super.tick();

		if (!homeInitialized) {
			homeX = x;
			homeY = y;
			homeZ = z;
			homeInitialized = true;
		}

		if (wanderCooldown > 0)
			wanderCooldown--;

		wander();

		move(targetXd, targetYd, targetZd);
		updateAnimation();
	}

	/* ===================== Wandering ===================== */

	private void wander() {
		if ((currentPath == null || pathIndex >= currentPath.length) && wanderCooldown == 0) {
			generateWanderNode();
			wanderCooldown = 10;
		}
		followCurrentPath();
	}

	private void generateWanderNode() {
		AntNode current = getCurrentNode();
		if (current == null) return;

		List<AntNode> neighbors = pathFinder.getNeighbors(current);
		if (neighbors.isEmpty()) return;

		AntNode chosen = neighbors.get(random.nextInt(neighbors.size()));
		currentPath = new AntNode[] { chosen };
		pathIndex = 0;
	}

	/* ===================== Movement ===================== */

	private void followCurrentPath() {
		if (currentPath == null || pathIndex >= currentPath.length) return;

		// === DEBUG: Hearts show chosen nodes ===
		for (AntNode node : currentPath) {
			Vec3 p = getAttachPoint(node);
			world.spawnParticle("heart", p.x, p.y, p.z, 0, 0.05, 0, 0);
		}

		AntNode node = currentPath[pathIndex];
		Vec3 target = getAttachPoint(node);

		double tx = target.x - x;
		double ty = target.y - y;
		double tz = target.z - z;

		double dist = Math.sqrt(tx * tx + ty * ty + tz * tz);
		if (dist < NODE_REACH_DIST) {
			pathIndex++;
			return;
		}

		double speed = Math.min(dist, WANDER_SPEED);
		targetXd = (tx / dist) * speed;
		targetYd = (ty / dist) * speed;
		targetZd = (tz / dist) * speed;
	}

	/* ===================== Surface Detection ===================== */

	private AntNode getCurrentNode() {
		int bx = MathHelper.floor(x);
		int by = MathHelper.floor(y);
		int bz = MathHelper.floor(z);

		// UP (standing on block)
		if (isSolid(bx, by - 1, bz))
			return new AntNode(bx, by - 1, bz, AntNode.Face.UP);

		// EAST wall
		if (isSolid(bx - 1, by, bz))
			return new AntNode(bx, by, bz, AntNode.Face.EAST);

		// WEST wall
		if (isSolid(bx + 1, by, bz))
			return new AntNode(bx, by, bz, AntNode.Face.WEST);

		// SOUTH wall
		if (isSolid(bx, by, bz - 1))
			return new AntNode(bx, by, bz, AntNode.Face.SOUTH);

		// NORTH wall
		if (isSolid(bx, by, bz + 1))
			return new AntNode(bx, by, bz, AntNode.Face.NORTH);

		return null;
	}

	private boolean isSolid(int x, int y, int z) {
		return world.getBlockMaterial(x, y, z).isSolidBlocking();
	}

	/* ===================== Attach Points ===================== */

	private Vec3 getAttachPoint(AntNode n) {
		double cx = n.x + 0.5 + offsetX;
		double cz = n.z + 0.5 + offsetZ;

		switch (n.face) {
			case UP:
				// IMPORTANT: raise above block so ground nodes are reachable
				return Vec3.getTempVec3(cx, n.y + 1.01, cz);

			case DOWN:
				return Vec3.getTempVec3(cx, n.y - 0.01, cz);

			case EAST:
				return Vec3.getTempVec3(n.x + 0.01, n.y + 0.5, cz);

			case WEST:
				return Vec3.getTempVec3(n.x + 0.99, n.y + 0.5, cz);

			case SOUTH:
				return Vec3.getTempVec3(cx, n.y + 0.5, n.z + 0.01);

			case NORTH:
				return Vec3.getTempVec3(cx, n.y + 0.5, n.z + 0.99);
		}
		return Vec3.getTempVec3(cx, n.y + 0.5, cz);
	}

	/* ===================== Animation ===================== */

	private void updateAnimation() {
		if (Math.abs(targetXd) + Math.abs(targetZd) + Math.abs(targetYd) > 0.001) {
			if (++animTimer >= 6) {
				animTimer = 0;
				animFrame ^= 1;
			}
		} else animFrame = 0;
	}

	public int getAnimFrame() {
		return animFrame;
	}

	public int getVariant() {
		return variant;
	}


	@Override
	public String getEntityTexture() {
		return animFrame == 0
			? "funnyfauna:entity/ant/bug1"
			: "funnyfauna:entity/ant/bug2";
	}

	/* ===================== Save Data ===================== */

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		if (tag.containsKey("HomeX")) {
			homeX = tag.getDouble("HomeX");
			homeY = tag.getDouble("HomeY");
			homeZ = tag.getDouble("HomeZ");
			homeInitialized = true;
		}
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		tag.putDouble("HomeX", homeX);
		tag.putDouble("HomeY", homeY);
		tag.putDouble("HomeZ", homeZ);
	}
}
