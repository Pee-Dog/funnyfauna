package peedog.funnyfauna.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.util.helper.MathHelper;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.useless.dragonfly.models.entity.BoneTransform;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import org.useless.dragonfly.renderer.MobRenderer;
import peedog.funnyfauna.entity.bunny.MobBunny;

@Environment(EnvType.CLIENT)
public class MobRendererBunny extends MobRenderer<MobBunny> {

	public MobRendererBunny() {
		super(0.3F);
	}

	@Override
	protected @Nullable StaticEntityModel getAndSetupModelForLayer(@NonNull MobBunny entity, float brightness, float partialTick, int layer) {
		StaticEntityModel model = this.getModel("main");
		model.resetBones();

		BoneTransform head = model.getTransform("head");
		BoneTransform body = model.getTransform("body");

		float bodyYaw = this.getBodyYaw(entity, partialTick);
		float headYaw = this.getHeadYaw(entity, partialTick) - bodyYaw;
		float headPitch = this.getHeadPitch(entity, partialTick);
		head.rotX = headPitch;
		head.rotY = headYaw;

		if (!entity.onGround && entity.vehicle == null) {
			if (entity.yd > 0.5) {
				GL11.glRotatef(15.0F, -1.0F, 0.0F, 0.0F);
			} else if (entity.yd < -0.5) {
				GL11.glRotatef(-15.0F, -1.0F, 0.0F, 0.0F);
			} else {
				GL11.glRotatef((float) (entity.yd * 30.0), -1.0F, 0.0F, 0.0F);
			}
		}

		return model;
	}


}
