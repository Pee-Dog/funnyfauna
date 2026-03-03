package peedog.funnyfauna.client.render.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.render.block.model.BlockModel;
import net.minecraft.client.render.block.model.BlockModelDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.camera.ICamera;
import net.minecraft.client.render.entity.MobRenderer;
import net.minecraft.client.render.item.model.ItemModelDispatcher;
import net.minecraft.client.render.model.ModelBase;
import net.minecraft.client.render.tessellator.Tessellator;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.item.ItemStack;
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
		GL11.glTranslatef((float) x, (float) y + 0.2F, (float) z);

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
		tessellator.addVertexWithUV(size, -size, 0.0, 1.0, 1.0);
		tessellator.addVertexWithUV(size, size, 0.0, 1.0, 0.0);
		tessellator.addVertexWithUV(-size, size, 0.0, 0.0, 0.0);

		tessellator.draw();

		GL11.glDisable(GL11.GL_NORMALIZE);
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glPopMatrix();
		// ----- ITEM (separate world transform) -----
		ItemStack stack = ant.getHeldItem();
		if (stack != null && stack.getItem() != null) {

			GL11.glPushMatrix();

			// Position item at ant location in world
			GL11.glTranslatef((float)x, (float)y + 0.35F, (float)z);

			// Rotate with entity yaw (NOT camera)
			GL11.glRotatef(180.0F - yaw, 0F, 1F, 0F);

			if (stack.itemID > 0 && stack.itemID < Blocks.blocksList.length &&
				((BlockModel) BlockModelDispatcher.getInstance()
					.getDispatch(Blocks.blocksList[stack.itemID]))
					.shouldItemRender3d()) {

				float scale = 0.35F;
				scale *= 0.75F;
				GL11.glRotatef(90F, 0F, 1F, 0F);
				GL11.glScalef(scale, -scale, scale);
				GL11.glDisable(GL11.GL_CULL_FACE);

			} else {

				float scale = 0.375F;
				GL11.glScalef(scale, scale, scale);
				GL11.glTranslatef(0.5F, .1F, 0.25F);
				GL11.glRotatef(50F, 0F, 0F, 1F);
				GL11.glRotatef(-90F, 1F, 0F, 0F);
				GL11.glRotatef(30F, 1F, 0F, 1F);
			}

			ItemModelDispatcher.getInstance()
				.getDispatch(stack)
				.renderItem(
					tessellator,
					this.renderDispatcher.itemRenderer,
					ant,
					stack
				);
			GL11.glEnable(GL11.GL_CULL_FACE);
			GL11.glPopMatrix();
		}
	}
}
