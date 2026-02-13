package peedog.funnyfauna.entity.ai.path.follow;

import net.minecraft.core.entity.Entity;
import net.minecraft.core.util.helper.MathHelper;
import peedog.funnyfauna.entity.MobTaskrunner;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.interfaces.IFollower;
import peedog.funnyfauna.entity.ai.path.PathTask;

public class FollowLeaderTask<T extends MobTaskrunner & IFollower>
	extends PathTask<T> {

	public FollowLeaderTask(T mob) {
		super(mob);
	}

	@Override
	protected void onStart() {

	}

	@Override
	public Task onTick() {

		Entity leader = mob.leader();
		if (leader == null) {
			this.path = null;
			return null;
		}

		float dist = leader.distanceTo(mob);

		// Too close → stop
		if (dist < mob.followStartDistance()) {
			this.path = null;
			return null;
		}

		// Too far → teleport
		if (dist > mob.teleportDistance()) {
			teleportNear(leader);
			return null;
		}

		this.moveSpeed = mob.followSpeed();
		this.path = mob.world.getPathToEntity(mob, leader, 16.0F);

		return super.onTick();
	}

	private void teleportNear(Entity leader) {

		int x = MathHelper.floor(leader.x);
		int y = MathHelper.floor(leader.bb.minY);
		int z = MathHelper.floor(leader.z);

		for (int dx = -2; dx <= 2; ++dx) {
			for (int dz = -2; dz <= 2; ++dz) {

				if ((Math.abs(dx) > 1 || Math.abs(dz) > 1)
					&& mob.world.isBlockNormalCube(x + dx, y - 1, z + dz)
					&& !mob.world.isBlockNormalCube(x + dx, y, z + dz)
					&& !mob.world.isBlockNormalCube(x + dx, y + 1, z + dz)) {

					mob.moveTo(
						x + dx + 0.5,
						y,
						z + dz + 0.5,
						mob.yRot,
						mob.xRot
					);

					mob.fallDistance = 0.0F;
					return;
				}
			}
		}
	}

	@Override
	protected boolean isEqual(Task other) {
		return other instanceof FollowLeaderTask;
	}
}
