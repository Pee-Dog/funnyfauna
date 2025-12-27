package peedog.funnyfauna.mixin.player;

import net.minecraft.core.item.ItemStack;
import net.minecraft.core.net.packet.PacketContainerOpen;
import net.minecraft.server.entity.player.PlayerServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import peedog.funnyfauna.FunnyFauna;
import peedog.funnyfauna.PlayerInventoryDisplay;
import peedog.funnyfauna.gui.ContainerSatchel;
import peedog.funnyfauna.item.ItemSatchel;

@Mixin(value = PlayerServer.class, remap = false)
public class PlayerServerMixin implements PlayerInventoryDisplay {
	@Unique
	private final PlayerServer thisAs = (PlayerServer)(Object)this;
	@Shadow
	private void getNextWindowId() {}
	@Shadow
	private int currentWindowId = 0;
	@Override
	public void funnyfauna$displayGUISatchel(final ItemStack stack) {
		if (stack != null && stack.getItem() instanceof ItemSatchel) {
			getNextWindowId();
			final ContainerSatchel satchel = new ContainerSatchel(this.thisAs.inventory, stack);
			this.thisAs.playerNetServerHandler.sendPacket(new PacketContainerOpen(this.currentWindowId, FunnyFauna.GUI_SATCHEL_ID, "Satchel", satchel.inventorySatchel.getContainerSize()));
			this.thisAs.craftingInventory = satchel;
			this.thisAs.craftingInventory.containerId = this.currentWindowId;
			this.thisAs.craftingInventory.addSlotListener(this.thisAs);
		}
	}
}
