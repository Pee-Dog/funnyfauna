package peedog.funnyfauna.entity.worm;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.item.Items;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.World;
import org.jetbrains.annotations.NotNull;
import peedog.funnyfauna.item.FunnyFaunaItems;

public class EntityWorm extends Entity {

	/* ===================== Movement ===================== */
	private static final int DATA_COLOR = 16;
	private static final double CRAWL_SPEED = 0.02;
	private static final double GRAVITY = 0.04;

	private int pauseTime = 0;
	private int directionTime = 0;

	private int animTick = 0;   // counts game ticks
	private int animFrame = 0;  // 0, 1, 2 for 3 frames
	private static final int ANIM_SPEED = 6; // ticks per frame

	/** Worm color (0xRRGGBB) */
	private int color;

	public EntityWorm(World world) {
		super(world);
		this.setSize(0.25F, 0.25F);
		this.footSize = 1.0F; // snaps up blocks like horses

		// generate color ONCE on spawn
		if (!world.isClientSide) {
			this.color = generateWormColor();
		}
		System.out.println("Worm constructor called");

	}

	@Override
	protected void defineSynchedData() {}

	/* ===================== Color ===================== */

	private int generateWormColor() {
		float hue, sat, val;

		int choice = random.nextInt(4); // pick 0–3
		switch (choice) {
			case 0: // light pink
				hue = 0.95f + random.nextFloat() * 0.05f;
				sat = 0.3f + random.nextFloat() * 0.2f;
				val = 0.8f + random.nextFloat() * 0.2f;
				break;
			case 1: // dark pink
				hue = 0.95f + random.nextFloat() * 0.05f;
				sat = 0.4f + random.nextFloat() * 0.2f;
				val = 0.4f + random.nextFloat() * 0.3f;
				break;
			case 2: // neutral brown
				hue = 0.08f + random.nextFloat() * 0.03f;
				sat = 0.3f + random.nextFloat() * 0.3f;
				val = 0.5f + random.nextFloat() * 0.3f;
				break;
			default: // dark brown
				hue = 0.08f + random.nextFloat() * 0.03f;
				sat = 0.4f + random.nextFloat() * 0.3f;
				val = 0.25f + random.nextFloat() * 0.25f;
				break;
		}

		return java.awt.Color.HSBtoRGB(hue, sat, val) & 0xFFFFFF;
	}

	public int getColor() {
		return color;
	}

	public void setColor(int color) {
		this.color = color;
	}


	/* ===================== Tick ===================== */

	@Override
	public void tick() {
		super.tick();
		if (tickCount == 1) {
			System.out.println("Worm first tick");
		}

		// ===================== Distance Despawn =====================
		if (!world.isClientSide) {
			Player nearest = world.getClosestPlayerToEntity(this, 40);
			if (nearest == null) { remove(); return; }
		}


		animTick++;

		/* ===== Gravity ===== */
		if (!onGround) {
			yd -= GRAVITY;
		}

		move(xd, yd, zd);

		int bx = MathHelper.floor(x);
		int by = MathHelper.floor(y - 0.01);
		int bz = MathHelper.floor(z);

		onGround = world.isBlockNormalCube(bx, by, bz);
		if (onGround && yd <= 0) yd = 0;

		/* ===== Crawl Logic ===== */
		if (onGround) {
			if (pauseTime > 0) {
				pauseTime--;
				xd *= 0.6;
				zd *= 0.6;
			} else {
				crawlContinuously();
			}
		}

		updateAnimation();
	}

	private void crawlContinuously() {
		// Occasionally pause
		if (random.nextInt(200) == 0) {
			pauseTime = 10 + random.nextInt(30);
			return;
		}

		// Change direction every few seconds
		if (directionTime-- <= 0) {
			float angle = random.nextFloat() * (float)Math.PI * 2F;
			xd = MathHelper.cos(angle) * CRAWL_SPEED;
			zd = MathHelper.sin(angle) * CRAWL_SPEED;

			yRot = (float)(Math.atan2(zd, xd) * 180.0 / Math.PI) - 90.0F;
			directionTime = 40 + random.nextInt(80);
		}

		// Maintain crawl speed
		xd *= 0.98;
		zd *= 0.98;
	}

	/* ===================== Animation ===================== */

	private void updateAnimation() {
		if (Math.abs(xd) > 0.001 || Math.abs(zd) > 0.001) {
			// Worm is moving
			int phase = (tickCount / 5) % 4; // Change frame every 5 ticks
			switch (phase) {
				case 0: animFrame = 0; break; // worm_a
				case 1: animFrame = 2; break; // worm_c
				case 2: animFrame = 1; break; // worm_b
				case 3: animFrame = 2; break; // worm_c
			}
		} else {
			// Resting
			animFrame = 2; // worm_c
		}
	}

	public int getAnimFrame() {
		return animFrame;
	}

	/* ===================== Squash ===================== */

	@Override
	public void playerTouch(Player player) {
		if (!this.onGround) return;
		if (player.y <= this.y + 0.05) return;

		double dx = player.x - this.x;
		double dz = player.z - this.z;
		if (dx * dx + dz * dz > 0.3 * 0.3) return;

		world.spawnParticle(
			"bug_squash",
			x,
			y + 0.01,
			z,
			0.0,
			0.0,
			0.0,
			0
		);

		if (!world.isClientSide) {
			this.remove();
		}
	}

	/* ===================== Interaction ===================== */

	@Override
	public boolean interact(@NotNull Player player) {
		ItemStack held = player.inventory.getCurrentItem();

		if (held != null && held.itemID == Items.JAR.id) {
			if (!player.world.isClientSide) {

				int slot = player.inventory.getCurrentItemIndex();
				player.inventory.removeItem(slot, 1);

				ItemStack wormJar = new ItemStack(FunnyFaunaItems.JAR_WORM);

				CompoundTag tag = new CompoundTag();
				tag.putInt("WormColor", color);
				wormJar.setData(tag);

				player.inventory.insertItem(wormJar, true);
				if (wormJar.stackSize > 0) {
					player.dropPlayerItemWithRandomChoice(wormJar, false);
				}

				remove();
			}
			return true;
		}

		return false;
	}


	/* ===================== Required ===================== */

	@Override
	public boolean isPickable() {
		return true;
	}

	@Override
	protected boolean makeStepSound() {
		return false;
	}

	/* ===================== Save ===================== */

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		pauseTime = tag.getInteger("PauseTime");
		directionTime = tag.getInteger("DirectionTime");
		if (tag.containsKey("WormColor")) color = tag.getInteger("WormColor");
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		tag.putInt("PauseTime", pauseTime);
		tag.putInt("DirectionTime", directionTime);
		tag.putInt("WormColor", color);
	}

}
