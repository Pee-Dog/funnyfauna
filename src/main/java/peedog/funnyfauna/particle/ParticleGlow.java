package peedog.funnyfauna.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.entity.particle.Particle;
import net.minecraft.client.render.LightmapHelper;
import net.minecraft.client.render.tessellator.Tessellator;
import net.minecraft.client.render.texture.stitcher.IconCoordinate;
import net.minecraft.client.render.texture.stitcher.TextureRegistry;
import net.minecraft.core.util.helper.DyeColor;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.World;

@Environment(EnvType.CLIENT)
public class ParticleGlow extends Particle {
	private final float oSize;
	private final float baseR, baseG, baseB; // The color derived from dye metadata

	public ParticleGlow(World world, double x, double y, double z,
						double xd, double yd, double zd, int colorMeta) {
		super(world, x, y, z, xd, yd, zd);

		// Slight random offset, like flame
		this.x += (this.random.nextFloat() - this.random.nextFloat()) * 0.05F;
		this.y += (this.random.nextFloat() - this.random.nextFloat()) * 0.05F;
		this.z += (this.random.nextFloat() - this.random.nextFloat()) * 0.05F;

		// Motion similar to flame: small random addition
		this.xd = this.xd * 0.01 + xd;
		this.yd = this.yd * 0.01 + yd;
		this.zd = this.zd * 0.01 + zd;

		this.oSize = this.size; // store initial size
		this.lifetime = (int) (8.0F / (Math.random() * 0.8 + 0.2)) + 4;
		this.noPhysics = true; // floats in air

		// Set texture – using the firefly sprite (a glowing dot)
		this.tex = TextureRegistry.getTexture("funnyfauna:particle/glow");

		// Extract RGB from dye metadata (0-15) using DyeColor's embedded Color object
		DyeColor dye = DyeColor.colorFromBlockMeta(colorMeta & 0xF);
		this.baseR = dye.color.getRed() / 100.0f;
		this.baseG = dye.color.getGreen() / 100.0f;
		this.baseB = dye.color.getBlue() / 100.0f;

		// Initialise particle color
		this.rCol = baseR;
		this.gCol = baseG;
		this.bCol = baseB;
	}

	@Override
	public void render(Tessellator t, float partialTick,
					   double xOff, double yOff, double zOff,
					   float xa, float ya, float za, float xa2, float za2) {
		// Shrink over time, same as flame
		float s = ((float) this.age + partialTick) / (float) this.lifetime;
		this.size = this.oSize * (1.0F - s * s * 0.5F);

		// Color remains constant (already set)
		super.render(t, partialTick, xOff, yOff, zOff, xa, ya, za, xa2, za2);
	}

	@Override
	public float getBrightness(float partialTick) {
		// Fade from full brightness to world brightness as it ages, like flame
		float decay = MathHelper.clamp(((float) this.age + partialTick) / (float) this.lifetime, 0.0F, 1.0F);
		return super.getBrightness(partialTick) * decay + (1.0F - decay);
	}

	@Override
	public int getLightmapCoord(float partialTick) {
		// Always emit maximum block light (glow even in darkness)
		return LightmapHelper.setBlocklightValue(super.getLightmapCoord(partialTick), 15);
	}

	@Override
	public void tick() {
		this.xo = this.x;
		this.yo = this.y;
		this.zo = this.z;

		if (this.age++ >= this.lifetime) {
			this.remove();
		}

		this.move(this.xd, this.yd, this.zd);

		// Air friction
		this.xd *= 0.96;
		this.yd *= 0.96;
		this.zd *= 0.96;

		if (this.onGround) {
			this.xd *= 0.7;
			this.zd *= 0.7;
		}
	}
}
