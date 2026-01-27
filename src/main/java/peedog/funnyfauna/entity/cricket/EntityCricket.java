package peedog.funnyfauna.entity.cricket;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.client.entity.particle.ParticleDispatcher;
import net.minecraft.client.entity.particle.ParticleLambda;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.item.Items;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.World;
import org.jetbrains.annotations.NotNull;
import peedog.funnyfauna.FunnyFaunaClient;
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

	public EntityCricket(World world) {
		super(world);
		this.setSize(0.3F, 0.25F);

		// Generate color ONCE on server
		if (!world.isClientSide) {
			this.color = generateCricketColor();
		}
	}

	@Override
	protected void defineSynchedData() {}

	/* ===================== Color ===================== */

	private int generateCricketColor() {
		float hue = 0.22F + random.nextFloat() * 0.1F;
		float sat = 0.4F + random.nextFloat() * 0.4F;
		float val = 0.4F + random.nextFloat() * 0.4F;

		return java.awt.Color.HSBtoRGB(hue, sat, val) & 0xFFFFFF;
	}

	public int getColor() {
		return color;
	}

	/* ===================== Tick ===================== */

	@Override
	public void tick() {
		super.tick();

		// ===================== Physics =====================
		if (!onGround) {
			yd -= GRAVITY;
			airborne = true;
		}

		move(xd, yd, zd);

		int blockX = MathHelper.floor(x);
		int blockY = MathHelper.floor(y - 0.01);
		int blockZ = MathHelper.floor(z);

		onGround = world.isBlockNormalCube(blockX, blockY, blockZ);
		if (onGround && yd <= 0) {
			yd = 0;
			airborne = false;
		}

		if (onGround) {
			xd *= 0.7;
			zd *= 0.7;

			if (hopCooldown > 0) {
				hopCooldown--;
			} else {
				doSmartHop();
			}
		}

		updateAnimation();




	}


	/* ===================== Squash (FIXED) ===================== */

	@Override
	public void playerTouch(Player player) {
		if (!this.onGround) return;
		if (player.y <= this.y + 0.05) return;

		double dx = player.x - this.x;
		double dz = player.z - this.z;
		if (dx * dx + dz * dz > 0.25 * 0.25) return;

		System.out.println("Cricket stepped on! Player at " + player.x + "," + player.y + "," + player.z);

		world.spawnParticle("bug_squash", x, y + 0.01, z, 0.0, 0.2, 0.0, 0);


		if (!world.isClientSide) {
			System.out.println("Removing cricket on server");
			this.remove();
		}
	}












	/* ===================== Hop ===================== */

	private void doSmartHop() {
		boolean nearBlock = false;
		int bx = MathHelper.floor(x);
		int by = MathHelper.floor(y);
		int bz = MathHelper.floor(z);

		for (int dx = -1; dx <= 1 && !nearBlock; dx++) {
			for (int dz = -1; dz <= 1 && !nearBlock; dz++) {
				if (dx == 0 && dz == 0) continue;
				if (world.isBlockNormalCube(bx + dx, by, bz + dz)) {
					nearBlock = true;
				}
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

	/* ===================== Animation ===================== */

	private void updateAnimation() {
		animFrame = airborne ? 1 : 0;
	}

	public int getAnimFrame() {
		return animFrame;
	}

	/* ===================== Interaction ===================== */

	@Override
	public boolean interact(@NotNull Player player) {
		ItemStack held = player.inventory.getCurrentItem();

		if (held != null && held.itemID == Items.JAR.id) {
			if (!player.world.isClientSide) {
				int slot = player.inventory.getCurrentItemIndex();
				player.inventory.removeItem(slot, 1);

				ItemStack cricketJar = new ItemStack(FunnyFaunaItems.JAR_CRICKET);
				CompoundTag tag = new CompoundTag();
				tag.putInt("CricketColor", this.color);
				cricketJar.setData(tag);

				player.inventory.insertItem(cricketJar, true);
				if (cricketJar.stackSize > 0) {
					player.dropPlayerItemWithRandomChoice(cricketJar, false);
				}

				this.remove();
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
		hopCooldown = tag.getInteger("HopCooldown");

		if (tag.containsKey("CricketColor")) {
			color = tag.getInteger("CricketColor");
		}
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		tag.putInt("HopCooldown", hopCooldown);
		tag.putInt("CricketColor", color);
	}
}
