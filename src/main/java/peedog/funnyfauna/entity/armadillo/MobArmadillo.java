package peedog.funnyfauna.entity.armadillo;

import net.minecraft.core.WeightedRandomLootObject;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.animal.MobAnimal;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.item.Items;
import net.minecraft.core.item.tag.ItemTags;
import net.minecraft.core.util.collection.NamespaceID;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.World;
import peedog.funnyfauna.FunnyFauna;
import peedog.funnyfauna.item.FunnyFaunaItems;

import java.util.Random;

public class MobArmadillo extends MobAnimal {
	public MobArmadillo(World world) {
		super(world);
		this.textureIdentifier = NamespaceID.getPermanent("funnyfauna", "armadillo");
		this.setSize(0.8F, 0.8F);
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

	public void onLivingUpdate() {
		super.onLivingUpdate();
	}
	public boolean isFavouriteItem(ItemStack itemStack) {
		return itemStack != null && itemStack.getItem() == FunnyFaunaItems.JAR_CRICKET;
	}

}
