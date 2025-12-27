package peedog.funnyfauna;

import net.minecraft.core.item.Item;
import peedog.funnyfauna.item.FunnyFaunaItems;
import turniplabs.halplibe.helper.RecipeBuilder;
import turniplabs.halplibe.util.RecipeEntrypoint;

import static peedog.funnyfauna.FunnyFauna.MOD_ID;

public class FunnyFaunaRecipes implements RecipeEntrypoint {

	public void initializeRecipes() {
		RecipeBuilder.Furnace(MOD_ID)
			.setInput(FunnyFaunaItems.FOOD_BLUBBER)
			.create("oil", FunnyFaunaItems.OIL.getDefaultStack());

	}

	@Override
	public void onRecipesReady() {
		initializeRecipes();

	}

	@Override
	public void initNamespaces() {
		RecipeBuilder.initNameSpace(MOD_ID);
		RecipeBuilder.getRecipeNamespace(MOD_ID);
	}
}
