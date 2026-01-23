package peedog.funnyfauna.entity.lizard;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.core.WeightedRandomLootObject;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.animal.MobAnimal;
import net.minecraft.core.entity.animal.MobSheep;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.item.Items;
import net.minecraft.core.util.collection.NamespaceID;
import net.minecraft.core.util.helper.DamageType;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.util.phys.AABB;
import net.minecraft.core.util.phys.Vec3;
import net.minecraft.core.world.World;
import net.minecraft.core.world.pathfinder.Path;
import org.jetbrains.annotations.NotNull;
import peedog.funnyfauna.entity.cricket.EntityCricket;
import peedog.funnyfauna.item.FunnyFaunaItems;

import java.util.List;

public class MobLizard extends MobAnimal {
	private int tickTargetCooldown = 0; // prevents switching crickets too often
	private static final double EAT_DISTANCE = 1.5; // distance at which cricket is eaten
	private static final int TARGET_COOLDOWN = 20; // ticks to wait before picking new target

	public MobLizard(World world) {
		super(world);
		this.textureIdentifier = NamespaceID.getPermanent("funnyfauna", "lizard");
		this.setSize(1F, 0.5F);
		this.mobDrops.add(new WeightedRandomLootObject(FunnyFaunaItems.SCALES.getDefaultStack(), 2, 4));
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
	protected void updateAI() {
		super.updateAI();

		// Reset target if current target is invalid
		if (this.getTarget() != null) {
			Entity target = this.getTarget();
			if (target.isRemoved() || !target.onGround || target.isInWater() || target.isInLava()) {
				this.setTarget(null);
			}
		}

		// If no target, find nearby crickets
		if (this.getTarget() == null) {
			List<EntityCricket> nearbyCrickets = this.world.getEntitiesWithinAABB(
				EntityCricket.class,
				AABB.getTemporaryBB(this.x, this.y, this.z,
						this.x + 1.0, this.y + 1.0, this.z + 1.0)
					.grow(12.0, 4.0, 12.0) // 12 block radius
			);

			if (!nearbyCrickets.isEmpty()) {
				// Pick a random cricket to chase
				EntityCricket cricket = nearbyCrickets.get(this.random.nextInt(nearbyCrickets.size()));
				this.setTarget(cricket);
			}
		}

		// Chase target if exists
		if (this.getTarget() != null && !this.hasPath()) {
			Entity target = this.getTarget();
			float distance = target.distanceTo(this);

			// Teleport closer if too far (like wolf)
			if (distance > 12.0F) {
				int tx = MathHelper.floor(target.x);
				int ty = MathHelper.floor(target.bb.minY);
				int tz = MathHelper.floor(target.z);

				for (int dx = -2; dx <= 2; dx++) {
					for (int dz = -2; dz <= 2; dz++) {
						if ((Math.abs(dx) > 1 || Math.abs(dz) > 1)
							&& this.world.isBlockNormalCube(tx + dx, ty - 1, tz + dz)
							&& !this.world.isBlockNormalCube(tx + dx, ty, tz + dz)
							&& !this.world.isBlockNormalCube(tx + dx, ty + 1, tz + dz)) {

							this.moveTo((tx + dx) + 0.5, ty, (tz + dz) + 0.5, this.yRot, this.xRot);
							this.fallDistance = 0;
							return;
						}
					}
				}
			} else {
				// Otherwise, pathfind normally
				Path path = this.world.getPathToEntity(this, target, 16.0F);
				this.setPathToEntity(path);
			}

			// Direct movement if very close to cricket to avoid stopping early
			if (target instanceof EntityCricket && distance < 3.0F) {
				double dx = (target.x + target.bbWidth / 2) - this.x;
				double dz = (target.z + target.bbWidth / 2) - this.z;
				double dy = (target.y + target.bbHeight / 2) - this.y;

				double total = MathHelper.sqrt(dx * dx + dz * dz);
				if (total > 0.0) { // avoid divide by zero
					this.xd = dx / total * 0.15 + this.xd * 0.2;
					this.zd = dz / total * 0.15 + this.zd * 0.2;
					this.yd = dy * 0.1; // optional: small vertical adjustment
				}
			}
		}

		// Attack/eat cricket if close
		if (this.getTarget() instanceof EntityCricket) {
			EntityCricket cricket = (EntityCricket) this.getTarget();
			float distance = cricket.distanceTo(this);

			if (distance < 2F && cricket.bb.maxY > this.bb.minY && cricket.bb.minY < this.bb.maxY) {
				this.attackTime = 20;
				cricket.remove(); // Eat the cricket
				this.setTarget(null); // Stop chasing
				this.heal(2); // Optional: regain health when eating
			}
		}
	}


	@Override
	public void tick() {
		super.tick();

		// If we have a target and it's a cricket
		Entity target = this.getTarget();
		if (target instanceof EntityCricket) {
			double dx = target.x - this.x;
			double dy = target.y - this.y;
			double dz = target.z - this.z;
			double distance = Math.sqrt(dx*dx + dy*dy + dz*dz);

			// If close enough, "eat" the cricket
			if (distance < 1.5) { // tweak this radius as needed
				target.outOfWorld(); // removes the cricket from the world
				this.setTarget(null); // clear target so lizard can search for next cricket
			}
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

	public boolean isFavouriteItem(ItemStack itemStack) {
		return itemStack != null && itemStack.getItem() == FunnyFaunaItems.JAR_CRICKET;
	}




}
