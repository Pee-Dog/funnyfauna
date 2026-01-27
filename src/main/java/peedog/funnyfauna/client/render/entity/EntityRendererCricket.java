package peedog.funnyfauna.client.render.entity;

import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.tessellator.Tessellator;
import net.minecraft.core.util.helper.MathHelper;
import org.lwjgl.opengl.GL11;
import peedog.funnyfauna.entity.cricket.EntityCricket;

public class EntityRendererCricket extends EntityRenderer<EntityCricket> {

	@Override
	public void render(
		Tessellator tessellator,
		EntityCricket cricket,
		double x, double y, double z,
		float yaw,
		float partialTick
	) {
		GL11.glPushMatrix();
		GL11.glTranslatef((float)x, (float)y + 0.2F, (float)z);

		GL11.glEnable(GL11.GL_NORMALIZE);
		GL11.glDisable(GL11.GL_LIGHTING);

		GL11.glRotatef(180.0F - this.renderDispatcher.viewLerpYaw, 0.0F, 1.0F, 0.0F);
		GL11.glRotatef(-this.renderDispatcher.viewLerpPitch, 1.0F, 0.0F, 0.0F);

		boolean airborne = cricket.getAnimFrame() == 1;

		String texture =
			airborne
				? "/assets/funnyfauna/textures/entity/cricket/cricket_b.png"
				: "/assets/funnyfauna/textures/entity/cricket/cricket_a.png";

		this.bindTexture(texture);

		int color = cricket.getColor();
		float brightness = cricket.getBrightness(partialTick);

		float r = ((color >> 16) & 255) / 255.0F;
		float g = ((color >> 8) & 255) / 255.0F;
		float b = (color & 255) / 255.0F;

		float size = 0.1F;

		tessellator.startDrawingQuads();
		tessellator.setColorOpaque_F(
			r * brightness,
			g * brightness,
			b * brightness
		);

		tessellator.addVertexWithUV(-size, -size, 0.0, 0.0, 1.0);
		tessellator.addVertexWithUV( size, -size, 0.0, 1.0, 1.0);
		tessellator.addVertexWithUV( size,  size, 0.0, 1.0, 0.0);
		tessellator.addVertexWithUV(-size,  size, 0.0, 0.0, 0.0);

		tessellator.draw();

		GL11.glDisable(GL11.GL_NORMALIZE);
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glPopMatrix();
	}
}
