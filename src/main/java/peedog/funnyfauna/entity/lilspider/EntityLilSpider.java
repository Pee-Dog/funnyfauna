package peedog.funnyfauna.entity.lilspider;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.item.Items;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.World;
import org.jetbrains.annotations.NotNull;
import peedog.funnyfauna.entity.MobTaskrunner;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.controllers.LilSpiderTask;
import peedog.funnyfauna.entity.ai.interfaces.IHomeable;
import peedog.funnyfauna.item.FunnyFaunaItems;

public class EntityLilSpider extends MobTaskrunner implements IHomeable {

	// ── Synced data keys ──────────────────────────────────────────────────────
	private static final int DATA_CLIMBING     = 16;
	/**
	 * 0 = idle, 1 = shooting string upward, 2 = rising along string.
	 * Synced so the renderer on the client side can draw the silk thread.
	 */
	private static final int DATA_STRING_STATE = 17;
	/** World-space X coordinate of the silk thread's target (ceiling cobweb spot). */
	private static final int DATA_STRING_X     = 18;
	/** World-space Y coordinate of the silk thread's target. */
	private static final int DATA_STRING_Y     = 19;
	/** World-space Z coordinate of the silk thread's target. */
	private static final int DATA_STRING_Z     = 20;

	// ── String animation (read by renderer) ──────────────────────────────────
	/**
	 * Incremented each tick by the task on the server, and by tick() on the client.
	 * The renderer uses {@code stringAnimTick / STRING_DURATION} as a 0→1 progress
	 * value to animate the growing silk thread.
	 */
	public int stringAnimTick = 0;
	/** Duration (in ticks) of the shoot-string animation. */
	public static final int STRING_DURATION = 20;

	// ── Misc ─────────────────────────────────────────────────────────────────
	private int animFrame = 0;
	private int color;

	// ── Home ─────────────────────────────────────────────────────────────────
	public int     homeX   = -1, homeY = -1, homeZ = -1;
	public boolean hasHome = false;

	// ─────────────────────────────────────────────────────────────────────────

	public EntityLilSpider(World world) {
		super(world);
		this.setSize(0.23F, 0.23F);
		this.moveSpeed = 0.25F;
		if (!world.isClientSide) {
			this.color = generateSpiderColor();
		}
	}

	@Override
	public Task<EntityLilSpider> createTask() {
		return new LilSpiderTask(this);
	}

	@Override
	public void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(DATA_CLIMBING,     (byte) 0, Byte.class);
		this.entityData.define(DATA_STRING_STATE, (byte) 0, Byte.class);
		this.entityData.define(DATA_STRING_X,     0,        Integer.class);
		this.entityData.define(DATA_STRING_Y,     0,        Integer.class);
		this.entityData.define(DATA_STRING_Z,     0,        Integer.class);
	}

	// ── Tick ─────────────────────────────────────────────────────────────────

	@Override
	public void tick() {
		super.tick();

		// ── Client-side stringAnimTick increment ──────────────────────────────
		// The task drives this on the server. On the client, tick() is the only
		// place to advance the counter so the renderer sees a non-zero progress.
		if (world.isClientSide) {
			byte ss = getStringState();
			if (ss == 1) {
				// Growing the thread: count up toward STRING_DURATION
				if (stringAnimTick < STRING_DURATION) stringAnimTick++;
			} else {
				// Not shooting – reset so the next shoot starts fresh
				stringAnimTick = 0;
			}
		}

		// ── Web snap / home-loss logic ────────────────────────────────────────
		if (this.hasHome) {
			int bid = world.getBlockId(homeX, homeY, homeZ);

			if (bid != Blocks.COBWEB.id()) {
				// Cobweb was broken – release home and resume wandering
				this.hasHome = false;
				stopString();
			} else {
				double distSq = distanceSq(this.x, this.y, this.z, homeX + 0.5, homeY + 0.5, homeZ + 0.5);
				if (distSq < 1.0) {
					int myBlockX = (int) Math.floor(this.x);
					int myBlockY = (int) Math.floor(this.y);
					int myBlockZ = (int) Math.floor(this.z);
					if (myBlockX == homeX && myBlockY == homeY && myBlockZ == homeZ) {
						// Snap into place
						this.setPos(homeX + 0.5, homeY + 0.5, homeZ + 0.5);
						this.xd           = 0;
						this.yd           = 0;
						this.zd           = 0;
						this.moveForward  = 0f;
						this.moveStrafing = 0f;
						this.isJumping    = false;
						// EntityLilSpider.java
						world.spawnParticle("webspider", x, y, z, 0, 0, 0, 0);


					}
				}
			}
		}

		updateAnimation();
	}

	public boolean isOnHomeWeb() {
		if (!hasHome) return false;
		return world.getBlockId(homeX, homeY, homeZ) == Blocks.COBWEB.id();
	}


	// ── Climbing ─────────────────────────────────────────────────────────────

	@Override
	public boolean canClimb() { return this.isBesideClimbableBlock(); }

	public boolean isBesideClimbableBlock() {
		return (this.entityData.getByte(DATA_CLIMBING) & 1) != 0;
	}

	public void setBesideClimbableBlock(boolean climbing) {
		byte b0 = this.entityData.getByte(DATA_CLIMBING);
		b0 = climbing ? (byte) (b0 | 1) : (byte) (b0 & -2);
		this.entityData.set(DATA_CLIMBING, b0);
	}

	@Override
	public void moveEntityWithHeading(float moveStrafing, float moveForward) {
		// Prevent any movement while snapped to the cobweb home.
		if (hasHome) {
			double distSq = distanceSq(this.x, this.y, this.z, homeX + 0.5, homeY + 0.5, homeZ + 0.5);
			if (distSq < 0.1) return;
		}

		super.moveEntityWithHeading(moveStrafing, moveForward);

		if (this.isBesideClimbableBlock() && (this.horizontalCollision || !this.onGround)) {
			this.yd  = (moveForward > 0.0) ? 0.2 : 0.0;
			this.xd *= 0.8;
			this.zd *= 0.8;
		}
	}
	private boolean hasCeilingAbove() {
		int blockX = (int) Math.floor(this.x);
		int blockY = (int) Math.floor(this.y + this.bbHeight + 0.01);
		int blockZ = (int) Math.floor(this.z);

		int blockId = world.getBlockId(blockX, blockY, blockZ);
		return blockId != 0; // 0 = air
	}

	@Override
	public boolean isInWall() {
		return false;
	}

	@Override
	public void move(double xd, double yd, double zd) {
		this.stuckInCobweb = false;
		super.move(xd, yd, zd);
	}

	@Override
	public void causeFallDamage(float distance) {
	}

	@Override
	protected void jump() {
	}

	@Override
	protected boolean canDespawn() {
		return !this.hasHome && super.canDespawn();
	}
	// ── String-shooting state (called by LilSpiderTask) ──────────────────────

	/**
	 * Begins the silk-thread animation toward (x, y, z).
	 * Call this when entering the SHOOT_STRING state.
	 */
	public void startShootingString(int x, int y, int z) {
		this.entityData.set(DATA_STRING_X,     x);
		this.entityData.set(DATA_STRING_Y,     y);
		this.entityData.set(DATA_STRING_Z,     z);
		this.entityData.set(DATA_STRING_STATE, (byte) 1);
		this.stringAnimTick = 0;
	}

	/**
	 * Transitions to the RISE state: the spider now climbs the full string.
	 */
	public void setStringStateRising() {
		this.entityData.set(DATA_STRING_STATE, (byte) 2);
	}

	/**
	 * Clears all string state. Call when entering AT_HOME or returning to WANDER.
	 */
	public void stopString() {
		this.entityData.set(DATA_STRING_STATE, (byte) 0);
		this.stringAnimTick = 0;
	}

	/** Returns 0 (none), 1 (shooting), or 2 (rising). */
	public byte getStringState() { return this.entityData.getByte(DATA_STRING_STATE); }

	public int getStringTargetX() { return this.entityData.getInt(DATA_STRING_X); }
	public int getStringTargetY() { return this.entityData.getInt(DATA_STRING_Y); }
	public int getStringTargetZ() { return this.entityData.getInt(DATA_STRING_Z); }

	// ── Animation & Visuals ──────────────────────────────────────────────────

	private void updateAnimation() {
		if (Math.abs(xd) > 0.005 || Math.abs(zd) > 0.005) {
			animFrame = (tickCount / 4) % 2;
		} else {
			animFrame = 0;
		}
	}

	public int getAnimFrame() { return animFrame; }
	public int getColor()     { return color; }

	private int generateSpiderColor() {
		float hue, sat, val;
		switch (random.nextInt(3)) {
			case 0:  hue = 0.0f;  sat = 0.0f;                          val = 0.1f + random.nextFloat() * 0.2f; break;
			case 1:  hue = 0.08f; sat = 0.4f + random.nextFloat() * 0.2f; val = 0.2f + random.nextFloat() * 0.2f; break;
			default: hue = 0.6f;  sat = 0.1f + random.nextFloat() * 0.1f; val = 0.2f + random.nextFloat() * 0.2f; break;
		}
		return java.awt.Color.HSBtoRGB(hue, sat, val) & 0xFFFFFF;
	}

	// ── IHomeable ────────────────────────────────────────────────────────────

	@Override public boolean hasHome()  { return hasHome; }
	@Override public int    getHomeX()  { return homeX;   }
	@Override public int    getHomeY()  { return homeY;   }
	@Override public int    getHomeZ()  { return homeZ;   }

	@Override
	public void setHome(int x, int y, int z) {
		this.homeX   = x;
		this.homeY   = y;
		this.homeZ   = z;
		this.hasHome = true;
	}

	@Override
	public double getDistanceToHomeSq(double x, double y, double z) {
		if (!hasHome) return Double.MAX_VALUE;
		return distanceSq(homeX + 0.5, homeY + 0.5, homeZ + 0.5, x, y, z);
	}

	// ── Interaction ──────────────────────────────────────────────────────────

	@Override
	public void playerTouch(Player player) {
		if (!onGround) return;
		if (player.y <= y + 0.05) return;

		double dx = player.x - x;
		double dz = player.z - z;
		if (dx * dx + dz * dz > 0.5) return;

		world.spawnParticle("bug_squash", x, y + 0.01, z, 0.0, 0.2, 0.0, 0);

		if (!world.isClientSide) remove();
	}
	@Override
	public boolean interact(@NotNull Player player) {
		ItemStack held = player.inventory.getCurrentItem();
		if (held != null && held.itemID == Items.JAR.id) {
			if (!player.world.isClientSide) {
				int slot = player.inventory.getCurrentItemIndex();
				player.inventory.removeItem(slot, 1);

				ItemStack spiderJar = new ItemStack(FunnyFaunaItems.JAR_SPIDER);
				player.inventory.insertItem(spiderJar, true);
				if (spiderJar.stackSize > 0) player.dropPlayerItemWithRandomChoice(spiderJar, false);

				remove();
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean isPushable() {
		// Can't push lizard when riding player
		if (this.vehicle instanceof Player) {
			return false;
		}
		return super.isPushable();
	}


	// ── NBT ──────────────────────────────────────────────────────────────────

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		if (tag.containsKey("SpiderColor")) color = tag.getInteger("SpiderColor");
		if (tag.containsKey("homeX")) {
			this.homeX   = tag.getInteger("homeX");
			this.homeY   = tag.getInteger("homeY");
			this.homeZ   = tag.getInteger("homeZ");
			this.hasHome = tag.getBoolean("hasHome");
		}
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putInt("SpiderColor", color);
		tag.putInt("homeX",       homeX);
		tag.putInt("homeY",       homeY);
		tag.putInt("homeZ",       homeZ);
		tag.putBoolean("hasHome", hasHome);
	}

	// ── Helpers ──────────────────────────────────────────────────────────────

	private static double distanceSq(double ax, double ay, double az, double bx, double by, double bz) {
		double dx = ax - bx, dy = ay - by, dz = az - bz;
		return dx * dx + dy * dy + dz * dz;
	}

	@Override
	protected boolean makeStepSound() { return false; }

	@Override
	public String getHurtSound() {
		return "";
	}

}
