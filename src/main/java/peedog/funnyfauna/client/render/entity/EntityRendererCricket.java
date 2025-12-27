package peedog.funnyfauna.client.render.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.camera.ICamera;
import net.minecraft.client.render.tessellator.Tessellator;
import org.lwjgl.opengl.GL11;
import peedog.funnyfauna.entity.ant.EntityAnt;
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


		// Billboard to camera
		GL11.glRotatef(180.0F - this.renderDispatcher.viewLerpYaw, 0.0F, 1.0F, 0.0F);
		GL11.glRotatef(-this.renderDispatcher.viewLerpPitch, 1.0F, 0.0F, 0.0F);

		// Choose texture
		int variant = cricket.getVariant();
		boolean airborne = cricket.getAnimFrame() == 1;

		String texture =
			"/assets/funnyfauna/textures/entity/cricket/cricket"
				+ variant
				+ (airborne ? "_b.png" : "_a.png");

		this.bindTexture(texture);

		float size = 0.1F;
		float brightness = cricket.getBrightness(partialTick);

		// 🔴 START DRAWING FIRST
		tessellator.startDrawingQuads();

		// ✅ NOW it is legal to set color
		tessellator.setColorOpaque_F(brightness, brightness, brightness);

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
