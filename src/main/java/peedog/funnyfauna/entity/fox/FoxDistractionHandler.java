package peedog.funnyfauna.entity.fox;

import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.Mob;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.util.phys.AABB;

import java.util.Map;
import java.util.WeakHashMap;

public class FoxDistractionHandler {

	public static void alertFoxes(Player owner, Mob attacker) {
		AABB box = AABB.getTemporaryBB(
			owner.x, owner.y, owner.z,
			owner.x + 1, owner.y + 1, owner.z + 1
		).grow(16, 6, 16);

		for (MobFox fox : owner.world.getEntitiesWithinAABB(MobFox.class, box)) {
			if (!fox.isFoxTamed()) continue;
			if (!owner.uuid.equals(fox.getFoxOwner())) continue;
			if (fox.isFoxSitting()) continue;

			fox.beginDistraction(attacker);
		}
	}
}
