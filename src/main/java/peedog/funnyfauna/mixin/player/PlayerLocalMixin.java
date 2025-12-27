package peedog.funnyfauna.mixin.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.PlayerLocal;
import net.minecraft.core.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import peedog.funnyfauna.PlayerInventoryDisplay;
import peedog.funnyfauna.gui.GuiSatchel;

@Mixin(value = PlayerLocal.class, remap = false)
public class PlayerLocalMixin implements PlayerInventoryDisplay {
	@Unique
	private final Minecraft mc = Minecraft.getMinecraft();
	@Unique
	private final PlayerLocal thisAs = (PlayerLocal)(Object)this;

	@Override
	public void funnyfauna$displayGUISatchel(final ItemStack stack) {
		this.mc.displayScreen(new GuiSatchel(this.thisAs, stack));
	}
}
