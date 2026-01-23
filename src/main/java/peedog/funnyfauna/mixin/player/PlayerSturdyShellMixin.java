package peedog.funnyfauna.mixin.player;

import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.Mob;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.util.helper.DamageType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import peedog.funnyfauna.item.ItemSturdyShell;

@Mixin(Player.class)
public class PlayerSturdyShellMixin {

	// ----------------------------
	// Incoming damage reduction
	// ----------------------------
	@ModifyVariable(
		method = "damageEntity",
		at = @At("HEAD"),
		argsOnly = true
	)
	private int funnyfauna$reduceIncomingDamage(int damage) {
		Player player = (Player) (Object) this;

		if (!player.isSneaking()) return damage;

		for (int i = 0; i < player.inventory.getContainerSize(); i++) {
			ItemStack stack = player.inventory.getItem(i);
			if (stack == null) continue;
			if (!(stack.getItem() instanceof ItemSturdyShell)) continue;
			if (!ItemSturdyShell.isEnabled(stack)) continue;

			// Reduce damage by 75%
			int reduced = Math.max(1, damage / 4);
			int absorbed = damage - reduced;

			System.out.println("[FunnyFauna] Incoming damage absorbed by Sturdy Shell: " + absorbed);

			// Damage the shell
			stack.damageItem(absorbed, player);

			// Break shell if max damage exceeded
			if (stack.getMetadata() >= stack.getMaxDamage()) {
				player.inventory.setItem(i, null);
			}

			return reduced;
		}

		return damage;
	}

	// ----------------------------
	// Outgoing damage reduction
	// ----------------------------
	@Inject(
		method = "attackTargetEntityWithCurrentItem",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/core/entity/Entity;hurt(Lnet/minecraft/core/entity/Entity;ILnet/minecraft/core/util/helper/DamageType;)Z"
		),
		cancellable = true
	)
	private void funnyfauna$reduceOutgoingDamage(Entity target, CallbackInfo ci) {
		Player player = (Player) (Object) this;

		if (!player.isSneaking()) return;

		// Get original damage
		int damage = player.inventory.getDamageVsEntity(target);

		if (damage <= 0) return;

		// Reduce damage by 75%
		int reduced = Math.max(1, damage / 4);

		System.out.println("[FunnyFauna] Outgoing damage reduced by Sturdy Shell: " + reduced);

		// Apply reduced damage
		target.hurt(player, reduced, DamageType.COMBAT);

		// Cancel original method to prevent double damage
		ci.cancel();
	}
}
