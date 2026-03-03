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
import peedog.funnyfauna.entity.scorvid.MobScorvid;

@Environment(EnvType.CLIENT)
public class MobRendererScorvid extends MobRenderer<MobScorvid> {

	public MobRendererScorvid() {
		super(0.5F);
	}

	@Override
	protected @Nullable StaticEntityModel getAndSetupModelForLayer(
		@NonNull MobScorvid entity,
		float brightness,
		float partialTick,
		int layer) {

		boolean isFlying = entity.isFlying();
		StaticEntityModel model = isFlying ? this.getModel("flying") : this.getModel("main");

		model.resetBones();

		float limbSwing = this.getLimbSwing(entity, partialTick);
		float limbYaw = this.getLimbYaw(entity, partialTick);

		/* ---------------- ANIMATION ---------------- */

		if (isFlying) {
			BoneTransform wingL = model.getTransform("wing_l");
			BoneTransform wingR = model.getTransform("wing_r");
			BoneTransform tail = model.getTransform("tail");

			float flap = entity.oFlap + (entity.flap - entity.oFlap) * partialTick;
			float flapAmount = (MathHelper.sin(flap) + 1.0F) * entity.flapSpeed * 0.8F;

			wingL.rotZ = -flapAmount;
			wingR.rotZ = flapAmount;
			tail.rotX = 0.1F;

			if (entity.vehicle == null) {
				GL11.glRotatef((float)(entity.yd * -40.0), -1.0F, 0.0F, 0.0F);
			}
		} else {
			BoneTransform leg1 = model.getTransform("leg1");
			BoneTransform leg2 = model.getTransform("leg2");
			BoneTransform leg3 = model.getTransform("leg3");
			BoneTransform leg4 = model.getTransform("leg4");
			BoneTransform tail = model.getTransform("tail");

			float walkSpeed = 0.6662F;
			float walkDeg = 1.4F;

			leg1.rotX = MathHelper.cos(limbSwing * walkSpeed) * walkDeg * limbYaw;
			leg4.rotX = MathHelper.cos(limbSwing * walkSpeed) * walkDeg * limbYaw;

			leg2.rotX = MathHelper.cos(limbSwing * walkSpeed + (float)Math.PI) * walkDeg * limbYaw;
			leg3.rotX = MathHelper.cos(limbSwing * walkSpeed + (float)Math.PI) * walkDeg * limbYaw;

			tail.rotX = 0.2F + MathHelper.sin(limbSwing * 0.3F) * 0.1F * limbYaw;
		}

		/* ---------------- LAYERS ---------------- */

		if (layer == 0) {
			String texture = isFlying ? "2.png" : "0.png";
			this.bindTexture("/assets/funnyfauna/textures/entity/scorvid/" + texture);

			GL11.glDepthMask(true);
			GL11.glDisable(GL11.GL_BLEND);
			GL11.glEnable(GL11.GL_ALPHA_TEST);

			return model;
		}

		if (layer == 1) {
			if (isFlying) {
				this.bindTexture("/assets/funnyfauna/textures/entity/scorvid/1.png");

				// ✅ FIX: Overlapping geometry needs depth writing to not look inside out!
				GL11.glEnable(GL11.GL_BLEND);
				GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
				GL11.glEnable(GL11.GL_ALPHA_TEST); // Keep enabled to discard invisible pixels
				GL11.glDepthMask(true);            // Write depth to fix the optical illusion
			} else {
				this.bindTexture("/assets/funnyfauna/textures/entity/scorvid/eyes.png");

				// Additive glow pass for eyes (depth mask false is safe here)
				GL11.glEnable(GL11.GL_BLEND);
				GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
				GL11.glDisable(GL11.GL_ALPHA_TEST);
				GL11.glDepthMask(false);
			}

			GL11.glColor4f(1F, 1F, 1F, 1F);
			return model;
		}

		return null;
	}

	@Override
	protected int maxRenderLayer(@NonNull MobScorvid entity) {
		return 1;
	}

	@Override
	public void render(Tessellator tessellator,
					   MobScorvid entity,
					   double x,
					   double y,
					   double z,
					   float yaw,
					   float partialTick) {

		entity.bbWidth = 0.8F;
		entity.bbHeight = 0.8F;

		super.render(tessellator, entity, x, y, z, yaw, partialTick);

		// Restore states to prevent particle leaking
		GL11.glDepthMask(true);
		GL11.glEnable(GL11.GL_ALPHA_TEST);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glColor4f(1F, 1F, 1F, 1F);
	}
}
