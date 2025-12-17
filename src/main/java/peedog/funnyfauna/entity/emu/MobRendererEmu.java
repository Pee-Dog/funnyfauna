package peedog.funnyfauna.entity.emu;

import net.minecraft.client.render.entity.MobRenderer;
import net.minecraft.client.render.model.ModelBase;
import net.minecraft.core.entity.animal.MobChicken;
import net.minecraft.core.util.helper.MathHelper;

public class MobRendererEmu extends MobRenderer<MobEmu> {
	public MobRendererEmu(ModelBase modelbase, float f) {
		super(modelbase, f);
		this.shadowSize = 0.5F;
	}
}
