package peedog.funnyfauna.block;

import net.minecraft.core.block.Block;
import net.minecraft.core.block.entity.TileEntity;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.util.helper.Side;
import net.minecraft.core.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import peedog.funnyfauna.entity.cricket.EntityCricket;
import peedog.funnyfauna.block.entity.TileEntityJarCricket;

import java.util.Random;
import java.util.function.Supplier;

public class BlockLogicJarCricket extends BlockLogicJarAnimal {

	public BlockLogicJarCricket(Block<?> block, Supplier<net.minecraft.core.item.Item> itemSupplier) {
		super(block, itemSupplier);
	}

	/**
	 * Called when the block is placed. Copy the color from the ItemStack into the TE.
	 */
	@Override
	public int getPlacedBlockMetadata(@Nullable Player player, ItemStack stack, World world, int x, int y, int z, Side side, double xPlaced, double yPlaced) {
		TileEntity te = world.getTileEntity(x, y, z);
		if (te instanceof TileEntityJarCricket && stack != null) {
			((TileEntityJarCricket) te).readFromItemStack(stack);
			te.setChanged(); // ensure the TE saves its NBT
		}
		return 0;
	}

	/**
	 * Spawn colored cricket particles from the jar.
	 */
	@Override
	public void animationTick(World world, int x, int y, int z, Random rand) {
		if (rand.nextInt(3) != 0) return;
		int color = 0xFFFFFF;
		TileEntity te = world.getTileEntity(x, y, z);
		if (te instanceof TileEntityJarCricket) {
			color = ((TileEntityJarCricket) te).getCricketColor();
		}
		world.spawnParticle("cricket", x + 0.5, y + 0.15, z + 0.5, 0, 0, 0, color);
	}

	/**
	 * Release a cricket entity from the jar with its correct color.
	 */
	@Override
	protected EntityCricket createReleasedEntity(World world, int x, int y, int z, TileEntity te) {
		if (te instanceof TileEntityJarCricket) {
			TileEntityJarCricket jarTe = (TileEntityJarCricket) te;
			EntityCricket cricket = new EntityCricket(world, jarTe.getCricketColor());
			cricket.setPos(x + 0.5, y + 0.5, z + 0.5);
			return cricket;
		} else {
			return new EntityCricket(world, 0xFFFFFF); // fallback white
		}
	}


}
