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

		byte stringState = spider.getStringState();

		// ── Silk thread (world space) ──────────────────────────────────────
		if (stringState != 0) {
			renderSilkThread(tessellator, spider, x, y, z, partialTick, stringState);
		}

		if (!spider.hasHome()) {
		// ── Billboard sprite ───────────────────────────────────────────────
		GL11.glPushMatrix();

		GL11.glTranslatef((float) x, (float) y + 0.15F, (float) z);

		GL11.glEnable(GL11.GL_NORMALIZE);
		GL11.glDisable(GL11.GL_LIGHTING);

		// Face camera
		GL11.glRotatef(180.0F - this.renderDispatcher.viewLerpYaw, 0F, 1F, 0F);
		GL11.glRotatef(-this.renderDispatcher.viewLerpPitch, 1F, 0F, 0F);

		// ── Texture selection ──────────────────────────────────────────────
		this.bindTexture("/assets/funnyfauna/textures/entity/lilspider/"
			+ spider.getAnimFrame() + ".png");

		int color = spider.getColor();
		float brightness = spider.getBrightness(partialTick);

		float r = ((color >> 16) & 255) / 255.0F;
		float g = ((color >> 8) & 255) / 255.0F;
		float b = (color & 255) / 255.0F;

		float size = 0.15F;

		tessellator.startDrawingQuads();
		tessellator.setColorOpaque_F(r * brightness, g * brightness, b * brightness);

		tessellator.addVertexWithUV(-size, -size, 0.0, 0.0, 1.0);
		tessellator.addVertexWithUV(size, -size, 0.0, 1.0, 1.0);
		tessellator.addVertexWithUV(size, size, 0.0, 1.0, 0.0);
		tessellator.addVertexWithUV(-size, size, 0.0, 0.0, 0.0);

		tessellator.draw();

		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glDisable(GL11.GL_NORMALIZE);
		GL11.glPopMatrix();
	}
}
	// ───────────────────────────────────────────────────────────────────────

	private void renderSilkThread(
		Tessellator tessellator,
		EntityLilSpider spider,
		double x, double y, double z,
		float partialTick,
		byte stringState
	) {

		double entityX = spider.xo + (spider.x - spider.xo) * partialTick;
		double entityY = spider.yo + (spider.y - spider.yo) * partialTick;
		double entityZ = spider.zo + (spider.z - spider.zo) * partialTick;

		double targetWorldX = spider.getStringTargetX() + 0.5;
		double targetWorldY = spider.getStringTargetY() + 0.5;
		double targetWorldZ = spider.getStringTargetZ() + 0.5;

		double deltaX = targetWorldX - entityX;
		double deltaY = targetWorldY - entityY;
		double deltaZ = targetWorldZ - entityZ;

		double originX = x;
		double originY = y + 0.15;
		double originZ = z;

		float progress = (stringState == 1)
			? Math.min(1.0f, spider.stringAnimTick /
			(float) EntityLilSpider.STRING_DURATION)
			: 1.0f;

		double endX = originX + deltaX * progress;
		double endY = originY + deltaY * progress;
		double endZ = originZ + deltaZ * progress;

		GL11.glDisable(GL11.GL_TEXTURE_2D);

		float brightness = spider.getBrightness(partialTick);
		float silk = 0.75F;

		tessellator.startDrawing(3); // GL_LINE_STRIP
		tessellator.setColorOpaque_F(
			silk * brightness,
			silk * brightness,
			silk * brightness
		);

		int steps = 8;
		for (int i = 0; i <= steps; i++) {
			float t = (float) i / steps;
			tessellator.addVertex(
				originX + (endX - originX) * t,
				originY + (endY - originY) * t,
				originZ + (endZ - originZ) * t
			);
		}

		tessellator.draw();

		GL11.glEnable(GL11.GL_TEXTURE_2D);
	}
}
