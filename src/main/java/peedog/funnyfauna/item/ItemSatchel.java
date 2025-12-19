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

//	@Override
//	public ItemStack onUseItem(ItemStack stack, World world, Player player) {
//
//			}
//		}
//
//		return stack;
//	}
}

