package peedog.funnyfauna.entity;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.block.material.Material;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.util.helper.DamageType;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.World;
import org.useless.util.AnimationState;
import peedog.funnyfauna.entity.MobTaskrunner;
import peedog.funnyfauna.entity.ai.interfaces.IFlyable;

public abstract class MobFlying extends MobTaskrunner implements IFlyable {

	private static final int DATA_FLAGS = 16;

	// Bit flags
	protected static final int FLAG_FLYING = 1;
	protected static final int FLAG_LANDING = 4;

	public AnimationState flyState = new AnimationState();

	// Flight state
	protected int flightTime = 0;
	protected double groundY = 0.0;

	// Wing animation
	public float flap = 0.0F;
	public float flapSpeed = 0.0F;
	public float oFlap = 0.0F;
	public float oFlapSpeed = 0.0F;
	public float flapping = 1.0F;

	public MobFlying(World world) {
		super(world);
		setSize(0.5F, 0.5F);
	}

	@Override
	public void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(DATA_FLAGS, (byte) 0, Byte.class);
	}

	protected byte getFlags() {
		return this.entityData.getByte(DATA_FLAGS);
	}

	protected void setFlags(byte flags) {
		this.entityData.set(DATA_FLAGS, flags);
	}

	@Override
	public boolean isFlying() {
		return (getFlags() & FLAG_FLYING) != 0;
	}

	@Override
	public void setFlying(boolean flying) {
		byte f = getFlags();
		setFlags(flying ? (byte)(f | FLAG_FLYING) : (byte)(f & ~FLAG_FLYING));

		if (flying) {
			setSize(1.5F, 1.5F);
		} else {
			setSize(0.5F, 0.5F);
		}
		setPos(x, y, z);
	}

	public boolean isLanding() {
		return (getFlags() & FLAG_LANDING) != 0;
	}

	public void setLanding(boolean landing) {
		byte f = getFlags();
		setFlags(landing ? (byte)(f | FLAG_LANDING) : (byte)(f & ~FLAG_LANDING));
	}

	@Override
	public int getFlightTime() {
		return flightTime;
	}

	@Override
	public void setFlightTime(int ticks) {
		this.flightTime = ticks;
	}

	@Override
	public double getGroundY() {
		return groundY;
	}

	@Override
	public void setGroundY(double y) {
		this.groundY = y;
	}

	@Override
	public float getFlap() {
		return flap;
	}

	@Override
	public void setFlap(float flap) {
		this.flap = flap;
	}

	@Override
	public float getFlapSpeed() {
		return flapSpeed;
	}

	@Override
	public void setFlapSpeed(float speed) {
		this.flapSpeed = speed;
	}

	@Override
	public float getFlapping() {
		return flapping;
	}

	@Override
	public void setFlapping(float flapping) {
		this.flapping = flapping;
	}

	@Override
	public void setLandingSize(boolean landing) {
		if (landing) {
			setSize(0.5F, 0.5F);
		} else {
			setFlying(isFlying());
		}
		setPos(x, y, z);
	}

	protected void updateWingAnimation() {
		this.oFlap = this.flap;
		this.oFlapSpeed = this.flapSpeed;

		boolean grounded = onGround;
		this.flapSpeed += (!grounded ? 4 : -1) * 0.3F;

		if (this.flapSpeed < 0) this.flapSpeed = 0;
		if (this.flapSpeed > 1) this.flapSpeed = 1;

		if (!grounded && this.flapping < 1) this.flapping = 1;

		this.flap += this.flapping * 2.0F;
	}

	@Override
	public void tick() {
		super.tick();
		flyState.animateWhen(isFlying(), tickCount);
		updateWingAnimation();
	}

	@Override
	protected void causeFallDamage(float f) {
		// Flying mobs never take fall damage
	}

	protected double getGroundHeightAt(double px, double py, double pz) {
		int bx = MathHelper.floor(px);
		int bz = MathHelper.floor(pz);
		int by = MathHelper.floor(py);

		while (by > 0) {
			int id = world.getBlockId(bx, by, bz);
			if (id != 0 && Blocks.blocksList[id] != null) {
				Block block = Blocks.blocksList[id];
				if (block.isCubeShaped() && block.getMaterial() != Material.leaves) {
					return by + 1.0;
				}
			}
			by--;
		}
		return by + 1.0;
	}

	@Override
	public boolean hurt(Entity attacker, int damage, DamageType damageType) {
		boolean wasHurt = super.hurt(attacker, damage, damageType);
		if (wasHurt && !world.isClientSide) {
			setFlying(true);
			setFlightTime(0);
			setGroundY(getGroundHeightAt(x, y, z));
		}
		return wasHurt;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putDouble("GroundY", groundY);
		tag.putInt("FlightTime", flightTime);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		groundY = tag.getDouble("GroundY");
		flightTime = tag.getInteger("FlightTime");
	}
}
