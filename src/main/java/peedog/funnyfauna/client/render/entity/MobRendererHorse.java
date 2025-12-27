package peedog.funnyfauna.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.util.helper.MathHelper;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.useless.dragonfly.models.entity.BoneTransform;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import org.useless.dragonfly.renderer.MobRenderer;
import peedog.funnyfauna.entity.horse.MobHorse;

@Environment(EnvType.CLIENT)
public class MobRendererHorse extends MobRenderer<MobHorse> {
	public MobRendererHorse() {
		super(0.5F);
	}
	@Override
	protected @Nullable StaticEntityModel getAndSetupModelForLayer(@NonNull MobHorse entity, float brightness, float partialTick, int layer) {
		StaticEntityModel model;
		if (layer == 1) {
			this.bindTexture("/assets/funnyfauna/textures/entity/horse/saddle.png");
			model = this.getModel("main");
		} else {
			model = this.getModel("main");
		}

		model.resetBones();
		float limbSwing = this.getLimbSwing(entity, partialTick);
		float limbYaw = this.getLimbYaw(entity, partialTick);
		float limbPitch = this.getLimbPitch(entity, partialTick);
		float bodyYaw = this.getBodyYaw(entity, partialTick);
		float headYaw = this.getHeadYaw(entity, partialTick) - bodyYaw;
		float headPitch = this.getHeadPitch(entity, partialTick);

		BoneTransform head = model.getTransform("head");
		BoneTransform neck = model.getTransform("neck");
		BoneTransform legFrontLeft = model.getTransform("legFrontLeft");
		BoneTransform legFrontRight = model.getTransform("legFrontRight");
		BoneTransform legBackLeft = model.getTransform("legBackLeft");
		BoneTransform legBackRight = model.getTransform("legBackRight");
		BoneTransform tail = model.getTransform("tail");

		head.rotX = headPitch;
		neck.rotX = headPitch;
		head.rotY = headYaw;
		neck.rotY = headYaw;
		legFrontLeft.rotX = legBackRight.rotX = MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbYaw;
		legFrontRight.rotX = legBackLeft.rotX = MathHelper.cos(limbSwing * 0.6662F + 3.141593F) * 1.4F * limbYaw;
		tail.rotX = -0.90F;
		tail.rotY = MathHelper.cos(limbSwing * 0.6662F) * 0.5F * -0.55F;

		return model;
	}

	@Override
	protected int maxRenderLayer(@NonNull MobHorse entity) {
		return entity.getSaddled() ? 1 : 0;
	}
}
