package peedog.funnyfauna.mixin.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.controller.PlayerController;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import peedog.funnyfauna.item.FunnyFaunaItems;
import peedog.funnyfauna.item.ItemArmExtension;
import peedog.funnyfauna.item.ItemToggleable;

@Mixin(PlayerController.class)
public abstract class PlayerControllerMixin {

	@Shadow
	protected Minecraft mc;

	private static final float REACH_BONUS = 3.0F;

    /* ----------------------------
       Block reach
       ---------------------------- */

	@Inject(
		method = "getBlockReachDistance",
		at = @At("RETURN"),
		cancellable = true
	)
	private void armExtension_blockReach(CallbackInfoReturnable<Float> cir) {
		if (hasActiveArmExtension()) {
			cir.setReturnValue(cir.getReturnValue() + REACH_BONUS);
		}
	}

    /* ----------------------------
       Entity reach
       ---------------------------- */

	@Inject(
		method = "getEntityReachDistance",
		at = @At("RETURN"),
		cancellable = true
	)
	private void armExtension_entityReach(CallbackInfoReturnable<Float> cir) {
		if (hasActiveArmExtension()) {
			cir.setReturnValue(cir.getReturnValue() + REACH_BONUS);
		}
	}

    /* ----------------------------
       Inventory scan
       ---------------------------- */

	private boolean hasActiveArmExtension() {
		Player player = mc.thePlayer;
		if (player == null) return false;

		for (ItemStack stack : player.inventory.mainInventory) {
			if (stack != null
				&& stack.getItem() instanceof ItemArmExtension
				&& ItemToggleable.isToggled(stack)) {
				return true;
			}
		}

		return false;
	}

}
