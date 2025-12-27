package peedog.funnyfauna.block;

import net.minecraft.core.block.Block;
import net.minecraft.core.block.BlockLogic;
import net.minecraft.core.block.material.Material;
import net.minecraft.core.world.World;
import peedog.funnyfauna.FunnyFauna;
import peedog.funnyfauna.block.entity.TileEntityEmuEgg;
import peedog.funnyfauna.entity.ant.EntityAnt;
import peedog.funnyfauna.entity.emu.MobEmu;

import java.util.Random;

public class BlockLogicAntHill extends BlockLogic {

	public BlockLogicAntHill(Block<?> block) {
		super(block, Material.sand);
	}

	@Override
	public void animationTick(World world, int x, int y, int z, Random rand) {
		if (rand.nextInt(4) == 0) { // control frequency
			EntityAnt ant = (EntityAnt) FunnyFauna.createEntity(EntityAnt.class, world);
			ant.moveTo(x + 0.5, y + 1, z + 0.5, world.rand.nextFloat() * 360F, 0);
			ant.spawnInit();

			world.entityJoinedWorld(ant);
		}
	}
	}
