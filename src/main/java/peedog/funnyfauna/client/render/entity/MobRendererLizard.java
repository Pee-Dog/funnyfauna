package peedog.funnyfauna.client.render.entity;

import net.minecraft.client.render.entity.MobRenderer;
import net.minecraft.client.render.model.ModelBase;
import net.minecraft.client.render.tessellator.Tessellator;
import peedog.funnyfauna.entity.lizard.MobLizard;

public class  MobRendererLizard extends MobRenderer<MobLizard> {

	public MobRendererLizard(ModelBase modelbase, float f) {
		super(modelbase, f);
		this.shadowSize = 0.4F;
	}

	@Override
	public void render(Tessellator tessellator, MobLizard entity, double x, double y, double z, float yaw, float partialTick) {
		// Pass the entity to the model so it can check hasTail
		((ModelLizard)this.mainModel).entity = entity;

		// Call the original render logic from MobRenderer
		super.render(tessellator, entity, x, y, z, yaw, partialTick);
	}
}
