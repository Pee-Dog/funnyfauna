package peedog.funnyfauna.item;

import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.Item;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.world.World;
import peedog.funnyfauna.entity.camel.MobCamel;
import peedog.funnyfauna.entity.tumbleweed.EntityTumbleweed;

import java.util.List;

public class ItemTumbleweed extends Item {

	public ItemTumbleweed(String name, String namespaceId, int id) {
		super(name, namespaceId, id);
		this.maxStackSize = 16;
	}

	@Override
	public ItemStack onUseItem(ItemStack stack, World world, Player player) {
		double reach = 5.0;
		MobCamel targetCamel = null;

		// --- Check for nearby camels ---
		List<Entity> nearby = world.getEntitiesWithinAABBExcludingEntity(player,
			player.bb.grow(reach, reach, reach));

		for (Entity e : nearby) {
			if (e instanceof MobCamel) {
				MobCamel camel = (MobCamel) e;
				double dx = camel.x - player.x;
				double dy = camel.y - player.y;
				double dz = camel.z - player.z;
				if (dx * dx + dy * dy + dz * dz <= reach * reach) {
					targetCamel = camel;
					break;
				}
			}
		}

		// --- Feed camel if nearby and able to breed ---
		if (targetCamel != null && !targetCamel.isBaby() && targetCamel.breedingCooldown <= 0 && !targetCamel.canBreed) {
			if (!world.isClientSide) {
				targetCamel.interact(player); // purely visual, no stack modification
			}
			stack.consumeItem(player); // consume exactly one tumbleweed
			return stack;
		}

		// --- Not feeding camel: throw tumbleweed ---
		if (!world.isClientSide) {
			EntityTumbleweed tumbleweed = new EntityTumbleweed(world);

			// Spawn at player eye height
			double px = player.x;
			double py = player.y + player.bbHeight;
			double pz = player.z;
			tumbleweed.moveTo(px, py, pz, player.yRot, player.xRot);

			// Throw along player's look vector
			double velocity = 0.5;
			double radYaw = Math.toRadians(player.yRot);
			double radPitch = Math.toRadians(player.xRot);

			tumbleweed.xd = -Math.sin(radYaw) * Math.cos(radPitch) * velocity;
			tumbleweed.yd = -Math.sin(radPitch) * velocity + 0.1;
			tumbleweed.zd = Math.cos(radYaw) * Math.cos(radPitch) * velocity;

			world.entityJoinedWorld(tumbleweed);

			// Play sound
			world.playSoundAtEntity(player, player, "random.bow", 0.5F,
				0.4F / (world.rand.nextFloat() * 0.4F + 0.8F));
		}

		// Consume exactly one tumbleweed
		stack.consumeItem(player);
		return stack;
	}
}
