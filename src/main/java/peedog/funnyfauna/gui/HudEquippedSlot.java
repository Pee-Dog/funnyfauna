package peedog.funnyfauna.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.hud.HudIngame;
import net.minecraft.client.gui.hud.component.HudComponent;
import net.minecraft.client.render.Lighting;
import net.minecraft.client.render.tessellator.Tessellator;
import net.minecraft.client.render.item.model.ItemModel;
import net.minecraft.client.render.item.model.ItemModelDispatcher;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemStack;
import org.lwjgl.opengl.GL11;
import peedog.funnyfauna.PlayerInventoryDisplay;

public class HudEquippedSlot extends HudComponent {

	private static final int SIZE = 16;

	public HudEquippedSlot() {
		super("funnyfaunaSlot", SIZE, SIZE, null);
	}

	@Override
	public boolean isVisible(Minecraft mc) {
		return true;
	}

	@Override
	public void render(Minecraft mc, HudIngame hud, int width, int height, float partialTicks) {
		Player player = mc.thePlayer;
		if (player == null) return;

		PlayerInventoryDisplay display = (PlayerInventoryDisplay) player;
		ItemStack stack = display.funnyfauna$getEquippedSlot();

		if (stack == null) return;

		// Position: left of hotbar
		int hotbarX = (width / 2) - 96; // left edge of vanilla hotbar
		int hotbarY = height - 19;      // hotbar vertical
		int x = hotbarX - SIZE - 2;     // 2px padding
		int y = hotbarY;

		GL11.glPushMatrix();
		GL11.glEnable(GL11.GL_BLEND);

		// Draw white box behind the item if enabled
		if (display.funnyfauna$isEquippedEnabled()) {
			hud.drawRect(x - 1, y - 1, x + SIZE + 1, y + SIZE + 1, 0x55FFFFFF);
		}

		// Render item on top
		ItemModel model = (ItemModel) ItemModelDispatcher.getInstance().getDispatch(stack.getItem());
		model.renderItemIntoGui(Tessellator.instance, mc.font, mc.textureManager, stack, x, y, 1.0F);
		model.renderItemOverlayIntoGUI(Tessellator.instance, mc.font, mc.textureManager, stack, x, y, 1.0F);

		Lighting.disable();
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glPopMatrix();
	}

	@Override
	public void renderPreview(Minecraft mc, net.minecraft.client.gui.Gui gui,
							  net.minecraft.client.gui.hud.component.layout.Layout layout, int x, int y) {
		// optional designer preview
	}
}
