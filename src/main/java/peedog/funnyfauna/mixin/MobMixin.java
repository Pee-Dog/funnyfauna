package peedog.funnyfauna.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.Mob;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.util.helper.DamageType;
import net.minecraft.core.world.World;

import peedog.funnyfauna.entity.fox.MobFox;

@Mixin(Mob.class)
public abstract class MobMixin {

	@Inject(
		method = "hurt",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/core/entity/Mob;markHurt()V"
		)
	)
	private void funnyfauna$foxAggroOnPlayerHit(
		Entity attacker,
		int damage,
		DamageType type,
		CallbackInfoReturnable<Boolean> cir
	) {
		// Only react to player attacks
		if (!(attacker instanceof Player)) return;

		Mob victim = (Mob)(Object)this;
		Player player = (Player) attacker;

		if (!victim.isAlive()) return;

		World world = victim.world;
		if (world == null || world.isClientSide) return;

		double radius = 16.0;

		List<MobFox> foxes = world.getEntitiesWithinAABB(
			MobFox.class,
			victim.bb.grow(radius, 8.0, radius)
		);

		for (MobFox fox : foxes) {
			if (!fox.isAlive()) continue;
			if (!fox.isFoxTamed()) continue;
			if (!player.uuid.equals(fox.getFoxOwner())) continue;
			if (fox.isFoxSitting()) continue;

			// Start distraction / aggro
			fox.beginDistraction(victim);
		}
	}
}
