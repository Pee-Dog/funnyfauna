package peedog.funnyfauna.entity.ant;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.World;
import peedog.funnyfauna.entity.MobTaskrunner;
import peedog.funnyfauna.entity.ai.path.PheromoneManager;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.controllers.AntTask;
import peedog.funnyfauna.entity.ai.interfaces.IHomeable;
import peedog.funnyfauna.entity.ai.interfaces.IItemHolder;

public class EntityAnt extends MobTaskrunner implements IHomeable, IItemHolder {

	private static final int DATA_CLIMBING = 16;
	private static final int DATA_HELD_ITEM = 20;

	// How long (ticks) after being released before the ant can re-enter a hill.
	// Set by TileEntityAntHill.releaseAnts(); counts down every server tick.
	public int exitCooldown = 0;

	public int homeX = -1;
	public int homeY = -1;
	public int homeZ = -1;
	public boolean hasHome = false;

	public EntityAnt(World world) {
		super(world);
		this.setSize(0.25F, 0.25F);
		this.footSize = 1F;
		this.moveSpeed = 0.25F;
		this.heartsHalvesLife = 10;
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
			// Tick down the exit cooldown so the ant can eventually re-enter
			if (exitCooldown > 0) exitCooldown--;

			// Handle wall climbing
			this.setBesideClimbableBlock(this.horizontalCollision);

			// Leave pheromone trail only when carrying an item home
			if (this.getHeldItem() != null && this.hasHome && this.tickCount % 10 == 0) {
				PheromoneManager.addScent(
					MathHelper.floor(this.x),
					MathHelper.floor(this.y),
					MathHelper.floor(this.z)
				);
			}
		}
	}

	// --- Climbing Logic ---

	@Override
	public boolean canClimb() {
		return this.isBesideClimbableBlock();
	}

	public boolean isBesideClimbableBlock() {
		return (this.entityData.getByte(DATA_CLIMBING) & 1) != 0;
	}

	public void setBesideClimbableBlock(boolean climbing) {
		byte b0 = this.entityData.getByte(DATA_CLIMBING);
		b0 = climbing ? (byte)(b0 | 1) : (byte)(b0 & -2);
		this.entityData.set(DATA_CLIMBING, b0);
	}

	@Override
	public void moveEntityWithHeading(float moveStrafing, float moveForward) {
		super.moveEntityWithHeading(moveStrafing, moveForward);

		if (this.isBesideClimbableBlock() && (this.horizontalCollision || !this.onGround)) {
			this.yd = (moveForward > 0.0) ? 0.2 : 0.0;
			this.xd *= 0.8;
			this.zd *= 0.8;
		}
	}

	// --- IHomeable ---

	@Override
	public boolean hasHome() { return this.hasHome; }

	public void setHome(int x, int y, int z) {
		this.homeX = x;
		this.homeY = y;
		this.homeZ = z;
		this.hasHome = true;
	}

	@Override public int getHomeX() { return this.homeX; }
	@Override public int getHomeY() { return this.homeY; }
	@Override public int getHomeZ() { return this.homeZ; }

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

	// --- IItemHolder ---

	@Override
	public ItemStack getHeldItem() {
		return this.entityData.getItemStack(DATA_HELD_ITEM);
	}

	@Override
	public void setHeldItem(ItemStack stack) {
		this.entityData.set(DATA_HELD_ITEM, stack);
	}

	// --- NBT ---

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putInt("age", this.time);
		tag.putInt("homeX", this.homeX);
		tag.putInt("homeY", this.homeY);
		tag.putInt("homeZ", this.homeZ);
		tag.putBoolean("hasHome", this.hasHome);
		tag.putInt("exitCooldown", this.exitCooldown);

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

		if (tag.containsKey("homeX")) {
			this.homeX = tag.getInteger("homeX");
			this.homeY = tag.getInteger("homeY");
			this.homeZ = tag.getInteger("homeZ");
			this.hasHome = tag.getBoolean("hasHome");
		}

		if (tag.containsKey("exitCooldown")) {
			this.exitCooldown = tag.getInteger("exitCooldown");
		}

		if (tag.containsKey("HeldItem")) {
			ItemStack stack = ItemStack.readItemStackFromNbt(tag.getCompound("HeldItem"));
			this.setHeldItem(stack);
		}
	}

	// --- Standard Properties ---

	@Override public int getMaxHealth() { return 8; }
	@Override public boolean collidesWith(Entity entity) { return false; }

	@Override
	public void spawnInit() {
		System.out.println("Ant spawned at " + (int)this.x + "," + (int)this.y + "," + (int)this.z);
	}

	public int getAnimFrame() {
		double motion = Math.abs(this.xd) + Math.abs(this.yd) + Math.abs(this.zd);
		return motion > 0.002 ? (this.tickCount / 6) & 1 : 0;
	}

	@Override
	public String getEntityTexture() {
		return getAnimFrame() == 0 ? "funnyfauna:entity/ant/bug1" : "funnyfauna:entity/ant/bug2";
	}

	@Override public String getLivingSound() { return null; }
	@Override public String getHurtSound()   { return "random.hurt"; }
	@Override public String getDeathSound()  { return "random.hurt"; }
}
