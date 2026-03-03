

package peedog.funnyfauna.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.entity.particle.Particle;
import net.minecraft.client.render.tessellator.Tessellator;
import net.minecraft.client.render.texture.stitcher.TextureRegistry;
import net.minecraft.core.world.World;

@Environment(EnvType.CLIENT)
public class ParticlePlume extends Particle {
	private final float oSize;

	public ParticlePlume(World world, double x, double y, double z, double xa, double ya, double za) {
		this(world, x, y, z, xa, ya, za, 1.0F);
	}

	public ParticlePlume(World world, double x, double y, double z, double xa, double ya, double za, float scale) {
		super(world, x, y, z, (double)0.0F, (double)0.0F, (double)0.0F);
		this.xd *= 0.1;
		this.yd *= 0.1;
		this.zd *= 0.1;
		this.xd += xa;
		this.yd += ya;
		this.zd += za;
		this.rCol = this.gCol = this.bCol = (float)(Math.random() * 0.3);
		this.size *= 1F;
		this.size *= scale;
		this.oSize = this.size;
		this.lifetime = (int)((double)10.0F / (Math.random() * 0.8 + 0.2));
		this.lifetime *= (int)scale;
		this.noPhysics = false;
	}

	public void render(Tessellator t, float partialTick, double xOff, double yOff, double zOff, float xa, float ya, float za, float xa2, float za2) {
		float l = ((float)this.age + partialTick) / (float)this.lifetime * 32.0F;
		if (l < 0.0F) {
			l = 0.0F;
		}

		if (l > 1.0F) {
			l = 1.0F;
		}

		this.size = this.oSize * l;
		super.render(t, partialTick, xOff, yOff, zOff, xa, ya, za, xa2, za2);
	}

	public void tick() {
		this.xo = this.x;
		this.yo = this.y;
		this.zo = this.z;
		if (this.age++ >= this.lifetime) {
			this.remove();
		}

		int val = 5 - this.age * 10 / this.lifetime;
		if (val >= 0) {
			this.tex = TextureRegistry.getTexture("funnyfauna:particle/plume_" + val);
		} else {
			this.tex = null;
		}

		this.yd += 0.001;
		this.move(this.xd, this.yd, this.zd);
		if (this.y == this.yo) {
			this.xd *= 1.1;
			this.zd *= 1.1;
		}

		this.xd *= 0.96;
		this.yd *= 0.7;
		this.zd *= 0.96;
		if (this.onGround) {
			this.xd *= 0.7;
			this.zd *= 0.7;
		}

	}
}
