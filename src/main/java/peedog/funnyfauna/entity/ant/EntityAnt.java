package peedog.funnyfauna.entity.ant;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.util.helper.DamageType;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.World;
import peedog.funnyfauna.entity.MobTaskrunner;
import peedog.funnyfauna.entity.ai.PheromoneManager;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.controllers.AntTask;
import peedog.funnyfauna.entity.ai.i.IHomeable;
import peedog.funnyfauna.entity.ai.i.IItemHolder;

public class EntityAnt extends MobTaskrunner implements IHomeable, IItemHolder {

	// Data Watcher ID for climbing state (Byte)
	private static final int DATA_CLIMBING = 16;
	private static final int DATA_HELD_ITEM = 20;

	public int homeX = -1;
	public int homeY = -1;
	public int homeZ = -1;
	public boolean hasHome = false;
	private boolean initializedHome = false; // Track if home has been set

	public EntityAnt(World world) {
		super(world);
		this.setSize(0.25F, 0.25F); // Standard small bug size
		this.footSize = 1F; // Allows stepping up full blocks automatically
		this.moveSpeed = 0.25F;
		this.heartsHalvesLife = 10;

		// Set initial home if spawning naturally
		if (!world.isClientSide) {
			setHomeToCurrentPosition();
		}
	}

	@Override
	public void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(DATA_CLIMBING, (byte)0, Byte.class);
		this.entityData.define(DATA_HELD_ITEM, null, ItemStack.class);
	}

	@Override
	public Task<EntityAnt> createTask() {
		return new AntTask(this);
	}

	@Override
	public void tick() {
		super.tick();

		if (!this.world.isClientSide) {
			// Initialize home on first tick if not already set
			if (!initializedHome && this.tickCount > 5) {
				setHomeToCurrentPosition();
				initializedHome = true;
			}

			// 1. Handle Wall Climbing Physics
			this.setBesideClimbableBlock(this.horizontalCollision);

			// 2. Leave Pheromone Trail ONLY when carrying item back to home
			if (this.getHeldItem() != null && this.tickCount % 10 == 0) {
				PheromoneManager.addScent(
					MathHelper.floor(this.x),
					MathHelper.floor(this.y),
					MathHelper.floor(this.z)
				);
			}
		}
	}

	// Method to set home to current position
	private void setHomeToCurrentPosition() {
		if (!this.hasHome) {
			this.homeX = MathHelper.floor(this.x);
			this.homeY = MathHelper.floor(this.y);
			this.homeZ = MathHelper.floor(this.z);
			this.hasHome = true;
		}
	}

	// --- Climbing Logic (Spider Style) ---
	@Override
	public boolean canClimb() {
		return this.isBesideClimbableBlock();
	}

	public boolean isBesideClimbableBlock() {
		return (this.entityData.getByte(DATA_CLIMBING) & 1) != 0;
	}

	public void setBesideClimbableBlock(boolean climbing) {
		byte b0 = this.entityData.getByte(DATA_CLIMBING);
		if (climbing) {
			b0 = (byte)(b0 | 1);
		} else {
			b0 = (byte)(b0 & -2);
		}
		this.entityData.set(DATA_CLIMBING, b0);
	}

	@Override
	public void moveEntityWithHeading(float moveStrafing, float moveForward) {
		super.moveEntityWithHeading(moveStrafing, moveForward);

		if (this.isBesideClimbableBlock() && (this.horizontalCollision || !this.onGround)) {
			if (moveForward > 0.0) {
				this.yd = 0.2; // Climb speed
			} else {
				this.yd = 0.0; // Hold position
			}
			this.xd *= 0.8;
			this.zd *= 0.8;
		}
	}

	@Override
	public boolean hasHome() {
		return this.hasHome;
	}

	public void setHome(int x, int y, int z) {
		this.homeX = x;
		this.homeY = y;
		this.homeZ = z;
		this.hasHome = true;
		this.initializedHome = true;
	}

	@Override
	public int getHomeX() {
		return this.homeX;
	}

	@Override
	public int getHomeY() {
		return this.homeY;
	}

	@Override
	public int getHomeZ() {
		return this.homeZ;
	}

	@Override
	public ItemStack getHeldItem() {
		ItemStack stack = this.entityData.getItemStack(DATA_HELD_ITEM);
		return stack;
	}

	@Override
	public void setHeldItem(ItemStack stack) {
		if (stack == null) {
			this.entityData.set(DATA_HELD_ITEM, null);
		} else {
			this.entityData.set(DATA_HELD_ITEM, stack);
		}
	}

	public double getDistanceToHomeSq(double x, double y, double z) {
		if (!hasHome) return Double.MAX_VALUE;
		double dx = this.homeX + 0.5 - x;
		double dy = this.homeY + 0.5 - y;
		double dz = this.homeZ + 0.5 - z;
		return dx * dx + dy * dy + dz * dz;
	}

	public double getDistanceToHomeSq() {
		return getDistanceToHomeSq(this.x, this.y, this.z);
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putInt("age", this.time);

		// Save home
		tag.putInt("homeX", this.homeX);
		tag.putInt("homeY", this.homeY);
		tag.putInt("homeZ", this.homeZ);
		tag.putBoolean("hasHome", this.hasHome);

		// Save held item
		ItemStack held = this.getHeldItem();
		if (held != null && held.stackSize > 0) {
			CompoundTag itemTag = new CompoundTag();
			held.writeToNBT(itemTag);
			tag.putCompound("HeldItem", itemTag);
		}
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		this.time = tag.getInteger("age");

		// Load home
		if (tag.containsKey("homeX")) {
			this.homeX = tag.getInteger("homeX");
			this.homeY = tag.getInteger("homeY");
			this.homeZ = tag.getInteger("homeZ");
			this.hasHome = tag.getBoolean("hasHome");
			this.initializedHome = true;
		}

		// Load held item
		if (tag.containsKey("HeldItem")) {
			CompoundTag itemTag = tag.getCompound("HeldItem");
			ItemStack stack = ItemStack.readItemStackFromNbt(itemTag);
			this.setHeldItem(stack);
		}
	}

	// --- Standard Properties ---
	@Override
	public int getMaxHealth() {
		return 8;
	}

	@Override
	public boolean collidesWith(Entity entity) {
		return false;
	}

	@Override
	public void spawnInit() {
	}

	public int getAnimFrame() {
		double motion = Math.abs(this.xd) + Math.abs(this.yd) + Math.abs(this.zd);
		return motion > 0.002 ? (this.tickCount / 6) & 1 : 0;
	}

	@Override
	public String getEntityTexture() {
		return getAnimFrame() == 0
			? "funnyfauna:entity/ant/bug1"
			: "funnyfauna:entity/ant/bug2";
	}

	@Override
	public String getLivingSound() {
		return null;
	}

	@Override
	public String getHurtSound() {
		return "random.hurt";
	}

	@Override
	public String getDeathSound() {
		return "random.hurt";
	}
}
