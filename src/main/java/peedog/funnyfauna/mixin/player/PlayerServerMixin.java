package peedog.funnyfauna.mixin.player;

import net.minecraft.core.item.ItemStack;
import net.minecraft.core.net.packet.PacketContainerOpen;
import net.minecraft.core.player.inventory.container.Container;
import net.minecraft.server.entity.player.PlayerServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import peedog.funnyfauna.FunnyFauna;
import peedog.funnyfauna.PlayerInventoryDisplay;
import peedog.funnyfauna.entity.camel.MobCamel;
import peedog.funnyfauna.gui.camel.ContainerCamel;
import peedog.funnyfauna.gui.satchel.ContainerSatchel;
import peedog.funnyfauna.item.ItemSatchel;

@Mixin(value = PlayerServer.class, remap = false)
public class PlayerServerMixin implements PlayerInventoryDisplay {

	@Unique
	private final PlayerServer thisAs = (PlayerServer) (Object) this;

	@Shadow
	private void getNextWindowId() {}

	@Shadow
	private int currentWindowId;

	/* ----------------------------
	   Equipped slot state
	---------------------------- */
	@Unique
	private ItemStack funnyfauna$equippedSlot = null;

	@Unique
	private boolean funnyfauna$equippedEnabled = false;

	/* ----------------------------
	   Server-only climbing state
	---------------------------- */
	@Unique
	private boolean funnyfauna$serverOnlyClimbing = false;

	@Unique
	public boolean funnyfauna$isServerOnlyClimbing() {
		return funnyfauna$serverOnlyClimbing;
	}

	@Unique
	public void funnyfauna$setServerOnlyClimbing(boolean climbing) {
		this.funnyfauna$serverOnlyClimbing = climbing;
	}

	/* ----------------------------
	   Interface: state access
	---------------------------- */
	@Override
	public ItemStack funnyfauna$getEquippedSlot() {
		return funnyfauna$equippedSlot;
	}

	@Override
	public boolean funnyfauna$isEquippedEnabled() {
		return funnyfauna$equippedEnabled;
	}

	@Override
	public void funnyfauna$setEquippedSlot(ItemStack stack) {
		this.funnyfauna$equippedSlot = stack;
	}

	@Override
	public void funnyfauna$setEquippedEnabled(boolean enabled) {
		this.funnyfauna$equippedEnabled = enabled;
	}


	/* ----------------------------
	   GUI methods (server-side)
	---------------------------- */
	@Override
	public void funnyfauna$displayGUISatchel(ItemStack stack) {
		if (stack == null || !(stack.getItem() instanceof ItemSatchel)) return;

		getNextWindowId();
		ContainerSatchel satchel = new ContainerSatchel(this.thisAs.inventory, stack);

		this.thisAs.playerNetServerHandler.sendPacket(
			new PacketContainerOpen(
				this.currentWindowId,
				FunnyFauna.GUI_SATCHEL_ID,
				"Satchel",
				satchel.inventorySatchel.getContainerSize()
			)
		);

		this.thisAs.craftingInventory = satchel;
		this.thisAs.craftingInventory.containerId = this.currentWindowId;
		this.thisAs.craftingInventory.addSlotListener(this.thisAs);
	}

	@Override
	public void funnyfauna$displayGUICamel(MobCamel camel) {
		if (camel == null || !camel.isTamed()) return;
		if (!this.thisAs.uuid.equals(camel.ownerName)) return;
		if (this.thisAs.distanceToSqr(camel) > 64) return;

		getNextWindowId();
		Container inventory = camel.getCamelInventory().getContainerForGui();
		ContainerCamel container = new ContainerCamel(this.thisAs.inventory, inventory, camel);

		this.thisAs.playerNetServerHandler.sendPacket(
			new PacketContainerOpen(
				this.currentWindowId,
				FunnyFauna.GUI_CAMEL_ID,
				"Camel",
				inventory.getContainerSize()
			)
		);

		this.thisAs.craftingInventory = container;
		this.thisAs.craftingInventory.containerId = this.currentWindowId;
		this.thisAs.craftingInventory.addSlotListener(this.thisAs);
	}
}
