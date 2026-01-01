package peedog.funnyfauna.block;

import net.minecraft.client.Minecraft;
import net.minecraft.core.block.Block;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.util.phys.AABB;
import net.minecraft.core.world.World;
import peedog.funnyfauna.entity.cricket.EntityCricket;

import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import net.minecraft.core.item.Item;
import peedog.funnyfauna.particle.ParticleCricket;

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
	@Override
	public void animationTick(World world, int x, int y, int z, Random rand) {
		if (!world.isClientSide) {
			return;
		}
		// DEBUG: vanilla smoke
		world.spawnParticle(
			"smoke",
			x + 0.5,
			y + 3,
			z + 0.5,
			0.0, 0.0, 0.0,
			0
		);

		// Throttle checks
		if (rand.nextInt(10) != 0) {
			return;
		}

		double minX = x + 0.25;
		double minY = y;
		double minZ = z + 0.25;
		double maxX = x + 0.75;
		double maxY = y + 1.0;
		double maxZ = z + 0.75;

		@SuppressWarnings("unchecked")
		List<ParticleCricket> particles =
			world.getEntitiesWithinAABB(
				ParticleCricket.class,
				AABB.getTemporaryBB(minX, minY, minZ, maxX, maxY, maxZ)
			);

		world.spawnParticle("funnyfauna:cricket", x + 0.5, y + 0.1, z + 0.5, 0.0, 0.0, 0.0, 0);
	}


}
