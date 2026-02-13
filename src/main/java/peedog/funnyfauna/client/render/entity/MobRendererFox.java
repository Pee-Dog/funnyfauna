package peedog.funnyfauna.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.entity.animal.MobSheep;
import net.minecraft.core.util.helper.DyeColor;
import net.minecraft.core.util.helper.MathHelper;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.useless.dragonfly.models.entity.BoneTransform;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import org.useless.dragonfly.renderer.MobRenderer;
import peedog.funnyfauna.entity.fox.MobFox;

@Environment(EnvType.CLIENT)
public class MobRendererFox extends MobRenderer<MobFox> {

	public MobRendererFox() {
		super(0.4F);
	}

	@Override
	protected @Nullable StaticEntityModel getAndSetupModelForLayer(
		@NonNull MobFox entity,
		float brightness,
		float partialTick,
		int layer
	) {

		// ==================================================
		// BASE FOX (layer 0 ONLY)
		// ==================================================
		if (layer == 0) {
			StaticEntityModel model = this.getModel("main");
			model.resetBones();
			this.bindTexture("/assets/funnyfauna/textures/entity/fox/0.png");
			applyFoxAnimation(model, entity, partialTick);
			return model;
		}

		// ==================================================
// ==================================================
		// COLLAR LAYER (layer 1 ONLY)
		if (layer == 1 && entity.isFoxTamed()) {
			StaticEntityModel model = this.getModel("main");
			model.resetBones();

			this.bindTexture("/assets/funnyfauna/textures/entity/fox/collar.png");

			// 1. GET THE COLOR TABLE (Fixes the type error)
			DyeColor collarColor = entity.getCollarColor();
			float[] rgb = MobSheep.FLEECE_COLOR_TABLE[collarColor.blockMeta];

			// 2. CALCULATE BRIGHTNESS
			float finalBrightness = entity.getBrightness(partialTick);

			// 3. APPLY TINT
			float r = rgb[0] * finalBrightness;
			float g = rgb[1] * finalBrightness;
			float b = rgb[2] * finalBrightness;

			GL11.glColor4f(r, g, b, 1.0F);

			// 4. APPLY ANIMATION
			applyFoxAnimation(model, entity, partialTick);

			return model;
		}

		// ==================================================
		// RESET COLOR FOR OTHER ENTITIES
		// ==================================================
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		return null;
	}

	// ==================================================
	// Animation logic (UNCHANGED)
	// ==================================================
	private void applyFoxAnimation(
		StaticEntityModel model,
		MobFox entity,
		float partialTick
	) {
		float limbSwing = this.getLimbSwing(entity, partialTick);
		float limbYaw = this.getLimbYaw(entity, partialTick);
		float bodyYaw = this.getBodyYaw(entity, partialTick);
		float headYaw = this.getHeadYaw(entity, partialTick) - bodyYaw;
		float headPitch = this.getHeadPitch(entity, partialTick);

		BoneTransform head = model.getTransform("head");
		BoneTransform body = model.getTransform("body");
		BoneTransform legFL = model.getTransform("legFrontLeft");
		BoneTransform legFR = model.getTransform("legFrontRight");
		BoneTransform legBL = model.getTransform("legBackLeft");
		BoneTransform legBR = model.getTransform("legBackRight");
		BoneTransform tail = model.getTransform("tail");

		// Walking
		if (legFL != null && legBR != null) {
			legFL.rotX = legBR.rotX =
				MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbYaw;
		}

		if (legFR != null && legBL != null) {
			legFR.rotX = legBL.rotX =
				MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbYaw;
		}

		if (tail != null) {
			tail.rotZ = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbYaw;
		}

		// Sitting
		if (entity.isFoxSitting()) {
			limbSwing = 0.0F;

			if (head != null) {
				head.posY += 1.5F;
				body.posZ -= 1.0F;
			}

			if (body != null) {
				body.posY += -0.5F;
				body.rotX = -0.7853982F;
			}

			if (legBL != null) {
				legBL.posY += -5.5F;
				legBL.posZ -= 2.5F;
				legBL.rotX = (float) Math.PI * 1.5F;
			}
			if (legBR != null) {
				legBR.posY += -5.5F;
				legBR.posZ -= 2.5F;
				legBR.rotX = (float) Math.PI * 1.5F;
			}

			if (tail != null) {
				tail.posY += -5.5F;
				tail.posZ -= 2.0F;
				tail.rotX = 0.9F;
			}
		}

		// Head look
		if (head != null) {
			head.rotX = headPitch;
			head.rotY = headYaw;
		}

		// Tail animation
		if (tail != null) {

			float idleSway =
				MathHelper.cos((entity.tickCount + partialTick) * 0.1F) * 0.05F;

			if (entity.isFoxSitting()) {
				tail.rotX = 0.9F;
			} else {

				float baseRotation;

				if (entity.isFoxTamed()) {
					// Same logic wolves use
					float healthFactor =
						(entity.getMaxHealth() - entity.getHealth()) * 0.04F;

					baseRotation =
						(0.30F - healthFactor) * (float)Math.PI;
				} else {
					// Wild fox neutral tail
					baseRotation = (float)Math.PI / 5F;
				}

				tail.rotX = baseRotation + idleSway;
			}

		}
	}
	@Override
	protected int maxRenderLayer(@NonNull MobFox entity) {
		return entity.isFoxTamed() ? 1 : 0;
	}

}
