package peedog.funnyfauna.entity.projectile;

import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.entity.Mob;
import net.minecraft.core.entity.projectile.Projectile;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.util.helper.DamageType;
import net.minecraft.core.util.helper.Side;
import net.minecraft.core.util.phys.HitResult;
import net.minecraft.core.world.World;
import peedog.funnyfauna.block.FunnyFaunaBlocks;
import peedog.funnyfauna.item.FunnyFaunaItems;

public class ProjectileGlowstick extends Projectile {

	/**
	 * The dye color metadata (0-15) inherited from the thrown item.
	 * Used to tint the entity texture, set the placed block's metadata,
	 * and ensure dropped items are the correct color.
	 * Defaults to 0 (white / the default color) if not set.
	 */
	public int colorMeta = 0;

	// ---- Constructors ----

	/** Thrown by a player or mob — color comes from the item stack, set after construction. */
	public ProjectileGlowstick(World world, Mob owner) {
		super(world, owner);
		this.damage = 1;
		this.setSize(0.2F, 0.2F);
	}

	/** Spawned at a fixed position (activator / dispenser). */
	public ProjectileGlowstick(World world, double x, double y, double z) {
		super(world, x, y, z);
		this.damage = 0;
		this.setSize(0.2F, 0.2F);
	}

	// ---- Tick ----

	@Override
	public void tick() {
		super.tick();

		// Ground-clip safety net: if the projectile ends up inside a solid block
		// between ticks (steep-angle wall bounce tunnelling into the floor), drop
		// the item and remove rather than leaving a ghost projectile.
		if (!this.world.isClientSide && !this.removed) {
			int cx = (int) Math.floor(this.x);
			int cy = (int) Math.floor(this.y);
			int cz = (int) Math.floor(this.z);
			Block<?> inside = Blocks.blocksList[this.world.getBlockId(cx, cy, cz)];
			if (inside != null && inside.isSolidRender()) {
				dropGlowstickItem();
				this.remove();
			}
		}
	}

	// ---- Collision ----

	@Override
	public void onHit(HitResult hit) {

		/* ===== ENTITY HIT ===== */
		if (hit.entity != null) {
			hit.entity.hurt(this.owner, this.damage, DamageType.COMBAT);
			return;
		}

		if (this.world.isClientSide) return;

		int bx = hit.x;
		int by = hit.y;
		int bz = hit.z;

		Block<?> hitBlock = Blocks.blocksList[this.world.getBlockId(bx, by, bz)];

		/* ===== REPLACEABLE BLOCK – PLACE INSIDE IT ===== */
		// If the block we hit is replaceable (tall grass, dead bush, etc.),
		// attempt to place the glowstick there (replacing that block).
		// Placement only succeeds if the block below is solid (can support the glowstick).
		if (isReplaceable(hitBlock)) {
			// Check if the block below (bx, by-1, bz) can support a block on top.
			if (this.world.canPlaceOnSurfaceOfBlock(bx, by - 1, bz)) {
				// Place the glowstick block at the replaceable block's coordinates.
				this.world.setBlockWithNotify(bx, by, bz, FunnyFaunaBlocks.GLOWSTICK.id());
				this.world.setBlockMetadataWithNotify(bx, by, bz, colorMeta);
				this.remove();
			}
			// If placement fails (no solid ground below), do nothing – the projectile
			// continues flying through the replaceable block as before.
			return; // return early to skip further collision handling
		}

		/* ===== WALL / CEILING – BOUNCE (with new ground‑placement shortcut) ===== */
		if (hit.side != Side.TOP) {
			// NEW: If this is a side hit (north/south/east/west) and there is solid
			// ground directly below the projectile's current position, place the
			// glowstick on that ground instead of bouncing.
			if (hit.side == Side.NORTH || hit.side == Side.SOUTH || hit.side == Side.EAST || hit.side == Side.WEST) {
				int px = (int) Math.floor(this.x);
				int py = (int) Math.floor(this.y);
				int pz = (int) Math.floor(this.z);

				Block<?> blockBelow = Blocks.blocksList[this.world.getBlockId(px, py - 1, pz)];
				Block<?> blockAtFeet = Blocks.blocksList[this.world.getBlockId(px, py, pz)];

				// Ground must be solid, and the spot where we want to place must be empty/replaceable.
				if (blockBelow != null && blockBelow.isSolidRender() &&
					(blockAtFeet == null || isReplaceable(blockAtFeet))) {
					// Also ensure the ground can actually support a block (e.g. fences are special‑cased).
					if (this.world.canPlaceOnSurfaceOfBlock(px, py - 1, pz)) {
						this.world.setBlockWithNotify(px, py, pz, FunnyFaunaBlocks.GLOWSTICK.id());
						this.world.setBlockMetadataWithNotify(px, py, pz, colorMeta);
						this.remove();
						return; // skip bounce
					}
				}
			}

			// Original bounce logic (unchanged) – only runs if the above placement didn't happen.
			if (hit.side == Side.NORTH || hit.side == Side.SOUTH) {
				this.zd *= -0.3;
				this.xd *=  0.3;
			} else if (hit.side == Side.EAST || hit.side == Side.WEST) {
				this.xd *= -0.3;
				this.zd *=  0.3;
			} else if (hit.side == Side.BOTTOM) {
				this.yd *= -0.3;
				this.yd -= 0.1;
			}
			// Push the projectile slightly off the face to avoid re‑collision.
			this.x += hit.side.getOffsetX() * 0.1;
			this.y += hit.side.getOffsetY() * 0.1;
			this.z += hit.side.getOffsetZ() * 0.1;
			return;
		}

		/* ===== FLOOR COLLISION – PLACEMENT (unchanged) ===== */
		// Re‑read hitBlock now that we know the side is TOP.
		hitBlock = Blocks.blocksList[this.world.getBlockId(bx, by, bz)];

		// The surface we landed on must be a real solid block.
		if (hitBlock == null || !hitBlock.isSolidRender()) {
			dropGlowstickItem();
			this.remove();
			return;
		}

		int px = bx;
		int py = by + 1;
		int pz = bz;

		Block<?> placeBlock = Blocks.blocksList[this.world.getBlockId(px, py, pz)];

		// The destination space must be empty or replaceable.
		if (placeBlock != null && !isReplaceable(placeBlock)) {
			dropGlowstickItem();
			this.remove();
			return;
		}

		boolean canStay = this.world.canPlaceOnSurfaceOfBlock(bx, by, bz)
			|| hitBlock.id() == Blocks.FENCE_PLANKS_OAK.id()
			|| hitBlock.id() == Blocks.FENCE_STEEL.id();

		if (!canStay) {
			dropGlowstickItem();
			this.remove();
			return;
		}

		this.world.setBlockWithNotify(px, py, pz, FunnyFaunaBlocks.GLOWSTICK.id());
		this.world.setBlockMetadataWithNotify(px, py, pz, colorMeta);
		this.remove();
	}

	// ---- Helpers ----

	/**
	 * Returns true for blocks that are passable / replaceable by placement
	 * (tall grass, dead bush, flowers, etc.) using the block's material flag.
	 * Air (null block) is also considered replaceable.
	 * Leaves, torches, and other non-replaceable non-solid blocks return false.
	 */
	private static boolean isReplaceable(Block<?> block) {
		if (block == null) return true; // air
		return block.getMaterial().isReplaceable();
	}

	/**
	 * Drop an item whose metadata matches colorMeta so the player picks up
	 * the same color they threw.
	 */
	private void dropGlowstickItem() {
		this.dropItem(
			new ItemStack(FunnyFaunaItems.GLOWSTICK, 1, colorMeta),
			0.1F
		);
	}
}
