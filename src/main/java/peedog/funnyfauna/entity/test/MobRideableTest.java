package peedog.funnyfauna.entity.test;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.core.WeightedRandomLootObject;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.block.tag.BlockTags;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.item.Items;
import net.minecraft.core.util.collection.NamespaceID;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.World;
import org.jspecify.annotations.NonNull;
import peedog.funnyfauna.entity.MobFunnyRideable;

import java.util.ArrayList;
import java.util.List;

public class MobRideableTest extends MobFunnyRideable {
	public MobRideableTest(World world) {
		super(world);
		this.textureIdentifier = NamespaceID.getPermanent("funnyfauna", "horse");
		this.setSize(0.9F, 0.9F);
		this.rideFootSize = 1.0f;

		this.mobDrops.add(new WeightedRandomLootObject(Items.FOOD_PORKCHOP_RAW.getDefaultStack(), 1, 2));
		this.mobDrops.add(new WeightedRandomLootObject(Items.FEATHER_CHICKEN.getDefaultStack(), 0, 2));
	}

	@Override
	public void tick() {
		super.tick();
	}

	@Override
	public void jump() {
		this.yd = 0.6;
	}

	@Override
	public void defineSynchedData() {
		this.entityData.define(16, (byte) 0, Byte.class);
	}

	@Override
	public void addAdditionalSaveData(@NonNull CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putBoolean("Saddle", this.getSaddled());
	}

	@Override
	public void readAdditionalSaveData(@NonNull CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		this.setSaddled(tag.getBoolean("Saddle"));
	}

	@Override
	public String getLivingSound() {
		return "mob.pig";
	}

	@Override
	public String getHurtSound() {
		return "mob.pig";
	}

	@Override
	public String getDeathSound() {
		return "mob.pigdeath";
	}

	@Override
	public boolean interact(@NonNull Player player) {
		if (super.interact(player)) return true;

		if (this.world == null || this.world.isClientSide) return false;
		if (this.passenger != null && this.passenger != player) return false;

		player.startRiding(this);
		return true;
	}

	@Override
	public void dropDeathItems() {
		if (this.getSaddled()) {
			this.dropItem(Items.SADDLE.id, 1);
		}

		super.dropDeathItems();
	}

	public boolean getSaddled() {
		return (this.entityData.getByte(16) & 1) != 0;
	}

	public void setSaddled(boolean flag) {
		if (flag) this.entityData.set(16, (byte) 1);
		else this.entityData.set(16, (byte) 0);
	}

}
