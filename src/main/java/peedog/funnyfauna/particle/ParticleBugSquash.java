package peedog.funnyfauna.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.entity.particle.Particle;
import net.minecraft.client.render.tessellator.Tessellator;
import net.minecraft.client.render.texture.stitcher.TextureRegistry;
import net.minecraft.core.world.World;

@Environment(EnvType.CLIENT)
public class ParticleBugSquash extends Particle {

	private final float rotation; // rotation around vertical axis

	public ParticleBugSquash(World world, double x, double y, double z, double xd, double yd, double zd, float scale) {
		super(world, x, y, z, 0, 0, 0); // no motion
		this.size = scale;
		this.tex = TextureRegistry.getTexture("funnyfauna:particle/bug_squash"); // must exist!
		this.lifetime = 100;

		// Random rotation around vertical axis (Y-axis)
		this.rotation = world.rand.nextFloat() * 2.0f * (float)Math.PI;

		// Ensure no motion
		this.xd = this.yd = this.zd = 0;
	}

	@Override
	public void tick() {
		super.tick();
		if (age++ >= lifetime) this.remove();
		// Fixed in place
		this.xd = this.yd = this.zd = 0;
	}

	@Override
	public void render(Tessellator t, float partialTick, double xOff, double yOff, double zOff,
					   float xa, float ya, float za, float xa2, float za2) {
		if (this.tex == null) return;

		// Particle position interpolated
		double px = this.xo + (this.x - this.xo) * partialTick - xOff;
		double py = this.yo + (this.y - this.yo) * partialTick - yOff;
		double pz = this.zo + (this.z - this.zo) * partialTick - zOff;

		float halfSize = this.size * 0.5f;

		// Texture coordinates
		float u0 = (float) this.tex.getIconUMin();
		float u2 = (float) this.tex.getIconUMax();
		float v0 = (float) this.tex.getIconVMin();
		float v2 = (float) this.tex.getIconVMax();

		t.setColorOpaque_F(this.rCol, this.gCol, this.bCol);

		// Rotate only in XZ plane
		double cos = Math.cos(rotation) * halfSize;
		double sin = Math.sin(rotation) * halfSize;

		t.addVertexWithUV(px - cos, py, pz - sin, u0, v2);
		t.addVertexWithUV(px - cos, py, pz + sin, u0, v0);
		t.addVertexWithUV(px + cos, py, pz + sin, u2, v0);
		t.addVertexWithUV(px + cos, py, pz - sin, u2, v2);
	}
}
