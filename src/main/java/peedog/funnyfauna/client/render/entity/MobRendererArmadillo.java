package peedog.funnyfauna.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.util.helper.MathHelper;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.useless.dragonfly.models.entity.BoneTransform;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import org.useless.dragonfly.renderer.MobRenderer;
import peedog.funnyfauna.entity.armadillo.MobArmadillo;
import peedog.funnyfauna.entity.horse.MobHorse;

@Environment(EnvType.CLIENT)
public class MobRendererArmadillo extends MobRenderer<MobArmadillo> {
	public MobRendererArmadillo() {
		super(0.5F);
	}
	@Override
	protected @Nullable StaticEntityModel getAndSetupModelForLayer(@NonNull MobArmadillo entity, float brightness, float partialTick, int layer) {
		StaticEntityModel model;
		model = this.getModel("main");

		model.resetBones();
		float limbSwing = this.getLimbSwing(entity, partialTick);
		float limbYaw = this.getLimbYaw(entity, partialTick);
		float limbPitch = this.getLimbPitch(entity, partialTick);
		float bodyYaw = this.getBodyYaw(entity, partialTick);
		float headYaw = this.getHeadYaw(entity, partialTick) - bodyYaw;
		float headPitch = this.getHeadPitch(entity, partialTick);

		BoneTransform head = model.getTransform("head");
		BoneTransform legFrontLeft = model.getTransform("leg1");
		BoneTransform legFrontRight = model.getTransform("leg2");
		BoneTransform legBackLeft = model.getTransform("leg3");
		BoneTransform legBackRight = model.getTransform("leg4");
		BoneTransform tail = model.getTransform("tail");

		head.rotX = headPitch;
		head.rotY = headYaw;
		legFrontLeft.rotX = legBackRight.rotX = MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbYaw;
		legFrontRight.rotX = legBackLeft.rotX = MathHelper.cos(limbSwing * 0.6662F + 3.141593F) * 1.4F * limbYaw;

		return model;
	}

}
