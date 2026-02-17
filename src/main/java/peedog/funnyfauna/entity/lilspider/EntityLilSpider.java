package peedog.funnyfauna.entity.lilspider;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.Mob;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.item.Items;
import net.minecraft.core.world.World;
import org.jetbrains.annotations.NotNull;
import peedog.funnyfauna.item.FunnyFaunaItems;

public class EntityLilSpider extends Mob {

	private int pauseTime = 0;
	private int directionTime = 0;
	private float climbFacing = Float.NaN;
	private int animFrame = 0;
	private int color;

	public EntityLilSpider(World world) {
		super(world);
		this.setSize(0.3F, 0.3F); // Slightly wider than a worm

		if (!world.isClientSide) {
			this.color = generateSpiderColor();
		}
	}

	@Override
	protected void defineSynchedData() {}

	/* ===================== Color (Spider Palette) ===================== */

	private int generateSpiderColor() {
		float hue, sat, val;
		int choice = random.nextInt(3);

		switch (choice) {
			case 0: // Dark Grey/Black
				hue = 0.0f;
				sat = 0.0f;
				val = 0.1f + random.nextFloat() * 0.2f;
				break;
			case 1: // Brownish
				hue = 0.08f;
				sat = 0.4f + random.nextFloat() * 0.2f;
				val = 0.2f + random.nextFloat() * 0.2f;
				break;
			default: // Dusky Blue/Grey
				hue = 0.6f;
				sat = 0.1f + random.nextFloat() * 0.1f;
				val = 0.2f + random.nextFloat() * 0.2f;
				break;
		}
		return java.awt.Color.HSBtoRGB(hue, sat, val) & 0xFFFFFF;
	}

	public int getColor() { return color; }

	/* ===================== AI & Movement ===================== */

	@Override
	public void tick() {
		super.tick();

		// Despawn logic
		if (!world.isClientSide) {
			Player nearest = world.getClosestPlayerToEntity(this, 40);
			if (nearest == null) { remove(); return; }
		}

		// Climbing logic
		boolean climbing = this.horizontalCollision && this.canClimb();
		if (climbing) {
			if (Float.isNaN(climbFacing)) {
				climbFacing = Math.round(this.yRot / 90F) * 90F;
			}
			this.yRot = climbFacing;
			if (this.yd > 0.07) this.yd = 0.07; // Spiders climb slightly faster than worms
		} else {
			climbFacing = Float.NaN;
		}

		updateAnimation();
	}

	@Override
	protected void updateAI() {
		handleSpiderAI();
	}

	private void handleSpiderAI() {
		if (pauseTime > 0) {
			pauseTime--;
			this.moveForward = 0F;
			return;
		}

		if (directionTime <= 0) {
			if (random.nextFloat() < 0.2F) { // Lower pause chance than worms
				pauseTime = 20 + random.nextInt(30);
				return;
			}
			randomYawVelocity = (random.nextFloat() - 0.5F) * 60F;
			directionTime = 30 + random.nextInt(40);
		}
		directionTime--;

		this.yRot += randomYawVelocity * 0.2F;
		this.moveForward = 0.25F; // Faster than worms
		randomYawVelocity *= 0.8F;
	}

	@Override
	public boolean canClimb() { return true; }

	/* ===================== Animation (2 Frames) ===================== */

	private void updateAnimation() {
		if (Math.abs(xd) > 0.005 || Math.abs(zd) > 0.005) {
			// Toggle between 0 and 1 every 4 ticks when moving
			animFrame = (tickCount / 4) % 2;
		} else {
			animFrame = 0; // Idle frame
		}
	}

	public int getAnimFrame() { return animFrame; }

	/* ===================== Squash & Interaction ===================== */

	@Override
	public void playerTouch(Player player) {
		if (!this.onGround || player.y <= this.y + 0.05) return;

		double dx = player.x - this.x;
		double dz = player.z - this.z;
		if (dx * dx + dz * dz > 0.4 * 0.4) return;

		world.spawnParticle("bug_squash", x, y + 0.01, z, 0, 0, 0, 0);
		if (!world.isClientSide) this.remove();
	}

//	@Override
//	public boolean interact(@NotNull Player player) {
//		ItemStack held = player.inventory.getCurrentItem();
//		if (held != null && held.itemID == Items.JAR.id) {
//			if (!player.world.isClientSide) {
//				// Assuming you have a JAR_SPIDER in your FunnyFaunaItems
//				ItemStack spiderJar = new ItemStack(FunnyFaunaItems.JAR_SPIDER);
//				CompoundTag tag = new CompoundTag();
//				tag.putInt("SpiderColor", color);
//				spiderJar.setData(tag);
//
//				player.inventory.removeItem(player.inventory.getCurrentItemIndex(), 1);
//				player.inventory.insertItem(spiderJar, true);
//				if (spiderJar.stackSize > 0) player.dropPlayerItemWithRandomChoice(spiderJar, false);
//				remove();
//			}
//			return true;
//		}
//		return false;
//	}

	@Override public void readAdditionalSaveData(CompoundTag tag) {
		if (tag.containsKey("SpiderColor")) color = tag.getInteger("SpiderColor");
	}
	@Override public void addAdditionalSaveData(CompoundTag tag) {
		tag.putInt("SpiderColor", color);
	}
}
