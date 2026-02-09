package peedog.funnyfauna.item;

import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.Item;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.world.World;
import peedog.funnyfauna.PlayerInventoryDisplay;

public abstract class ItemToggleable extends Item {

	public static final String TAG_TOGGLED = "Enabled";

	public ItemToggleable(String translationKey, String namespace, int id) {
		super(translationKey, namespace, id);
		this.setMaxStackSize(1);
		this.setHasSubtypes(true);
	}

	/* ----------------------------
	   NBT helpers
	---------------------------- */
	public static boolean isToggled(ItemStack stack) {
		return stack != null && stack.getData().getBoolean(TAG_TOGGLED);
	}

	public static void setToggled(ItemStack stack, boolean toggled) {
		if (stack == null) return;
		stack.getData().putBoolean(TAG_TOGGLED, toggled);
	}

	/* ----------------------------
	   Right-click toggle
	---------------------------- */
	@Override
	public ItemStack onUseItem(ItemStack stack, World world, Player player) {
		boolean wasToggled = isToggled(stack);
		boolean nowToggled = !wasToggled;

		// Disable other toggleable items
		if (nowToggled) {
			for (ItemStack s : player.inventory.mainInventory) {
				if (s != null && s != stack && s.getItem() instanceof ItemToggleable) {
					setToggled(s, false);
				}
			}
		}

		setToggled(stack, nowToggled);

		// Update HUD
		if (player instanceof PlayerInventoryDisplay) {
			PlayerInventoryDisplay display = (PlayerInventoryDisplay) player;

			if (nowToggled) {
				display.funnyfauna$setEquippedSlot(stack);
				display.funnyfauna$setEquippedEnabled(true);
			} else {
				display.funnyfauna$setEquippedSlot(null);
				display.funnyfauna$setEquippedEnabled(false);
			}
		}

		// Sound feedback
		if (!world.isClientSide) {
			world.playSoundAtEntity(
				player,
				player,
				nowToggled ? "tile.piston.out" : "tile.piston.in",
				0.3F,
				1.2F
			);
		}

		return stack;
	}

	/* ----------------------------
	   Auto-disable when dropped
	---------------------------- */
	/* ----------------------------
   Auto-disable when dropped / removed
---------------------------- */
	@Override
	public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
		if (!(entity instanceof Player)) return;
		Player player = (Player) entity;

		boolean stillInInventory = false;
		for (ItemStack s : player.inventory.mainInventory) {
			if (s == stack) {
				stillInInventory = true;
				break;
			}
		}

		// Disable if removed from inventory (e.g., other mods remove it)
		if (!stillInInventory && isToggled(stack)) {
			disableStack(stack, player);
		}
	}

	/**
	 * Call this when the item is dropped or removed from inventory
	 */
	public static void disableStack(ItemStack stack, Player player) {
		setToggled(stack, false);
		if (player instanceof PlayerInventoryDisplay) {
			PlayerInventoryDisplay display = (PlayerInventoryDisplay) player;
			display.funnyfauna$setEquippedSlot(null); // clear HUD
			display.funnyfauna$setEquippedEnabled(false); // disable highlight
		}
	}

}
