package peedog.funnyfauna.client.render.entity;

import net.minecraft.client.render.entity.MobRenderer;
import net.minecraft.client.render.model.ModelBase;
import peedog.funnyfauna.entity.emu.MobEmu;

public class MobRendererEmu extends MobRenderer<MobEmu> {
	public MobRendererEmu(ModelBase modelbase, float f) {
		super(modelbase, f);
		this.shadowSize = 0.5F;
	}
}
