package peedog.funnyfauna;

import net.minecraft.core.item.Item;
import net.minecraft.core.item.Items;
import peedog.funnyfauna.item.FunnyFaunaItems;
import turniplabs.halplibe.helper.RecipeBuilder;
import turniplabs.halplibe.util.RecipeEntrypoint;

import static peedog.funnyfauna.FunnyFauna.MOD_ID;

public class FunnyFaunaRecipes implements RecipeEntrypoint {

	public void initializeRecipes() {
		RecipeBuilder.Furnace(MOD_ID)
			.setInput(FunnyFaunaItems.FOOD_BLUBBER)
			.create("oil", FunnyFaunaItems.OIL.getDefaultStack());
		RecipeBuilder.Shaped(MOD_ID,
				"###",  // top row
				"# #"   // bottom row
			)
			.addInput('#', FunnyFaunaItems.SCUTE)
			.create("sturdy_shell", FunnyFaunaItems.STURDY_SHELL.getDefaultStack());
		RecipeBuilder.Shaped(MOD_ID,
				"###",
				"#X#",
				"###"
			)
			.addInput('#', FunnyFaunaItems.COARSEHIDE)
			.addInput('X', Items.INGOT_STEEL_CRUDE)
			.create("pocket", FunnyFaunaItems.POCKET.getDefaultStack());
		RecipeBuilder.Shapeless(MOD_ID)
			.addInput(FunnyFaunaItems.COARSEHIDE)
			.create("paper", Items.PAPER.getDefaultStack());
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
