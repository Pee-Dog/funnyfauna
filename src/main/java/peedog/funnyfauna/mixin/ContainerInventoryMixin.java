package peedog.funnyfauna.mixin;

import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.player.inventory.container.ContainerInventory;
import peedog.funnyfauna.item.ItemScales;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ContainerInventory.class)
public class ContainerInventoryMixin {

	private static final int REPAIR_AMOUNT = 5;

	@Inject(method = "handleSlotClick", at = @At("HEAD"), cancellable = true)
	private void onSlotClick(int slotIndex, int mouseButton, boolean shiftPressed, Player player, CallbackInfo ci) {
		ContainerInventory container = (ContainerInventory)(Object)this;

		ItemStack held = container.getHeldItemStack();
		ItemStack clicked = container.getItem(slotIndex);

		if (held == null) return;
		if (!(held.getItem() instanceof ItemScales)) return;
		if (clicked == null) return;
		if (!clicked.isItemStackDamageable()) return;
		if (clicked.getMetadata() <= 0) return;

		// Repair clicked item
		clicked.setMetadata(Math.max(0, clicked.getMetadata() - REPAIR_AMOUNT));

		// Consume one scale
		held.stackSize--;
		if (held.stackSize <= 0) {
			container.setHeldItemStack(null);
		}

		// Mark inventory changed
		container.setChanged();

		// Prevent normal click logic from running
		ci.cancel();
	}
}
