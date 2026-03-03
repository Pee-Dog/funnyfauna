package peedog.funnyfauna.entity.scorvid;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.block.material.Material;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.util.collection.NamespaceID;
import net.minecraft.core.util.helper.DamageType;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.World;
import peedog.funnyfauna.entity.MobFlying;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.controllers.ScorvidTask;
import peedog.funnyfauna.entity.ai.interfaces.IFlockable;

import java.util.List;

public class MobScorvid extends MobFlying implements IFlockable {

	public double homeX = 0.0;
	public double homeZ = 0.0;
	private int ambientSoundTimer;

	public MobScorvid(World world) {
		super(world);
		this.textureIdentifier = NamespaceID.getPermanent("funnyfauna", "scorvid");
		this.speed = 0.18f; // Slightly faster than the average bird
		this.ambientSoundTimer = random.nextInt(200);
		this.fireImmune = true;
	}

	@Override
	public void defineSynchedData() {
		super.defineSynchedData();
	}

	@Override
	public Task<MobScorvid> createTask() {
		return new ScorvidTask(this);
	}

	// ================= FLOCKING =================

	@Override
	public boolean canFlockWith(Entity other) {
		return other instanceof MobScorvid && ((MobScorvid) other).getSkinVariant() == this.getSkinVariant();
	}

	@Override
	public boolean isSoloFlying() {
		return false;
	}

	@Override
	public void setSoloFlying(boolean solo) {

	}

	protected void alertFlockToFlee() {
		List<MobScorvid> nearby = world.getEntitiesWithinAABB(MobScorvid.class, bb.grow(8, 4, 8));
		for (MobScorvid scorvid : nearby) {
			if (scorvid != this && !scorvid.isFlying()) {
				scorvid.spook();
			}
		}
	}

	// ================= BEHAVIOR =================

	public void spook() {
		if (!isFlying()) {
			setFlying(true);
			setFlightTime(0);
			setGroundY(getGroundHeightAt(x, y, z));
			alertFlockToFlee();
			for (int i = 0; i < 6; i++) {
				double rx = (x + world.rand.nextDouble() * 2) - 0.5;
				double ry = (MathHelper.floor(y)+ world.rand.nextDouble() );
				double rz = (z + world.rand.nextDouble() * 2) - 0.5 ;
				world.spawnParticle("plume", rx, ry, rz, 0, 0, 0, 4);
			}
		}
	}

	@Override
	public void updateAI() {
		super.updateAI();

		if (!isFlying() && onGround) {
			xd *= 0.3;
			zd *= 0.3;
			homeX = x;
			homeZ = z;
		}

		checkForThreats();
	}

	@Override
	public boolean hurt(Entity attacker, int damage, DamageType damageType) {
		boolean wasHurt = super.hurt(attacker, damage, damageType);
		if (wasHurt && !world.isClientSide) {
			// super.hurt() (MobFlying) already called setFlying(true), so we can't
			// use spook() here — its isFlying() guard would short-circuit everything.
			// Directly alert the flock and spawn feather particles instead.
			alertFlockToFlee();
			for (int i = 0; i < 6; i++) {
				double rx = (x + world.rand.nextDouble() * 2) - 0.5;
				double ry = (MathHelper.floor(y) + world.rand.nextDouble());
				double rz = (z + world.rand.nextDouble() * 2) - 0.5;
				world.spawnParticle("plume", rx, ry, rz, 0, 0, 0, 4);
			}
		}
		return wasHurt;
	}

	protected void checkForThreats() {
		Player player = world.getClosestPlayer(x, y, z, 8);
		if (player != null && !player.isSneaking() && player.isSprinting()) {
			spook();
		}
	}

	// ================= NETHER ADAPTATION =================

	@Override
	protected double getGroundHeightAt(double px, double py, double pz) {
		int bx = MathHelper.floor(px);
		int bz = MathHelper.floor(pz);
		int by = MathHelper.floor(py);

		while (by > 0) {
			int id = world.getBlockId(bx, by, bz);
			if (id != 0) {
				Block block = Blocks.blocksList[id];
				if (block != null) {
					// CRITICAL: If we hit lava, this is NOT a valid landing spot.
					// Return a very low value so the flight AI doesn't try to land here.
					if (block.getMaterial() == Material.lava) {
						return -10.0;
					}
					if (block.isCubeShaped()) {
						return by + 1.0;
					}
				}
			}
			by--;
		}
		return -10.0;
	}

	@Override
	public boolean isInWall() {
		return false;
	}

	// ================= SOUND & DATA =================

	@Override
	public void tick() {
		super.tick();
		if (!world.isClientSide && ambientSoundTimer-- <= 0) {
			world.playSoundAtEntity(null, this, "funnyfauna:mob.scorvid.idle", 1.5F, 0.8F + random.nextFloat() * 0.3F);
			ambientSoundTimer = 200 + random.nextInt(400);
		}
		if (world.rand.nextInt(2) == 1 && isFlying()) {
			double rx = (x + world.rand.nextDouble()) - 0.5;
			double ry = y + world.rand.nextDouble();
			double rz = (z + world.rand.nextDouble()) - 0.5;
			world.spawnParticle("flame", rx, ry, rz, 0, 0, 0, 0);
		}
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
	}

	@Override
	public int getMaxHealth() { return 10; }

	@Override
	public boolean isPerched() {
		return false;
	}

	@Override
	public void setPerched(boolean perched) {

	}
}
