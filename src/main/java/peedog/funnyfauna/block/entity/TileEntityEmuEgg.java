package peedog.funnyfauna.block.entity;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.core.block.material.Material;
import net.minecraft.core.block.entity.TileEntity;
import net.minecraft.core.world.World;
import peedog.funnyfauna.FunnyFauna;
import peedog.funnyfauna.entity.emu.MobEmu;

public class TileEntityEmuEgg extends TileEntity {

	private static final int HATCH_TIME = 20 * 160; // 30 seconds
	private int incubationTicks = 0;

	@Override
	public void tick() {
		if (worldObj == null || worldObj.isClientSide) return;

		// Only incubate if on sand or dirt
		if (!canIncubate()) return;

		incubationTicks++;

		if (incubationTicks >= HATCH_TIME) {
			hatch();
		}
	}

	private boolean canIncubate() {
		Material below = worldObj.getBlockMaterial(x, y - 1, z);
		return below == Material.sand || below == Material.dirt || below == Material.grass ;
	}

	private void hatch() {
		World world = worldObj;

		// Remove egg block
		world.setBlockWithNotify(x, y, z, 0);

		// Spawn emu
		MobEmu emu = (MobEmu) FunnyFauna.createEntity(MobEmu.class, world);
		emu.moveTo(x + 0.5, y, z + 0.5, world.rand.nextFloat() * 360F, 0);
		emu.spawnInit();

		world.entityJoinedWorld(emu);
	}

	@Override
	public void readFromNBT(CompoundTag tag) {
		super.readFromNBT(tag);
		this.incubationTicks = tag.getInteger("IncubationTicks");
	}

	@Override
	public void writeToNBT(CompoundTag tag) {
		super.writeToNBT(tag);
		tag.putInt("IncubationTicks", incubationTicks);
	}
}
