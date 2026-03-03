package peedog.funnyfauna.block;

import java.util.Random;
import net.minecraft.core.block.Block;
import net.minecraft.core.block.BlockLogic;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.block.IPainted;
import net.minecraft.core.block.entity.TileEntity;
import net.minecraft.core.block.material.Material;
import net.minecraft.core.entity.Mob;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.enums.EnumDropCause;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.item.Items;
import net.minecraft.core.util.helper.DyeColor;
import net.minecraft.core.util.helper.Side;
import net.minecraft.core.util.phys.AABB;
import net.minecraft.core.world.World;
import net.minecraft.core.world.WorldSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import peedog.funnyfauna.item.FunnyFaunaItems;

public class BlockLogicGlowstick extends BlockLogic implements IPainted {

	public BlockLogicGlowstick(Block<?> block) {
		super(block, Material.decoration);
		block.setTicking(true);
	}

	// --- Color / IPainted ---

	/**
	 * Block metadata IS the dye color (0-15), matching the item stack metadata.
	 * Orientation is always "floor" and is no longer stored in metadata.
	 */
	@Override
	public int getPlacedBlockMetadata(@Nullable Player player, ItemStack stack, World world,
									  int x, int y, int z, Side side, double xPlaced, double yPlaced) {
		// Item metadata carries the color; pass it straight through to the block.
		return stack.getMetadata();
	}

	@Override
	public ItemStack[] getBreakResult(World world, EnumDropCause dropCause, int meta, TileEntity tileEntity) {
		return new ItemStack[]{ new ItemStack(FunnyFaunaItems.GLOWSTICK, 1, meta & 15) };
	}

	@Override
	public DyeColor fromMetadata(int meta) {
		return DyeColor.colorFromBlockMeta(meta & 15);
	}

	@Override
	public int toMetadata(DyeColor color) {
		return color.blockMeta;
	}

	@Override
	public int stripColorFromMetadata(int meta) {
		// No orientation bits to preserve; stripping color resets to 0 (default color).
		return 0;
	}

	@Override
	public void removeDye(World world, int x, int y, int z) {
		// Replace with the un-dyed glowstick variant (metadata 0).
		world.setBlockMetadataWithNotify(x, y, z, 0);
	}

	// --- Placement / Collision ---

	@Override
	public AABB getCollisionBoundingBoxFromPool(WorldSource world, int x, int y, int z) {
		return null; // Walk-through, like a torch.
	}

	@Override
	public AABB getBlockBoundsFromState(WorldSource world, int x, int y, int z) {
		// Hardcoded to the "Floor" torch bounds.
		return AABB.getTemporaryBB(
			(double) 0.4F,
			(double) 0.0F,
			(double) 0.4F,
			(double) 0.6F,
			(double) 0.6F,
			(double) 0.6F
		);
	}

	@Override
	public boolean isSolidRender() {
		return false;
	}

	@Override
	public boolean isCubeShaped() {
		return false;
	}

	private boolean canStayOnFloor(World world, int x, int y, int z) {
		int blockBelow = world.getBlockId(x, y, z);
		return world.canPlaceOnSurfaceOfBlock(x, y, z)
			|| blockBelow == Blocks.FENCE_PLANKS_OAK.id()
			|| blockBelow == Blocks.FENCE_PLANKS_OAK_PAINTED.id()
			|| blockBelow == Blocks.FENCE_CHAINLINK.id()
			|| blockBelow == Blocks.FENCE_PAPER_WALL.id()
			|| blockBelow == Blocks.FENCE_STEEL.id();
	}

	@Override
	public boolean canPlaceBlockAt(World world, int x, int y, int z) {
		return this.canStayOnFloor(world, x, y - 1, z);
	}

	@Override
	public void onBlockPlacedByMob(World world, int x, int y, int z, @NotNull Side side,
								   Mob mob, double xPlaced, double yPlaced) {
		// Validity check only — color metadata was already written by getPlacedBlockMetadata.
		if (!this.canStayOnFloor(world, x, y - 1, z)) {
			this.dropBlockWithCause(world, EnumDropCause.WORLD, x, y, z,
				world.getBlockMetadata(x, y, z), null, null);
			world.setBlockWithNotify(x, y, z, 0);
		}
	}

	@Override
	public void onBlockPlacedOnSide(World world, int x, int y, int z, @NotNull Side side,
									double xPlaced, double yPlaced) {
		// Identical validity check.
		if (!this.canStayOnFloor(world, x, y - 1, z)) {
			this.dropBlockWithCause(world, EnumDropCause.WORLD, x, y, z,
				world.getBlockMetadata(x, y, z), null, null);
			world.setBlockWithNotify(x, y, z, 0);
		}
	}

	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, int blockId) {
		if (!this.canStayOnFloor(world, x, y - 1, z)) {
			this.dropBlockWithCause(world, EnumDropCause.WORLD, x, y, z,
				world.getBlockMetadata(x, y, z), (TileEntity) null, (Player) null);
			world.setBlockWithNotify(x, y, z, 0);
		}
	}

	// --- Visuals / Ticking ---

	@Override
	public void animationTick(World world, int x, int y, int z, Random rand) {
		double xPos = (double) x + 0.3 + (rand.nextFloat() * (0.7 - 0.3));
		double yPos = (double) y + (rand.nextFloat() * 0.7);
		double zPos = (double) z + 0.3 + (rand.nextFloat() * (0.7 - 0.3));

		world.spawnParticle("glow", xPos, yPos, zPos, 0.0, 0.0, 0.0,
			world.getBlockMetadata(x, y, z) & 15);
	}

	@Override
	public int getLightmapCoord(WorldSource blockAccess, int x, int y, int z) {
		return blockAccess.getLightmapCoord(x, y, z, this.block.emission > 0 ? 15 : 0);
	}
}
