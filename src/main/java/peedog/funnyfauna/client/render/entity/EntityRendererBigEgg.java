package peedog.funnyfauna.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.tessellator.Tessellator;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import peedog.funnyfauna.entity.projectile.ProjectileBigEgg;

@Environment(EnvType.CLIENT)
public class EntityRendererBigEgg extends EntityRenderer<ProjectileBigEgg> {

	public EntityRendererBigEgg() {
	}

	@Override
	public void render(
		Tessellator tessellator,
		ProjectileBigEgg egg,
		double x,
		double y,
		double z,
		float yaw,
		float partialTick
	) {
		this.bindTexture("/assets/funnyfauna/textures/entity/emueggblock.png");

		// SAFETY: ensure correct matrix mode
		GL11.glMatrixMode(GL11.GL_MODELVIEW);

		GL11.glPushMatrix();
		GL11.glTranslatef((float) x, (float) y, (float) z);

		// Smooth rotation
		GL11.glRotatef(
			egg.yRotO + (egg.yRot - egg.yRotO) * partialTick,
			0.0F, 1.0F, 0.0F
		);

		GL11.glEnable(GL12.GL_RESCALE_NORMAL);

		float scale = 0.0625F;
		GL11.glScalef(scale, scale, scale);

		// Cube bounds (same as addBox)
		float x1 = -11.0F;
		float x2 = -5.0F;
		float y1 = -9.0F;
		float y2 = 0.0F;
		float z1 = 5.0F;
		float z2 = 11.0F;

		float texW = 64.0F;
		float texH = 32.0F;

		float px = 1.0F / texW;
		float py = 1.0F / texH;

		// FRONT
		float fu0 = 6 * px,  fu1 = 12 * px;
		float fv0 = 6 * py,  fv1 = 15 * py;

		// BACK
		float bu0 = 18 * px, bu1 = 24 * px;
		float bv0 = 6 * py,  bv1 = 15 * py;

		// LEFT
		float lu0 = 0 * px,  lu1 = 6 * px;
		float lv0 = 6 * py,  lv1 = 15 * py;

		// RIGHT
		float ru0 = 12 * px, ru1 = 18 * px;
		float rv0 = 6 * py,  rv1 = 15 * py;

		// TOP
		float tu0 = 6 * px,  tu1 = 12 * px;
		float tv0 = 0 * py,  tv1 = 6 * py;

		// BOTTOM
		float du0 = 12 * px, du1 = 18 * px;
		float dv0 = 0 * py,  dv1 = 6 * py;

		tessellator.startDrawingQuads();

		/* FRONT */
		tessellator.addVertexWithUV(x1, y1, z2, fu0, fv1);
		tessellator.addVertexWithUV(x2, y1, z2, fu1, fv1);
		tessellator.addVertexWithUV(x2, y2, z2, fu1, fv0);
		tessellator.addVertexWithUV(x1, y2, z2, fu0, fv0);

		/* BACK */
		tessellator.addVertexWithUV(x2, y1, z1, bu0, bv1);
		tessellator.addVertexWithUV(x1, y1, z1, bu1, bv1);
		tessellator.addVertexWithUV(x1, y2, z1, bu1, bv0);
		tessellator.addVertexWithUV(x2, y2, z1, bu0, bv0);

		/* LEFT */
		tessellator.addVertexWithUV(x1, y1, z1, lu0, lv1);
		tessellator.addVertexWithUV(x1, y1, z2, lu1, lv1);
		tessellator.addVertexWithUV(x1, y2, z2, lu1, lv0);
		tessellator.addVertexWithUV(x1, y2, z1, lu0, lv0);

		/* RIGHT */
		tessellator.addVertexWithUV(x2, y1, z2, ru0, rv1);
		tessellator.addVertexWithUV(x2, y1, z1, ru1, rv1);
		tessellator.addVertexWithUV(x2, y2, z1, ru1, rv0);
		tessellator.addVertexWithUV(x2, y2, z2, ru0, rv0);

		/* TOP */
		tessellator.addVertexWithUV(x1, y2, z2, tu0, tv1);
		tessellator.addVertexWithUV(x2, y2, z2, tu1, tv1);
		tessellator.addVertexWithUV(x2, y2, z1, tu1, tv0);
		tessellator.addVertexWithUV(x1, y2, z1, tu0, tv0);

		/* BOTTOM */
		tessellator.addVertexWithUV(x1, y1, z1, du0, dv1);
		tessellator.addVertexWithUV(x2, y1, z1, du1, dv1);
		tessellator.addVertexWithUV(x2, y1, z2, du1, dv0);
		tessellator.addVertexWithUV(x1, y1, z2, du0, dv0);

		tessellator.draw();

		// 🔒 RESTORE GL STATE (CRITICAL)
		GL11.glDisable(GL12.GL_RESCALE_NORMAL);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDepthMask(true);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_CULL_FACE);

		GL11.glPopMatrix();
	}
}
