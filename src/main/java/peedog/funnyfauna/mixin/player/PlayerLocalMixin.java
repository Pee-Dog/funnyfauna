package peedog.funnyfauna.mixin.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.PlayerLocal;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.player.inventory.container.ContainerInventory;
import net.minecraft.core.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import peedog.funnyfauna.PlayerInventoryDisplay;
import peedog.funnyfauna.entity.camel.MobCamel;
import peedog.funnyfauna.gui.camel.ContainerCamel;
import peedog.funnyfauna.gui.camel.GuiCamel;
import peedog.funnyfauna.gui.satchel.GuiSatchel;
import peedog.funnyfauna.item.ItemToggleable;

@Mixin(value = PlayerLocal.class, remap = false)
public abstract class PlayerLocalMixin extends Player implements PlayerInventoryDisplay {
	public PlayerLocalMixin(@Nullable World world) {
		super(world);
	}

	@Unique
	private final Minecraft mc = Minecraft.getMinecraft();

	@Unique
	private final PlayerLocal thisAs = (PlayerLocal) (Object) this;

	/* ----------------------------
	   Equipped slot state
	---------------------------- */
	@Unique
	private ItemStack funnyfauna$equippedSlot;

	@Unique
	private boolean funnyfauna$equippedEnabled;

	@Unique
	private boolean funnyfauna$equippedSynced = false;

	/* ----------------------------
	   PlayerInventoryDisplay API
	---------------------------- */
	@Override
	public ItemStack funnyfauna$getEquippedSlot() {
		return this.funnyfauna$equippedSlot;
	}

	@Override
	public void funnyfauna$setEquippedSlot(ItemStack stack) {
		this.funnyfauna$equippedSlot = stack;
	}

	@Override
	public boolean funnyfauna$isEquippedEnabled() {
		return this.funnyfauna$equippedEnabled;
	}

	@Override
	public void funnyfauna$setEquippedEnabled(boolean enabled) {
		this.funnyfauna$equippedEnabled = enabled;
	}

	/* ----------------------------
	   GUI display hooks
	---------------------------- */
	@Override
	public void funnyfauna$displayGUISatchel(ItemStack stack) {
		this.mc.displayScreen(new GuiSatchel(this.thisAs, stack));
	}

	@Override
	public void funnyfauna$displayGUICamel(MobCamel camel) {
		ContainerInventory playerInventory = thisAs.inventory;
		ContainerCamel containerCamel = new ContainerCamel(playerInventory, camel.getCamelInventory().getContainerForGui(), camel);
		this.mc.displayScreen(new GuiCamel(this.thisAs, containerCamel, camel));
	}

	/* ----------------------------
	   Sync equipped item on join
	---------------------------- */
	@Unique
	public void funnyfauna$syncEquippedFromInventory() {
		for (ItemStack stack : thisAs.inventory.mainInventory) {
			if (stack != null && stack.getItem() instanceof ItemToggleable &&
				ItemToggleable.isToggled(stack)) {
				this.funnyfauna$setEquippedSlot(stack);
				this.funnyfauna$setEquippedEnabled(true);
				return; // Only one toggleable can be equipped
			}
		}
		// No toggled item found
		this.funnyfauna$setEquippedSlot(null);
		this.funnyfauna$setEquippedEnabled(false);
	}

	/* ----------------------------
	   Inject: sync equipped on first tick
	---------------------------- */
	@Inject(method = "onLivingUpdate", at = @At("HEAD"))
	private void funnyfauna$syncEquippedOnJoin(CallbackInfo ci) {
		if (!funnyfauna$equippedSynced) {
			funnyfauna$syncEquippedFromInventory();
			funnyfauna$equippedSynced = true;
		}
	}
	@Inject(method = "getFovModifier", at = @At("HEAD"), cancellable = true)
	public void getFovModifier(CallbackInfoReturnable<Float> cir) {
		float f = 1.0F;
		double speed = this.baseSpeed;
		if (this.isSprinting()) speed = (speed + this.baseSpeed * 0.3);
		f *= ((float)speed / this.baseSpeed + 1.0F) / 2.0F;
		f *= this.heldObject == null ? 1.0F : 0.8F;
		cir.setReturnValue(f);
		cir.cancel();
	}
}
