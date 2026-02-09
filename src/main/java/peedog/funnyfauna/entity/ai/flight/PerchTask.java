package peedog.funnyfauna.entity.ai.flight;

import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.block.material.Material;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.util.phys.AABB;
import peedog.funnyfauna.entity.MobTaskrunner;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.i.IFlyable;

import java.util.List;

public class PerchTask<T extends MobTaskrunner & IFlyable> extends Task<T> {

	public PerchTask(T mob) {
		super(mob);
	}

	@Override
	protected void onStart() {
		// Freeze motion when perching
		mob.xd = 0;
		mob.yd = 0;
		mob.zd = 0;
		mob.setMoveForward(0);

		// Freeze wing animation
		mob.setFlapSpeed(0);
		mob.setFlapping(0);
	}

	@Override
	protected Task onTick() {
		// Keep bird frozen
		mob.xd = 0;
		mob.yd = 0;
		mob.zd = 0;
		mob.setMoveForward(0);

		// Check if we should exit perch
		if (shouldExitPerch()) {
			mob.setPerched(false);
			return null;
		}

		return null;
	}

	private boolean shouldExitPerch() {
		// Exit if spooked by nearby sprinting players
		List<Entity> nearbyLiving = mob.world.getEntitiesWithinAABB(
			Entity.class,
			AABB.getTemporaryBB(mob.x, mob.y, mob.z, mob.x + 1, mob.y + 1, mob.z + 1).grow(8, 6, 8)
		);

		for (Entity entity : nearbyLiving) {
			if (entity == mob) continue;

			if (entity instanceof net.minecraft.core.entity.player.Player) {
				net.minecraft.core.entity.player.Player player = (net.minecraft.core.entity.player.Player) entity;
				if (player.isSprinting()) {
					double dx = player.x - mob.x;
					double dz = player.z - mob.z;
					double distSqr = dx * dx + dz * dz;
					if (distSqr <= 64.0) { // 8 blocks
						return true;
					}
				}
			}
		}

		// Exit if it's daytime and not on leaves
		if (!isNight() && !isStandingOnLeaves()) {
			return true;
		}

		return false;
	}

	private boolean isStandingOnLeaves() {
		if (!mob.onGround && !mob.isPerched()) return false;

		int bx = MathHelper.floor(mob.x);
		int by = MathHelper.floor(mob.y - 0.1);
		int bz = MathHelper.floor(mob.z);

		int id = mob.world.getBlockId(bx, by, bz);
		if (id == 0) return false;

		Block block = Blocks.blocksList[id];
		return block != null && block.getMaterial() == Material.leaves;
	}

	private boolean isNight() {
		long time = mob.world.getWorldTime() % 24000;
		return time >= 12500 && time <= 23500;
	}

	@Override
	protected void onStop(Task interruptTask) {}

	@Override
	protected boolean isEqual(Task other) {
		return other instanceof PerchTask;
	}
}
