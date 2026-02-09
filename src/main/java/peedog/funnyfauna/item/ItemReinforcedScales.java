package peedog.funnyfauna.item;

public class ItemReinforcedScales extends ItemScales {
	public ItemReinforcedScales(String translationKey, String namespaceId, int id) {
		super(translationKey, namespaceId, id);
		this.maxStackSize = 16;
	}
}
