package peedog.funnyfauna.item;

import net.minecraft.core.item.Item;
import net.minecraft.core.item.ItemFood;
import turniplabs.halplibe.helper.ItemBuilder;

import static peedog.funnyfauna.FunnyFauna.MOD_ID;

public class FunnyFaunaItems {

	int itemID = 17550;

	public static Item FOOD_LIZARDTAIL;
	public static Item COARSEHIDE;
	public static Item SATCHEL;
	public static Item BIRDFOOT;


	public void initializeItems() {
		// Items
		FOOD_LIZARDTAIL = new ItemBuilder(MOD_ID)
			.build(new ItemFood("food.lizardtail", "funnyfauna:item/food_lizardtail", itemID++, 2, 12, false, 8));
		COARSEHIDE = new ItemBuilder(MOD_ID)
			.build(new Item("coarsehide", "funnyfauna:item/coarsehide", itemID++));
		SATCHEL = new ItemBuilder(MOD_ID)
			.build(new ItemSatchel("satchel", "funnyfauna:item/satchel", itemID++));
		BIRDFOOT = new ItemBuilder(MOD_ID)
			.build(new Item("birdfoot", "funnyfauna:item/birdfoot", itemID++));
	}
}
