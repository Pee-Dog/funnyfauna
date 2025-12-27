package peedog.funnyfauna.entity.lizard;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.animal.MobAnimal;
import net.minecraft.core.item.Items;
import net.minecraft.core.util.collection.NamespaceID;
import net.minecraft.core.util.helper.DamageType;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.World;
import org.jetbrains.annotations.NotNull;
import peedog.funnyfauna.item.FunnyFaunaItems;

public class MobLizard extends MobAnimal {

	public MobLizard(World world) {
		super(world);
		this.textureIdentifier = NamespaceID.getPermanent("funnyfauna", "lizard");
		this.setSize(1F, 0.5F);
	}
	@Override
	public int getMaxHealth() {
		return 4;
	}
	@Override
	public boolean canSpawnHere() {
		int x = MathHelper.floor(this.x);
		int y = MathHelper.floor(this.bb.minY);
		int z = MathHelper.floor(this.z);

		int id = this.world.getBlockId(x, y - 1, z);

		// Prevent spawning on air, water, lava
		if (id == 0 || id == 8 || id == 9 || id == 10 || id == 11) {
			return false;
		}

		// Allow spawning on any other block
		return true;
	}
	public boolean hasTail = true;

	@Override
	public boolean hurt(Entity attacker, int i, DamageType type) {
		boolean result = super.hurt(attacker, i, type);
		// Lose tail the first time it gets hurt
		if (result && this.hasTail) {
			this.hasTail = false;
			this.dropItem(FunnyFaunaItems.FOOD_LIZARDTAIL.id, 1);
		}

		return result;
	}
	@Override
	public void onLivingUpdate() {
		super.onLivingUpdate();
		if (!this.hasTail && this.random.nextInt(1200) == 0) {
			this.hasTail = true;
		}
	}
	@Override
	public void addAdditionalSaveData(@NotNull CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putBoolean("HasTail", this.hasTail);
	}

	@Override
	public void readAdditionalSaveData(@NotNull CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		this.hasTail = tag.getBoolean("HasTail");
	}
	@Override
	public String getLivingSound() {
		return "funnyfauna:mob.lizard.idle";
	}

	@Override
	public String getHurtSound() {
		return "funnyfauna:mob.lizard.hurt";
	}

	@Override
	public String getDeathSound() {
		return "funnyfauna:mob.lizard.death";

	}




}
