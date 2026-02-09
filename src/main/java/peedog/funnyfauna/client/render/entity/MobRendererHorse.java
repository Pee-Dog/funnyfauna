package peedog.funnyfauna.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.tessellator.Tessellator;
import net.minecraft.core.util.helper.MathHelper;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.useless.dragonfly.models.entity.BoneTransform;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import org.useless.dragonfly.renderer.MobRenderer;
import org.lwjgl.opengl.GL11;
import peedog.funnyfauna.entity.horse.MobHorse;

@Environment(EnvType.CLIENT)
public class MobRendererHorse extends MobRenderer<MobHorse> {
	public MobRendererHorse() {
		super(0.5F);
	}

	@Override
	protected @Nullable StaticEntityModel getAndSetupModelForLayer(@NonNull MobHorse entity, float brightness, float partialTick, int layer) {
		StaticEntityModel model = this.getModel("main");

		// --- Bind texture based on layer ---
		if (layer == 0) {
			this.bindTexture("/assets/funnyfauna/textures/entity/horse/skin_" + entity.getSkinVariant() + ".png");
		} else if (layer == 1 && entity.getBodyVariant() != -1) {
			this.bindTexture("/assets/funnyfauna/textures/entity/horse/body_" + entity.getBodyVariant() + ".png");
		} else if (layer == 2 && entity.getLegsVariant() != -1) {
			this.bindTexture("/assets/funnyfauna/textures/entity/horse/legs_" + entity.getLegsVariant() + ".png");
		} else if (layer == 3 && entity.getSaddled()) {
			this.bindTexture("/assets/funnyfauna/textures/entity/horse/saddle.png");
		}

		// --- Reset bones & apply animation ---
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
		int layers = 0;
		if (entity.getBodyVariant() != -1) layers = Math.max(layers, 1);
		if (entity.getLegsVariant() != -1) layers = Math.max(layers, 2);
		if (entity.getSaddled()) layers = Math.max(layers, 3);
		return layers;
	}

	@Override
	public void render(@NonNull Tessellator tessellator, @NonNull MobHorse entity, double x, double y, double z, float yaw, float partialTick) {
		// --- Apply baby scaling before rendering layers ---
		GL11.glPushMatrix();
		GL11.glTranslated(x, y, z);

		float scale = 1.0f;
		if (entity.isBaby()){
			scale = Math.max(0.5f, 0.5f + 0.5f * (1 - (entity.getGrowthTimer() / 1200f)));
		}
		GL11.glScalef(scale, scale, scale);

		super.render(tessellator, entity, 0, 0, 0, yaw, partialTick);

		GL11.glPopMatrix();
	}
}
