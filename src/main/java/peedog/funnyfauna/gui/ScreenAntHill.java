package peedog.funnyfauna.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ButtonElement;
import net.minecraft.client.gui.container.ScreenContainerAbstract;
import org.lwjgl.opengl.GL11;

@Environment(EnvType.CLIENT)
public class ScreenAntHill extends ScreenContainerAbstract {

	private final MenuAntHill menu;
	private ButtonElement releaseOneButton;
	private ButtonElement releaseAllButton;

	public ScreenAntHill(MenuAntHill menu) {
		super(menu);
		this.menu = menu;
	}

	@Override
	public void init() {
		super.init();

		int centerX = (this.width - this.xSize) / 2;
		int centerY = (this.height - this.ySize) / 2;

		// Button to release one ant
		this.releaseOneButton = new ButtonElement(
			0, // id
			centerX + 10, // x
			centerY + 2, // y
			70, // width
			20, // height
			"Release 1"
		);
		releaseOneButton.listener = button -> {
			// Release one ant
			menu.releaseAnts(1);
		};

		// Button to release all ants
		this.releaseAllButton = new ButtonElement(
			1, // id
			centerX + 90, // x
			centerY + 2, // y
			70, // width
			20, // height
			"Release All"
		);
		releaseAllButton.listener = button -> {
			// Release all ants
			menu.releaseAllAnts();
		};

		this.add(releaseOneButton);
		this.add(releaseAllButton);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTick) {
		GL11.glColor3d(1.0, 1.0, 1.0);
		// Use textureManager instead of renderEngine
		this.mc.textureManager.loadTexture("/assets/funnyfauna/gui/anthill.png").bind();

		int centerX = (this.width - this.xSize) / 2;
		int centerY = (this.height - this.ySize) / 2;

		this.drawTexturedModalRect(centerX, centerY, 0, 0, this.xSize, this.ySize);
	}

	@Override
	protected void drawGuiContainerForegroundLayer() {
		// Use font instead of fontRenderer
		this.font.drawString("Ant Hill", 8, 6, 4210752);

		// Draw ant count
		int antCount = menu.getStoredAntCount();
		int maxAnts = menu.getMaxAnts();
		String antText = "Ants: " + antCount + "/" + maxAnts;
		this.font.drawString(antText, this.xSize - 70, 6, 4210752);

		// Draw "Inventory" label for player inventory
		this.font.drawString("Inventory", 8, this.ySize - 96 + 2, 4210752);
	}

	@Override
	public void render(int mx, int my, float partialTick) {
		// Update button enabled state based on ant count
		int antCount = menu.getStoredAntCount();
		if (releaseOneButton != null) {
			releaseOneButton.enabled = antCount > 0;
		}
		if (releaseAllButton != null) {
			releaseAllButton.enabled = antCount > 0;
		}

		super.render(mx, my, partialTick);
	}
}
