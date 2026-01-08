package peedog.funnyfauna.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.entity.particle.Particle;
import net.minecraft.client.render.texture.stitcher.TextureRegistry;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.World;
import peedog.funnyfauna.block.FunnyFaunaBlocks;

@Environment(EnvType.CLIENT)
public class ParticleCricket extends Particle {

	private final double baseX;
	private final double baseY;
	private final double baseZ;

	public ParticleCricket(World world, double x, double y, double z) {
		super(world, x, y, z, 0, 0, 0);

		// Lock position
		this.baseX = x;
		this.baseY = y;
		this.baseZ = z;

		// Disable physics & motion
		this.xd = 0;
		this.yd = 0;
		this.zd = 0;
		this.noPhysics = true;
		this.gravity = 0;

		// Visuals
		this.setScale(0.6F);
		this.rCol = this.gCol = this.bCol = 1.0F;

		this.tex = TextureRegistry.getTexture("funnyfauna:particle/cricket/cricket1_a");

		// Effectively infinite
		this.lifetime = Integer.MAX_VALUE;
	}

	@Override
	public void tick() {
		this.xo = this.x;
		this.yo = this.y;
		this.zo = this.z;

		this.age++;

		// Kill if not in cricket jar
		int xi = MathHelper.floor(this.baseX);
		int yi = MathHelper.floor(this.baseY);
		int zi = MathHelper.floor(this.baseZ);

		if (this.world.getBlockId(xi, yi, zi) != FunnyFaunaBlocks.JAR_CRICKET.id()) {
			this.remove();
			return;
		}

		// Gentle hop animation
		double hop = Math.sin(this.age * 0.1) * 0.2;
		this.setPos(baseX, baseY + hop, baseZ);
	}
}
