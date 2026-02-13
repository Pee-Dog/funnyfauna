package peedog.funnyfauna.entity.ai.path;

import net.minecraft.core.entity.Entity;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.util.phys.Vec3;
import net.minecraft.core.world.pathfinder.Path;
import peedog.funnyfauna.entity.MobTaskrunner;
import peedog.funnyfauna.entity.ai.Task;

public abstract class PathTask<T extends MobTaskrunner> extends Task<T> {

	public Path path;
	public float moveSpeed = 0.7F;
	private boolean keepJumping;

	public PathTask(T mob) {
		super(mob);
	}

	public Entity lookTarget() {return null;}

	@Override
	public Task onTick() {
		if (this.mob.world != null) {
			int i = MathHelper.floor(this.mob.bb.minY + (double)0.5F);
			this.mob.setMoveForward(0.0F);
			this.mob.setMoveStrafing(0.0F);
			Entity target = lookTarget();
			if (target == null) this.mob.xRot = 0.0F;
			if (this.path != null && this.random.nextInt(100) != 0) {
				Vec3 coordsForNextPath = this.path.getPos(this.mob);
				double d = (double)(this.mob.bbWidth * 2.0F);

				while(coordsForNextPath != null && coordsForNextPath.distanceToSquared(this.mob.x, coordsForNextPath.y, this.mob.z) < d * d) {
					this.path.next();
					if (this.path.isDone()) {
						coordsForNextPath = null;
						this.path = null;
					} else {
						coordsForNextPath = this.path.getPos(this.mob);
					}
				}

				this.mob.stopJumping();
				if (coordsForNextPath != null) {
					double x1 = coordsForNextPath.x - this.mob.x;
					double z1 = coordsForNextPath.z - this.mob.z;
					double y1 = coordsForNextPath.y - (double)i;
					float f2 = (float)(Math.atan2(z1, x1) * (double)180.0F / Math.PI) - 90.0F;
					float f3;

					for(f3 = f2 - this.mob.yRot; f3 < -180.0F; f3 += 360.0F) {
					}
					this.mob.setMoveForward(this.moveSpeed);

					while(f3 >= 180.0F) {
						f3 -= 360.0F;
					}

					if (f3 > 30.0F) {
						f3 = 30.0F;
					}

					if (f3 < -30.0F) {
						f3 = -30.0F;
					}

					this.mob.yRot += f3;
					if (target != null) {

						double dX = target.x - this.mob.x;
						double dY = target.y - i;
						double dZ = target.z - this.mob.z;
						Vec3 n = Vec3.getTempVec3(dX, dY, dZ).normalize();
						float f5 = this.mob.yRot;
						this.mob.yRot = (float)(Math.atan2(n.z, n.x) * (double)180.0F / Math.PI) - 90.0F;
						this.mob.xRot = (float)(-Math.asin(n.y)* (double)180.0F / Math.PI);
						float f4 = (f5 - this.mob.yRot) * (float)Math.PI / 180.0F;
						this.mob.setMoveStrafing(-MathHelper.sin(f4) * this.moveSpeed * 1.0F);
						this.mob.setMoveForward(MathHelper.cos(f4) * this.moveSpeed * 1.0F);
					}

					if (y1 > (double)0.0F || this.keepJumping) {
						this.keepJumping = this.mob.isInWater() && y1 > (double)-1.0F;
						this.mob.startJumping();
					}
				}

				if (this.mob.horizontalCollision) {
					this.mob.startJumping();
				}
			} else {
				this.path = null;
			}
		}
		return null;
	}

	@Override
	protected void onStop(Task interruptTask) {
		this.path = null;
		this.mob.setMoveForward(0.0F);
		this.mob.setMoveStrafing(0.0F);
	}
}
