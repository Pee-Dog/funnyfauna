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
import peedog.funnyfauna.entity.moth.MobMoth;

@Environment(EnvType.CLIENT)
public class MobRendererMoth extends MobRenderer<MobMoth> {

	public MobRendererMoth() {
		super(0.3F);
	}

	@Override
	protected @Nullable StaticEntityModel getAndSetupModelForLayer(
		@NonNull MobMoth entity, float brightness, float partialTick, int layer) {

		StaticEntityModel model = this.getModel("main");

		// Bind texture (flying vs folded handled in entity)
		this.bindTexture(entity.getEntityTexture());

		model.resetBones();

		// Animation calculations
		float limbSwing = this.getLimbSwing(entity, partialTick);
		float limbYaw = this.getLimbYaw(entity, partialTick);
		float bodyYaw = this.getBodyYaw(entity, partialTick);
		float headYaw = this.getHeadYaw(entity, partialTick) - bodyYaw;
		float headPitch = this.getHeadPitch(entity, partialTick);

		// Bones
		BoneTransform wingLeft = model.getTransform("wing_l");
		BoneTransform wingRight = model.getTransform("wing_r");

		// Wings flap only when flying
		if (entity.isFlying()) {

			float flap = entity.oFlap + (entity.flap - entity.oFlap) * partialTick;
			float flapAmount = (MathHelper.sin(flap) + 1.0F) * entity.flapSpeed * 12.0F;

			wingLeft.rotZ = -flapAmount;
			wingRight.rotZ = flapAmount;

		} else {
			wingLeft.rotX = wingLeft.rotZ = 0;
			wingRight.rotX = wingRight.rotZ = 0;
		}

		// Slight forward tilt while flying
		if (entity.isFlying() && entity.vehicle == null) {
			GL11.glRotatef((float)(entity.yd * -50.0), -1.0F, 0.0F, 0.0F);
		}

		return model;
	}

	@Override
	protected int maxRenderLayer(@NonNull MobMoth entity) {
		return 0;
	}

	@Override
	public void render(@NonNull Tessellator tessellator, @NonNull MobMoth entity,
					   double x, double y, double z, float yaw, float partialTick) {

		GL11.glPushMatrix();
		GL11.glTranslated(x, y + 0.05, z);

		// Ensure minimum bounding box for culling
		entity.bbWidth = Math.max(entity.bbWidth, 0.4F);
		entity.bbHeight = Math.max(entity.bbHeight, 0.4F);

		GL11.glScalef(0.8F, 0.8F, 0.8F);

		// =============================
		// WALL ROTATION
		// =============================

		if (!entity.isFlying()) {
			if (entity.getWallDirection() != MobMoth.WallDirection.NONE) {
				// Wall landing
				switch (entity.getWallDirection()) {
					case NORTH: GL11.glRotatef(180F, 0F, 1F, 0F); break;
					case WEST: GL11.glRotatef(90F, 0F, 1F, 0F); break;
					case EAST: GL11.glRotatef(-90F, 0F, 1F, 0F); break;
				}
				GL11.glRotatef(90F, 1F, 0F, 0F);
			} else {
				// Ground landing → no tilt
			}
		}

		super.render(tessellator, entity, 0, 0, 0, yaw, partialTick);
		GL11.glPopMatrix();
	}

	@Override
	public void postRender(@NonNull Tessellator tessellator, @NonNull MobMoth entity,
						   double x, double y, double z, float yaw, float partialTick) {
		// Optional shadow/fire handling
	}
}
