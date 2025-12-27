package peedog.funnyfauna.entity.emu;

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
import peedog.funnyfauna.block.FunnyFaunaBlocks;
import peedog.funnyfauna.item.FunnyFaunaItems;

import java.util.Random;

public class MobEmu extends MobAnimal {
	public int eggTimer;

	public MobEmu(World world) {
		super(world);
		this.textureIdentifier = NamespaceID.getPermanent("funnyfauna", "emu");
		this.setSize(0.3F, 1.8F);
		this.eggTimer = this.random.nextInt(6000) + 6000;
		this.mobDrops.add(new WeightedRandomLootObject(FunnyFaunaItems.BIRDFOOT.getDefaultStack(), 0, 2));
		this.mobDrops.add(new WeightedRandomLootObject(Items.FEATHER_CHICKEN.getDefaultStack(), 0, 4));
	}
	@Override
	public int getMaxHealth() {
		return 10;
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

	@Override
	public String getLivingSound() {
		return "funnyfauna:mob.emu.idle";
	}

	@Override
	public String getHurtSound() {
		return "funnyfauna:mob.emu.idle";
	}

	@Override
	public String getDeathSound() {
		return "funnyfauna:mob.emu.idle";

	}

	public void onLivingUpdate() {
		super.onLivingUpdate();
		int blockX = MathHelper.floor(this.x);
		int blockY = MathHelper.floor(this.bb.minY);
		int blockZ = MathHelper.floor(this.z);
		if (!this.world.isClientSide && --this.eggTimer <= 0 && !(id == 0 || id == 8 || id == 9 || id == 10 || id == 11)) {
			this.world.playSoundAtEntity((Entity)null, this, "mob.chickenplop", 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
			this.eggTimer = this.random.nextInt(6000) + 6000;
			this.world.setBlockWithNotify(blockX, blockY, blockZ, FunnyFaunaBlocks.EGG_EMU_BLOCK.id());
		}

	}
	public boolean isFavouriteItem(ItemStack itemStack) {
		return itemStack != null && itemStack.getItem().hasTag(ItemTags.CHICKENS_FAVOURITE_ITEM);
	}


}
