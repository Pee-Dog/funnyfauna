package peedog.funnyfauna.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.tessellator.Tessellator;
import net.minecraft.core.util.helper.MathHelper;
import org.lwjgl.opengl.GL11;
import org.useless.dragonfly.models.entity.BoneTransform;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import org.useless.dragonfly.renderer.MobRenderer;
import peedog.funnyfauna.entity.camel.MobCamel;

@Environment(EnvType.CLIENT)
public class MobRendererCamel extends MobRenderer<MobCamel> {

	public MobRendererCamel() {
		super(0.5F);
	}

	@Override
	protected StaticEntityModel getAndSetupModelForLayer(
		MobCamel entity, float brightness, float partialTick, int layer) {

		StaticEntityModel model = getModel("main");

		/* =====================
		   === TEXTURE LAYERS ===
		   ===================== */

		// Layer 0 — base camel skin
		if (layer == 0) {
			bindTexture("/assets/funnyfauna/textures/entity/camel/skin_"
				+ entity.getSkinVariant() + ".png");
		}

		// Layer 1 — chest(s)
		if (layer == 1 && entity.getChestCount() > 0) {
			if (entity.getChestCount() == 1) {
				bindTexture("/assets/funnyfauna/textures/entity/camel/chest_1.png");
			} else {
				bindTexture("/assets/funnyfauna/textures/entity/camel/chest_2.png");
			}
		}

		// Layer 2 — saddle
		if (layer == 2 && entity.isSaddled()) {
			bindTexture("/assets/funnyfauna/textures/entity/camel/saddle.png");
		}

		model.resetBones();

		/* ======================
		   === ANIMATION SETUP ===
		   ====================== */

		float limbSwing = getLimbSwing(entity, partialTick);
		float limbYaw = getLimbYaw(entity, partialTick);
		float bodyYaw = getBodyYaw(entity, partialTick);
		float headYaw = getHeadYaw(entity, partialTick) - bodyYaw;
		float headPitch = getHeadPitch(entity, partialTick);

		BoneTransform neck  = model.getTransform("neck");
		BoneTransform legFL = model.getTransform("leg4");
		BoneTransform legFR = model.getTransform("leg3");
		BoneTransform legBL = model.getTransform("leg2");
		BoneTransform legBR = model.getTransform("leg1");
		BoneTransform tail  = model.getTransform("tail");

		neck.rotX = headPitch;
		neck.rotY = headYaw;

		legFL.rotX = legBR.rotX =
			MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbYaw;
		legFR.rotX = legBL.rotX =
			MathHelper.cos(limbSwing * 0.6662F + (float)Math.PI) * 1.4F * limbYaw;

		tail.rotX = -0.9F;
		tail.rotY = MathHelper.cos(limbSwing * 0.6662F) * 0.3F;

		return model;
	}

	@Override
	protected int maxRenderLayer(MobCamel entity) {
		int max = 0;

		if (entity.getChestCount() > 0) max = 1;
		if (entity.isSaddled()) max = 2;

		return max;
	}

	@Override
	public void render(Tessellator tessellator, MobCamel entity,
					   double x, double y, double z,
					   float yaw, float partialTick) {

		GL11.glPushMatrix();
		GL11.glTranslated(x, y, z);

		// Baby scaling
		float scale = 1.0f;
		int growthTimer = entity.getGrowthTimer(); // reads from entityData
		if (entity.isBaby()) {
			scale = Math.max(0.5f,
				0.5f + 0.5f * (1 - (growthTimer / 1200f)));
		}

		GL11.glScalef(scale, scale, scale);

		super.render(tessellator, entity, 0, 0, 0, yaw, partialTick);

		GL11.glPopMatrix();
	}
}
