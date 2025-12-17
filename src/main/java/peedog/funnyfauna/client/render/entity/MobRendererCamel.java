package peedog.funnyfauna.client.render.entity;

import net.minecraft.client.render.entity.MobRenderer;
import net.minecraft.client.render.model.ModelBase;
import peedog.funnyfauna.entity.camel.MobCamel;

public class MobRendererCamel extends MobRenderer<MobCamel> {
	public MobRendererCamel(ModelBase modelbase, float f) {
		super(modelbase, f);
		this.shadowSize = 0.7F;
	}
}
