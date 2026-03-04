package peedog.funnyfauna.entity.ai.path.follow;

import net.minecraft.core.entity.Entity;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.fox.MobFox;

public class FoxDistractionTask extends Task<MobFox> {

	public FoxDistractionTask(MobFox mob) {
		super(mob);
	}

	@Override
	protected void onStart() {

	}

	@Override
	protected Task onTick() {

		Entity target = mob.getDistractionTarget();

		if (target == null
			|| !target.isAlive()
			|| target.isRemoved()
			|| mob.distanceToSqr(target) > 400) {

			mob.clearDistraction();
			return null;
		}

		mob.lookAt(target, 30f, 30f);

		if (mob.distanceToSqr(target) < 4.0) {
			mob.tryNip(target);
		}

		mob.runRandomNear(target);

		return null;
	}


	@Override
	protected void onStop(Task interruptTask) {
		mob.setDistracting(false);
	}

	@Override
	protected boolean isEqual(Task other) {
		return other instanceof FoxDistractionTask;
	}
}
