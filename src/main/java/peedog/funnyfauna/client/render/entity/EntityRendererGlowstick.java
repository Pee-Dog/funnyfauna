package peedog.funnyfauna.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.tessellator.Tessellator;
import net.minecraft.core.util.helper.DyeColor;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import peedog.funnyfauna.entity.projectile.ProjectileGlowstick;

@Environment(EnvType.CLIENT)
public class EntityRendererGlowstick extends EntityRenderer<ProjectileGlowstick> {

	@Override
	public void render(Tessellator tessellator, ProjectileGlowstick glowstick, double x, double y, double z, float yaw, float partialTick) {
		DyeColor color = DyeColor.colorFromBlockMeta(glowstick.colorMeta & 15);
		this.bindTexture("/assets/funnyfauna/textures/entity/glowstick/" + color.colorID + ".png");

		GL11.glPushMatrix();
		GL11.glTranslatef((float) x, (float) y, (float) z);

		GL11.glRotatef(glowstick.yRotO + (glowstick.yRot - glowstick.yRotO) * partialTick - 90.0F, 0.0F, 1.0F, 0.0F);
		GL11.glRotatef(glowstick.xRotO + (glowstick.xRot - glowstick.xRotO) * partialTick, 0.0F, 0.0F, 1.0F);
		GL11.glRotatef(-90.0F, 0.0F, 0.0F, 1.0F);

		GL11.glEnable(GL12.GL_RESCALE_NORMAL);

		float scale = 0.0625F; // 1/16 scale to match Blockbench pixels
		GL11.glScalef(scale, scale, scale);

		// Geometry matches s.java: Width 2, Height 10, Depth 2
		float x1 = -1.0F; float x2 = 1.0F;
		float y1 = -10.0F; float y2 = 0.0F;
		float z1 = -1.0F; float z2 = 1.0F;

		// s.java defines a 16x16 texture
		float tw = 16.0F;
		float th = 16.0F;

		tessellator.startDrawingQuads();

		// TOP (Y2)
		tessellator.setNormal(0.0F, 1.0F, 0.0F);
		tessellator.addVertexWithUV(x1, y2, z1, 7/tw, 6/th);
		tessellator.addVertexWithUV(x1, y2, z2, 7/tw, 16/th);
		tessellator.addVertexWithUV(x2, y2, z2, 9/tw, 16/th);
		tessellator.addVertexWithUV(x2, y2, z1, 9/tw, 14/th);

// BOTTOM (Y1)
		tessellator.setNormal(0.0F, -1.0F, 0.0F);
		tessellator.addVertexWithUV(x1, y1, z1, 7/tw, 14/th);
		tessellator.addVertexWithUV(x2, y1, z1, 9/tw, 14/th);
		tessellator.addVertexWithUV(x2, y1, z2, 9/tw, 16/th);
		tessellator.addVertexWithUV(x1, y1, z2, 7/tw, 16/th);
		// FRONT (Z2) - texOffs(7, 6), size 2x10
		tessellator.setNormal(0.0F, 0.0F, 1.0F);
		tessellator.addVertexWithUV(x1, y2, z2, 7/tw, 6/th);
		tessellator.addVertexWithUV(x1, y1, z2, 7/tw, 16/th);
		tessellator.addVertexWithUV(x2, y1, z2, 9/tw, 16/th);
		tessellator.addVertexWithUV(x2, y2, z2, 9/tw, 6/th);

		// BACK (Z1) - texOffs(7, 6), size 2x10
		tessellator.setNormal(0.0F, 0.0F, -1.0F);
		tessellator.addVertexWithUV(x2, y2, z1, 7/tw, 6/th);
		tessellator.addVertexWithUV(x2, y1, z1, 7/tw, 16/th);
		tessellator.addVertexWithUV(x1, y1, z1, 9/tw, 16/th);
		tessellator.addVertexWithUV(x1, y2, z1, 9/tw, 6/th);

		// LEFT (X1) - texOffs(7, 4), size 2x10
		tessellator.setNormal(-1.0F, 0.0F, 0.0F);
		tessellator.addVertexWithUV(x1, y2, z1, 7/tw, 6/th);
		tessellator.addVertexWithUV(x1, y1, z1, 7/tw, 16/th);
		tessellator.addVertexWithUV(x1, y1, z2, 9/tw, 16/th);
		tessellator.addVertexWithUV(x1, y2, z2, 9/tw, 6/th);

		// RIGHT (X2) - texOffs(7, 4), size 2x10
		tessellator.setNormal(1.0F, 0.0F, 0.0F);
		tessellator.addVertexWithUV(x2, y2, z2, 7/tw, 6/th);
		tessellator.addVertexWithUV(x2, y1, z2, 7/tw, 16/th);
		tessellator.addVertexWithUV(x2, y1, z1, 9/tw, 16/th);
		tessellator.addVertexWithUV(x2, y2, z1, 9/tw, 6/th);

		tessellator.draw();

		GL11.glDisable(GL12.GL_RESCALE_NORMAL);
		GL11.glPopMatrix();
	}
}
