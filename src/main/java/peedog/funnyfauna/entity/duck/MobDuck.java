package peedog.funnyfauna.entity.duck;

import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.util.collection.NamespaceID;
import net.minecraft.core.world.World;
import peedog.funnyfauna.entity.MobFlying;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.controllers.DuckTask;
import peedog.funnyfauna.entity.ai.interfaces.IFlockable;
import peedog.funnyfauna.entity.bird.MobBird;

import java.util.List;

public class MobDuck extends MobFlying implements IFlockable {

	public MobDuck(World world) {
		super(world);
		this.textureIdentifier = NamespaceID.getPermanent("funnyfauna", "duck");
		speed = 0.18f;
	}

	@Override
	public Task<MobDuck> createTask() {
		return new DuckTask(this);
	}

	@Override
	public boolean isPerched() {
		return false;
	}

	@Override
	public void setPerched(boolean perched) {

	}

	@Override
	public boolean canFlockWith(Entity other) {
		return (other instanceof MobDuck);
	}

	@Override
	public boolean isSoloFlying() {
		return false;
	}

	@Override
	public void setSoloFlying(boolean solo) {

	}
}
