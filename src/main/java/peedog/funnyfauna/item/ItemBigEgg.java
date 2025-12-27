package peedog.funnyfauna.item;

import net.minecraft.core.block.entity.TileEntityActivator;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.IDispensable;
import net.minecraft.core.item.Item;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.util.helper.Direction;
import net.minecraft.core.world.World;
import peedog.funnyfauna.entity.projectile.ProjectileBigEgg;

import java.util.Random;

public class ItemBigEgg extends Item implements IDispensable {
	public ItemBigEgg(String name, String namespaceId, int id) {
		super(name, namespaceId, id);
		this.maxStackSize = 8;
	}

	public ItemStack onUseItem(ItemStack itemstack, World world, Player entityplayer) {
		itemstack.consumeItem(entityplayer);
		world.playSoundAtEntity(entityplayer, entityplayer, "random.bow", 0.5F, 0.4F / (itemRand.nextFloat() * 0.4F + 0.8F));
		if (!world.isClientSide) {
			world.entityJoinedWorld(new ProjectileBigEgg(world, entityplayer));
		}

		return itemstack;
	}

	public void onUseByActivator(ItemStack itemStack, TileEntityActivator activatorBlock, World world, Random random, int blockX, int blockY, int blockZ, double offX, double offY, double offZ, Direction direction) {
		ProjectileBigEgg projectileBigEgg = new ProjectileBigEgg(world, (double)blockX + offX, (double)blockY + offY, (double)blockZ + offZ);
		projectileBigEgg.setHeading((double)direction.getOffsetX() * 0.6, direction.getOffsetY() == 0 ? 0.1 : (double)direction.getOffsetY() * 0.6, (double)((float)direction.getOffsetZ() * 0.6F), 1.1F, 6.0F);
		world.entityJoinedWorld(projectileBigEgg);
		--itemStack.stackSize;
	}

	public void onDispensed(ItemStack itemStack, World world, double x, double y, double z, int xOffset, int yOffset, int zOffset, Random random) {
		ProjectileBigEgg projectileBigEgg = new ProjectileBigEgg(world, x, y, z);
		projectileBigEgg.setHeading((double)xOffset, (double)yOffset + 0.1, (double)zOffset, 1.1F, 6.0F);
		world.entityJoinedWorld(projectileBigEgg);
	}
}
