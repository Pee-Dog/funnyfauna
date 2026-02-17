package peedog.funnyfauna.client.render.entity;

import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.tessellator.Tessellator;
import org.lwjgl.opengl.GL11;
import peedog.funnyfauna.entity.worm.EntityWorm;

public class EntityRendererWorm extends EntityRenderer<EntityWorm> {

	@Override
	public void render(
		Tessellator t,
		EntityWorm worm,
		double x, double y, double z,
		float yaw,
		float partialTick
	) {
		GL11.glPushMatrix();
		GL11.glTranslatef((float)x, (float)y + 0.03F, (float)z);

		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_NORMALIZE);

		boolean climbing = worm.horizontalCollision && worm.canClimb();

		if (climbing) {
			// Snap facing to nearest 90°
			float snappedYaw = Math.round(worm.yRot / 90F) * 90F;

			// Rotate worm to "stick" to wall
			GL11.glRotatef(-snappedYaw, 0F, 1F, 0F); // face correct cardinal direction
			GL11.glRotatef(-90F, 1F, 0F, 0F);       // stand upright on wall

			// Push slightly off wall so it doesn't clip
			GL11.glTranslatef(0F, 0F, -0.21F);

		} else {
			// Normal ground rotation
			GL11.glRotatef(-worm.yRot, 0F, 1F, 0F);
		}

		// Select texture based on animation frame
		String texture;
		int frame = worm.getAnimFrame();
		switch (frame) {
			case 0: texture = "/assets/funnyfauna/textures/entity/worm/worm_a.png"; break;
			case 1: texture = "/assets/funnyfauna/textures/entity/worm/worm_b.png"; break;
			default: texture = "/assets/funnyfauna/textures/entity/worm/worm_c.png"; break;
		}
		this.bindTexture(texture);

		// Extract RGB from worm color
		int color = worm.getColor();
		float r = ((color >> 16) & 255) / 255.0F;
		float g = ((color >> 8) & 255) / 255.0F;
		float b = (color & 255) / 255.0F;

		float brightness = worm.getBrightness(partialTick);
		float size = 0.2F;

		t.startDrawingQuads();
		t.setColorOpaque_F(r * brightness, g * brightness, b * brightness);

		// Render flat quad centered at origin
		t.addVertexWithUV(-size, 0, -size, 0, 1);
		t.addVertexWithUV(-size, 0,  size, 0, 0);
		t.addVertexWithUV( size, 0,  size, 1, 0);
		t.addVertexWithUV( size, 0, -size, 1, 1);
		t.draw();

		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glDisable(GL11.GL_NORMALIZE);
		GL11.glPopMatrix();
	}
}
