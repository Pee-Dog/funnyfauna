package peedog.funnyfauna.block.entity;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.core.block.entity.TileEntity;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.world.World;
import peedog.funnyfauna.entity.cricket.EntityCricket;

public class TileEntityJarCricket extends TileEntity {

	private int cricketColor = 0xFFFFFF; // default white

	public int getCricketColor() { return cricketColor; }
	public void setCricketColor(int color) { this.cricketColor = color; }

	@Override
	public void writeToNBT(CompoundTag tag) {
		super.writeToNBT(tag);
		tag.putInt("CricketColor", cricketColor);
	}

	@Override
	public void readFromNBT(CompoundTag tag) {
		super.readFromNBT(tag);
		if (tag.containsKey("CricketColor")) cricketColor = tag.getInteger("CricketColor");
	}

	/** Call when placing a jar block from item */
	public void readFromItemStack(ItemStack stack) {
		if (stack != null && stack.getData() != null && stack.getData().containsKey("CricketColor")) {
			cricketColor = stack.getData().getInteger("CricketColor");
		}
	}
	public EntityCricket createCricket(World world, double x, double y, double z) {
		EntityCricket cricket = new EntityCricket(world, cricketColor);
		cricket.setPos(x, y, z);
		return cricket;
	}

}
