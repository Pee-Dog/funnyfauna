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
import peedog.funnyfauna.entity.duck.MobDuck;

@Environment(EnvType.CLIENT)
public class MobRendererDuck extends MobRenderer<MobDuck> {

	public MobRendererDuck() {
		super(0.5F);
	}

	@Override
	protected @Nullable StaticEntityModel getAndSetupModelForLayer(
		@NonNull MobDuck entity, float brightness, float partialTick, int layer) {

		StaticEntityModel model = this.getModel("main");

		model.resetBones();

		// Animation calculations
		float limbSwing = this.getLimbSwing(entity, partialTick);
		float limbYaw = this.getLimbYaw(entity, partialTick);
		float bodyYaw = this.getBodyYaw(entity, partialTick);
		float headYaw = this.getHeadYaw(entity, partialTick) - bodyYaw;
		float headPitch = this.getHeadPitch(entity, partialTick);

		// Get bones
		BoneTransform head = model.getTransform("head");
		BoneTransform tail = model.getTransform("tail");
		BoneTransform legLeft = model.getTransform("left_leg");
		BoneTransform legRight = model.getTransform("right_leg");
		BoneTransform wingLeft = model.getTransform("left_wing");
		BoneTransform wingRight = model.getTransform("right_wing");

		// Head and tail rotation
		head.rotX = headPitch;
		head.rotY = headYaw;
		tail.rotX = 0.25F;

		// Leg animation
		legLeft.rotX = MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbYaw;
		legRight.rotX = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbYaw;

		// Wing flapping (airborne only)
		if (!(entity.onGround || entity.isInWater())) {
			float flap = entity.oFlap + (entity.flap - entity.oFlap) * partialTick;
			float flapAmount = (MathHelper.sin(flap) + 1.0F) * entity.flapSpeed * 10.0F;

			wingLeft.rotZ = -flapAmount;
			wingRight.rotZ = flapAmount;
		} else {
			wingLeft.rotX = wingLeft.rotZ = 0;
			wingRight.rotX = wingRight.rotZ = 0;
		}
		if (!entity.onGround && entity.vehicle == null) {
			GL11.glRotatef((float) (entity.yd * -50.0), -1.0F, 0.0F, 0.0F);
		}

		return model;
	}


	@Override
	protected int maxRenderLayer(@NonNull MobDuck entity) {
		return 0; // no extra layers
	}

	@Override
	public void render(@NonNull Tessellator tessellator, @NonNull MobDuck entity,
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
	public void postRender(@NonNull Tessellator tessellator, @NonNull MobDuck entity,
						   double x, double y, double z, float yaw, float partialTick) {
		// Shadow/fire code if needed
	}
}
