package peedog.funnyfauna.entity.tumbleweed;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.util.helper.BlockParticleHelper;
import net.minecraft.core.util.helper.DamageType;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.util.helper.Side;
import net.minecraft.core.world.World;
import peedog.funnyfauna.item.FunnyFaunaItems;
import peedog.funnyfauna.world.WorldWind;

public class EntityTumbleweed extends Entity {

	private static final double GRAVITY = 0.015;
	private static final double WIND_FORCE = 0.0025;
	private static final double MAX_SPEED = 0.10;
	private static final float ROLL_MULTIPLIER = 60.0F;
	private static final double FRICTION = 0.985;
	private static final double MIN_BOUNCE_Y = 0.25;

	private int lifeTime = 0;

	public float rollRotation = 0.0F;
	public float prevRollRotation = 0.0F;

	// Settling control
	private int lowSpeedCounter = 0;
	private static final int SETTLE_TIME = 40; // ticks before settle mode
	private static final double MIN_SPEED_TO_KEEP_ROLLING = 0.01;

	private boolean settling = false;
	private double settleBounce = MIN_BOUNCE_Y;
	private static final double SETTLE_BOUNCE_DECAY = 0.5; // smaller bounces while settling
	private static final double MIN_SETTLE_BOUNCE = 0.02;

	public EntityTumbleweed(World world) {
		super(world);
		this.setSize(0.6F, 0.6F);

		if (!world.isClientSide) {
			float angle = random.nextFloat() * (float)Math.PI * 2F;
			xd = MathHelper.cos(angle) * 0.06;
			zd = MathHelper.sin(angle) * 0.06;
			yd = MIN_BOUNCE_Y;
		}
	}

	@Override
	protected void defineSynchedData() {}

	@Override
	public void tick() {
		super.tick();
		prevRollRotation = rollRotation;
		lifeTime++;

		// -------------------- Kill / despawn --------------------
		if (!world.isClientSide) {
			Player nearest = world.getClosestPlayerToEntity(this, 64);
			if (nearest == null) { remove(); return; }
		}

		if (this.isInWaterOrRain()
			|| world.getBlock(blockX(), blockY(), blockZ()) == Blocks.FLUID_LAVA_STILL
			|| world.getBlock(blockX(), blockY(), blockZ()) == Blocks.FLUID_LAVA_FLOWING) {
			remove();
			return;
		}

		double prevX = x;
		double prevZ = z;

		// -------------------- Apply gravity --------------------
		yd -= GRAVITY;

		// -------------------- Move entity --------------------
		move(xd, yd, zd);

		// -------------------- Global wind --------------------
		double windXGlobal = WorldWind.getWindX();
		double windZGlobal = WorldWind.getWindZ();
		xd += windXGlobal * WIND_FORCE;
		zd += windZGlobal * WIND_FORCE;

		// -------------------- Friction --------------------
		xd *= FRICTION;
		zd *= FRICTION;

		// -------------------- Clamp horizontal speed --------------------
		double speed = Math.sqrt(xd * xd + zd * zd);
		if (speed > MAX_SPEED) {
			double scale = MAX_SPEED / speed;
			xd *= scale;
			zd *= scale;
		}

		// -------------------- Settling detection --------------------
		if (!settling) {
			if (speed < MIN_SPEED_TO_KEEP_ROLLING && horizontalCollision) {
				lowSpeedCounter++;
				if (lowSpeedCounter >= SETTLE_TIME) {
					settling = true;
					settleBounce = MIN_BOUNCE_Y; // start small bounces
				}
			} else {
				lowSpeedCounter = 0;
			}
		}

		// -------------------- Bounce handling --------------------
		if (settling) {
			if (onGround) {
				if (settleBounce > MIN_SETTLE_BOUNCE) {
					yd = settleBounce; // bounce up
					settleBounce *= SETTLE_BOUNCE_DECAY; // decay for next bounce

					// Slowly damp horizontal motion
					xd *= 0.7;
					zd *= 0.7;
				} else {
					// Stop completely
					yd = 0;
					xd = 0;
					zd = 0;
				}
			}
		} else {
			// Regular bounce while moving
			if (onGround && yd < MIN_BOUNCE_Y) {
				yd = MIN_BOUNCE_Y;
			}
		}

		// -------------------- Wall collision --------------------
		if (horizontalCollision) {
			xd = windXGlobal * 0.04;
			zd = windZGlobal * 0.04;
		}

		// -------------------- Rolling --------------------
		double dx = x - prevX;
		double dz = z - prevZ;
		double distance = Math.sqrt(dx * dx + dz * dz);
		if (distance > 0) rollRotation += distance * ROLL_MULTIPLIER;
	}

	private int blockX() { return MathHelper.floor(x); }
	private int blockY() { return MathHelper.floor(y); }
	private int blockZ() { return MathHelper.floor(z); }

	@Override
	protected boolean makeStepSound() { return false; }
	@Override
	public boolean isPickable() { return true; }

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		lifeTime = tag.getInteger("LifeTime");
		rollRotation = tag.getFloat("Roll");
		xd = tag.getDouble("VelX");
		yd = tag.getDouble("VelY");
		zd = tag.getDouble("VelZ");
		lowSpeedCounter = tag.getInteger("LowSpeedCounter");
		settling = tag.getBoolean("Settling");
		settleBounce = tag.getDouble("SettleBounce");
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		tag.putInt("LifeTime", lifeTime);
		tag.putFloat("Roll", rollRotation);
		tag.putDouble("VelX", xd);
		tag.putDouble("VelY", yd);
		tag.putDouble("VelZ", zd);
		tag.putInt("LowSpeedCounter", lowSpeedCounter);
		tag.putBoolean("Settling", settling);
		tag.putDouble("SettleBounce", settleBounce);
	}

	// -------------------- HIT / DAMAGE --------------------

	@Override
	public boolean hurt(Entity source, int amount, DamageType type) {
		if (!world.isClientSide) {
			// Spawn a few block particles (like boat breaking)
			int particleCount = 5 + random.nextInt(4); // 5–8 particles
			for (int i = 0; i < particleCount; i++) {
				world.spawnParticle(
					"block",
					x, y, z,
					xd * 0.2, yd * 0.2, zd * 0.2,
					BlockParticleHelper.encodeBlockData(Blocks.DEADBUSH.id(), 0, Side.BOTTOM) // you can use a tumbleweed-looking block
				);
			}

			// Drop the tumbleweed item
			dropItem(FunnyFaunaItems.TUMBLEWEED.id, 1);

			// Remove entity immediately
			remove();
		}

		return true; // signal that it was “hurt”
	}


}
