package peedog.funnyfauna.client.render.entity;

import net.minecraft.client.render.entity.MobRenderer;
import net.minecraft.client.render.model.ModelBase;
import net.minecraft.core.util.helper.MathHelper;
import peedog.funnyfauna.entity.penguin.MobPenguin;

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
