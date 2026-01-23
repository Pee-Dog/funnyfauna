package peedog.funnyfauna.item;

import net.minecraft.core.item.Item;

public class ItemScales extends Item {

	public ItemScales(String translationKey, String namespaceId, int id) {
		super(translationKey, namespaceId, id);
		this.maxStackSize = 16;
	}
}
