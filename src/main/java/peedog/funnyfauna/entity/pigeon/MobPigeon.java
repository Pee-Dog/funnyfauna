package peedog.funnyfauna.entity.pigeon;

import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.util.collection.NamespaceID;
import net.minecraft.core.util.helper.DamageType;
import net.minecraft.core.util.phys.AABB;
import net.minecraft.core.world.World;
import peedog.funnyfauna.entity.MobFlying;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.controllers.DuckTask;
import peedog.funnyfauna.entity.ai.controllers.PigeonTask;
import peedog.funnyfauna.entity.ai.interfaces.IFlockable;
import peedog.funnyfauna.entity.bird.MobBird;

import java.util.List;

public class MobPigeon extends MobFlying implements IFlockable {

	public MobPigeon(World world) {
		super(world);
		this.textureIdentifier = NamespaceID.getPermanent("funnyfauna", "duck");
		speed = 0.18f;
	}

	@Override
	public Task<MobPigeon> createTask() {
		return new PigeonTask(this);
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
		return (other instanceof MobPigeon);
	}

	@Override
	public boolean isSoloFlying() {
		return false;
	}

	@Override
	public void setSoloFlying(boolean solo) {

	}
	protected void alertFlockToFlee() {
		List<MobPigeon> nearbyBirds = world.getEntitiesWithinAABB(
			MobPigeon.class,
			AABB.getTemporaryBB(x, y, z, x + 1, y + 1, z + 1).grow(8, 4, 8)
		);

		for (MobPigeon bird : nearbyBirds) {
			if (bird != this && bird.getSkinVariant() == this.getSkinVariant()) {
				if (!bird.isFlying()) {
					bird.setFlying(true);
					bird.setFlightTime(0);
					bird.setGroundY(bird.getGroundHeightAt(bird.x, bird.y, bird.z));
				}
			}
		}
	}

	protected void checkForThreats() {
		List<Entity> nearby = world.getEntitiesWithinAABB(
			Entity.class,
			AABB.getTemporaryBB(x, y, z, x + 1, y + 1, z + 1).grow(8, 6, 8)
		);

		for (Entity entity : nearby) {
			if (entity == this) continue;

			if (entity instanceof Player) {
				Player player = (Player) entity;
				if (!player.isSneaking() && player.isSprinting()) {
					spookBird();
				}
			}
		}
	}

	protected void spookBird() {
		if (isPerched()) {
			setPerched(false);
		}

		if (!isFlying()) {
			setFlying(true);
			setFlightTime(0);
			setGroundY(getGroundHeightAt(x, y, z));
			alertFlockToFlee();
		}
	}

	@Override
	public boolean hurt(Entity attacker, int damage, DamageType damageType) {
		boolean wasHurt = super.hurt(attacker, damage, damageType);
		if (wasHurt && !world.isClientSide) {
			spookBird();
		}
		return wasHurt;
	}

	@Override
	public void updateAI() {
		super.updateAI();
		checkForThreats();
	}
}
