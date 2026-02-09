package peedog.funnyfauna.item;

import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.world.World;

public class ItemClimbingClaws extends ItemToggleable {

	private static final int MAX_DURABILITY = 200;
	private static final int DURABILITY_TICK = 1;

	public ItemClimbingClaws(String translationKey, String namespaceId, int id) {
		super(translationKey, namespaceId, id);
		this.setMaxDamage(MAX_DURABILITY);
	}

	@Override
	public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
		super.inventoryTick(stack, world, entity, slot, selected);

		if (!(entity instanceof Player)) return;
		Player player = (Player) entity;

		if (!isToggled(stack)) return;

		if (player.horizontalCollision) {
			double climbUpSpeed = 0.2;    // speed when moving up
			double climbDownSpeed = 0.15; // speed when descending
			double stickSpeed = 0.0;      // stick in place if no input

			// Vertical motion
			if (player.isSneaking()) {
				player.yd = -climbDownSpeed;  // go down
			} else if (playerForwardPressed(player)) {
				player.yd = climbUpSpeed;     // move up
			} else {
				player.yd = stickSpeed;       // stick in place
			}

			// Limit horizontal sliding to avoid slipping
			double maxHorizontal = 0.15;
			if (player.xd > maxHorizontal) player.xd = maxHorizontal;
			if (player.xd < -maxHorizontal) player.xd = -maxHorizontal;
			if (player.zd > maxHorizontal) player.zd = maxHorizontal;
			if (player.zd < -maxHorizontal) player.zd = -maxHorizontal;

			player.fallDistance = 0; // prevent fall damage

			// Force sticking to wall horizontally
			player.xd = 0;
			player.zd = 0;

			// Damage claws every second
			if (!world.isClientSide && world.getWorldTime() % 5 == 0) {
				stack.damageItem(DURABILITY_TICK, player);
				if (stack.getMetadata() >= stack.getMaxDamage()) {
					player.inventory.setItem(slot, null);
					System.out.println("[FunnyFauna] Climbing Claws broke!");
				}
			}
		}
	}

	/**
	 * Checks if the player is pressing forward.
	 * Since your Player class has no inputForward, you may need to implement your own detection.
	 * For now, we'll assume always climbing if not sneaking.
	 */
	private boolean playerForwardPressed(Player player) {
		return true; // keep climbing unless sneaking
	}



	@Override
	public int getMaxDamage() {
		return MAX_DURABILITY;
	}

	/** Compatibility helper */
	public static boolean isEnabled(ItemStack stack) {
		return isToggled(stack);
	}
}
