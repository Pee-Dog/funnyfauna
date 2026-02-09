package peedog.funnyfauna.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.container.ScreenContainerAbstract;
import net.minecraft.core.item.ItemArmor;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.player.inventory.container.ContainerInventory;
import net.minecraft.core.player.inventory.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import peedog.funnyfauna.gui.BundleContents;
import peedog.funnyfauna.gui.ContainerSatchel;
import peedog.funnyfauna.gui.GuiSatchel;
import peedog.funnyfauna.item.ItemScales;
import peedog.funnyfauna.item.ItemReinforcedScales;
import peedog.funnyfauna.item.ItemSatchel;
import peedog.funnyfauna.gui.InventorySatchel;

@Mixin(ScreenContainerAbstract.class)
public abstract class ScreenContainerAbstractMixin {

	private static final int REPAIR_AMOUNT = 5;
	private static final int REINFORCED_REPAIR_AMOUNT = 20;

	@Inject(method = "clickInventory", at = @At("HEAD"), cancellable = true)
	private void funnyfauna$rightClickHandlers(
		int mouseX, int mouseY, int mouseButton, CallbackInfo ci
	) {
		// Right-click only
		if (mouseButton != 1) return;

		Minecraft mc = Minecraft.getMinecraft();
		ContainerInventory inventory = mc.thePlayer.inventory;
		ItemStack cursor = inventory.getHeldItemStack();

		ScreenContainerAbstract screen = (ScreenContainerAbstract)(Object)this;
		Slot slot = screen.getSlotAtPosition(mouseX, mouseY);
		if (slot == null) return;

		ItemStack target = slot.getItemStack();
		if (target == null) return;

		/* ============================================================
		 * 1. SCALES REPAIR LOGIC
		 * ============================================================ */
		if (cursor != null && cursor.getItem() instanceof ItemScales) {
			if (!target.isItemStackDamageable() || target.getMetadata() <= 0) return;

			int repairAmount = REPAIR_AMOUNT;
			if (cursor.getItem() instanceof ItemReinforcedScales) {
				repairAmount = REINFORCED_REPAIR_AMOUNT;
			} else {
				if (target.getItem() instanceof ItemArmor) return;
			}

			target.setMetadata(Math.max(0, target.getMetadata() - repairAmount));
			cursor.stackSize--;
			if (cursor.stackSize <= 0) {
				inventory.setHeldItemStack(null);
			}

			inventory.setChanged();
			ci.cancel();
			return;
		}

		/* ============================================================
		 * 2. ITEM INTO SATCHEL LOGIC (Right-clicking items INTO a Satchel)
		 * ============================================================ */
		if (target != null && target.getItem() instanceof ItemSatchel) {
			ItemStack satchel = target;
			BundleContents contents;
			ContainerSatchel openContainer = null;

			// Check if we are currently looking at a satchel GUI
			if (mc.thePlayer.inventorySlots instanceof ContainerSatchel) {
				ContainerSatchel container = (ContainerSatchel) mc.thePlayer.inventorySlots;
				ItemStack openStack = container.inventorySatchel.stack;

				// REJECTION LOGIC:
				// If this is the satchel currently open, we do NOT want the mixin to handle the click.
				// This prevents the "desync" because the GUI will handle the click naturally.
				if (openStack == satchel || (openStack.isItemEqual(satchel) && ItemStack.areItemStacksEqual(openStack, satchel))) {
					// We return to let the GUI handle the click on its own icon/slot
					return;
				}

				// If we got here, a satchel GUI is open, but we clicked a DIFFERENT satchel.
				// (Though unlikely to be 'openContainer' based on the check above, we keep the detection structure)
			}

			// LOGIC: If cursor has item, add it. If cursor is empty, open GUI.
			if (cursor != null) {
				// Standard bundle insertion for a satchel that is NOT the currently open one
				contents = ItemSatchel.getContents(satchel);

				int added = contents.add(cursor);
				if (added > 0) {
					cursor.stackSize -= added;
					inventory.setHeldItemStack(cursor.stackSize <= 0 ? null : cursor);

					// Since we've already verified this isn't the openContainer above,
					// we update the NBT directly.
					ItemSatchel.saveContents(satchel, contents);
					ItemSatchel.updateDurability(satchel);

					inventory.setChanged();
					ci.cancel();
				}
				return;
			} else {
				// Empty cursor: Open GUI (Since we checked above, we know this satchel isn't already open)
				mc.displayScreen(new GuiSatchel(mc.thePlayer, satchel));
				ci.cancel();
				return;
			}
		}

		/* ============================================================
		 * 3. REVERSE SATCHEL LOGIC (Holding Satchel, clicking items)
		 * ============================================================ */
		if (cursor != null && cursor.getItem() instanceof ItemSatchel && target != null) {
			// Prevent nesting
			if (target.getItem() instanceof ItemSatchel) return;

			ItemStack satchel = cursor;
			BundleContents contents;

			// REJECTION LOGIC:
			// If the satchel on the cursor is the one currently open, block the mixin logic.
			if (mc.thePlayer.inventorySlots instanceof ContainerSatchel) {
				ContainerSatchel container = (ContainerSatchel) mc.thePlayer.inventorySlots;
				ItemStack openStack = container.inventorySatchel.stack;

				if (openStack == satchel || (openStack.isItemEqual(satchel) && ItemStack.areItemStacksEqual(openStack, satchel))) {
					return;
				}
			}

			// If we reach here, we are holding a satchel and clicking an item,
			// and that satchel is NOT the one currently open in the GUI.
			contents = ItemSatchel.getContents(satchel);

			int added = contents.add(target);
			if (added > 0) {
				target.stackSize -= added;
				slot.set(target.stackSize <= 0 ? null : target);

				// Update the held item's NBT
				ItemSatchel.saveContents(satchel, contents);
				ItemSatchel.updateDurability(satchel);

				inventory.setChanged();
				ci.cancel();
				return;
			}
		}
	}
}
