package peedog.funnyfauna.entity.cricket;

import com.mojang.nbt.tags.CompoundTag;
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

	private static final double SQUASH_DISTANCE = 0.4;

	private int hopCooldown = 0;
	private boolean airborne = false;
	private int animFrame = 0;
	private int variant;

	public EntityCricket(World world) {
		super(world);
		this.setSize(0.3F, 0.25F);
		this.variant = 1 + random.nextInt(4);
	}

	@Override
	protected void defineSynchedData() {}

	@Override
	public void tick() {
		super.tick();

		checkForSquash();

		// === Gravity ===
		if (!onGround) {
			yd -= GRAVITY;
			airborne = true;
		}

		move(xd, yd, zd);

		// === Ground collision ===
		int blockX = MathHelper.floor(x);
		int blockY = MathHelper.floor(y - 0.01);
		int blockZ = MathHelper.floor(z);

		onGround = world.isBlockNormalCube(blockX, blockY, blockZ);
		if (onGround && yd <= 0) {
			yd = 0;
			airborne = false;
		}

		// === Ground logic ===
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

	/* ===================== Squash Logic ===================== */
	private void checkForSquash() {
		Player player = world.getClosestPlayerToEntity(this, 0.8);
		if (player == null) return;

		if (this.distanceTo(player) < SQUASH_DISTANCE) {

			// Client: spawn particle
			if (world.isClientSide) {
				world.spawnParticle(
					"bug_squash",
					this.x,
					this.y + 0.01,
					this.z,
					0, 0, 0,
					0
				);
			}

			// Server: remove entity
			if (!world.isClientSide) {
				this.remove();
			}
		}
	}

	/* ===================== Smart Hop ===================== */
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

	public int getVariant() {
		return variant;
	}

	@Override
	public boolean isPickable() {
		return !this.removed;
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
				tag.putInt("Variant", this.variant);
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

	@Override
	protected boolean makeStepSound() {
		return false;
	}

	/* ===================== Save ===================== */
	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		hopCooldown = tag.getInteger("HopCooldown");
		if (tag.containsKey("Variant")) {
			variant = tag.getInteger("Variant");
		}
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		tag.putInt("HopCooldown", hopCooldown);
		tag.putInt("Variant", variant);
	}
}
