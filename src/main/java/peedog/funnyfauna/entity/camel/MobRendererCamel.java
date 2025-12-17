package peedog.funnyfauna.entity.camel;

import net.minecraft.client.render.entity.MobRenderer;
import net.minecraft.client.render.model.ModelBase;
import net.minecraft.core.entity.animal.MobChicken;
import net.minecraft.core.util.helper.MathHelper;

public class MobRendererCamel extends MobRenderer<MobCamel> {
	public MobRendererCamel(ModelBase modelbase, float f) {
		super(modelbase, f);
		this.shadowSize = 0.7F;
	}
}
