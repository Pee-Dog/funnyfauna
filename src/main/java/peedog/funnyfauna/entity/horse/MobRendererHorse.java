package peedog.funnyfauna.entity.horse;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.useless.dragonfly.models.entity.BoneTransform;
import org.useless.dragonfly.models.entity.StaticEntityModel;
import org.useless.dragonfly.renderer.MobRenderer;

@Environment(EnvType.CLIENT)
public class MobRendererHorse extends MobRenderer<MobHorse> {
	public MobRendererHorse() {
		super(0.5F);
	}
	@Override
	protected @Nullable StaticEntityModel getAndSetupModelForLayer(@NonNull MobHorse entity, float brightness, float partialTick, int layer) {
		StaticEntityModel model;
		if (layer == 1) {
			this.bindTexture("/assets/funnyfauna/textures/entity/horse/saddle.png");
			model = this.getModel("main");
		} else {
			model = this.getModel("main");
		}

		model.resetBones();
		BoneTransform head = model.getTransform("head");
		BoneTransform neck = model.getTransform("neck");
		BoneTransform legFrontLeft = model.getTransform("legFrontLeft");
		BoneTransform legFrontRight = model.getTransform("legFrontRight");
		BoneTransform legBackLeft = model.getTransform("legBackLeft");
		BoneTransform legBackRight = model.getTransform("legBackRight");
		BoneTransform tail = model.getTransform("tail");

		return model;
	}

	@Override
	protected int maxRenderLayer(@NonNull MobHorse entity) {
		return entity.getSaddled() ? 1 : 0;
	}
}
