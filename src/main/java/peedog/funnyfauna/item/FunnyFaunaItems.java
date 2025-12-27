package peedog.funnyfauna.item;

import net.minecraft.core.item.Item;
import net.minecraft.core.item.ItemFood;
import turniplabs.halplibe.helper.ItemBuilder;

import static peedog.funnyfauna.FunnyFauna.MOD_ID;

public class FunnyFaunaItems {

	int itemID = 17550;

	public static Item FOOD_LIZARDTAIL;
	public static Item COARSEHIDE;
	public static Item POCKET;
	public static Item BIRDFOOT;
	public static Item OIL;
	public static Item FOOD_BLUBBER;
	public static Item EGG_EMU;
	public static Item ARM_EXTENSION;



	public void initializeItems() {
		// Items
		FOOD_LIZARDTAIL = new ItemBuilder(MOD_ID)
			.build(new ItemFood("food.lizardtail", "funnyfauna:item/food_lizardtail", itemID++, 2, 12, false, 8));
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
			.build(new ItemBigEgg("egg_emu", "funnyfauna:item/egg_emu", itemID++));
		ARM_EXTENSION = new ItemBuilder(MOD_ID)
			.build(new ItemArmExtension("arm_extension", "funnyfauna:item/arm_extension", itemID++));


	}
}
