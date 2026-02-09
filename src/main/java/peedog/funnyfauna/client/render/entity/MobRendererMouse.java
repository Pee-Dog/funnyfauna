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
import peedog.funnyfauna.entity.bird.MobBird;
import peedog.funnyfauna.entity.mouse.MobMouse;

@Environment(EnvType.CLIENT)
public class MobRendererMouse extends MobRenderer<MobMouse> {

	public MobRendererMouse() {
		super(0.5F);
	}

	@Override
	protected @Nullable StaticEntityModel getAndSetupModelForLayer(
		@NonNull MobMouse entity, float brightness, float partialTick, int layer) {

		StaticEntityModel model = this.getModel("main");

		// Bind the skin texture based on skinVariant
		this.bindTexture(entity.getEntityTexture());

		model.resetBones();

		// Animation calculations
		float limbSwing = this.getLimbSwing(entity, partialTick);
		float limbYaw = this.getLimbYaw(entity, partialTick);
		float bodyYaw = this.getBodyYaw(entity, partialTick);
		float headYaw = this.getHeadYaw(entity, partialTick) - bodyYaw;
		float headPitch = this.getHeadPitch(entity, partialTick);

		BoneTransform tail = model.getTransform("tail");
		BoneTransform legLeft = model.getTransform("legLeft");
		BoneTransform legRight = model.getTransform("legRight");
		BoneTransform legLeft2 = model.getTransform("legLeft2");
		BoneTransform legRight2 = model.getTransform("legRight2");

		tail.rotX = -0.25F;

		// Leg animation
		legLeft.rotX = legRight2.rotX = MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbYaw;
		legRight.rotX = legLeft2.rotX = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbYaw;

		return model;
	}

	@Override
	public void render(@NonNull Tessellator tessellator, @NonNull MobMouse entity,
					   double x, double y, double z, float yaw, float partialTick) {

		GL11.glPushMatrix();
		GL11.glTranslated(x, y, z);

		// Ensure minimum bounding box for culling
		entity.bbWidth = Math.max(entity.bbWidth, 0.5F);
		entity.bbHeight = Math.max(entity.bbHeight, 0.5F);

		// Scale (optional, keep at 1.0)
		GL11.glScalef(1.0F, 1.0F, 1.0F);

		super.render(tessellator, entity, 0, 0, 0, yaw, partialTick);
		GL11.glPopMatrix();
	}

	@Override
	public void postRender(@NonNull Tessellator tessellator, @NonNull MobMouse entity,
						   double x, double y, double z, float yaw, float partialTick) {
		// Shadow/fire code if needed
	}
}
