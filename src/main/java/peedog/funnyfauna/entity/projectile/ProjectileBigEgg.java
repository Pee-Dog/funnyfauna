package peedog.funnyfauna.entity.projectile;

import net.minecraft.core.entity.Mob;
import net.minecraft.core.entity.projectile.Projectile;
import net.minecraft.core.item.Items;
import net.minecraft.core.world.World;

public class ProjectileBigEgg extends Projectile {
	public ProjectileBigEgg(World world) {
		super(world);
	}

	public ProjectileBigEgg(World world, Mob owner) {
		super(world, owner);
	}

	public ProjectileBigEgg(World world, double x, double y, double z) {
		super(world, x, y, z);
	}
}
