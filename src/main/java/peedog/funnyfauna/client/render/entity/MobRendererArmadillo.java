package peedog.funnyfauna.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.util.helper.MathHelper;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.useless.dragonfly.animation.Animation;
import org.useless.dragonfly.models.entity.BoneTransform;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import org.useless.dragonfly.models.entity.mojang.StaticEntityModelMojang;
import org.useless.dragonfly.renderer.MobRenderer;
import peedog.funnyfauna.entity.armadillo.MobArmadillo;

import static peedog.funnyfauna.FunnyFauna.MOD_ID;

@Environment(EnvType.CLIENT)
public class MobRendererArmadillo extends MobRenderer<MobArmadillo> {

	private Animation cowerAnimation;

	public MobRendererArmadillo() {
		super(0.5F);
	}

	@Override
	protected @Nullable StaticEntityModel getAndSetupModelForLayer(
		@NonNull MobArmadillo entity,
		float brightness,
		float partialTick,
		int layer
	) {
		if (layer != 0) return null;

		StaticEntityModel model = this.getModel("main");
		if (model == null) return null;

		model.resetBones();

		// --- LOAD ANIMATION ONCE ---
		if (cowerAnimation == null) {
			cowerAnimation = org.useless.util.AnimationHelper
				.getOrCreateEntityAnimation(MOD_ID, "armadillo.animation");
		}

		// --- APPLY DRAGONFLY ANIMATION ---
		if (model instanceof StaticEntityModelMojang) {
			StaticEntityModelMojang mojang = (StaticEntityModelMojang) model;

			this.animate(
				mojang,
				entity.cowerState,
				cowerAnimation,
				entity.tickCount + partialTick,
				1.0F
			);
		}


		// --- VANILLA WALKING (only if NOT cowering) ---
		if (!entity.isCowering) {
			float limbSwing = this.getLimbSwing(entity, partialTick);
			float limbYaw = this.getLimbYaw(entity, partialTick);

			BoneTransform leg1 = model.getTransform("leg1");
			BoneTransform leg2 = model.getTransform("leg2");
			BoneTransform leg3 = model.getTransform("leg3");
			BoneTransform leg4 = model.getTransform("leg4");

			float walkA = MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbYaw;
			float walkB = MathHelper.cos(limbSwing * 0.6662F + (float)Math.PI) * 1.4F * limbYaw;

			if (leg1 != null) leg1.rotX = walkA;
			if (leg4 != null) leg4.rotX = walkA;
			if (leg2 != null) leg2.rotX = walkB;
			if (leg3 != null) leg3.rotX = walkB;
		}

		return model;
	}
}
