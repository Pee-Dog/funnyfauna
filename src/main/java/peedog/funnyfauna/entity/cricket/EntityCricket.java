package peedog.funnyfauna.entity.cricket;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.item.Items;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.World;
import org.jetbrains.annotations.NotNull;
import peedog.funnyfauna.item.FunnyFaunaItems;

public class EntityCricket extends Entity {

	private static final double GRAVITY = 0.04;
	private static final double HOP_Y = 0.22;
	private static final double HOP_Y_BOOST = 0.4;
	private static final double HOP_XZ = 0.12;

	private int hopCooldown = 0;
	private boolean airborne = false;
	private int animFrame = 0;

	/** Stored RGB color (0xRRGGBB) */
	private int color;

	/** Ambient chirp timer */
	private int ambientSoundTime = 0;

	// Default constructor for natural spawn (random color)
	public EntityCricket(World world) {
		super(world);
		this.setSize(0.3F, 0.25F);
		if (!world.isClientSide) {
			this.color = generateCricketColor();
		}
	}

	// Constructor for releasing from jar with preset color
	public EntityCricket(World world, int color) {
		super(world);
		this.setSize(0.3F, 0.25F);
		this.color = color;
	}

	@Override
	protected void defineSynchedData() {}

	/* ===================== Color ===================== */
	private int generateCricketColor() {
		float hue, sat, val;
		if (random.nextBoolean()) {
			// Dark green
			hue = 0.18f + random.nextFloat() * 0.02f;
			sat = 0.5f + random.nextFloat() * 0.3f;
			val = 0.25f + random.nextFloat() * 0.25f;
		} else {
			// Brown → dark brown
			hue = 0.08f + random.nextFloat() * 0.03f;
			sat = 0.4f + random.nextFloat() * 0.3f;
			val = 0.25f + random.nextFloat() * 0.3f;
		}
		return java.awt.Color.HSBtoRGB(hue, sat, val) & 0xFFFFFF;
	}

	public int getColor() { return color; }
	public void setColor(int color) { this.color = color; }

	/* ===================== Tick ===================== */
	@Override
	public void tick() {
		super.tick();

		if (!world.isClientSide) {
			Player nearest = world.getClosestPlayerToEntity(this, 40);
			if (nearest == null) { remove(); return; }
		}

		if (this.isInWaterOrRain() || world.getBlock(blockX(), blockY(), blockZ()) == Blocks.FLUID_LAVA_STILL || world.getBlock(blockX(), blockY(), blockZ()) == Blocks.FLUID_LAVA_FLOWING) {
			this.remove();
			return;
		}

		if (!onGround) { yd -= GRAVITY; airborne = true; }

		move(xd, yd, zd);

		int blockX = MathHelper.floor(x);
		int blockY = MathHelper.floor(y - 0.01);
		int blockZ = MathHelper.floor(z);

		onGround = world.isBlockNormalCube(blockX, blockY, blockZ);
		if (onGround && yd <= 0) { yd = 0; airborne = false; }

		if (onGround) {
			xd *= 0.7; zd *= 0.7;
			if (hopCooldown > 0) hopCooldown--;
			else doSmartHop();
		}

		updateAnimation();

		if (!world.isClientSide) {
			if (!world.isDaytime()) {
				if (random.nextInt(1200) < ambientSoundTime++) {
					ambientSoundTime = -200;
					playCricketSound();
				}
			} else ambientSoundTime = 0;
		}
	}

	private int blockX() { return MathHelper.floor(x); }
	private int blockY() { return MathHelper.floor(y); }
	private int blockZ() { return MathHelper.floor(z); }

	private void playCricketSound() {
		world.playSoundAtEntity(null, this, "funnyfauna:mob.cricket.ambient", 0.6F, 0.9F + random.nextFloat() * 0.2F);
	}

	@Override
	public void playerTouch(Player player) {
		if (!onGround) return;
		if (player.y <= y + 0.05) return;

		double dx = player.x - x;
		double dz = player.z - z;
		if (dx * dx + dz * dz > 0.25 * 0.25) return;

		world.spawnParticle("bug_squash", x, y + 0.01, z, 0.0, 0.2, 0.0, 0);

		if (!world.isClientSide) remove();
	}

	private void doSmartHop() {
		boolean nearBlock = false;
		int bx = MathHelper.floor(x);
		int by = MathHelper.floor(y);
		int bz = MathHelper.floor(z);

		for (int dx = -1; dx <= 1 && !nearBlock; dx++) {
			for (int dz = -1; dz <= 1 && !nearBlock; dz++) {
				if (dx == 0 && dz == 0) continue;
				if (world.isBlockNormalCube(bx + dx, by, bz + dz)) nearBlock = true;
			}
		}

		double hopY = nearBlock ? HOP_Y_BOOST : HOP_Y;
		float angle = random.nextFloat() * (float)Math.PI * 2F;
		xd = MathHelper.cos(angle) * HOP_XZ;
		zd = MathHelper.sin(angle) * HOP_XZ;
		yd = hopY;

		yRot = (float)(Math.atan2(zd, xd) * 180.0 / Math.PI) - 90.0F;
		hopCooldown = 15 + random.nextInt(30);
	}

	private void updateAnimation() { animFrame = airborne ? 1 : 0; }
	public int getAnimFrame() { return animFrame; }

	@Override
	public boolean interact(@NotNull Player player) {
		ItemStack held = player.inventory.getCurrentItem();
		if (held != null && held.itemID == Items.JAR.id) {
			if (!player.world.isClientSide) {
				int slot = player.inventory.getCurrentItemIndex();
				player.inventory.removeItem(slot, 1);

				ItemStack cricketJar = new ItemStack(FunnyFaunaItems.JAR_CRICKET);
				CompoundTag tag = new CompoundTag();
				tag.putInt("CricketColor", color);
				cricketJar.setData(tag);

				player.inventory.insertItem(cricketJar, true);
				if (cricketJar.stackSize > 0) player.dropPlayerItemWithRandomChoice(cricketJar, false);

				remove();
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean isPickable() { return true; }

	@Override
	protected boolean makeStepSound() { return false; }

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		hopCooldown = tag.getInteger("HopCooldown");
		if (tag.containsKey("CricketColor")) color = tag.getInteger("CricketColor");
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		tag.putInt("HopCooldown", hopCooldown);
		tag.putInt("CricketColor", color);
	}
}
