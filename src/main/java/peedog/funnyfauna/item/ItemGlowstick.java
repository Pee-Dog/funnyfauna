package peedog.funnyfauna.item;

import java.util.Random;
import net.minecraft.core.block.entity.TileEntityActivator;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.IDispensable;
import net.minecraft.core.item.Item;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.util.helper.Direction;
import net.minecraft.core.util.helper.DyeColor;
import net.minecraft.core.world.World;
import peedog.funnyfauna.entity.projectile.ProjectileGlowstick;

public class ItemGlowstick extends Item implements IDispensable {

	public ItemGlowstick(String name, String namespaceId, int id) {
		super(name, namespaceId, id);
		this.maxStackSize = 64;
		this.setHasSubtypes(true);
	}

	@Override
	public ItemStack onUseItem(ItemStack itemstack, World world, Player entityplayer) {
		itemstack.consumeItem(entityplayer);
		world.playSoundAtEntity(entityplayer, entityplayer, "random.bow",
			0.5F, 0.4F / (itemRand.nextFloat() * 0.4F + 0.8F));

		if (!world.isClientSide) {
			ProjectileGlowstick projectile = new ProjectileGlowstick(world, entityplayer);
			// Carry the item's color metadata into the projectile so placement
			// and drops both use the correct color.
			projectile.colorMeta = itemstack.getMetadata() & 15;
			world.entityJoinedWorld(projectile);
		}

		return itemstack;
	}

	@Override
	public void onUseByActivator(ItemStack itemStack, TileEntityActivator activatorBlock, World world,
								 Random random, int blockX, int blockY, int blockZ,
								 double offX, double offY, double offZ, Direction direction) {

		ProjectileGlowstick projectile = new ProjectileGlowstick(world,
			(double) blockX + offX,
			(double) blockY + offY,
			(double) blockZ + offZ);
		projectile.colorMeta = itemStack.getMetadata() & 15;
		projectile.setHeading(
			(double) direction.getOffsetX() * 0.6,
			direction.getOffsetY() == 0 ? 0.1 : (double) direction.getOffsetY() * 0.6,
			(double) ((float) direction.getOffsetZ() * 0.6F),
			1.1F,
			6.0F);
		world.entityJoinedWorld(projectile);
		--itemStack.stackSize;
	}

	@Override
	public void onDispensed(ItemStack itemStack, World world,
							double x, double y, double z,
							int xOffset, int yOffset, int zOffset, Random random) {

		ProjectileGlowstick projectile = new ProjectileGlowstick(world, x, y, z);
		projectile.colorMeta = itemStack.getMetadata() & 15;
		projectile.setHeading((double) xOffset, (double) yOffset + 0.1,
			(double) zOffset, 1.1F, 6.0F);
		world.entityJoinedWorld(projectile);
	}

	public String getLanguageKey(ItemStack itemstack) {
		return super.getKey() + "." + DyeColor.colorFromBlockMeta(itemstack.getMetadata()).colorID;
	}

}
