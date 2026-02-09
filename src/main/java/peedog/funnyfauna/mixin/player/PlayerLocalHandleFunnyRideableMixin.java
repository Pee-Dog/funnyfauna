package peedog.funnyfauna.mixin.player;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.entity.player.PlayerLocal;
import net.minecraft.client.input.PlayerInput;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import peedog.funnyfauna.entity.FunnyRideable;

@Environment(EnvType.CLIENT)
@Mixin(value = PlayerLocal.class, remap = false)
public abstract class PlayerLocalHandleFunnyRideableMixin extends Player {
	protected PlayerLocalHandleFunnyRideableMixin(World world) {
		super(world);
	}

	@Shadow
	public PlayerInput input;

	@Inject(method = "handleSpecialVehicleControl", at = @At("HEAD"))
	private void handleFunnyRideableControl(CallbackInfo ci) {
		// Only control the vehicle if it exists and is a FunnyRideable
		if (vehicle != null && vehicle instanceof FunnyRideable) {
			((FunnyRideable) vehicle).controlEntity(
				input.moveForward,
				input.moveStrafe,
				isJumping,
				xRot,
				yRot
			);
		}
		// If somehow the passenger itself is a FunnyRideable (rare case)
		else if (passenger != null && passenger instanceof FunnyRideable) {
			((FunnyRideable) passenger).controlEntity(
				input.moveForward,
				input.moveStrafe,
				isJumping,
				xRot,
				yRot
			);
		}
	}
}
