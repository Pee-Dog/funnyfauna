package peedog.funnyfauna.mixin.player;

import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import peedog.funnyfauna.PlayerInventoryDisplay;

@Mixin(value = Player.class, remap = false)
public class PlayerMixin implements PlayerInventoryDisplay {
	@Override
	public void funnyfauna$displayGUISatchel(final ItemStack stack) {

	}
}
