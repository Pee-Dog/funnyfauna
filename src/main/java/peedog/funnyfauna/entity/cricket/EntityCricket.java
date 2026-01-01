package peedog.funnyfauna.entity.cricket;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.Item;
import net.minecraft.core.item.ItemBucketEmpty;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.item.Items;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.World;
import org.jetbrains.annotations.NotNull;
import peedog.funnyfauna.item.FunnyFaunaItems;

public class EntityCricket extends Entity {

	private static final double GRAVITY = 0.04;
	private static final double HOP_Y = 0.22;
	private static final double HOP_Y_BOOST = 0.4; // Higher hop if near block
	private static final double HOP_XZ = 0.12;

	private int hopCooldown = 0;
	private boolean airborne = false;
	private int animFrame = 0;
	private int variant;


	public EntityCricket(World world) {
		super(world);
		this.setSize(0.3F, 0.25F);
		this.variant = 1 + random.nextInt(4); // variants 1–4

	}

	@Override
	protected void defineSynchedData() {}

	@Override
	public void tick() {
		super.tick();

		// === Gravity ===
		if (!onGround) {
			yd -= GRAVITY;
			airborne = true;
		}

		// === Apply movement ===
		move(xd, yd, zd);

		// === Collision & onGround check ===
		int blockX = MathHelper.floor(x);
		int blockY = MathHelper.floor(y - 0.01);
		int blockZ = MathHelper.floor(z);

		onGround = world.isBlockNormalCube(blockX, blockY, blockZ);
		if (onGround && yd <= 0) {
			yd = 0;
			airborne = false;
		}

		// === Ground movement damping ===
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

		// === DEBUG ===
	}

	/* ===================== Smart Hop ===================== */
	private void doSmartHop() {
		// Check for neighboring blocks horizontally (X ±1, Z ±1)
		boolean nearBlock = false;
		int bx = MathHelper.floor(x);
		int by = MathHelper.floor(y);
		int bz = MathHelper.floor(z);

		for (int dx = -1; dx <= 1 && !nearBlock; dx++) {
			for (int dz = -1; dz <= 1 && !nearBlock; dz++) {
				if (dx == 0 && dz == 0) continue; // skip center
				if (world.isBlockNormalCube(bx + dx, by, bz + dz)) {
					nearBlock = true;
				}
			}
		}

		double hopY = nearBlock ? HOP_Y_BOOST : HOP_Y;

		// Random horizontal direction
		float angle = random.nextFloat() * (float)Math.PI * 2F;
		xd = MathHelper.cos(angle) * HOP_XZ;
		zd = MathHelper.sin(angle) * HOP_XZ;

		yd = hopY;

		yRot = (float) (Math.atan2(zd, xd) * 180.0 / Math.PI) - 90.0F;

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

	public boolean isPickable() {return !this.removed;}

	@Override
	public boolean interact(@NotNull Player player) {
		System.out.println("[FunnyFauna] Cricket interact called");
		ItemStack held = player.inventory.getCurrentItem();

		// Only capture with empty jar
		if (held != null && held.itemID == Items.JAR.id) {

			// Server-side only
			if (!player.world.isClientSide) {

				// Remove ONE empty jar from the current slot
				int slot = player.inventory.getCurrentItemIndex();
				player.inventory.removeItem(slot, 1);

				// Create cricket jar item
				ItemStack cricketJar = new ItemStack(FunnyFaunaItems.JAR_CRICKET);

				// Optional: store variant
				CompoundTag tag = new CompoundTag();
				tag.putInt("Variant", this.variant);
				cricketJar.setData(tag);

				// Try to insert into inventory
				player.inventory.insertItem(cricketJar, true);

				// If insertion failed (still has stack size), drop it
				if (cricketJar.stackSize > 0) {
					player.dropPlayerItemWithRandomChoice(cricketJar, false);
				}

				// Remove the cricket entity
				this.remove();
			}

			return true;
		}

		return super.interact(player);
	}

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
