package peedog.funnyfauna.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.net.handler.PacketHandlerClient;
import net.minecraft.core.net.packet.PacketContainerOpen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import peedog.funnyfauna.FunnyFauna;
import peedog.funnyfauna.PlayerInventoryDisplay;
import peedog.funnyfauna.entity.camel.MobCamel;

@Mixin(value= PacketHandlerClient.class,remap = false)
public abstract class PacketHandlerClientMixin {
	@Final
	@Shadow
	private Minecraft mc;

	@Inject(method="handleOpenWindow", at=@At("TAIL"))
	public void handleOpenWindow_injection(final PacketContainerOpen packet, final CallbackInfo ci) {

		if (packet.inventoryType == FunnyFauna.GUI_SATCHEL_ID) {
			((PlayerInventoryDisplay) mc.thePlayer)
				.funnyfauna$displayGUISatchel(mc.thePlayer.getHeldItem());
			mc.thePlayer.craftingInventory.containerId = packet.windowId;
		}

		if (packet.inventoryType == FunnyFauna.GUI_CAMEL_ID) {
			if (mc.thePlayer.passenger instanceof MobCamel) {
				MobCamel camel = (MobCamel) mc.thePlayer.passenger;
				((PlayerInventoryDisplay) mc.thePlayer)
					.funnyfauna$displayGUICamel(camel);
				mc.thePlayer.craftingInventory.containerId = packet.windowId;
			}
		}
	}

}
