package peedog.funnyfauna.client.render.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.camera.ICamera;
import net.minecraft.client.render.tessellator.Tessellator;
import org.lwjgl.opengl.GL11;
import peedog.funnyfauna.entity.ant.EntityAnt;

public class EntityRendererAnt extends EntityRenderer<EntityAnt> {

	@Override
	public void render(
		Tessellator tessellator,
		EntityAnt ant,
		double x, double y, double z,
		float yaw,
		float partialTick
	) {
		GL11.glPushMatrix();
		GL11.glTranslatef((float)x, (float)y + 0.2F, (float)z);

		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_NORMALIZE);

		// Billboard to camera
		GL11.glRotatef(180.0F - this.renderDispatcher.viewLerpYaw, 0.0F, 1.0F, 0.0F);
		GL11.glRotatef(-this.renderDispatcher.viewLerpPitch, 1.0F, 0.0F, 0.0F);

		// Bind animated texture
		String textureKey = ant.getAnimFrame() == 0
			? "/assets/funnyfauna/textures/entity/ant/bug1.png"
			: "/assets/funnyfauna/textures/entity/ant/bug2.png";
		this.bindTexture(textureKey);

		float size = 0.1F;

		tessellator.startDrawingQuads();
		tessellator.setColorOpaque_F(1.0F, 1.0F, 1.0F);

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
