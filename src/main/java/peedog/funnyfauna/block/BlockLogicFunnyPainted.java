// BlockLogicPainted.java
package peedog.funnyfauna.block;

import net.minecraft.core.block.Block;
import net.minecraft.core.block.BlockLogic;
import net.minecraft.core.block.IPainted;
import net.minecraft.core.block.entity.TileEntity;
import net.minecraft.core.block.material.Material;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.enums.EnumDropCause;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.util.helper.DyeColor;
import net.minecraft.core.util.helper.Side;
import net.minecraft.core.world.World;
import org.jetbrains.annotations.Nullable;
import java.util.function.Supplier;

public class BlockLogicFunnyPainted extends BlockLogic implements IPainted {
	private final Supplier<Block<?>> unpaintedVariant;

	public BlockLogicFunnyPainted(Block<?> block, Material material, Supplier<Block<?>> unpaintedVariant) {
		super(block, material);
		this.unpaintedVariant = unpaintedVariant;
	}

	@Override
	public int getPlacedBlockMetadata(@Nullable Player player, ItemStack stack, World world,
									  int x, int y, int z, Side side, double xp, double yp) {
		return stack.getMetadata();
	}

	@Override
	public ItemStack[] getBreakResult(World world, EnumDropCause cause, int meta, TileEntity te) {
		return new ItemStack[]{ new ItemStack(this, 1, meta) };
	}

	@Override public DyeColor fromMetadata(int meta) { return DyeColor.colorFromBlockMeta(meta & 15); }
	@Override public int toMetadata(DyeColor color)  { return color.blockMeta; }
	@Override public int stripColorFromMetadata(int meta) { return 0; }

	@Override
	public void removeDye(World world, int x, int y, int z) {
		world.setBlockWithNotify(x, y, z, unpaintedVariant.get().id());
	}
}
