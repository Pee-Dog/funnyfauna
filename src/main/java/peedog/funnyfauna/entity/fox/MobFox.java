package peedog.funnyfauna.entity.fox;

import com.mojang.nbt.tags.CompoundTag;

import java.util.UUID;

import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.Mob;
import net.minecraft.core.entity.animal.MobAnimal;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemPaintBrush;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.item.Items;
import net.minecraft.core.util.collection.NamespaceID;
import net.minecraft.core.util.helper.DamageType;
import net.minecraft.core.util.helper.DyeColor;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.util.helper.UUIDHelper;
import net.minecraft.core.world.World;
import net.minecraft.core.world.pathfinder.Path;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Unique;
import peedog.funnyfauna.entity.MobTaskrunner;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.controllers.FoxTask;
import peedog.funnyfauna.entity.ai.interfaces.IFollower;

public class MobFox extends MobTaskrunner implements IFollower {

	// ===== Data IDs =====
	public static final int DATA_FLAGS = 16;
	public static final int DATA_OWNER_UUID = 17;
	public static final int DATA_COLLAR_COLOR = 18; // added

	// ===== Flag masks =====
	public static final int MASK_SITTING = 1;
	public static final int MASK_TAMED   = 2;
	public static final int MASK_DISTRACTING = 4; // new flag

	public static final int DEFAULT_COLLAR_COLOR = 0xFF0000; // red

	private Mob distractionTarget;
	private int distractionTicks;
	private int nipCooldown;
	private int skirmishMoveCooldown;
	private int strikeCooldown;
	private int strikeTicks;
	private boolean striking;
	private boolean orbitClockwise = true;
	private Mob forcedTarget;





	public MobFox(World world) {
		super(world);
		this.textureIdentifier = NamespaceID.getPermanent("funnyfauna", "fox");
		this.setSize(0.7F, 0.6F);
		this.moveSpeed = 1.2F;
		this.scoreValue = 300;
	}

	@Override
	public Task<? extends MobTaskrunner> createTask() {
		return new FoxTask(this);
	}


	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(DATA_FLAGS, (byte)0, Byte.class);
		this.entityData.define(DATA_OWNER_UUID, (UUID) null, UUID.class);
		this.entityData.define(DATA_COLLAR_COLOR, (byte)14, Byte.class);
	}

	// =========================================================
	// Saving / Loading
	// =========================================================

	@Override
	public void addAdditionalSaveData(@NotNull CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putBoolean("Sitting", this.isFoxSitting());
		UUIDHelper.writeToTag(tag, this.getFoxOwner(), "OwnerUUID");
		tag.putByte("CollarColor", (byte)this.getCollarColor().blockMeta);
	}

	@Override
	public void readAdditionalSaveData(@NotNull CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		this.setFoxSitting(tag.getBoolean("Sitting"));

		if (tag.containsKey("CollarColor")) {
			this.setCollarColor(DyeColor.colorFromBlockMeta(tag.getByte("CollarColor")));
		}
		UUID owner = UUIDHelper.readFromTag(tag, "OwnerUUID");
		if (owner != null) {
			this.setFoxOwner(owner);
			this.setFoxTamed(true);
		}
	}

	@Override
	protected boolean canDespawn() {
		return !this.isFoxTamed() && super.canDespawn();
	}
	@Override
	public void push(Entity entity) {
		// If we are currently distracting/orbiting, disable physical collision
		// with the target to prevent getting stuck or pushing them around.
		if (this.isDistracting() && entity == this.distractionTarget) {
			return;
		}
		super.push(entity);
	}

	@Override
	protected boolean isMovementCeased() {
		return this.isFoxSitting();
	}
	public void beginDistraction(Mob attacker) {
		if (this.isFoxSitting()) return;
		if (attacker == null || !attacker.isAlive()) return;

		this.distractionTarget = attacker;
		this.distractionTicks = 200;

		this.skirmishMoveCooldown = 0;
		this.strikeCooldown = 20 + random.nextInt(20);
		this.striking = false;

		this.setPathToEntity(null);
		this.setDistracting(true);
	}





	private void updateDistraction() {
		if (!this.isDistracting()) return;

		Mob target = this.distractionTarget;

		// Stop if target is dead/gone or timer runs out
		if (target == null || target.isRemoved() || this.distractionTicks-- <= 0) {
			clearDistraction();
			return;
		}

		// 1. Always look at the target
		this.lookAt(target, 30.0F, 30.0F);

		// 2. Try to bite if close enough
		double distSqr = this.distanceToSqr(target); // Helper for (dx*dx + dz*dz)
		if (distSqr < 4.0) {
			tryNip(target);
		}

		// 3. CONTINUOUS MOVEMENT
		// We call this every single tick so friction doesn't stop the fox.
		runRandomNear(target);
	}



	public void clearDistraction() {
		this.setDistracting(false);
		this.distractionTarget = null;
		this.distractionTicks = 0;

		// HARD reset movement
		this.setPathToEntity(null);
		this.xd = 0;
		this.yd = 0;
		this.zd = 0;

		this.moveSpeed = 1.2F;
	}

	public void tryNip(Entity target) {
		if (--this.nipCooldown > 0) return;

		double dist = this.distanceTo(target);
		if (dist > 2.2) return;

		this.nipCooldown = 20; // once per second

		// Play feedback
		this.world.spawnParticle(
			"crit",
			target.x,
			target.y + 0.5,
			target.z,
			0, 0, 0, 0
		);

		// Apply minimal damage
		target.hurt(this, 1, DamageType.GENERIC);
	}

	// =========================================================
	// Interaction
	// =========================================================

	@Override
	public boolean interact(@NotNull Player player) {
		ItemStack stack = player.inventory.getCurrentItem();

		// ---- Taming Logic ----
		if (!this.isFoxTamed()) {
			if (stack != null && stack.itemID == Items.EGG_CHICKEN.id) {
				stack.consumeItem(player);
				if (!this.world.isClientSide) {
					if (this.random.nextInt(3) == 0) {
						this.setFoxTamed(true);
						this.setFoxOwner(player.uuid);
						this.setFoxSitting(true);
						this.setHealthRaw(this.getMaxHealth());
						this.showHearts(true);
					} else {
						this.showHearts(false);
					}
				}
				return true;
			}
		} else if (player.uuid.equals(this.getFoxOwner())) {

			// 2. PAINTBRUSH SUPPORT (Based on ItemPaintBrush source)
			if (stack != null && stack.getItem() instanceof ItemPaintBrush) {
				DyeColor brushColor = ItemPaintBrush.getColor(stack);

				// If the brush has paint and it's a different color than the current collar
				if (brushColor != null && brushColor != this.getCollarColor()) {
					this.setCollarColor(brushColor);

					// Call the brush's internal method to reduce paint/durability
					((ItemPaintBrush)stack.getItem()).consumePaint(stack, player);

					return true;
				}
			}
			// 4. SITTING TOGGLE (Empty hand or non-tool)
			if (!this.world.isClientSide) {
				this.setFoxSitting(!this.isFoxSitting());
				this.setPathToEntity(null);
			}
			return true;
		}

		return super.interact(player);
	}
	private void showHearts(boolean success) {
		String particle = success ? "heart" : "smoke";
		for (int i = 0; i < 7; ++i) {
			this.world.spawnParticle(
				particle,
				this.x + (this.random.nextFloat() - 0.5) * this.bbWidth,
				this.y + this.random.nextFloat() * this.bbHeight,
				this.z + (this.random.nextFloat() - 0.5) * this.bbWidth,
				0, 0, 0, 0
			);
		}
	}

	// =========================================================
	// Combat (Disabled)
	// =========================================================

	@Override
	protected Entity findPlayerToAttack() {
		return null;
	}

	@Override
	protected void attackEntity(@NotNull Entity entity, float distance) {
		// Foxes do not attack
	}
	private void hopAwayFrom(Mob target) {
		if (!this.onGround) return;

		double dx = target.x - this.x;
		double dz = target.z - this.z;

		double angle = Math.atan2(dz, dx) + (random.nextDouble() - 0.5) * 0.7;
		this.xd = -Math.cos(angle) * 0.2;
		this.zd = -Math.sin(angle) * 0.2;

		this.yd = 0.3; // vertical hop
		this.isJumping = true;

		this.setPathToEntity(null);
	}

	public void runRandomNear(Entity target) {
		if (target == null) return;

		double dx = target.x - this.x;
		double dz = target.z - this.z;
		double dist = Math.sqrt(dx * dx + dz * dz);

		// 1. APPROACH LOGIC
		// If too far, run straight at them.
		if (dist > 4.5) {
			double speed = 0.4;
			this.xd = (dx / dist) * speed;
			this.zd = (dz / dist) * speed;
			this.lookAt(target, 30.0F, 30.0F);
			return;
		}

		// 2. RANDOM DIRECTION FLIPPING
		// About once every 2-3 seconds, the fox might suddenly change its mind.
		if (this.random.nextInt(50) == 0) {
			orbitClockwise = !orbitClockwise;
		}

		// 3. THE WOBBLE (Non-circularity)
		// We vary the 'target distance' using a sine wave.
		// This makes the fox move in an "egg" shape or a wavy path.
		double wobble = Math.sin(this.tickCount * 0.15) * 1.2;
		double dynamicIdealDist = 2.2 + wobble;

		// 4. CALCULATE VELOCITY COMPONENTS
		// Tangential (The Orbit)
		double tangentX = -dz / dist;
		double tangentZ = dx / dist;
		if (!orbitClockwise) {
			tangentX = -tangentX;
			tangentZ = -tangentZ;
		}

		// --- NEW: AGGRESSIVE RADIAL LOGIC ---
		double radialX = 0;
		double radialZ = 0;

		if (this.nipCooldown <= 0) {
			// NIP IS READY: Dive straight in!
			double diveSpeed = 0.25;
			radialX = (dx / dist) * diveSpeed;
			radialZ = (dz / dist) * diveSpeed;
		} else {
			// NIP ON COOLDOWN: Perform the wobbly dance
			double diff = dist - dynamicIdealDist;
			double correctionStrength = 0.15;
			radialX = (dx / dist) * (diff * correctionStrength);
			radialZ = (dz / dist) * (diff * correctionStrength);
		}

		// 5. APPLY FINAL VELOCITY
		double baseSpeed = 0.32;
		this.xd = (tangentX * baseSpeed) + radialX;
		this.zd = (tangentZ * baseSpeed) + radialZ;

		// 6. FLAIR: Little hops when changing direction or diving in
		if (this.onGround && Math.abs(wobble) > 1.0 && random.nextInt(5) == 0) {
			this.yd = 0.2;
			this.isJumping = true;
		}

		this.lookAt(target, 30.0F, 30.0F);
		this.setPathToEntity(null);
	}
	@Override
	public boolean hurt(Entity attacker, int damage, DamageType type) {
		// 1. Owner Immunity
//		if (this.isFoxTamed() && attacker instanceof Player) {
//			if (((Player)attacker).uuid.equals(this.getFoxOwner())) {
//				return false;
//			}
//		}

		// 2. Player Attacks: No dodging allowed.
		if (attacker instanceof Player) {
			return super.hurt(attacker, damage, type);
		}

		// 3. Mob Attacks: The Dodge Logic
		if (attacker instanceof Mob) {
			Mob mobAttacker = (Mob) attacker;

			// Roll the dice: 0 to 19.
			// If it's NOT 0, the fox dodges (95% chance).
			if (this.random.nextInt(10) != 0) {
				// SUCCESSFUL DODGE
				System.out.println("Fox Dodged!"); // Debug line

				hopAwayFrom(mobAttacker);
				this.skirmishMoveCooldown = 0;

				if (!this.isDistracting()) {
					beginDistraction(mobAttacker);
				}

				return false;
			} else {
				// FAILED DODGE (1 in 20 chance)
				System.out.println("Fox Failed to Dodge!"); // Debug line

				if (!this.isDistracting()) {
					beginDistraction(mobAttacker);
				}
				return super.hurt(attacker, damage, type);
			}
		}

		// 4. Everything else (Fire, Fall, etc.)
		return super.hurt(attacker, damage, type);
	}







	// =========================================================
	// Flags / Getters / Setters
	// =========================================================

	public boolean isFoxSitting() {
		return (this.entityData.getByte(DATA_FLAGS) & MASK_SITTING) != 0;
	}

	public void setFoxSitting(boolean flag) {
		byte data = this.entityData.getByte(DATA_FLAGS);
		this.entityData.set(DATA_FLAGS,
			flag ? (byte)(data | MASK_SITTING) : (byte)(data & ~MASK_SITTING));
	}
	public boolean isDistracting() {
		return (this.entityData.getByte(DATA_FLAGS) & MASK_DISTRACTING) != 0;
	}
	public void setDistracting(boolean flag) {
		byte data = this.entityData.getByte(DATA_FLAGS);
		this.entityData.set(DATA_FLAGS,
			flag ? (byte)(data | MASK_DISTRACTING) : (byte)(data & ~MASK_DISTRACTING));
	}


	public boolean isFoxTamed() {
		return (this.entityData.getByte(DATA_FLAGS) & MASK_TAMED) != 0;
	}

	public void setFoxTamed(boolean flag) {
		byte data = this.entityData.getByte(DATA_FLAGS);
		this.entityData.set(DATA_FLAGS,
			flag ? (byte)(data | MASK_TAMED) : (byte)(data & ~MASK_TAMED));
	}

	public @Nullable UUID getFoxOwner() {
		return this.entityData.getUUID(DATA_OWNER_UUID);
	}

	public void setFoxOwner(UUID uuid) {
		this.entityData.set(DATA_OWNER_UUID, uuid);
	}

	@Override
	public int getMaxHealth() {
		return this.isFoxTamed() ? 20 : 10;
	}

	public DyeColor getCollarColor() {
		return DyeColor.colorFromBlockMeta(this.entityData.getByte(DATA_COLLAR_COLOR) & 15);
	}

	public void setCollarColor(DyeColor color) {
		this.entityData.set(DATA_COLLAR_COLOR, (byte)(color.blockMeta & 15));
	}
	@Override
	public boolean collidesWith(Entity entity) {
		if (entity instanceof Player) {
			Player player = (Player)entity;
			if (player.uuid.equals(this.getFoxOwner()) && !this.isFoxSitting()) {
				return false;
			}
		}
		return true;
	}

	public Entity getDistractionTarget() {
		return this.distractionTarget;
	}


	@Override
	public String getLivingSound() {
		return "funnyfauna:mob.fox.idle";
	}

	@Override
	public String getHurtSound() {
		return "funnyfauna:mob.fox.hurt";
	}

	@Override
	public String getDeathSound() {
		return "funnyfauna:mob.fox.hurt";
	}

	@Override
	public @Nullable Entity leader() {
		if (!this.isFoxTamed()) return null;
		return this.world.getPlayerEntityByUUID(this.getFoxOwner());
	}

	@Override
	public float followSpeed() {
		return 1.2F;
	}
}
