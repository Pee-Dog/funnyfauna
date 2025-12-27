package peedog.funnyfauna.item;

import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.Item;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.world.World;
import peedog.funnyfauna.PlayerInventoryDisplay;

public class ItemSatchel extends Item {
	public final int satchelSize;
	public ItemSatchel(final String translationKey, final String namespaceId, final int id) {
		this(translationKey, namespaceId, id, 9);
	}
	public ItemSatchel(final String translationKey, final String namespaceId, final int id, final int satchelSize) {
		super(translationKey, namespaceId, id);
		this.maxStackSize = 1;
		this.satchelSize = satchelSize;
	}
	@Override
	public ItemStack onUseItem(final ItemStack itemstack, final World world, final Player entityplayer) {
			//noinspection CastToIncompatibleInterface
			((PlayerInventoryDisplay) entityplayer).funnyfauna$displayGUISatchel(itemstack);
		return itemstack;
	}
}
