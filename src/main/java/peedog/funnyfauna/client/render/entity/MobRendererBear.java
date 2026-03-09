package peedog.funnyfauna.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.util.helper.MathHelper;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.useless.dragonfly.models.entity.BoneTransform;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import org.useless.dragonfly.renderer.MobRenderer;
import peedog.funnyfauna.entity.bear.MobBear;

@Environment(EnvType.CLIENT)
public class MobRendererBear extends MobRenderer<MobBear> {

	// Converted from animation.json degrees → radians
	// Peak values at keyframe 0.25s (midpoint of 0.5s animation):
	//   rotX = -80°  →  -1.3963 rad
	//   rotZ = ±15°  →  ±0.2618 rad  (sign flips between left/right paw)
	// Lateral Y sweep: ~-11° early / ~+6° late → approximated via double-frequency sin
	//   rotY peak ≈ -11.14° = -0.1944 rad (right swipe, outward early)
	private static final float SWIPE_ROT_X   = -1.3963F;
	private static final float SWIPE_ROT_Z   =  0.2618F;
	private static final float SWIPE_ROT_Y   =  0.1944F;

	public MobRendererBear() {
		super(0.7F);
	}

	@Override
	protected @Nullable StaticEntityModel getAndSetupModelForLayer(
		@NonNull MobBear entity, float brightness, float partialTick, int layer) {

		StaticEntityModel model = this.getModel("main");
		model.resetBones();

		float limbSwing  = this.getLimbSwing(entity, partialTick);
		float limbYaw    = this.getLimbYaw(entity, partialTick);
		float bodyYaw    = this.getBodyYaw(entity, partialTick);
		float headYaw    = this.getHeadYaw(entity, partialTick) - bodyYaw;
		float headPitch  = this.getHeadPitch(entity, partialTick);

		BoneTransform head         = model.getTransform("head");
		BoneTransform legLeftFront  = model.getTransform("legLeftFront");
		BoneTransform legRightFront = model.getTransform("legRightFront");
		BoneTransform legLeftBack   = model.getTransform("legLeftBack");
		BoneTransform legRightBack  = model.getTransform("legRightBack");

		// -----------------------------------------------------------------------
		// Head
		// -----------------------------------------------------------------------
		head.rotX = headPitch;
		head.rotY = headYaw;

		// -----------------------------------------------------------------------
		// Walk cycle  (diagonal pairs, same as boar)
		// -----------------------------------------------------------------------
		float walkCycle = MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbYaw;
		legLeftFront.rotX  = walkCycle;
		legRightBack.rotX  = walkCycle;
		legRightFront.rotX = -walkCycle;
		legLeftBack.rotX   = -walkCycle;

		// -----------------------------------------------------------------------
		// Swipe attack animation
		// -----------------------------------------------------------------------
		if (entity.attackAnimTime > 0) {
			// attackProgress: 1.0 at the start of the swing, 0.0 at the end
			float attackProgress = (entity.attackAnimTime - partialTick) / 10.0F;
			attackProgress = MathHelper.clamp(attackProgress, 0.0F, 1.0F);

			// Primary arc: sin(progress * PI) peaks at the midpoint (progress = 0.5),
			// matching the animation.json peak at t=0.25s (halfway through 0.5s clip).
			float arc = MathHelper.sin(attackProgress * (float) Math.PI);

			// Secondary arc at 2x frequency — approximates the lateral Y sweep:
			// positive (outward) early in the swing, negative (back) late.
			// sin((1 - progress) * 2PI) gives: 0 → peak outward at t=0.125 → 0 → tuck back → 0
			float lateralSweep = MathHelper.sin((1.0F - attackProgress) * 2.0F * (float) Math.PI);

			float rotX = SWIPE_ROT_X * arc;
			float rotZ = SWIPE_ROT_Z * arc;
			float rotY = SWIPE_ROT_Y * lateralSweep;

			if (entity.attackSwipeRight) {
				// Right paw swings; Z and Y are positive as-defined
				legRightFront.rotX =  rotX;
				legRightFront.rotZ =  rotZ;
				legRightFront.rotY =  rotY;
			} else {
				// Left paw: Z and Y are mirrored
				legLeftFront.rotX =  rotX;
				legLeftFront.rotZ = -rotZ;
				legLeftFront.rotY = -rotY;
			}
		}

		return model;
	}
}
