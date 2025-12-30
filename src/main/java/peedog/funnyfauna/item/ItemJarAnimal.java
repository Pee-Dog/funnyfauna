package peedog.funnyfauna.item;

import net.minecraft.core.block.Block;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemPlaceable;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.util.helper.Side;
import net.minecraft.core.world.World;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Generic jar item that can place a block and optionally release an entity when shift-right-clicked.
 */
public class ItemJarAnimal extends ItemPlaceable {

	private final Supplier<Block<?>> placedBlock;
	private final BiFunction<World, Player, Entity> entityFactory;

	public ItemJarAnimal(
		String name,
		String namespaceId,
		int id,
		Supplier<Block<?>> placedBlock,
		BiFunction<World, Player, Entity> entityFactory
	) {
		super(name, namespaceId, id, null); // lazy block assignment
		this.placedBlock = placedBlock;
		this.entityFactory = entityFactory;
		this.setMaxStackSize(1);
	}

	/* =====================
	   SHIFT + RIGHT CLICK → RELEASE ENTITY
	   ===================== */
	@Override
	public ItemStack onUseItem(ItemStack stack, World world, Player player) {
		if (!player.isSneaking() || entityFactory == null) return stack;

		if (!world.isClientSide) {
			Entity entity = entityFactory.apply(world, player);
			entity.setPos(player.x, player.y, player.z);
			world.entityJoinedWorld(entity);

			// Replace with empty jar item
			return new ItemStack(net.minecraft.core.item.Items.JAR);
		}

		return stack;
	}

	/* =====================
	   NORMAL RIGHT CLICK → PLACE BLOCK
	   ===================== */
	@Override
	public boolean onUseItemOnBlock(ItemStack stack, Player player, World world,
									int x, int y, int z, Side side,
									double xPlaced, double yPlaced) {

		if (player.isSneaking()) return false; // handled by onUseItem

		Block<?> block = placedBlock.get();
		if (block == null) return false; // safety check

		// Calculate placement position
		int placeX = x + side.getOffsetX();
		int placeY = y + side.getOffsetY();
		int placeZ = z + side.getOffsetZ();

		if (!world.isAirBlock(placeX, placeY, placeZ)) return false;

		// Place the block in the world
		world.setBlockWithNotify(placeX, placeY, placeZ, block.id());
		block.getLogic().onBlockPlacedByWorld(world, placeX, placeY, placeZ);

		// Consume the jar
		if (player.getGamemode().consumeBlocks()) {
			player.swingItem();
			player.getHeldItem().stackSize--;
		}
		world.playSoundAtEntity(player, player, "item.pickup", 1.0F, 1.0F);
		return true;
	}
}
