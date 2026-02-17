package peedog.funnyfauna.gui.satchel;

import net.minecraft.client.gui.container.ScreenContainerAbstract;
import net.minecraft.client.gui.container.ScreenInventory;
import net.minecraft.core.InventoryAction;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.player.inventory.slot.Slot;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import peedog.funnyfauna.FunnyFauna;

public class GuiSatchel extends ScreenContainerAbstract {
	private int GUIx;
	private int GUIy;
	private final ContainerSatchel satchel;

	public GuiSatchel(final Player player, final ItemStack stack) {
		super(new ContainerSatchel(player.inventory, stack));
		this.satchel = (ContainerSatchel) this.inventorySlots;
	}

	@Override
	public void init() {
		this.GUIx = (this.width - this.xSize) / 2;
		this.GUIy = (this.height - this.ySize) / 2;
		super.init();
	}

	@Override
	protected void drawGuiContainerForegroundLayer() {
		this.font.drawString(this.satchel.inventorySatchel.getNameTranslationKey(), 8, 6, FunnyFauna.GUI_LABEL_COLOR);

		// Display storage used / 64
		float occupancy = this.satchel.inventorySatchel.contents.getOccupancy();
		int used = (int)(occupancy * 64);
		String storageText = used + " / 64";
		int textX = this.xSize - this.font.getStringWidth(storageText) - 8;
		this.font.drawString(storageText, textX, 6, FunnyFauna.GUI_LABEL_COLOR);

		this.font.drawString("Inventory", 8, this.ySize - 96 + 2, FunnyFauna.GUI_LABEL_COLOR);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(final float partialTick) {
		GL11.glColor3d(1d, 1d, 1d);
		this.mc.textureManager.loadTexture("/assets/funnyfauna/textures/gui/pocket.png").bind();
		drawTexturedModalRect(this.GUIx, this.GUIy, 0, 0, this.xSize, this.ySize);
	}

	@Override
	public void keyPressed(char eventCharacter, int eventKey, int mx, int my) {
		// ESC key (keyCode 1) should return to inventory instead of closing
		if (eventKey == Keyboard.KEY_ESCAPE || this.mc.gameSettings.keyInventory.isKeyboardKey(eventKey) || eventKey == Keyboard.KEY_BACK) {
			this.mc.displayScreen(new ScreenInventory(this.mc.thePlayer));
			return;
		}
		super.keyPressed(eventCharacter, eventKey, mx, my);
	}

	@Override
	public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
		Slot clickedSlot = this.getSlotAtPosition(mouseX, mouseY);
		ItemStack cursorStack = this.mc.thePlayer.inventory.getHeldItemStack();

		// Logic for background clicks (the "click anywhere" feature)
		if (clickedSlot == null && isInsideSatchelArea(mouseX, mouseY)) {
			InventoryAction action = (mouseButton == 1) ? InventoryAction.MOVE_SINGLE_ITEM : InventoryAction.MOVE_STACK;
			this.satchel.handleItemMove(action, null, 0, this.mc.thePlayer);
			return; // Handled, don't call super
		}

		// CRITICAL FIX: If clicking on a satchel slot while holding an item, use our custom logic
		if (clickedSlot instanceof SlotSatchel && cursorStack != null) {
			// Determine the action based on button press
			InventoryAction action = (mouseButton == 1) ? InventoryAction.CLICK_RIGHT : InventoryAction.CLICK_LEFT;
			this.satchel.handleItemMove(action, clickedSlot, 0, this.mc.thePlayer);
			return; // Don't call super - we handled it
		}

		// Standard container behavior for other slots (player inventory, or empty cursor on satchel)
		super.mouseClicked(mouseX, mouseY, mouseButton);
	}

	@Override
	public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
		super.mouseReleased(mouseX, mouseY, mouseButton);

		// After releasing the mouse (completing a click or drag), refresh slots
		// This ensures slots disappear immediately when items are removed
		this.satchel.refreshSlots();
	}

	// Add this method to help the Mixin force a UI update
	public void updateLayout() {
		// Clears the button list and recalculates GUI center/size
		this.init();
	}


	/** Checks if click is inside the full satchel area (including empty slots) */
	private boolean isInsideSatchelArea(int mouseX, int mouseY) {
		int left = this.GUIx + 8;
		int top = this.GUIy + 18;

		int rows = Math.max(
			(int) Math.ceil(this.satchel.inventorySatchel.getContainerSize() / 9.0),
			3
		);

		int width = 9 * 18;
		int height = rows * 18;

		return mouseX >= left && mouseX < left + width &&
			mouseY >= top && mouseY < top + height;
	}
}
