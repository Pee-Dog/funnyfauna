package peedog.funnyfauna.block;

import net.minecraft.core.block.Block;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.world.World;
import peedog.funnyfauna.entity.cricket.EntityCricket;

import java.util.function.Supplier;
import net.minecraft.core.item.Item;

public class BlockLogicJarCricket extends BlockLogicJarAnimal {

	public BlockLogicJarCricket(
		Block<?> block,
		Supplier<Item> itemSupplier
	) {
		super(block, itemSupplier);
	}

	@Override
	protected Entity createReleasedEntity(World world, int x, int y, int z) {
		EntityCricket cricket = new EntityCricket(world);
		cricket.setPos(
			x + 0.5F,
			y + 0.1F,
			z + 0.5F
		);
		return cricket;
	}
}
