package peedog.funnyfauna.item;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Global;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.Item;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.world.World;
import net.minecraft.client.gui.Screen;
import peedog.funnyfauna.inventory.InventorySatchel;
import peedog.funnyfauna.client.gui.SatchelScreen;
import peedog.funnyfauna.inventory.MenuSatchel;

public class ItemSatchel extends Item {
	public ItemSatchel(String translationKey, String namespaceId, int id) {
		super(translationKey, namespaceId, id);
		this.maxStackSize = 1;
	}

	@Override
	public ItemStack onUseItem(ItemStack stack, World world, Player player) {
		// Debug message
		System.out.println("Satchel right-clicked!");

		// Load or create the satchel inventory
		InventorySatchel inventory = new InventorySatchel(stack);

		// Create the menu for the player
		MenuSatchel menu = new MenuSatchel(inventory, player);

		// Assign it to the player's currently open inventory
		player.craftingInventory = menu;

		// Open the GUI only on the client side
		if (world.isClientSide) {
			Minecraft mc = Global.accessor; // use the global accessor
			if (mc != null) {
				int width = mc.resolution != null ? mc.resolution.width : 800;
				int height = mc.resolution != null ? mc.resolution.height : 600;

				SatchelScreen screen = new SatchelScreen(menu);
				screen.opened(mc, width, height); // open the screen
			}
		}

		return stack;
	}
}

