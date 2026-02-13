package peedog.funnyfauna.entity.ai.path.follow;

import net.minecraft.core.entity.Entity;
import peedog.funnyfauna.entity.MobTaskrunner;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.interfaces.IFollower;
import peedog.funnyfauna.entity.ai.path.PathTask;

public class FollowPlayerTask<T extends MobTaskrunner> extends PathTask<T> {
	public FollowPlayerTask(T mob) {
		super(mob);
		this.moveSpeed = 15.0F;
	}

	@Override
	protected void onStart() {

	}

	@Override
	public Entity lookTarget() {
		if (this.mob instanceof IFollower) return ((IFollower)this.mob).leader();
		else return null;
	}

	@Override
	public Task onTick() {
		Entity target = this.lookTarget();
		if (target != null) {
			this.path = this.mob.world.getPathToEntity(this.mob, target, 20.0F);
		}
		return super.onTick();
	}

	@Override
	protected boolean isEqual(Task other) {
		return false;
	}
}
