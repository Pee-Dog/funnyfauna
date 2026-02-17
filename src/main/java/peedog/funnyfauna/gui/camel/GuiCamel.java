package peedog.funnyfauna.gui.camel;

import net.minecraft.client.gui.container.ScreenContainerAbstract;
import net.minecraft.core.entity.player.Player;
import org.lwjgl.opengl.GL11;
import peedog.funnyfauna.FunnyFauna;
import peedog.funnyfauna.entity.camel.MobCamel;

public class GuiCamel extends ScreenContainerAbstract {

	private final ContainerCamel containerCamel;
	private final MobCamel camel;

	private int GUIx;
	private int GUIy;
	private int rows;
	private int slotsNum;

	public GuiCamel(Player player, ContainerCamel containerCamel, MobCamel camel) {
		super(containerCamel);
		this.containerCamel = containerCamel;
		this.camel = camel;

		// Number of slots and rows
		this.slotsNum = camel.getCamelInventory().getContainerForGui().getContainerSize();
		this.rows = (int) Math.ceil(slotsNum / 9d);

		// GUI size
		this.xSize = 176;
		this.ySize = 17 + rows * 18 + 96; // 17 header + rows + 96 for player inv
	}

	@Override
	public void init() {
		super.init();
		this.GUIx = (this.width - this.xSize) / 2;
		this.GUIy = (this.height - this.ySize) / 2;
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTick) {
		GL11.glColor3d(1.0, 1.0, 1.0);

		// Bind texture
		this.mc.textureManager.loadTexture("/assets/minecraft/textures/gui/container/container.png").bind();

		// Draw camel inventory (top)
		int topRows = Math.min(rows, 6);
		int camelTopHeight = topRows * 18 + 17;
		drawTexturedModalRect(GUIx, GUIy, 0, 0, xSize, camelTopHeight);

		// Draw extra rows if more than 6
		int remainingRows = rows - 6;
		int offsetY = camelTopHeight;
		while (remainingRows > 0) {
			int drawRows = Math.min(remainingRows, 6);
			int height = drawRows * 18;
			drawTexturedModalRect(GUIx, GUIy + offsetY, 0, 17, xSize, height);
			offsetY += height;
			remainingRows -= drawRows;
		}

		// Draw player inventory (bottom)
		drawTexturedModalRect(GUIx, GUIy + offsetY, 0, 126, xSize, 96);
	}


	@Override
	protected void drawGuiContainerForegroundLayer() {
		this.font.drawString(this.camel.getCamelInventory().getNameTranslationKey(), 8, 6, FunnyFauna.GUI_LABEL_COLOR);
		this.font.drawString("Inventory", 8, this.ySize - 96 + 2, FunnyFauna.GUI_LABEL_COLOR);
	}
}
