package peedog.funnyfauna.entity.boar;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.core.WeightedRandomLootObject;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.block.tag.BlockTags;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.EntityItem;
import net.minecraft.core.entity.animal.MobAnimal;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.enums.Difficulty;
import net.minecraft.core.item.Item;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.item.Items;
import net.minecraft.core.util.collection.NamespaceID;
import net.minecraft.core.util.helper.DamageType;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.util.phys.AABB;
import net.minecraft.core.world.World;
import peedog.funnyfauna.entity.MobTaskrunner;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.controllers.BoarTask;
import peedog.funnyfauna.entity.ai.controllers.BunnyTask;
import peedog.funnyfauna.entity.ai.interfaces.IAttacker;
import peedog.funnyfauna.entity.bunny.MobBunny;
import peedog.funnyfauna.item.FunnyFaunaItems;

import java.util.ArrayList;
import java.util.List;

public class MobBoar extends MobTaskrunner implements IAttacker {
	private boolean angry;
	private int angerCounter;
	public int attackAnimTime;
	private int attackCooldown = 0;

	public List<WeightedRandomLootObject> burningMobDrops = new ArrayList<>();

	public MobBoar(World world) {
		super(world);
		setSize(0.9F, 0.9F);
		this.textureIdentifier = NamespaceID.getPermanent("funnyfauna", "boar");
		mobDrops.add(new WeightedRandomLootObject(Items.FOOD_PORKCHOP_RAW.getDefaultStack(), 0, 2));
		mobDrops.add(new WeightedRandomLootObject(FunnyFaunaItems.COARSEHIDE.getDefaultStack(), 1, 3));
		burningMobDrops.add(new WeightedRandomLootObject(Items.FOOD_PORKCHOP_COOKED.getDefaultStack(), 1, 2));
		burningMobDrops.add(new WeightedRandomLootObject(FunnyFaunaItems.COARSEHIDE.getDefaultStack(), 1, 3));
	}

	@Override
	public Task<MobBoar> createTask() {
		return new BoarTask(this);
	}


	@Override
	public void tick() {
		super.tick();

		if (attackAnimTime > 0) attackAnimTime--;
		if (attackCooldown > 0) attackCooldown--;

		angry = angerCounter-- > 0 && world.getDifficulty() != Difficulty.PEACEFUL;
	}

	@Override
	public boolean hurt(Entity attacker, int damage, DamageType type) {

		boolean result = super.hurt(attacker, damage, type);

		if (!world.isClientSide && attacker != null && attacker.isAlive()) {

			this.setTarget(attacker);      // <-- ALWAYS set target
			this.angerCounter = 400;       // keep anger logic
		}

		return result;
	}

	@Override
	public boolean isAttacking() {
		return attackAnimTime > 0;
	}

	@Override
	public int getAttackCooldown() {
		return attackCooldown;
	}

	@Override
	public void setAttackCooldown(int ticks) {
		this.attackCooldown = ticks;
	}

	@Override
	public void startAttack(Entity target) {
		if (target == null || !target.isAlive()) return;

		// Deal damage only if cooldown is done
		if (attackCooldown <= 0) {
			target.hurt(this, 2, DamageType.COMBAT);
			attackCooldown = 20;
		}

		// Only start animation if not already playing
		if (attackAnimTime <= 0) {
			attackAnimTime = 10; // full animation duration
		}
	}






	@Override
	public float getAttackRange() {
		return 2.5F;
	}
//	@Override
//	protected Entity findPlayerToAttack() {
//		return angry ? world.getClosestPlayerToEntity(this, 16.0D) : null;
//	}
//
//
//	@Override
//	protected void attackEntity(Entity entity, float distance) {
//		if (!(entity instanceof EntityItem)) {
//			if (!(distance > 2.0F) || !(distance < 6.0F) || random.nextInt(10) != 0) {
//				if ((double)distance < 1.5 && entity.bb.maxY > bb.minY && entity.bb.minY < bb.maxY) {
//					attackTime = 20;
//					attackAnimTime = 10;
//					entity.hurt(this, 2, DamageType.COMBAT);
//				}
//			} else if (onGround) {
//				double d = entity.x - x;
//				double d1 = entity.z - z;
//				float f1 = MathHelper.sqrt(d * d + d1 * d1);
//				xd = d / (double)f1 * 0.5 * 0.8F + xd * 0.2F;
//				zd = d1 / (double)f1 * 0.5 * 0.8F + zd * 0.2F;
//				yd = 0.4F;
//			}
//		}
//	}

//	@Override
//	protected void updateAI() {
//		super.updateAI();
//		if (target == null && !hasPath() && world.getDifficulty() != Difficulty.PEACEFUL && world.rand.nextInt(200) == 0) {
//			List<Player> nearbyPlayers = world
//				.getEntitiesWithinAABB(
//					Player.class, AABB.getTemporaryBB(x, y, z, x + 1.0, y + 1.0, z + 1.0).expand(16.0, 4.0, 16.0)
//				);
//
//			for (Entity entity : nearbyPlayers) {
//				if (entity instanceof Player && ((Player) entity).gamemode.areMobsHostile())
//					setTarget(entity);
//			}
//		}
//	}

	@Override
	public void playLivingSound() {
		String s = getLivingSound();
		if (s != null && !world.isClientSide) {
			world.playSoundAtEntity(null, this, s, getSoundVolume(), (random.nextFloat() - random.nextFloat()) * 0.2F + 0.6F);
		}
	}

	@Override
	public void playHurtSound() {
		String s = getHurtSound();
		if (s != null && !world.isClientSide) {
			world.playSoundAtEntity(null, this, s, getSoundVolume(), (random.nextFloat() - random.nextFloat()) * 0.2F + 0.6F);
		}
	}

	@Override
	public void playDeathSound() {
		String s = getDeathSound();
		if (s != null && !world.isClientSide) {
			world.playSoundAtEntity(null, this, s, getSoundVolume(), (random.nextFloat() - random.nextFloat()) * 0.2F + 0.6F);
		}
	}

	public String getLivingSound() {
		return "mob.pig";
	}

	protected String getHurtSound() {
		return "mob.pig";
	}

	protected String getDeathSound() {
		return "mob.pigdeath";
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putBoolean("Angry", angry);
		tag.putInt("Anger", angerCounter);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		angry = tag.getBoolean("Angry");
		angerCounter = tag.getInteger("Anger");
	}

	@Override
	protected List<WeightedRandomLootObject> getMobDrops() {
		return remainingFireTicks > 0 ? burningMobDrops : mobDrops;
	}
	public boolean isFavouriteItem(ItemStack itemStack) {
		return itemStack != null && itemStack.itemID < Blocks.blocksList.length ? Blocks.blocksList[itemStack.itemID].hasTag(BlockTags.PIGS_FAVOURITE_BLOCK) : false;
	}
}
