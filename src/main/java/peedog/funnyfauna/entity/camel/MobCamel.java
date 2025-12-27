package peedog.funnyfauna.entity.camel;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.client.input.PlayerInput;
import net.minecraft.core.WeightedRandomLootObject;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.block.tag.BlockTags;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.animal.MobAnimal;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.Item;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.item.Items;
import net.minecraft.core.item.tag.ItemTags;
import net.minecraft.core.util.collection.NamespaceID;
import net.minecraft.core.util.helper.DamageType;
import net.minecraft.core.world.World;
import peedog.funnyfauna.entity.MobFunnyRideable;
import peedog.funnyfauna.item.FunnyFaunaItems;

import java.util.Objects;

public class MobCamel extends MobFunnyRideable {
	boolean isTamed;
	int annoyance = 0;
	int chanceForTame = 0;
	int tameCounter = 0;
	public boolean isSaddled;
	public String ownerName;
	public MobCamel(World world) {
		super(world);
		this.textureIdentifier = NamespaceID.getPermanent("funnyfauna", "camel");
		this.setSize(1F, 1.8F);
		this.rideFootSize = 1.5f;
		this.mobDrops.add(new WeightedRandomLootObject(FunnyFaunaItems.COARSEHIDE.getDefaultStack(), 2, 5));
	}
	@Override
	public int getMaxHealth() {
		return 20;
	}
	@Override
	public boolean interact(Player player) {
		ItemStack item = player.getHeldItem();
		if (item != null) {
			if (!isTamed) {
				if (item.itemID == Blocks.CACTUS.id()) {
					chanceForTame += random.nextInt(5) + 1;
					if (player.getGamemode().consumeBlocks()) {
						player.swingItem();
						player.getHeldItem().stackSize--;
					}

					//world.playSoundAtEntity(null, this, "creatures.eating", 1.0f, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F);
				}
			}

			if (isTamed) {
				if (item.itemID == Items.SADDLE.id) {
					isSaddled = true;
					if (player.getGamemode().consumeBlocks()) {
						player.swingItem();
						player.getHeldItem().stackSize--;
					}
				}

				if (getHealth() < getMaxHealth()) {
					if (item.itemID == Blocks.CACTUS.id()) {
						heal(4);
						if (player.getGamemode().consumeBlocks()) {
							player.swingItem();
							player.getHeldItem().stackSize--;
						}
						//world.playSoundAtEntity(null, this, "creatures.eating", 1.0f, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F);
					}
				}
			}
		} else {
			player.startRiding(this);
		}
		return super.interact(player);
	}
	public boolean getSaddled() {
		return this.isSaddled;
	}
	@Override
	public void updateAI() {
		super.updateAI();

		if (passenger != null && !isTamed) {
			Player player = (Player) passenger;

			if (random.nextInt(6) == 0) {
				annoyance += 20;
			}
			if (random.nextInt(10) == 0) {
				tameCounter += 20 * chanceForTame;
			}

			if (annoyance >= 300) {
				annoyance = 0;
				player.yd += 0.75F;
				player.xd -= yRot * 0.0015F;
				ejectRider();
				world.playSoundAtEntity(null,
					this,
					"funnyfauna:mob.horse.angry",
					getSoundVolume(),
					(random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F);
			}

			if (tameCounter++ >= 1000) {
				isTamed = true;
				ownerName = ((Player) passenger).username;

				for (int i = 0; i < 8; i++) {
					double randX = x + random.nextDouble();
					double randY = y + random.nextDouble();
					double randZ = z + random.nextDouble();

					world.spawnParticle("heart", randX, randY + 0.22, randZ, 0.0, 0.2, 0.0, 0);
				}
			}
		}
	}


	@Override
	public float getYRotDelta(){
		return 0;
	}

	@Override
	public float getXRotDelta(){
		return 0;
	}

	@Override
	protected boolean canDespawn() {
		return !isTamed || super.canDespawn();
	}

	@Override
	public String getLivingSound() {
		return "funnyfauna:mob.camel.idle";
	}

	@Override
	protected String getHurtSound() {
		return "funnyfauna:mob.camel.hurt";
	}

	@Override
	protected String getDeathSound() {
		return "funnyfauna:mob.camel.death";
	}

	@Override
	protected void dropDeathItems() {
		if (this.getSaddled()) {
			this.dropItem(Items.SADDLE.id, 1);
		}

		super.dropDeathItems();
	}
	@Override
	public double getRideHeight() {
		return (double)bbHeight * 1;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putBoolean("Tamed", isTamed);
		tag.putBoolean("Saddled", isSaddled);
		tag.putInt("ChanceForTame", chanceForTame);
		tag.putInt("Annoyance", annoyance);
		tag.putInt("TameCounter", tameCounter);

		if (isTamed) {
			tag.putString("Owner", ownerName);
		}
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		isTamed = tag.getBoolean("Tamed");
		isSaddled = tag.getBoolean("Saddled");
		chanceForTame = tag.getInteger("ChanceForTame");
		annoyance = tag.getInteger("Annoyance");
		tameCounter = tag.getInteger("TameCounter");

		if (isTamed) {
			ownerName = tag.getString("Owner");
		}
	}

	@Override
	public boolean hurt(Entity attacker, int damage, DamageType type) {
		if (!world.isClientSide) {
			if (attacker instanceof Player && !isTamed) {
				tameCounter -= 150;
			}
		}
		return super.hurt(attacker, damage, type);
	}

	@Override
	protected boolean canBeControlled() {
		return isSaddled;
	}

	public boolean isFavouriteItem(ItemStack itemStack) {
		return itemStack != null
			&& itemStack.itemID < Blocks.blocksList.length
			&& Blocks.blocksList[itemStack.itemID] == Blocks.CACTUS;
	}




}
