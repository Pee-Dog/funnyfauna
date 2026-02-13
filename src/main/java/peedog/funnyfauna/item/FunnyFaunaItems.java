package peedog.funnyfauna.item;

import net.minecraft.core.block.Blocks;
import net.minecraft.core.item.Item;
import net.minecraft.core.item.ItemFood;
import peedog.funnyfauna.block.FunnyFaunaBlocks;
import peedog.funnyfauna.entity.cricket.EntityCricket;
import peedog.funnyfauna.entity.worm.EntityWorm;
import turniplabs.halplibe.helper.ItemBuilder;

import static peedog.funnyfauna.FunnyFauna.MOD_ID;

public class FunnyFaunaItems {

	int itemID = 17550;

	public static Item FOOD_LIZARDTAIL;
	public static Item FOOD_EGGEMU_COOKED;
	public static Item COARSEHIDE;
	public static Item POCKET;
	public static Item BIRDFOOT;
	public static Item OIL;
	public static Item FOOD_BLUBBER;
	public static Item EGG_EMU;
	public static Item ARM_EXTENSION;
	public static Item JAR_CRICKET;
	public static Item SCALES;
	public static Item SCUTE;
	public static Item STURDY_SHELL;
	public static Item TUMBLEWEED;
	public static Item CLIMBING_CLAWS;
	public static Item SCALES_REINFORCED;
	public static Item BIOME_COMPASS;
	public static Item JAR_WORM;



	public void initializeItems() {
		// Items
		FOOD_LIZARDTAIL = new ItemBuilder(MOD_ID)
			.build(new ItemFood("food.lizardtail", "funnyfauna:item/food_lizardtail", itemID++, 2, 12, false, 8));
		FOOD_EGGEMU_COOKED = new ItemBuilder(MOD_ID)
			.build(new ItemFood("food.egg_emu.cooked", "funnyfauna:item/food_eggemu_cooked", itemID++, 8, 16, false, 4));
		FOOD_BLUBBER = new ItemBuilder(MOD_ID)
			.build(new ItemFood("food.blubber", "funnyfauna:item/food_blubber", itemID++, 2, 12, false, 8));
		COARSEHIDE = new ItemBuilder(MOD_ID)
			.build(new Item("coarsehide", "funnyfauna:item/coarsehide", itemID++));
		POCKET = new ItemBuilder(MOD_ID)
			.build(new ItemSatchel("pocket", "funnyfauna:item/pocket", itemID++, 5));
		BIRDFOOT = new ItemBuilder(MOD_ID)
			.build(new Item("birdfoot", "funnyfauna:item/birdfoot", itemID++));
		OIL = new ItemBuilder(MOD_ID)
			.build(new Item("oil", "funnyfauna:item/oil", itemID++));
		EGG_EMU = new ItemBuilder(MOD_ID)
			.build(new ItemBigEgg("egg.emu", "funnyfauna:item/egg_emu", itemID++));
		JAR_CRICKET = new ItemBuilder(MOD_ID)
			.build(new ItemJarAnimal(
				"jar.cricket",
				"funnyfauna:item/jar_cricket",
				itemID++,
				() -> FunnyFaunaBlocks.JAR_CRICKET,   // placed block
				(world, player) -> new EntityCricket(world) // released entity
			));
		JAR_WORM = new ItemBuilder(MOD_ID)
			.build(new ItemJarAnimal(
				"jar.worm",
				"funnyfauna:item/jar_worm",
				itemID++,
				() -> FunnyFaunaBlocks.JAR_CRICKET,   // placed block
				(world, player) -> new EntityWorm(world) // released entity
			));
		SCALES = new ItemBuilder(MOD_ID)
			.build(new ItemScales("scales", "funnyfauna:item/scales", itemID++));
		SCALES_REINFORCED = new ItemBuilder(MOD_ID)
			.build(new ItemReinforcedScales("scales.reinforced", "funnyfauna:item/scales_reinforced", itemID++));
		SCUTE = new ItemBuilder(MOD_ID)
			.build(new Item("scute", "funnyfauna:item/scute", itemID++));
		TUMBLEWEED = new ItemBuilder(MOD_ID)
			.build(new ItemTumbleweed("tumbleweed", "funnyfauna:item/tumbleweed", itemID++));
		BIOME_COMPASS = new ItemBuilder(MOD_ID)
			.build(new ItemBiomeCompass("biome.compass", "funnyfauna:item/biome_compass", itemID++));

// Toggleable items
		ARM_EXTENSION = new ItemBuilder(MOD_ID)
			.build(new ItemArmExtension("arm_extension", "funnyfauna:item/arm_extension", itemID++));
		STURDY_SHELL = new ItemBuilder(MOD_ID)
			.build(new ItemSturdyShell("sturdy.shell", "funnyfauna:item/sturdy_shell", itemID++));
		CLIMBING_CLAWS = new ItemBuilder(MOD_ID)
			.build(new ItemClimbingClaws("climbing.claws", "funnyfauna:item/climbing_claws", itemID++));

	}
}
