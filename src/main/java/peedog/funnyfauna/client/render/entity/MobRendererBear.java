package peedog.funnyfauna.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.block.model.BlockModel;
import net.minecraft.client.render.block.model.BlockModelDispatcher;
import net.minecraft.client.render.item.model.ItemModelDispatcher;
import net.minecraft.client.render.tessellator.Tessellator;import net.minecraft.core.block.Blocks;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.util.helper.MathHelper;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.useless.dragonfly.models.entity.BoneTransform;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import org.useless.dragonfly.renderer.MobRenderer;
import peedog.funnyfauna.entity.bear.MobBear;

@Environment(EnvType.CLIENT)
public class MobRendererBear extends MobRenderer<MobBear> {

	// -------------------------------------------------------------------------
	// Swipe animation constants
	// -------------------------------------------------------------------------
	private static final float SWIPE_ROT_X = -1.3963F;
	private static final float SWIPE_ROT_Z =  0.2618F;
	private static final float SWIPE_ROT_Y =  0.1944F;

	// -------------------------------------------------------------------------
	// Sleeping pose constants
	// -------------------------------------------------------------------------
	private static final float SLEEP_BODY_DROP    =  10.0F;
	private static final float SLEEP_HEAD_ROT_X   =  0.55F;
	private static final float SLEEP_FRONT_ROT_X  = -0.5F;
	private static final float SLEEP_FRONT_ROT_Z  =  1.3F;
	private static final float SLEEP_BACK_ROT_X   =  0.3F;
	private static final float SLEEP_BACK_ROT_Z   =  1.5F;

	public MobRendererBear() {
		super(0.7F);
	}

	// =========================================================================
	// Model selection based on belly level
	// =========================================================================
	private String getModelNameForBelly(int belly) {
		// Belly 1 uses the "skinny" model variant; all others use "main"
		return belly <= 1 ? "skinny" : "main";
	}

	// =========================================================================
	// Main render method
	// =========================================================================
	@Override
	protected @Nullable StaticEntityModel getAndSetupModelForLayer(
		@NonNull MobBear entity, float brightness, float partialTick, int layer) {

		int belly = entity.getBelly();
		String modelName = getModelNameForBelly(belly);
		StaticEntityModel model = this.getModel(modelName);
		model.resetBones();

		// -----------------------------------------------------------------------
		// Fetch bones
		// -----------------------------------------------------------------------
		BoneTransform body          = model.getTransform("body");
		BoneTransform head          = model.getTransform("head");
		BoneTransform legLeftFront  = model.getTransform("legLeftFront");
		BoneTransform legRightFront = model.getTransform("legRightFront");
		BoneTransform legLeftBack   = model.getTransform("legLeftBack");
		BoneTransform legRightBack  = model.getTransform("legRightBack");
		BoneTransform body1         = model.getTransform("body1");
		BoneTransform body2         = model.getTransform("body2");
		BoneTransform body3         = model.getTransform("body3");

		// -----------------------------------------------------------------------
		// Belly overlays:
		//   belly 4 → body3 visible (fattest)
		//   belly 3 → body2 visible
		//   belly 2 → body1 visible (slight belly)
		//   belly 1 → skinny model, no overlays
		// All other overlay bones are hidden at scale 0.
		// -----------------------------------------------------------------------
		setBodyScale(body1, belly == 1 || belly == 2 ? 1.0F : 0.0F);
		setBodyScale(body2, belly == 3 ? 1.0F : 0.0F);
		setBodyScale(body3, belly == 4 ? 1.0F : 0.0F);

		// Front/back legs hidden via scale — re-shown below if not sleeping
		hideLegs(legLeftFront, legRightFront, legLeftBack, legRightBack);

		// -----------------------------------------------------------------------
		// Sleeping pose
		// -----------------------------------------------------------------------
		if (entity.sleeping) {
			if (body != null) body.posY -= SLEEP_BODY_DROP;

			if (head != null) {
				head.rotX = 0.0F;
				head.rotY = 0.0F;
				head.posY -= 4.0;
			}

			if (legLeftFront != null) {
				legLeftFront.scaleX = legLeftFront.scaleY = legLeftFront.scaleZ = 1.0F;
				legLeftFront.rotX   =  SLEEP_FRONT_ROT_X;
				legLeftFront.rotZ   = -SLEEP_FRONT_ROT_Z;
			}
			if (legRightFront != null) {
				legRightFront.scaleX = legRightFront.scaleY = legRightFront.scaleZ = 1.0F;
				legRightFront.rotX   =  SLEEP_FRONT_ROT_X;
				legRightFront.rotZ   =  SLEEP_FRONT_ROT_Z;
			}
			if (legLeftBack != null) {
				legLeftBack.scaleX = legLeftBack.scaleY = legLeftBack.scaleZ = 1.0F;
				legLeftBack.posY  -= SLEEP_BODY_DROP;
				legLeftBack.rotX   =  SLEEP_BACK_ROT_X;
				legLeftBack.rotZ   = -SLEEP_BACK_ROT_Z;
			}
			if (legRightBack != null) {
				legRightBack.scaleX = legRightBack.scaleY = legRightBack.scaleZ = 1.0F;
				legRightBack.posY  -= SLEEP_BODY_DROP;
				legRightBack.rotX   =  SLEEP_BACK_ROT_X;
				legRightBack.rotZ   =  SLEEP_BACK_ROT_Z;
			}

			return model;
		}

		// -----------------------------------------------------------------------
		// Awake — show legs at normal scale
		// -----------------------------------------------------------------------
		showLegs(legLeftFront, legRightFront, legLeftBack, legRightBack);

		float limbSwing = this.getLimbSwing(entity, partialTick);
		float limbYaw   = this.getLimbYaw(entity, partialTick);
		float bodyYaw   = this.getBodyYaw(entity, partialTick);
		float headYaw   = this.getHeadYaw(entity, partialTick) - bodyYaw;
		float headPitch = this.getHeadPitch(entity, partialTick);

		if (head != null) {
			head.rotX = headPitch;
			head.rotY = headYaw;

			// Head sway — headSwayAmount lerps 0→1 entering HEAD_SWAY behavior and
			// 1→0 leaving it, giving a smooth fade. headSwayTick drives the sin wave.
			if (entity.headSwayAmount > 0.001F) {
				float swayAngle = MathHelper.sin(entity.headSwayTick * 0.12F) * 0.35F * entity.headSwayAmount;
				head.rotZ += swayAngle;
			}
		}

		// Walk cycle — diagonal pairs
		float walkCycle = MathHelper.cos(limbSwing * 0.6662F) * 1F * limbYaw;
		if (legLeftFront  != null) legLeftFront.rotX  =  walkCycle;
		if (legRightBack  != null) legRightBack.rotX  =  walkCycle;
		if (legRightFront != null) legRightFront.rotX = -walkCycle;
		if (legLeftBack   != null) legLeftBack.rotX   = -walkCycle;

		// -----------------------------------------------------------------------
		// Swipe attack animation
		// -----------------------------------------------------------------------
		if (entity.attackAnimTime > 0) {
			float progress = MathHelper.clamp(
				(entity.attackAnimTime - partialTick) / 10.0F, 0.0F, 1.0F);

			float arc          = MathHelper.sin(progress * (float) Math.PI);
			float lateralSweep = MathHelper.sin((1.0F - progress) * 2.0F * (float) Math.PI);

			float rotX = SWIPE_ROT_X * arc;
			float rotZ = SWIPE_ROT_Z * arc;
			float rotY = SWIPE_ROT_Y * lateralSweep;

			if (entity.attackSwipeRight) {
				if (legRightFront != null) {
					legRightFront.rotX = rotX;
					legRightFront.rotZ = rotZ;
					legRightFront.rotY = rotY;
				}
			} else {
				if (legLeftFront != null) {
					legLeftFront.rotX =  rotX;
					legLeftFront.rotZ = -rotZ;
					legLeftFront.rotY = -rotY;
				}
			}
		}

		return model;
	}

	// =========================================================================
	// Mouth item rendering — rendered in world space via renderSpecials so the
	// item uses normal GL scale and is not crushed by preRenderTransform's
	// glScalef(0.0625, 0.0625, -0.0625). Mirrors the ant renderer approach.
	//
	// Bear snout is ~1.25 blocks up and ~1.6 blocks forward from entity feet.
	// MOUTH_FORWARD / MOUTH_UP are in world-unit blocks — easy to tune.
	// =========================================================================
	private static final float MOUTH_FORWARD = 1.5F;  // blocks in front of entity
	private static final float MOUTH_UP      = 1.25F; // blocks above entity feet
	private static final float MOUTH_SCALE   = 0.35F;

	@Override
	protected void renderSpecials(Tessellator tessellator, MobBear entity, double x, double y, double z) {
		super.renderSpecials(tessellator, entity, x, y, z);

		ItemStack mouthItem = entity.getMouthItem();
		if (mouthItem == null || mouthItem.itemID <= 0) return;

		// Body yaw in radians — same convention as preRenderTransform
		float yawRad = entity.yBodyRot * MathHelper.DEG_TO_RAD;
		// Forward vector in world XZ (entity faces -sin(yaw), +cos(yaw))
		float fwdX = -MathHelper.sin(yawRad);
		float fwdZ =  MathHelper.cos(yawRad);

		GL11.glPushMatrix();

		// Move to mouth position in world space (raw x,y,z from render call)
		GL11.glTranslatef(
			(float) x + fwdX * MOUTH_FORWARD,
			(float) y + MOUTH_UP,
			(float) z + fwdZ * MOUTH_FORWARD
		);

		// Rotate item to face the same direction as the entity body
		GL11.glRotatef(180.0F - entity.yBodyRot, 0.0F, 1.0F, 0.0F);

		float scale = MOUTH_SCALE;

		if (mouthItem.itemID > 0
			&& mouthItem.itemID < Blocks.blocksList.length
			&& Blocks.blocksList[mouthItem.itemID] != null
			&& ((BlockModel) BlockModelDispatcher.getInstance()
			.getDispatch(Blocks.blocksList[mouthItem.itemID]))
			.shouldItemRender3d()) {
			scale *= 0.75F;
			GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
			GL11.glScalef(scale, -scale, scale);
			GL11.glDisable(GL11.GL_CULL_FACE);
		} else {
			GL11.glScalef(scale, scale, scale);
			GL11.glTranslatef(0.5F, 0.1F, 0.25F);
			GL11.glRotatef(50.0F, 0.0F, 0.0F, 1.0F);
			GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
			GL11.glRotatef(30.0F, 1.0F, 0.0F, 1.0F);
		}

		ItemModelDispatcher.getInstance()
			.getDispatch(mouthItem)
			.renderItem(tessellator, this.renderDispatcher.itemRenderer, entity, mouthItem);

		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glPopMatrix();
	}

	// =========================================================================
	// Helpers
	// =========================================================================

	private void setBodyScale(BoneTransform bone, float s) {
		if (bone == null) return;
		bone.scaleX = s;
		bone.scaleY = s;
		bone.scaleZ = s;
	}

	private void hideLegs(BoneTransform... legs) {
		for (BoneTransform leg : legs) {
			if (leg != null) {
				leg.scaleX = 0.0F;
				leg.scaleY = 0.0F;
				leg.scaleZ = 0.0F;
			}
		}
	}

	private void showLegs(BoneTransform... legs) {
		for (BoneTransform leg : legs) {
			if (leg != null) {
				leg.scaleX = 1.0F;
				leg.scaleY = 1.0F;
				leg.scaleZ = 1.0F;
			}
		}
	}
}
