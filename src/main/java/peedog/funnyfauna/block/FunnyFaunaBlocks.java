package peedog.funnyfauna.block;

import net.minecraft.core.block.Block;
import net.minecraft.core.sound.BlockSounds;
import turniplabs.halplibe.helper.BlockBuilder;
import turniplabs.halplibe.util.BlockInitEntrypoint;

import static peedog.funnyfauna.FunnyFauna.MOD_ID;
import static peedog.funnyfauna.entity.FunnyFaunaEntities.hasInit;

public final class FunnyFaunaBlocks implements BlockInitEntrypoint {
	static int blockID = 5200;

	public static Block<?> EGG_EMU_BLOCK;
	public static Block<?> ANT_HILL;

	public static void init() {
		if (!hasInit) {
			hasInit = true;
			initializeBlocks();
		}
	}

	public static void initializeBlocks() {
		EGG_EMU_BLOCK = new BlockBuilder(MOD_ID)
			.setBlockSound(BlockSounds.STONE)
			.setHardness(0.2f)
			.setResistance(0.2f)
			.build("egg.emu.block", "egg_emu_block", blockID++,BlockLogicEggEmuBlock::new);
		ANT_HILL = new BlockBuilder(MOD_ID)
			.setBlockSound(BlockSounds.SAND)
			.setHardness(0.2f)
			.setResistance(0.2f)
			.build("ant.hill", "ant_hill", blockID++,BlockLogicAntHill::new);


	}

	@Override
	public void afterBlockInit() {

	}
}
