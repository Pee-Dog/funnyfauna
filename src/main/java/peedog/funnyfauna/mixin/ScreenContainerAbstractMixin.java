package peedog.funnyfauna.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.container.ScreenContainerAbstract;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.player.inventory.container.ContainerInventory;
import net.minecraft.core.player.inventory.slot.Slot;
import peedog.funnyfauna.item.ItemScales;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenContainerAbstract.class)
public abstract class ScreenContainerAbstractMixin {

	private static final int REPAIR_AMOUNT = 5;

	@Inject(method = "clickInventory", at = @At("HEAD"), cancellable = true)
	private void funnyfauna$repairWithScales(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
		// Right-click only
		if (mouseButton != 1) return;

		Minecraft mc = Minecraft.getMinecraft();
		ContainerInventory inventory = mc.thePlayer.inventory;

		ItemStack held = inventory.getHeldItemStack();
		if (held == null || !(held.getItem() instanceof ItemScales)) return;

		ScreenContainerAbstract screen = (ScreenContainerAbstract)(Object)this;
		Slot slot = screen.getSlotAtPosition(mouseX, mouseY);
		if (slot == null) return;

		ItemStack target = slot.getItemStack();
		if (target == null) return;

		if (!target.isItemStackDamageable()) return;
		if (target.getMetadata() <= 0) return;

		// Repair item
		target.setMetadata(Math.max(0, target.getMetadata() - REPAIR_AMOUNT));

		// Consume scale
		held.stackSize--;
		if (held.stackSize <= 0) {
			inventory.setHeldItemStack(null);
		}

		inventory.setChanged();

		// Stop normal click behavior
		ci.cancel();
	}
}
