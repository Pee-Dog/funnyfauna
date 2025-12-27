package peedog.funnyfauna.mixin;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.net.packet.PacketCustomPayload;
import net.minecraft.server.entity.player.PlayerServer;
import net.minecraft.server.net.handler.PacketHandlerServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import peedog.funnyfauna.net.FunnyFaunaPackets;

@Mixin(value = PacketHandlerServer.class, remap = false)
public abstract class PacketHandlerServerMixin {

	@Shadow
	private PlayerServer playerEntity;

	@Inject(method = "handleCustomPayload", at = @At("HEAD"), cancellable = true)
	private void funnyfauna$handleSatchelPacket(PacketCustomPayload packet, CallbackInfo ci) {
		if (!"funnyfauna$setItems".equals(packet.channel)) return;

		ci.cancel();

		CompoundTag tag = FunnyFaunaPackets.readTag(packet.data);
		if (tag == null) return;

		ItemStack heldItem = this.playerEntity.inventory
			.getItem(this.playerEntity.inventory.getCurrentItemIndex());

		if (heldItem != null) {
			heldItem.setData(tag);
		}
	}
}
