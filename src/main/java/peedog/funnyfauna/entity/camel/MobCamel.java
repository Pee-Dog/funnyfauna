package peedog.funnyfauna.entity.camel;

import net.minecraft.core.block.Block;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.animal.MobAnimal;
import net.minecraft.core.item.Items;
import net.minecraft.core.util.collection.NamespaceID;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.World;

import java.util.Random;

public class MobCamel extends MobAnimal {

	public MobCamel(World world) {
		super(world);
		this.textureIdentifier = NamespaceID.getPermanent("funnyfauna", "camel");
		this.setSize(1F, 1.8F);
	}
	@Override
	public boolean canSpawnHere() {
		int x = MathHelper.floor(this.x);
		int y = MathHelper.floor(this.bb.minY);
		int z = MathHelper.floor(this.z);

		int id = this.world.getBlockId(x, y - 1, z);

		// Prevent spawning on air, water, lava
		if (id == 0 || id == 8 || id == 9 || id == 10 || id == 11) {
			return false;
		}

		// Allow spawning on any other block
		return true;
	}

	@Override
	public String getLivingSound() {
		return "funnyfauna:mob.camel.idle";
	}

	@Override
	public String getHurtSound() {
		return "funnyfauna:mob.camel.hurt";
	}

	@Override
	public String getDeathSound() {
		return "funnyfauna:mob.camel.death";

	}

}
