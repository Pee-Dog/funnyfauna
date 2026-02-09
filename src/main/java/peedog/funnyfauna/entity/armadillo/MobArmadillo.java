package peedog.funnyfauna.entity.armadillo;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.core.entity.animal.MobAnimal;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.util.collection.NamespaceID;
import net.minecraft.core.util.helper.DamageType;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.util.phys.AABB;
import net.minecraft.core.util.phys.Vec3;
import net.minecraft.core.world.World;
import net.minecraft.core.WeightedRandomLootObject;
import org.jetbrains.annotations.Nullable;
import peedog.funnyfauna.entity.cricket.EntityCricket;
import peedog.funnyfauna.entity.worm.EntityWorm;
import peedog.funnyfauna.item.FunnyFaunaItems;

import java.util.ArrayList;
import java.util.List;

public class MobArmadillo extends MobAnimal {

	// --------------------
	// COWER STATE
	// --------------------
	public boolean isCowering = false;
	public int cowerTicks = 0;
	public static final int COWER_DURATION = 20 * 6; // 6 seconds
	public float cowerProgress = 0.0F; // 0 = standing, 1 = fully crouched

	// --------------------
	// FLEE / HUNT STATE
	// --------------------
	private int fleeTicks = 0;
	private @Nullable Entity fleeTarget = null;
	private @Nullable Entity preyTarget = null;

	public MobArmadillo(World world) {
		super(world);
		this.textureIdentifier = NamespaceID.getPermanent("funnyfauna", "armadillo");
		this.setSize(0.8F, 0.8F);
		this.mobDrops.add(new WeightedRandomLootObject(FunnyFaunaItems.SCUTE.getDefaultStack(), 2, 3));
	}

	@Override
	public int getMaxHealth() {
		return 16;
	}

	@Override
	public boolean canSpawnHere() {
		int x = MathHelper.floor(this.x);
		int y = MathHelper.floor(this.bb.minY);
		int z = MathHelper.floor(this.z);
		int ground = this.world.getBlockId(x, y - 1, z);
		return ground != 0 && super.canSpawnHere();
	}

	// --------------------
	// COWER CONTROL
	// --------------------
	public void startCowering() {
		if (!isCowering) {
			isCowering = true;
			cowerTicks = COWER_DURATION;
			fleeTicks = 0; // reset any current flee
			preyTarget = null; // stop hunting
		}
	}

	@Override
	public void onLivingUpdate() {
		super.onLivingUpdate();

		// -----------------
		// Handle Cower / Flee
		// -----------------
		if (isCowering) {
			cowerTicks--;

			// -----------------
			// Interpolate crouch
			// -----------------
			float target = 1.0F; // fully crouched
			float speed = 0.05F; // slower crouch
			cowerProgress += (target - cowerProgress) * speed;

			// Smaller hitbox while curled
			this.setSize(0.7F, 0.6F);

			// -----------------
			// Slow movement & flee
			// -----------------
			if (fleeTarget != null && !fleeTarget.isRemoved()) {
				double dx = fleeTarget.x - this.x;
				double dz = fleeTarget.z - this.z;
				double dist = Math.sqrt(dx * dx + dz * dz);
				if (dist < 0.001) dist = 0.001;

// Face the attacker
				this.yRot = (float)(Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;

// Move backward (away from attacker)
				double fleeSpeed = 0.06;
				this.xd = -dx / dist * fleeSpeed;
				this.zd = -dz / dist * fleeSpeed;

			} else {
				this.xd = 0;
				this.zd = 0;
			}

			// Stop cowering when timer runs out
			if (cowerTicks <= 0) {
				isCowering = false;
				fleeTarget = null;
			}
		} else {
			// -----------------
			// Stand up
			// -----------------
			float target = 0.0F;
			float speed = 0.05F;
			cowerProgress += (target - cowerProgress) * speed;
			if (cowerProgress < 0.01F) this.setSize(0.8F, 0.8F);

			// -----------------
			// Hunting behavior
			// -----------------
			updateHunting();
		}
	}

	private void updateHunting() {
		// Clear invalid prey
		if (preyTarget != null && (preyTarget.isRemoved() || !preyTarget.isAlive())) {
			preyTarget = null;
		}

		// Acquire new prey if none
		if (preyTarget == null) {
			List<EntityCricket> nearbyCrickets = this.world.getEntitiesWithinAABB(
				EntityCricket.class,
				AABB.getTemporaryBB(this.x, this.y, this.z, this.x + 1, this.y + 1, this.z + 1).grow(10, 5, 10)
			);
			List<EntityWorm> nearbyWorms = this.world.getEntitiesWithinAABB(
				EntityWorm.class,
				AABB.getTemporaryBB(this.x, this.y, this.z, this.x + 1, this.y + 1, this.z + 1).grow(10, 5, 10)
			);

			List<Entity> allPrey = new ArrayList<>();
			allPrey.addAll(nearbyCrickets);
			allPrey.addAll(nearbyWorms);

			if (!allPrey.isEmpty()) {
				preyTarget = allPrey.get(this.random.nextInt(allPrey.size()));
			}
		}

		// Move toward prey
		if (preyTarget != null) {
			double dx = preyTarget.x - this.x;
			double dz = preyTarget.z - this.z;
			double dist = Math.sqrt(dx * dx + dz * dz);
			if (dist < 0.001) dist = 0.001;

			double speed = 0.12; // normal movement speed
			this.xd = dx / dist * speed;
			this.zd = dz / dist * speed;

			this.yRot = (float)(Math.atan2(this.zd, this.xd) * 180 / Math.PI) - 90.0F;

			// Eat prey if close
			if (this.bb.expand(0.5, 0.5, 0.5).intersects(preyTarget.bb)) {
				preyTarget.outOfWorld();
				preyTarget = null;
				this.xd = 0;
				this.zd = 0;
			}
		}
	}

	@Override
	public boolean hurt(Entity attacker, int damage, DamageType type) {
		if (!world.isClientSide) {
			if (!isCowering) startCowering();
			if (isCowering) damage = Math.max(1, damage / 4); // reduce while cowering

			// Prevent vertical knockback while cowering
			if (isCowering) this.yd = 0;

			// Start fleeing
			this.fleeTarget = attacker;
			this.fleeTicks = COWER_DURATION; // match cower duration
		}
		return super.hurt(attacker, damage, type);
	}

	public float getCowerProgress(float partialTick) {
		return cowerProgress;
	}

	// --------------------
	// Sounds
	// --------------------
	@Override
	public String getLivingSound() { return "funnyfauna:mob.armadillo.idle"; }
	@Override
	public String getHurtSound() { return "funnyfauna:mob.armadillo.idle"; }
	@Override
	public String getDeathSound() { return "funnyfauna:mob.armadillo.idle"; }

	public boolean isFavouriteItem(ItemStack itemStack) {
		return itemStack != null && itemStack.getItem() == FunnyFaunaItems.JAR_CRICKET;
	}

	// --------------------
	// Save / Load
	// --------------------
	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putBoolean("Cowering", isCowering);
		tag.putInt("CowerTicks", cowerTicks);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		isCowering = tag.getBoolean("Cowering");
		cowerTicks = tag.getInteger("CowerTicks");
	}
}
