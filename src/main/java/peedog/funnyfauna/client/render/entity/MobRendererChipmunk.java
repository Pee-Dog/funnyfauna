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
import peedog.funnyfauna.entity.chipmunk.MobChipmunk;

@Environment(EnvType.CLIENT)
public class MobRendererChipmunk extends MobRenderer<MobChipmunk> {

	public MobRendererChipmunk() {
		super(0.3F);
	}

	@Override
	protected @Nullable StaticEntityModel getAndSetupModelForLayer(@NonNull MobChipmunk entity, float brightness, float partialTick, int layer) {
		StaticEntityModel model = this.getModel("main");
		model.resetBones();

		BoneTransform head = model.getTransform("head");
		BoneTransform body = model.getTransform("body");
		BoneTransform legFrontLeft = model.getTransform("leg2");
		BoneTransform legFrontRight = model.getTransform("leg1");
		BoneTransform legBackLeft = model.getTransform("leg4");
		BoneTransform legBackRight = model.getTransform("leg3");

		float bodyYaw = this.getBodyYaw(entity, partialTick);
		float headYaw = this.getHeadYaw(entity, partialTick) - bodyYaw;
		float headPitch = this.getHeadPitch(entity, partialTick);
		float limbSwing = this.getLimbSwing(entity, partialTick);
		float limbYaw = this.getLimbYaw(entity, partialTick);
		head.rotX = headPitch;
		head.rotY = headYaw;

		if (!entity.onGround && entity.vehicle == null) {
			GL11.glRotatef((float) (entity.yd * -50.0), -1.0F, 0.0F, 0.0F);
		}
		legFrontLeft.rotX = legBackRight.rotX = MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbYaw;
		legFrontRight.rotX = legBackLeft.rotX = MathHelper.cos(limbSwing * 0.6662F + 3.141593F) * 1.4F * limbYaw;

		return model;
	}


}
