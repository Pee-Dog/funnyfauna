package peedog.funnyfauna.mixin.player;

import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import peedog.funnyfauna.item.ItemToggleable;

@Mixin(Player.class)
public abstract class PlayerDropMixin {

	@Inject(
		method = "dropPlayerItemWithRandomChoice",
		at = @At("RETURN") // inject after the method finishes
	)
	private void onDropPlayerItem(ItemStack itemstack, boolean flag, CallbackInfo ci) {
		if (itemstack != null && itemstack.getItem() instanceof ItemToggleable) {
			ItemToggleable.disableStack(itemstack, (Player) (Object) this);
		}
	}
}
