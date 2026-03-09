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
import peedog.funnyfauna.entity.sasquatch.MobSasquatch;

@Environment(EnvType.CLIENT)
public class MobRendererSasquatch extends MobRenderer<MobSasquatch> {

	public MobRendererSasquatch() {
		super(1.0F); // shadow radius
	}

	@Override
	protected @Nullable StaticEntityModel getAndSetupModelForLayer(
		@NonNull MobSasquatch entity, float brightness, float partialTick, int layer) {

		StaticEntityModel model = this.getModel("main");
		model.resetBones();

		float limbSwing  = this.getLimbSwing(entity, partialTick);
		float limbYaw    = this.getLimbYaw(entity, partialTick);
		float bodyYaw    = this.getBodyYaw(entity, partialTick);
		float headYaw    = this.getHeadYaw(entity, partialTick) - bodyYaw;
		float headPitch  = this.getHeadPitch(entity, partialTick);

		BoneTransform head     = model.getTransform("head");
		BoneTransform leftArm  = model.getTransform("left_arm");
		BoneTransform rightArm = model.getTransform("right_arm");
		BoneTransform leftLeg  = model.getTransform("left_leg");
		BoneTransform rightLeg = model.getTransform("right_leg");

		// Head look
		head.rotX = headPitch;
		head.rotY = headYaw;

		// Leg swing — standard biped formula
		leftLeg.rotX  =  MathHelper.cos(limbSwing * 0.6662F)                    * 1.4F * limbYaw;
		rightLeg.rotX =  MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI)  * 1.4F * limbYaw;

		// Arm swing — opposite phase to legs, like a player
		leftArm.rotX  =  MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI)  * 1.0F * limbYaw;
		rightArm.rotX =  MathHelper.cos(limbSwing * 0.6662F)                    * 1.0F * limbYaw;

		return model;
	}

	@Override
	protected int maxRenderLayer(@NonNull MobSasquatch entity) {
		return 0;
	}

	@Override
	public void render(@NonNull Tessellator tessellator, @NonNull MobSasquatch entity,
					   double x, double y, double z, float yaw, float partialTick) {

		GL11.glPushMatrix();
		GL11.glTranslated(x, y, z);

		entity.bbWidth  = Math.max(entity.bbWidth,  0.5F);
		entity.bbHeight = Math.max(entity.bbHeight, 0.5F);

		float alpha = entity.alpha;
		boolean needsBlend = alpha < 1.0F;

		if (needsBlend) {
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			GL11.glDepthMask(false);
			GL11.glColor4f(1.0F, 1.0F, 1.0F, alpha);
		}

		super.render(tessellator, entity, 0, 0, 0, yaw, partialTick);

		if (needsBlend) {
			GL11.glDepthMask(true);
			GL11.glDisable(GL11.GL_BLEND);
			GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F); // restore full opacity for other renders
		}

		GL11.glPopMatrix();
	}

	@Override
	public void postRender(@NonNull Tessellator tessellator, @NonNull MobSasquatch entity,
						   double x, double y, double z, float yaw, float partialTick) {
	}
}
