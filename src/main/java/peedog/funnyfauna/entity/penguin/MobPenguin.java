package peedog.funnyfauna.entity.penguin;

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
import peedog.funnyfauna.item.FunnyFaunaItems;

import java.util.Random;

public class MobPenguin extends MobAnimal {
	public float flap = 0.0F;
	public float flapSpeed = 0.0F;
	public float oFlapSpeed;
	public float oFlap;
	public float flapping = 1.0F;
	public int eggTimer;

	public MobPenguin(World world) {
		super(world);
		this.textureIdentifier = NamespaceID.getPermanent("funnyfauna", "penguin");
		this.setSize(0.3F, 1.0F);
		this.eggTimer = this.random.nextInt(6000) + 6000;
		this.mobDrops.add(new WeightedRandomLootObject(FunnyFaunaItems.FOOD_BLUBBER.getDefaultStack(), 1, 2));
	}
	@Override
	public int getMaxHealth() {
		return 5;
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
		return "funnyfauna:mob.penguin.idle";
	}

	@Override
	public String getHurtSound() {
		return "funnyfauna:mob.penguin.hurt";
	}

	@Override
	public String getDeathSound() {
		return "funnyfauna:mob.penguin.death";

	}

	public void onLivingUpdate() {
		super.onLivingUpdate();
		this.oFlap = this.flap;
		this.oFlapSpeed = this.flapSpeed;
		this.flapSpeed = (float)((double)this.flapSpeed + (double)(this.onGround ? -1 : 4) * 0.3);
		if (this.flapSpeed < 0.0F) {
			this.flapSpeed = 0.0F;
		}

		if (this.flapSpeed > 1.0F) {
			this.flapSpeed = 1.0F;
		}

		if (!this.onGround && this.flapping < 1.0F) {
			this.flapping = 1.0F;
		}

		this.flap += this.flapping * 2.0F;
		if (!this.world.isClientSide && --this.eggTimer <= 0) {
			this.world.playSoundAtEntity((Entity)null, this, "mob.chickenplop", 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
			this.dropItem(Items.EGG_CHICKEN.id, 1);
			this.eggTimer = this.random.nextInt(6000) + 6000;
		}

	}
	public boolean isFavouriteItem(ItemStack itemStack) {
		return itemStack != null && itemStack.getItem() == Items.FOOD_FISH_RAW;
	}
//maybe add penguinfavoriteitem that checks if they're taggged as fish

}
