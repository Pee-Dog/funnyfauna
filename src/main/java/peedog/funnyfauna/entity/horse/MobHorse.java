package peedog.funnyfauna.entity.horse;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.client.input.PlayerInput;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.block.tag.BlockTags;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.animal.MobAnimal;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.Item;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.item.Items;
import net.minecraft.core.util.collection.NamespaceID;
import net.minecraft.core.util.helper.DamageType;
import net.minecraft.core.world.World;

import java.util.Objects;

public class MobHorse extends MobAnimal {
	boolean isTamed;
	int annoyance = 0;
	int chanceForTame = 0;
	int tameCounter = 0;
	public boolean isSaddled;
	public String ownerName;
	public MobHorse(World world) {
		super(world);
		this.textureIdentifier = NamespaceID.getPermanent("funnyfauna", "horse");
		this.setSize(1F, 1.8F);
	}
	@Override
	public int getMaxHealth() {
		return 20;
	}
	@Override
	public boolean interact(Player player) {
		super.interact(player);
		ItemStack item = player.inventory.getCurrentItem();
		if (item != null) {
			if (!isTamed) {
				if (item.itemID == Items.WHEAT.id) {
					chanceForTame += 1;
					item.consumeItem(player);
					//world.playSoundAtEntity(null, this, "creatures.eating", 1.0f, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F);
				}
				if (item.itemID == Items.FOOD_APPLE.id) {
					chanceForTame += random.nextInt(4) + 1;
					item.consumeItem(player);
					//world.playSoundAtEntity(null, this, "creatures.eating", 1.0f, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F);

				}
				if (item.itemID == Items.DUST_SUGAR.id) {
					chanceForTame += random.nextInt(8) + 1;
					item.consumeItem(player);
					//world.playSoundAtEntity(null, this, "creatures.eating", 1.0f, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F);
				}
			}

			if (isTamed && Objects.equals(player.username, ownerName)) {
				if (item.itemID == Items.SADDLE.id) {
					isSaddled = true;
					item.consumeItem(player);
				}

				if (getHealth() < getMaxHealth()) {
					if (item.itemID == Items.WHEAT.id) {
						heal(2);
						item.consumeItem(player);
						//world.playSoundAtEntity(null, this, "creatures.eating", 1.0f, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F);
					}
					if (item.itemID == Items.FOOD_APPLE.id) {
						heal(4);
						item.consumeItem(player);
						//world.playSoundAtEntity(null, this, "creatures.eating", 1.0f, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F);
					}
				}
			}
		} else {
			player.startRiding(this);
		}
		return false;
	}
	public boolean getSaddled() {
		return this.isSaddled;
	}
	@Override
	protected void updateAI() {
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
					"creatures.horsemad",
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
	public void moveEntityWithHeading(float moveStrafing, float moveForward) {
		if (passenger != null) {

			if (isSaddled) {
				// Match rider rotation
				yRot = passenger.yRot;
				yRotO = yRot;

				// Override AI movement with rider input
				this.moveStrafing = moveStrafing;
				this.moveForward = moveForward;

				// Jump inference (beta-style)
				if (onGround && passenger.yd > 0.0F && !noPhysics) {
					yd = 0.42F;
				}

				if (!onGround) {
					super.moveRelative(this.moveStrafing, this.moveForward, moveSpeed / 16.0F);
				} else {
					super.moveRelative(this.moveStrafing, this.moveForward, moveSpeed / 6.0F);
				}

				super.moveEntityWithHeading(this.moveStrafing, this.moveForward);
				return;
			}
		}

		super.moveEntityWithHeading(moveStrafing, moveForward);
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
		return "creatures.horsegrunt";
	}

	@Override
	protected String getHurtSound() {
		return "creatures.horsehurt";
	}

	@Override
	protected String getDeathSound() {
		return "creatures.horsedying";
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
		return (double)bbHeight * 0.7;
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


}
