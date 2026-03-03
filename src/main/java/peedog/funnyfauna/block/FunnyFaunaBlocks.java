package peedog.funnyfauna.block;

import net.minecraft.core.block.Block;
import net.minecraft.core.block.BlockLogic;
import net.minecraft.core.block.BlockLogicAxisAligned;
import net.minecraft.core.block.material.Material;
import net.minecraft.core.block.tag.BlockTags;
import net.minecraft.core.data.tag.Tag;
import net.minecraft.core.item.Items;
import net.minecraft.core.sound.BlockSounds;
import peedog.funnyfauna.item.FunnyFaunaItems;
import turniplabs.halplibe.helper.BlockBuilder;
import turniplabs.halplibe.util.BlockInitEntrypoint;

import static peedog.funnyfauna.FunnyFauna.MOD_ID;
import static peedog.funnyfauna.entity.FunnyFaunaEntities.hasInit;

public final class FunnyFaunaBlocks implements BlockInitEntrypoint {
	static int blockID = 5200;

	public static Block<?> EGG_EMU_BLOCK;
	public static Block<?> ANT_HILL;
	public static Block<?> JAR_CRICKET;
	public static Block<?> CACTUS_GOLDEN;
	public static Block<?> HAYBALE;
	public static Block<?> GLOWSTICK;

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
			.setTicking(true)
			.build("ant.hill", "ant_hill", blockID++,BlockLogicAntHill::new);
		JAR_CRICKET = new BlockBuilder(MOD_ID)
			.setBlockSound(BlockSounds.GLASS)
			.setHardness(0.1f)
			.setTicking(true)
			.setResistance(0.1f)
			.build("jar.cricket", "jar_cricket", blockID++, block -> new BlockLogicJarCricket(block, () -> FunnyFaunaItems.JAR_CRICKET));
		HAYBALE = new BlockBuilder(MOD_ID)
			.setBlockSound(BlockSounds.GRASS)
			.setHardness(0.2f)
			.setResistance(0.2f)
			.build("haybale", "haybale", blockID++, block -> new BlockLogicAxisAligned(block, Material.grass));
		GLOWSTICK = new BlockBuilder(MOD_ID)
			.setBlockSound(BlockSounds.GLASS)
			.setHardness(0.0f)
			.setLuminance(13)
			.setTicking(true)
			.setResistance(0.1f)
			.addTags(new Tag[]{BlockTags.BROKEN_BY_FLUIDS})
			.build("glowstick", "glowstick", blockID++, block -> new BlockLogicGlowstick(block));
		GLOWSTICK.setStatParent(() -> FunnyFaunaItems.GLOWSTICK);
	}

	@Override
	public void afterBlockInit() {

	}
}
