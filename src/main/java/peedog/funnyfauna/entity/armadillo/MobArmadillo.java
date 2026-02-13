package peedog.funnyfauna.entity.armadillo;

import net.minecraft.core.entity.Entity;
import net.minecraft.core.util.collection.NamespaceID;
import net.minecraft.core.util.helper.DamageType;
import net.minecraft.core.world.World;
import net.minecraft.core.WeightedRandomLootObject;
import peedog.funnyfauna.entity.MobTaskrunner;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.controllers.ArmadilloTask;
import peedog.funnyfauna.entity.ai.interfaces.IFleeable;
import peedog.funnyfauna.item.FunnyFaunaItems;

public class MobArmadillo extends MobTaskrunner implements IFleeable {
	private static final int DATA_COWERING = 16;
	private int fleeTimer = 0;
	private Entity fleeTarget = null;
	public float cowerProgress = 0.0F;

	public MobArmadillo(World world) {
		super(world);
		this.textureIdentifier = NamespaceID.getPermanent("funnyfauna", "armadillo");
		this.setSize(0.8F, 0.8F);
		this.mobDrops.add(new WeightedRandomLootObject(FunnyFaunaItems.SCUTE.getDefaultStack(), 2, 3));
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(DATA_COWERING, (byte)0, Byte.class);
	}

	@Override
	public Task<MobArmadillo> createTask() {
		return new ArmadilloTask(this);
	}

	public boolean isCowering() {
		return this.entityData.getByte(DATA_COWERING) != 0;
	}

	public void setCowering(boolean cowering) {
		this.entityData.set(DATA_COWERING, (byte)(cowering ? 1 : 0));
	}

	@Override
	public void onLivingUpdate() {
		super.onLivingUpdate();

		// Interpolate cower animation progress for the renderer
		float target = isCowering() ? 1.0F : 0.0F;
		cowerProgress += (target - cowerProgress) * 0.05F;

		if (isCowering()) {
			this.setSize(0.7F, 0.6F);
		} else {
			if (cowerProgress < 0.01F) this.setSize(0.8F, 0.8F);
		}
	}

	@Override
	public boolean hurt(Entity attacker, int damage, DamageType type) {
		if (!world.isClientSide && attacker != null) {
			this.fleeTarget = attacker;
			this.fleeTimer = 20 * 6; // 6 seconds
		}

		if (isCowering()) {
			damage = Math.max(1, damage / 4);
			this.yd = 0; // Prevent vertical knockback
		}

		return super.hurt(attacker, damage, type);
	}

	// IFleeable Implementation
	@Override public int getFleeTimer() { return fleeTimer; }
	@Override public void setFleeTimer(int ticks) { this.fleeTimer = ticks; }
	@Override public Entity getFleeTarget() { return fleeTarget; }
	@Override public void setFleeTarget(Entity entity) { this.fleeTarget = entity; }

	@Override public int getMaxHealth() { return 16; }

	public float cowerProgress(float partialTick) {return cowerProgress;}
}
