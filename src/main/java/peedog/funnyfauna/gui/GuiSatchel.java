package peedog.funnyfauna.gui;

import net.minecraft.client.gui.container.ScreenContainerAbstract;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemStack;
import org.lwjgl.opengl.GL11;
import peedog.funnyfauna.FunnyFauna;

public class GuiSatchel extends ScreenContainerAbstract {
	private int GUIx;
	private int GUIy;
	private int rows;
	private int slotsNum;
	private final ContainerSatchel satchel;
	public GuiSatchel(final Player player, final ItemStack stack) {
		super(new ContainerSatchel(player.inventory, stack));
		this.satchel = (ContainerSatchel) this.inventorySlots;
	}

	@Override
	public void init() {
		this.GUIx = (this.width - this.xSize) / 2;
		this.GUIy = (this.height - this.ySize) / 2;
		this.slotsNum = this.satchel.inventorySatchel.getContainerSize();
		this.rows = (int) Math.ceil(this.slotsNum /9d);
		super.init();
	}
	@Override
	protected void drawGuiContainerForegroundLayer() {
		this.font.drawString(this.satchel.inventorySatchel.getNameTranslationKey(), 8, 6, FunnyFauna.GUI_LABEL_COLOR);
		this.font.drawString("Inventory", 8, this.ySize - 96 + 2, FunnyFauna.GUI_LABEL_COLOR);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(final float partialTick) {
		GL11.glColor3d(1d,1d, 1d);
		this.mc.textureManager.loadTexture("/assets/funnyfauna/textures/gui/pocket.png").bind();
		drawTexturedModalRect(this.GUIx, this.GUIy, 0, 0, this.xSize, this.ySize);

		for (int i = 0; i < this.rows; i++) {
			if (i == this.rows - 1) {
				drawTexturedModalRect(this.GUIx + 7, this.GUIy + 17 + 18 * i, 0, 166, 18 * (this.slotsNum - (9 * i)), 18);
			} else {
				drawTexturedModalRect(this.GUIx + 7, this.GUIy + 17 + 18 * i, 0, 166, 162, 18);
			}
		}
	}
}
