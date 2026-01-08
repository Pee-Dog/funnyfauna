package peedog.funnyfauna.mixin;

import net.minecraft.core.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
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
}
