package peedog.funnyfauna.entity;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.core.entity.MobPathfinder;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.world.World;
import org.jetbrains.annotations.NotNull;
import peedog.funnyfauna.entity.ai.Task;

import java.util.Random;

public abstract class MobTaskrunner extends MobPathfinder {

	public Task<? extends MobTaskrunner> task;
	public int time;

	/**
	 * Runs a task, as well as some other QOL stuff that is common across all of my mobs.
	 */
	public MobTaskrunner(World world) {
		super(world);
		this.task = this.createTask();
	}

	public abstract Task<? extends MobTaskrunner> createTask();

	public void livingTick() {};

	@Override
	public void onLivingUpdate() {
		super.onLivingUpdate();
		this.livingTick();
		time++;
	}

	public float getBlockPathWeight(int x, int y, int z) {
		return 0.0F;
	}

	@Override
	public void updateAI() {
		if (task != null) task.tick();
	}

	@Override
	public void addAdditionalSaveData(@NotNull CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putInt("age", this.time);
	}

	@Override
	public void readAdditionalSaveData(@NotNull CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		this.time = tag.getInteger("age");
	}


	/**
	 * Mobs that can't swim will get stuck in place on the seafloor if this is false. Default is false.
	 */

	public void setMoveForward(float moveForward) {
		this.moveForward = moveForward;
	}
	public void setMoveStrafing(float moveStrafing) {
		this.moveStrafing = moveStrafing;
	}
	public float getMoveForward() {
		return this.moveForward;
	}
	public float getMoveStrafing() {
		return this.moveStrafing;
	}
	public void startJumping() {
		this.isJumping = true;
	}
	public void stopJumping() {
		this.isJumping = false;
	}
}
