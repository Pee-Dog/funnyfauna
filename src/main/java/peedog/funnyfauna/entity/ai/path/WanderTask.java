package peedog.funnyfauna.entity.ai.path;

import net.minecraft.core.block.material.Material;
import net.minecraft.core.util.helper.MathHelper;
import peedog.funnyfauna.entity.MobTaskrunner;
import peedog.funnyfauna.entity.ai.Task;

public class WanderTask<T extends MobTaskrunner> extends PathTask<T> {
	public WanderTask(T mob) {
		super(mob);
	}

	@Override
	protected void onStart() {

	}

	public int floorY(int x, int z) {
		int y = (int)mob.y;

		Material up = mob.world.getBlockMaterial(x, y+1, z);
		Material down = mob.world.getBlockMaterial(x, y, z);

		while (!(up == Material.air && down != Material.air)) {
			if (up != Material.air) {
				y += 1;
			} else {
				y -= 1;
			}
			up = mob.world.getBlockMaterial(x, y+1, z);
			down = mob.world.getBlockMaterial(x, y, z);
		}

		return y;
	}

	@Override
	public Task onTick() {
		if (this.path == null) {
			if (this.mob.world != null) {
				boolean canMoveToPoint = false;
				int x = -1;
				int y = -1;
				int z = -1;
				float bestPathWeight = -99999.0F;

				for(int l = 0; l < 25; ++l) {
					int x1 = MathHelper.floor(this.mob.x + (double)this.random.nextInt(13) - (double)6.0F);
					int z1 = MathHelper.floor(this.mob.z + (double)this.random.nextInt(13) - (double)6.0F);
					int y1 = this.floorY(x1, z1);
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

	@Override
	protected boolean isEqual(Task other) {
		return other instanceof WanderTask;
	}
}
