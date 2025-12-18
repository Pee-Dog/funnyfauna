package peedog.funnyfauna.client.gui;

import net.minecraft.client.gui.ButtonElement;
import net.minecraft.client.gui.Screen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.Font;
import org.lwjgl.input.Mouse;

import java.util.Arrays;
import java.util.List;

public class SatchelScreen extends Screen {

	public SatchelScreen(Screen parent) {
		super(parent);
	}

	@Override
	public void init() {
		// Add buttons with correct ButtonElement constructor
		this.add(new ButtonElement(1, 50, 50, "Button 1"));           // uses default width 200, height 20
		this.add(new ButtonElement(2, 50, 80, 150, 20, "Button 2")); // custom width 150
	}

	@Override
	public void render(int mouseX, int mouseY, float partialTicks) {
		// Draw background
		this.renderBackground();

		// Draw buttons
		super.render(mouseX, mouseY, partialTicks);

		// Draw tooltips if hovering over a button
		for (ButtonElement button : this.buttons) {
			if (isMouseOver(mouseX, mouseY, button)) {
				drawTooltip(mouseX, mouseY, getTooltipForButton(button));
			}
		}
	}

	private boolean isMouseOver(int mouseX, int mouseY, ButtonElement button) {
		return mouseX >= button.xPosition && mouseX <= button.xPosition + button.width
			&& mouseY >= button.yPosition && mouseY <= button.yPosition + button.height;
	}

	private List<String> getTooltipForButton(ButtonElement button) {
		// Return tooltip text based on button label
		switch (button.displayString) {
			case "Button 1":
				return Arrays.asList("This is Button 1", "It does something cool");
			case "Button 2":
				return Arrays.asList("This is Button 2", "It does something else");
			default:
				return Arrays.asList("Unknown button");
		}
	}

	private void drawTooltip(int x, int y, List<String> lines) {
		int yOffset = 0;
		for (String line : lines) {
			this.drawString(this.font, line, x, y + yOffset, 0xFFFFFF);
			yOffset += 10; // spacing between lines
		}
	}

	@Override
	protected void buttonClicked(ButtonElement button) {
		// Handle button clicks
		if (button.id == 1) {
			this.mc.sndManager.playSound("random.click", net.minecraft.core.sound.SoundCategory.GUI_SOUNDS, 1.0F, 1.0F);
		}
		if (button.id == 2) {
			this.mc.sndManager.playSound("random.click", net.minecraft.core.sound.SoundCategory.GUI_SOUNDS, 1.0F, 1.0F);
		}
	}
}
