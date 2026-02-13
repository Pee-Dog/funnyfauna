package peedog.funnyfauna.item;

import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.Item;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.world.World;

public class ItemBiomeCompass extends Item {

	private static final String[] TEMPS = new String[] {
		"HOT",
		"WARM",
		"COLD",
		"FROZEN"
	};

	public ItemBiomeCompass(String name, String namespaceId, int id) {
		super(name, namespaceId, id);
		this.maxStackSize = 1;
	}

	@Override
	public ItemStack onUseItem(ItemStack stack, World world, Player player) {

		if (!world.isClientSide) {

			String current = getTargetTemp(stack);
			String next = getNextTemp(current);

			setTargetTemp(stack, next);
			player.sendStatusMessage("Biome Compass target: " + next);
		}

		return stack;
	}

	private void setTargetTemp(ItemStack stack, String temp) {
		stack.getData().putString("targetTemp", temp);
	}

	private String getTargetTemp(ItemStack stack) {
		return stack.getData().getString("targetTemp");
	}

	private String getNextTemp(String current) {

		if (current == null || current.isEmpty())
			return TEMPS[0];

		for (int i = 0; i < TEMPS.length; i++) {
			if (TEMPS[i].equals(current)) {
				return TEMPS[(i + 1) % TEMPS.length];
			}
		}

		return TEMPS[0];
	}
}
