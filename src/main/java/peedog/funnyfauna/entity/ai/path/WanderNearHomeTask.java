package peedog.funnyfauna.entity.ai.path;

import net.minecraft.core.block.material.Material;
import net.minecraft.core.util.helper.MathHelper;
import peedog.funnyfauna.entity.MobTaskrunner;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.interfaces.IHomeable;

public class WanderNearHomeTask<T extends MobTaskrunner & IHomeable> extends PathTask<T> {

	private final int radius;

	public WanderNearHomeTask(T mob, int radius) {
		super(mob);
		this.radius = radius;
	}

	@Override
	protected void onStart() {

	}

	@Override
	public Task onTick() {
		if (!mob.hasHome()) {
			return null; // Failsafe
		}

		if (this.path == null) {
			if (this.mob.world != null) {

				boolean canMoveToPoint = false;
				int x = -1;
				int y = -1;
				int z = -1;
				float bestPathWeight = -99999.0F;

				int homeX = mob.getHomeX();
				int homeY = mob.getHomeY();
				int homeZ = mob.getHomeZ();

				for (int l = 0; l < 25; ++l) {

					int x1 = homeX + this.random.nextInt(radius * 2 + 1) - radius;
					int z1 = homeZ + this.random.nextInt(radius * 2 + 1) - radius;
					int y1 = floorY(x1, z1);

					// Optional: ensure still inside radius
					double distSq = mob.getDistanceToHomeSq(x1 + 0.5, y1, z1 + 0.5);
					if (distSq > radius * radius) continue;

					float currentPathWeight = this.mob.getBlockPathWeight(x1, y1, z1);

					if (currentPathWeight > bestPathWeight) {
						bestPathWeight = currentPathWeight;
						x = x1;
						y = y1;
						z = z1;
						canMoveToPoint = true;
					}
				}

				if (canMoveToPoint) {
					this.path = this.mob.world.getEntityPathToXYZ(this.mob, x, y, z, 10.0F);
				}
			}
		}

		return super.onTick();
	}

	public int floorY(int x, int z) {
		int y = (int) mob.y;

		Material up = mob.world.getBlockMaterial(x, y + 1, z);
		Material down = mob.world.getBlockMaterial(x, y, z);

		while (!(up == Material.air && down != Material.air)) {
			if (up != Material.air) {
				y += 1;
			} else {
				y -= 1;
			}
			up = mob.world.getBlockMaterial(x, y + 1, z);
			down = mob.world.getBlockMaterial(x, y, z);
		}

		return y;
	}

	@Override
	protected boolean isEqual(Task other) {
		return other instanceof WanderNearHomeTask;
	}
}
