package peedog.funnyfauna.mixin;

import net.minecraft.client.render.EntityRenderDispatcher;
import net.minecraft.client.render.tessellator.Tessellator;
import net.minecraft.core.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import peedog.funnyfauna.entity.bird.MobBird;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

	@Inject(method = "renderEntityWithPosYaw(Lnet/minecraft/client/render/tessellator/Tessellator;Lnet/minecraft/core/entity/Entity;DDDFF)V",
		at = @At("HEAD"),
		cancellable = true)
	private <T extends Entity> void renderEntityWithPosYaw(Tessellator tess, T entity, double x, double y, double z, float yaw, float partialTick, CallbackInfo ci) {
		// If it's our bird, force render
		if (entity instanceof MobBird) {
			EntityRenderDispatcher self = (EntityRenderDispatcher) (Object) this;
			self.getRenderer(entity).render(tess, (T) entity, x, y, z, yaw, partialTick);
			self.getRenderer(entity).postRender(tess, (T) entity, x, y, z, yaw, partialTick);
			ci.cancel(); // skip the default distance-culling render
		}
	}
}
