package peedog.funnyfauna.entity.penguin;

import net.minecraft.client.render.entity.MobRenderer;
import net.minecraft.client.render.model.ModelBase;
import net.minecraft.core.entity.animal.MobChicken;
import net.minecraft.core.util.helper.MathHelper;

public class MobRendererPenguin extends MobRenderer<MobPenguin> {
	public MobRendererPenguin(ModelBase modelbase, float f) {
		super(modelbase, f);
		this.shadowSize = 0.3F;

	}
	protected float limbSway(MobPenguin entity, float partialTick) {
		float flap = entity.oFlap + (entity.flap - entity.oFlap) * partialTick;
		float flapSpeed = entity.oFlapSpeed + (entity.flapSpeed - entity.oFlapSpeed) * partialTick;
		return (MathHelper.sin(flap) + 1.0F) * flapSpeed;
	}
}
