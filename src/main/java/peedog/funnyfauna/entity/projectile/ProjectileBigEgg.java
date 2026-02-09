package peedog.funnyfauna.entity.projectile;

import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.Mob;
import net.minecraft.core.entity.projectile.Projectile;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.util.helper.DamageType;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.util.helper.Side;
import net.minecraft.core.util.phys.HitResult;
import net.minecraft.core.world.World;
import peedog.funnyfauna.block.FunnyFaunaBlocks;
import peedog.funnyfauna.item.FunnyFaunaItems;

public class ProjectileBigEgg extends Projectile {

	public ProjectileBigEgg(World world) {
		super(world);
		this.damage = 3;
		this.setSize(0.5F, 0.5F);
	}

	public ProjectileBigEgg(World world, Mob owner) {
		super(world, owner);
		this.damage = 3;
		this.setSize(0.5F, 0.5F);
	}

	public ProjectileBigEgg(World world, double x, double y, double z) {
		super(world, x, y, z);
		this.damage = 2;
		this.setSize(0.5F, 0.5F);
	}

	@Override
	public void onHit(HitResult hit) {

		/* ===== ENTITY HIT ===== */
		if (hit.entity != null) {
			hit.entity.hurt(this.owner, this.damage, DamageType.COMBAT);
			this.remove();
			return;
		}

		if (this.world.isClientSide) return;

		// Block that was hit
		int bx = hit.x;
		int by = hit.y;
		int bz = hit.z;

		Block<?> hitBlock = Blocks.blocksList[this.world.getBlockId(bx, by, bz)];

		// 🚫 Non-solid blocks always drop item
		if (hitBlock == null || !hitBlock.isSolidRender()) {
			dropEggItem();
			this.remove();
			return;
		}

		// Only allow TOP face
		if (hit.side != Side.TOP) {
			dropEggItem();
			this.remove();
			return;
		}

		// Placement position = block above the hit block
		int px = bx;
		int py = by + 1;
		int pz = bz;

		Block<?> placeBlock = Blocks.blocksList[this.world.getBlockId(px, py, pz)];

		// Target space must be empty or non-solid
		if (placeBlock != null && placeBlock.isSolidRender()) {
			dropEggItem();
			this.remove();
			return;
		}

		// ✅ PLACE EGG BLOCK
		this.world.setBlockWithNotify(
			px,
			py,
			pz,
			FunnyFaunaBlocks.EGG_EMU_BLOCK.id()
		);

		this.remove();
	}

	private void dropEggItem() {
		this.dropItem(
			new ItemStack(FunnyFaunaItems.EGG_EMU),
			0.1F
		);
	}

}
