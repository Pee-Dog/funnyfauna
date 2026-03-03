package peedog.funnyfauna.item;

import net.minecraft.core.block.Block;
import net.minecraft.core.block.entity.TileEntity;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemPlaceable;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.item.Items;
import net.minecraft.core.util.helper.Side;
import net.minecraft.core.util.phys.HitResult;
import net.minecraft.core.util.phys.Vec3;
import net.minecraft.core.world.World;
import com.mojang.nbt.tags.CompoundTag;
import peedog.funnyfauna.block.BlockLogicJarCricket;
import peedog.funnyfauna.block.entity.TileEntityJarCricket;
import peedog.funnyfauna.entity.cricket.EntityCricket;

import java.util.function.BiFunction;
import java.util.function.Supplier;

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
		super(name, namespaceId, id, null);
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

			if (stack.getData() != null) {

				if (entity instanceof EntityCricket
					&& stack.getData().containsKey("CricketColor")) {

					((EntityCricket) entity).setColor(
						stack.getData().getInteger("CricketColor")
					);
				}

				if (entity instanceof peedog.funnyfauna.entity.worm.EntityWorm
					&& stack.getData().containsKey("WormColor")) {

					((peedog.funnyfauna.entity.worm.EntityWorm) entity)
						.setColor(stack.getData().getInteger("WormColor"));
				}
			}


			// -------- Raytrace spawn position --------
			Vec3 look = player.getLookAngle();

			double spawnX;
			double spawnY;
			double spawnZ;
				spawnX = player.x + look.x * 2.0;
				spawnY = player.y - 1;
				spawnZ = player.z + look.z * 2.0;

			entity.setPos(spawnX, spawnY, spawnZ);
			world.entityJoinedWorld(entity);


			if (player.getGamemode().consumeBlocks()) {
				return new ItemStack(Items.JAR);
			}
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
		return false;
	}

}
