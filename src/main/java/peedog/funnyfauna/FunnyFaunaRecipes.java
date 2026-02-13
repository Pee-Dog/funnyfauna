package peedog.funnyfauna;

import net.minecraft.core.block.Blocks;
import net.minecraft.core.item.Item;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.item.Items;
import peedog.funnyfauna.item.FunnyFaunaItems;
import turniplabs.halplibe.helper.RecipeBuilder;
import turniplabs.halplibe.util.RecipeEntrypoint;

import static peedog.funnyfauna.FunnyFauna.MOD_ID;

public class FunnyFaunaRecipes implements RecipeEntrypoint {

	public void initializeRecipes() {
		RecipeBuilder.Furnace(MOD_ID)
			.setInput(FunnyFaunaItems.EGG_EMU)
			.create("food_eggemu_cooked", FunnyFaunaItems.FOOD_EGGEMU_COOKED.getDefaultStack());
		RecipeBuilder.Shaped(MOD_ID,
				"###",  // top row
				"# #"   // bottom row
			)
			.addInput('#', FunnyFaunaItems.SCUTE)
			.create("sturdy_shell", FunnyFaunaItems.STURDY_SHELL.getDefaultStack());
		RecipeBuilder.Shaped(MOD_ID,
				"###",
				"# #",
				"###"
			)
			.addInput('#', FunnyFaunaItems.COARSEHIDE)
			.create("pocket", FunnyFaunaItems.POCKET.getDefaultStack());
		RecipeBuilder.Shaped(MOD_ID,
				"##",
				"##"
			)
			.addInput('#', Blocks.DEADBUSH)
			.create("tumbleweed", FunnyFaunaItems.TUMBLEWEED.getDefaultStack());
		RecipeBuilder.Shaped(MOD_ID,
				"B",
				"P",
				"S"
			)
			.addInput('P', Blocks.PISTON_BASE)
			.addInput('B', FunnyFaunaItems.BIRDFOOT)
			.addInput('S', Items.STICK)
			.create("arm_extension", FunnyFaunaItems.ARM_EXTENSION.getDefaultStack());
		RecipeBuilder.Shaped(MOD_ID,
				"# #",
				"X X"
			)
			.addInput('#', FunnyFaunaItems.BIRDFOOT)
			.addInput('X', FunnyFaunaItems.COARSEHIDE)
			.create("climbing_claws", FunnyFaunaItems.CLIMBING_CLAWS.getDefaultStack());
		RecipeBuilder.Shapeless(MOD_ID)
			.addInput(FunnyFaunaItems.COARSEHIDE)
			.create("paper", Items.PAPER.getDefaultStack());
		ItemStack deadBushes = Blocks.DEADBUSH.getDefaultStack();
		RecipeBuilder.Shapeless(MOD_ID)
			.addInput(FunnyFaunaItems.TUMBLEWEED)
			.create("dead_bush", deadBushes);
		deadBushes.stackSize = 4;
		ItemStack scales = FunnyFaunaItems.SCALES.getDefaultStack();
		RecipeBuilder.Shapeless(MOD_ID)
			.addInput(FunnyFaunaItems.FOOD_LIZARDTAIL)
			.create("scales", scales);
		scales.stackSize = 2;
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
