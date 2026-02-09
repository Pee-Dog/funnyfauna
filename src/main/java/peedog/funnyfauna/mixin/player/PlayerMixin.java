package peedog.funnyfauna.mixin.player;

import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.Mob;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.entity.projectile.ProjectileArrow;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.util.helper.DamageType;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import peedog.funnyfauna.PlayerInventoryDisplay;
import peedog.funnyfauna.entity.camel.MobCamel;
import peedog.funnyfauna.entity.fox.FoxDistractionHandler;
import peedog.funnyfauna.item.ItemClimbingClaws;

@Mixin(value = Player.class, remap = false)
public class PlayerMixin implements PlayerInventoryDisplay {

	/* ----------------------------
	   Equipped slot state
	   ---------------------------- */

	@Unique
	private ItemStack funnyfauna$equippedSlot = null;

	@Unique
	private boolean funnyfauna$equippedEnabled = false;

	/* ----------------------------
	   Interface: state access
	   ---------------------------- */

	@Override
	public ItemStack funnyfauna$getEquippedSlot() {
		return funnyfauna$equippedSlot;
	}

	@Override
	public boolean funnyfauna$isEquippedEnabled() {
		return funnyfauna$equippedEnabled;
	}

	@Override
	public void funnyfauna$setEquippedSlot(ItemStack stack) {
		this.funnyfauna$equippedSlot = stack;
	}

	@Override
	public void funnyfauna$setEquippedEnabled(boolean enabled) {
		this.funnyfauna$equippedEnabled = enabled;
	}

	/* ----------------------------
	   GUI methods (NO-OP here)
	   ---------------------------- */

	@Override
	public void funnyfauna$displayGUISatchel(ItemStack stack) {
		// intentionally empty
	}

	@Override
	public void funnyfauna$displayGUICamel(MobCamel camel) {
		// intentionally empty
	}
	@Inject(method = "hurt", at = @At("RETURN"))
	private void funnyfauna$onPlayerHurt(Entity attacker, int damage, DamageType type, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValue()) return;
		if (attacker == null) return;
		if (((Player)(Object)this).world.isClientSide) return;

		Player player = (Player)(Object)this;
		Entity blamed = attacker;

		if (attacker instanceof ProjectileArrow && ((ProjectileArrow) attacker).owner != null) {
			ProjectileArrow arrow = (ProjectileArrow) attacker;
			blamed = arrow.owner;
		}

		if (!(blamed instanceof Mob)) return;

		FoxDistractionHandler.alertFoxes(player, (Mob)blamed);
	}

}
