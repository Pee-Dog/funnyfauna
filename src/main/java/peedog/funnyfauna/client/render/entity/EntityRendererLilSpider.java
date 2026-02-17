package peedog.funnyfauna.client.render.entity;

import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.tessellator.Tessellator;
import org.lwjgl.opengl.GL11;
import peedog.funnyfauna.entity.lilspider.EntityLilSpider;

public class EntityRendererLilSpider extends EntityRenderer<EntityLilSpider> {

	@Override
	public void render(
		Tessellator tessellator,
		EntityLilSpider spider,
		double x, double y, double z,
		float yaw,
		float partialTick
	) {
		GL11.glPushMatrix();

		// Offset the spider slightly upward so it doesn't clip into the floor
		GL11.glTranslatef((float)x, (float)y + 0.15F, (float)z);

		GL11.glEnable(GL11.GL_NORMALIZE);
		GL11.glDisable(GL11.GL_LIGHTING);

		// Billboard logic: Rotate to face the camera's current Yaw and Pitch
		GL11.glRotatef(180.0F - this.renderDispatcher.viewLerpYaw, 0.0F, 1.0F, 0.0F);
		GL11.glRotatef(-this.renderDispatcher.viewLerpPitch, 1.0F, 0.0F, 0.0F);

		this.bindTexture("/assets/funnyfauna/textures/entity/lilspider/" + spider.getAnimFrame() + ".png");

		// Color and Brightness
		int color = spider.getColor();
		float brightness = spider.getBrightness(partialTick);

		float r = ((color >> 16) & 255) / 255.0F;
		float g = ((color >> 8) & 255) / 255.0F;
		float b = (color & 255) / 255.0F;

		// Size of the sprite
		float size = 0.15F;

		tessellator.startDrawingQuads();
		tessellator.setColorOpaque_F(
			r * brightness,
			g * brightness,
			b * brightness
		);

		// Vertices for a vertical quad facing the Z-axis (which is now facing the camera)
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
