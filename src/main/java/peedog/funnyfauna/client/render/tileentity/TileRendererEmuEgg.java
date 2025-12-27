package peedog.funnyfauna.client.render.tileentity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.LightmapHelper;
import net.minecraft.client.render.camera.ICamera;
import net.minecraft.client.render.tessellator.Tessellator;
import net.minecraft.client.render.tileentity.TileEntityRenderer;
import net.minecraft.core.block.Block;
import net.minecraft.core.util.phys.AABB;
import net.minecraft.core.world.World;
import org.lwjgl.opengl.GL11;
import org.jetbrains.annotations.NotNull;
import peedog.funnyfauna.block.entity.TileEntityEmuEgg;
import peedog.funnyfauna.client.render.model.ModelEmuEgg;

@Environment(EnvType.CLIENT)
public class TileRendererEmuEgg extends TileEntityRenderer<TileEntityEmuEgg> {

	private final ModelEmuEgg model = new ModelEmuEgg();
	private final Minecraft mc = Minecraft.getMinecraft();

	@Override
	public void doRender(
		@NotNull Tessellator t,
		@NotNull TileEntityEmuEgg tileEntity,
		double x,
		double y,
		double z,
		float partialTick
	) {
		Block<?> block = tileEntity.getBlock();
		if (block == null) return;

		GL11.glEnable(32826); // GL_RESCALE_NORMAL
		GL11.glPushMatrix();

		// Move to block center (same as sign)
		GL11.glTranslatef(
			(float) x + 0.5F,
			(float) y + 0.5F,
			(float) z + 0.5F
		);

		// Scale + flip Y/Z like sign renderer
		GL11.glPushMatrix();
		GL11.glScalef(1.0F, -1.0F, -1.0F);

		// Bind texture EXACTLY like sign does
		this.loadTexture("/assets/funnyfauna/textures/entity/emueggblock.png");

		GL11.glDisable(3042); // GL_BLEND

		model.render();

		GL11.glPopMatrix();

		GL11.glPopMatrix();
		GL11.glDisable(32826);
	}

	@Override
	public boolean isVisible(
		@NotNull TileEntityEmuEgg tileEntity,
		@NotNull ICamera camera,
		float partialTick
	) {
		return camera.getFrustum().isVisible(
			AABB.getTemporaryBB(
				tileEntity.x,
				tileEntity.y,
				tileEntity.z,
				tileEntity.x + 1,
				tileEntity.y + 1,
				tileEntity.z + 1
			),
			partialTick
		);
	}
}
