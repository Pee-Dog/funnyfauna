package peedog.funnyfauna.entity.armadillo;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.core.entity.animal.MobAnimal;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.util.collection.NamespaceID;
import net.minecraft.core.util.helper.DamageType;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.World;
import net.minecraft.core.WeightedRandomLootObject;
import org.useless.util.AnimationState;
import peedog.funnyfauna.item.FunnyFaunaItems;

public class MobArmadillo extends MobAnimal {

	// --- COWER STATE ---
	public AnimationState cowerState = new AnimationState();
	public boolean isCowering = false;
	public int cowerTicks = 0;
	public static final int COWER_DURATION = 20 * 6; // 6 seconds

	public MobArmadillo(World world) {
		super(world);
		this.textureIdentifier = NamespaceID.getPermanent("funnyfauna", "armadillo");
		this.setSize(0.8F, 0.8F);
		this.mobDrops.add(new WeightedRandomLootObject(FunnyFaunaItems.SCALES.getDefaultStack(), 2, 3));
	}

	@Override
	public int getMaxHealth() {
		return 14;
	}

	@Override
	public boolean canSpawnHere() {
		int x = MathHelper.floor(this.x);
		int y = MathHelper.floor(this.bb.minY);
		int z = MathHelper.floor(this.z);
		int ground = this.world.getBlockId(x, y - 1, z);
		if (ground == 0) return false;
		return super.canSpawnHere();
	}

	// --------------------
	// COWER CONTROL
	// --------------------
	public void startCowering() {
		if (!isCowering) {
			isCowering = true;
			cowerTicks = COWER_DURATION;
			cowerState.start(tickCount); // start animation
		}
	}

	public void stopCowering() {
		if (isCowering) {
			isCowering = false;
			cowerTicks = 0;
			cowerState.stop(); // end animation
		}
	}

	@Override
	public void onLivingUpdate() {
		super.onLivingUpdate();

		if (isCowering) {
			cowerTicks--;

			// Strong knockback resistance
			this.xd *= 0.1;
			this.zd *= 0.1;

			// Freeze movement
			this.moveForward = 0;
			this.moveStrafing = 0;

			// Slightly smaller hitbox while curled
			this.setSize(0.7F, 0.6F);

			// Stop cowering when timer runs out
			if (cowerTicks <= 0) stopCowering();
		} else {
			this.setSize(0.8F, 0.8F);
		}
	}

	@Override
	public boolean hurt(Entity attacker, int damage, DamageType type) {
		if (!world.isClientSide) {

			// Enter cower state on hit
			if (!isCowering) startCowering();

			// Heavy damage reduction while cowering
			if (isCowering) damage = Math.max(1, damage / 4);
		}

		return super.hurt(attacker, damage, type);
	}

	// --------------------
	// SOUNDS
	// --------------------
	@Override
	public String getLivingSound() {
		return "funnyfauna:mob.armadillo.idle";
	}

	@Override
	public String getHurtSound() {
		return "funnyfauna:mob.armadillo.idle";
	}

	@Override
	public String getDeathSound() {
		return "funnyfauna:mob.armadillo.idle";
	}

	public boolean isFavouriteItem(ItemStack itemStack) {
		return itemStack != null && itemStack.getItem() == FunnyFaunaItems.JAR_CRICKET;
	}

	// --------------------
	// SAVE DATA
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
