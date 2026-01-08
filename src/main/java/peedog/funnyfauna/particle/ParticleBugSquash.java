package peedog.funnyfauna.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.entity.particle.Particle;
import net.minecraft.client.render.tessellator.Tessellator;
import net.minecraft.client.render.texture.stitcher.TextureRegistry;
import net.minecraft.core.world.World;

@Environment(EnvType.CLIENT)
public class ParticleBugSquash extends Particle {

	public ParticleBugSquash(World world, double x, double y, double z) {
		super(world, x, y, z, 0, 0, 0);

		// Use TextureRegistry instead of passing IconCoordinate
		this.tex = TextureRegistry.getTexture("funnyfauna:particle/bug_squash");

		this.gravity = 0.0F;
		this.lifetime = 40;
		this.size = 1.0F;

		// Lock it to ground
		this.xd = this.yd = this.zd = 0;
		this.noPhysics = true;
		this.y -= 0.01; // avoid z-fighting
	}

	@Override
	public void tick() {
		if (++age >= lifetime) {
			remove();
		}
	}

	@Override
	public void render(
		Tessellator t,
		float partialTick,
		double xOff,
		double yOff,
		double zOff,
		float xa,
		float ya,
		float za,
		float xa2,
		float za2
	) {
		float u0 = (float) tex.getIconUMin();
		float u1 = (float) tex.getIconUMax();
		float v0 = (float) tex.getIconVMin();
		float v1 = (float) tex.getIconVMax();

		float x = (float)(this.x - xOff);
		float y = (float)(this.y - yOff);
		float z = (float)(this.z - zOff);
		float r = 0.25F;

		t.setColorOpaque_F(1F, 1F, 1F);

		// Flat quad on XZ plane (footprint)
		t.addVertexWithUV(x - r, y, z - r, u0, v1);
		t.addVertexWithUV(x - r, y, z + r, u0, v0);
		t.addVertexWithUV(x + r, y, z + r, u1, v0);
		t.addVertexWithUV(x + r, y, z - r, u1, v1);
	}
}
