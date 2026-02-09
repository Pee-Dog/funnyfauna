package peedog.funnyfauna.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.tessellator.Tessellator;
import net.minecraft.core.entity.Entity;
import org.lwjgl.opengl.GL11;
import peedog.funnyfauna.entity.tumbleweed.EntityTumbleweed;

@Environment(EnvType.CLIENT)
public class EntityRendererTumbleweed extends EntityRenderer<Entity> {

	public EntityRendererTumbleweed() {
		this.shadowSize = 0.5F;
	}

	@Override
	public void render(Tessellator tessellator, Entity entity,
					   double x, double y, double z,
					   float yaw, float partialTick) {

		if (!(entity instanceof EntityTumbleweed)) return;
		EntityTumbleweed tumbleweed = (EntityTumbleweed) entity;

		// Interpolated roll
		float roll = tumbleweed.prevRollRotation +
			(tumbleweed.rollRotation - tumbleweed.prevRollRotation) * partialTick;

		GL11.glPushMatrix();
		GL11.glTranslated(x, y + 0.3D, z);

		// Face movement direction
		if (tumbleweed.xd != 0 || tumbleweed.zd != 0) {
			float dirYaw = (float)(Math.atan2(tumbleweed.zd, tumbleweed.xd) * 180.0D / Math.PI);
			GL11.glRotatef(-dirYaw, 0F, 1F, 0F);

			// Rotate along axis perpendicular to horizontal motion
			float ax = (float) tumbleweed.zd;
			float ay = 0f;
			float az = (float) -tumbleweed.xd;

			// Normalize axis
			float length = (float) Math.sqrt(ax*ax + ay*ay + az*az);
			if (length != 0) {
				ax /= length;
				ay /= length;
				az /= length;
				GL11.glRotatef(roll, ax, ay, az);
			}
		}

		this.bindTexture("/assets/funnyfauna/textures/entity/tumbleweed.png");
		GL11.glDisable(GL11.GL_CULL_FACE);
		drawCube(tessellator, 0.3F);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glPopMatrix();
	}

	private void drawCube(Tessellator t, float r) {
		t.startDrawingQuads();

		// Front (-Z)
		t.setNormal(0, 0, -1);
		t.addVertexWithUV(-r, -r, -r, 0, 0);
		t.addVertexWithUV(r, -r, -r, 1, 0);
		t.addVertexWithUV(r, r, -r, 1, 1);
		t.addVertexWithUV(-r, r, -r, 0, 1);

		// Back (+Z)
		t.setNormal(0, 0, 1);
		t.addVertexWithUV(-r, r, r, 0, 1);
		t.addVertexWithUV(r, r, r, 1, 1);
		t.addVertexWithUV(r, -r, r, 1, 0);
		t.addVertexWithUV(-r, -r, r, 0, 0);

		// Left (-X)
		t.setNormal(-1, 0, 0);
		t.addVertexWithUV(-r, -r, r, 0, 0);
		t.addVertexWithUV(-r, -r, -r, 1, 0);
		t.addVertexWithUV(-r, r, -r, 1, 1);
		t.addVertexWithUV(-r, r, r, 0, 1);

		// Right (+X)
		t.setNormal(1, 0, 0);
		t.addVertexWithUV(r, -r, -r, 0, 0);
		t.addVertexWithUV(r, -r, r, 1, 0);
		t.addVertexWithUV(r, r, r, 1, 1);
		t.addVertexWithUV(r, r, -r, 0, 1);

		// Top (+Y)
		t.setNormal(0, 1, 0);
		t.addVertexWithUV(-r, r, -r, 0, 0);
		t.addVertexWithUV(r, r, -r, 1, 0);
		t.addVertexWithUV(r, r, r, 1, 1);
		t.addVertexWithUV(-r, r, r, 0, 1);

		// Bottom (-Y)
		t.setNormal(0, -1, 0);
		t.addVertexWithUV(-r, -r, r, 0, 1);
		t.addVertexWithUV(r, -r, r, 1, 1);
		t.addVertexWithUV(r, -r, -r, 1, 0);
		t.addVertexWithUV(-r, -r, -r, 0, 0);

		t.draw();
	}
}
