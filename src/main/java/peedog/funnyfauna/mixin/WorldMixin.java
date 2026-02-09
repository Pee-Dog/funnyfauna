package peedog.funnyfauna.mixin;

import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import peedog.funnyfauna.entity.lizard.MobLizard;
import peedog.funnyfauna.world.AmbientSpawn;

@Mixin(World.class)
public class WorldMixin {

	@Inject(
		method = "tick",
		at = @At("TAIL")
	)
	private void funnyfauna$tickCrickets(CallbackInfo ci) {
		AmbientSpawn.tick((World)(Object)this);
	}

	@Inject(
		method = "getClosestPlayerToEntity",
		at = @At("RETURN"),
		cancellable = true
	)
	private void funnyfauna$geckoStealth(Entity entity, double radius, CallbackInfoReturnable<Player> cir) {
		Player closest = cir.getReturnValue();
		if (closest == null) return;

		// If closest player is NOT stealthed, do nothing
		if (!(closest.getPassenger() instanceof MobLizard) || !((MobLizard) closest.getPassenger()).isTamed()) {
			return;
		}
		MobLizard gecko = (MobLizard) closest.getPassenger();

		double distToClosest = entity.distanceTo(closest);

		// If within stealth range, allow detection
		if (distToClosest <= 6.0) {
			return;
		}

		// Otherwise, search for another valid player
		World world = (World)(Object)this;
		Player fallback = null;
		double bestDist = Double.MAX_VALUE;

		for (Player player : world.players) {
			if (player == closest) continue;
			if (!player.isAlive()) continue;

			double d = entity.distanceTo(player);
			if (d <= radius && d < bestDist) {
				bestDist = d;
				fallback = player;
			}
		}

		// Use fallback if found, otherwise null
		cir.setReturnValue(fallback);
	}

}
