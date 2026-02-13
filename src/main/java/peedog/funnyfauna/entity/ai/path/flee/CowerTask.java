package peedog.funnyfauna.entity.ai.path.flee;

import net.minecraft.core.entity.Entity;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.path.PathTask;
import peedog.funnyfauna.entity.armadillo.MobArmadillo;

public class CowerTask extends PathTask<MobArmadillo> {
	public CowerTask(MobArmadillo mob) {
		super(mob);
		this.moveSpeed = 0.5F; // A slower retreat looks more natural
	}

	@Override
	public void onStart() {
		mob.footSize = 1.0F;
		mob.setCowering(true);
	}

	@Override
	public Entity lookTarget() {
		// This ensures PathTask faces the attacker
		return mob.getFleeTarget();
	}

	@Override
	public Task onTick() {
		if (mob.getFleeTimer() <= 0 || mob.getFleeTarget() == null) {
			return null;
		}

		mob.setFleeTimer(mob.getFleeTimer() - 1);

		// 1. Every second, find a path TOWARD the player.
		// We do this because PathTask is hardcoded to move TOWARD the path.
		// Since we will invert the movement later, moving 'toward' a path
		// at the player will result in backing away from the player.
		if (this.path == null || mob.tickCount % 20 == 0) {
			Entity danger = mob.getFleeTarget();
			this.path = mob.world.getPathToEntity(mob, danger, 16.0F);
		}

		// 2. Let PathTask calculate the movement to face the player and move forward
		super.onTick();

		// 3. THE FIX: Invert the movement.
		// PathTask just set these to move forward; we flip them to move backward.
		mob.setMoveForward(mob.getMoveForward() * -1.0F);
		mob.setMoveStrafing(mob.getMoveStrafing() * -1.0F);

		return null;
	}

	@Override
	public void onStop(Task interruptTask) {
		super.onStop(interruptTask);
		mob.setCowering(false);
		mob.setMoveForward(0);
		mob.setMoveStrafing(0);
		mob.footSize = 0.5F;
	}

	@Override
	protected boolean isEqual(Task other) {
		return other instanceof CowerTask;
	}
}
