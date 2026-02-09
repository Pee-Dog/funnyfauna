package peedog.funnyfauna.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.entity.particle.Particle;
import net.minecraft.client.render.LightmapHelper;
import net.minecraft.client.render.tessellator.Tessellator;
import net.minecraft.client.render.texture.stitcher.TextureRegistry;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.World;
import peedog.funnyfauna.block.FunnyFaunaBlocks;

import java.util.HashSet;
import java.util.Set;

@Environment(EnvType.CLIENT)
public class ParticleCricket extends Particle {

	private final double baseX, baseY, baseZ;
	private static final float CRICKET_SCALE = 0.6F;
	private static final double MAX_PLAYER_DISTANCE = 16.0;
	private static final Set<Long> ACTIVE_JARS = new HashSet<>();

	private final long key;
	private boolean registered = false;

	private final float rColParticle, gColParticle, bColParticle;

	private static long jarKey(int x, int y, int z) {
		return (((long)x & 0x3FFFFFF) << 38) | (((long)z & 0x3FFFFFF) << 12) | (y & 0xFFF);
	}

	public ParticleCricket(World world, double x, double y, double z, int color) {
		super(world, x, y, z, 0.0, 0.0, 0.0);

		this.baseX = MathHelper.floor(x);
		this.baseY = MathHelper.floor(y);
		this.baseZ = MathHelper.floor(z);
		this.key = jarKey((int) baseX, (int) baseY, (int) baseZ);

		this.noPhysics = true;
		this.gravity = 0.0F;
		this.lifetime = Integer.MAX_VALUE;

		this.setScale(CRICKET_SCALE);
		this.tex = TextureRegistry.getTexture("funnyfauna:particle/cricket");

		// Convert 0xRRGGBB → float RGB
		float rf = ((color >> 16) & 0xFF) / 255f;
		float gf = ((color >> 8) & 0xFF) / 255f;
		float bf = (color & 0xFF) / 255f;

		this.rCol = rf;
		this.gCol = gf;
		this.bCol = bf;

		this.rColParticle = rf;
		this.gColParticle = gf;
		this.bColParticle = bf;

		// Lock in jar center
		this.setPos(baseX + 0.5, baseY + 0.15, baseZ + 0.5);
	}

	@Override
	public void tick() {
		// Singleton per jar
		if (!registered) {
			if (ACTIVE_JARS.contains(key)) {
				this.remove();
				return;
			}
			ACTIVE_JARS.add(key);
			registered = true;
		}

		this.xo = this.x;
		this.yo = this.y;
		this.zo = this.z;

		// Remove if jar gone
		if (world.getBlockId((int) baseX, (int) baseY, (int) baseZ) != FunnyFaunaBlocks.JAR_CRICKET.id()) {
			this.remove();
			return;
		}

		// Player proximity check
		boolean nearby = false;
		for (Object obj : world.loadedEntityList) {
			if (obj instanceof Player) {
				Player p = (Player) obj;
				double dx = p.x - (baseX + 0.5);
				double dy = p.y - (baseY + 0.5);
				double dz = p.z - (baseZ + 0.5);
				if (dx*dx + dy*dy + dz*dz <= MAX_PLAYER_DISTANCE*MAX_PLAYER_DISTANCE) {
					nearby = true;
					break;
				}
			}
		}

		if (!nearby) {
			this.remove();
			return;
		}

		// Keep particle locked in jar
		this.setPos(baseX + 0.5, baseY + 0.15, baseZ + 0.5);
	}

	@Override
	public void render(Tessellator t, float partialTick,
					   double xOff, double yOff, double zOff,
					   float xa, float ya, float za,
					   float xa2, float za2) {

		if (this.tex == null) return;

		// Use full texture coords to avoid cut-off issues
		float u0 = (float)this.tex.getIconUMin();
		float u2 = (float)this.tex.getIconUMax();
		float v0 = (float)this.tex.getIconVMin();
		float v2 = (float)this.tex.getIconVMax();

		float r = 0.1F * this.size;
		float x = (float)(xo + (this.x - xo) * partialTick - xOff);
		float y = (float)(yo + (this.y - yo) * partialTick - yOff);
		float z = (float)(zo + (this.z - zo) * partialTick - zOff);

		float br = 1.0F;
		if (LightmapHelper.isLightmapEnabled()) {
			t.setLightmapCoord(this.getLightmapCoord(partialTick));
		} else {
			br = this.getBrightness(partialTick);
		}

		t.setColorOpaque_F(br * rColParticle, br * gColParticle, br * bColParticle);

		t.addVertexWithUV(x - xa*r - xa2*r, y - ya*r, z - za*r - za2*r, u0, v2);
		t.addVertexWithUV(x - xa*r + xa2*r, y + ya*r, z - za*r + za2*r, u0, v0);
		t.addVertexWithUV(x + xa*r + xa2*r, y + ya*r, z + za*r + za2*r, u2, v0);
		t.addVertexWithUV(x + xa*r - xa2*r, y - ya*r, z + za*r - za2*r, u2, v2);
	}

	@Override
	public void remove() {
		super.remove();
		if (registered) ACTIVE_JARS.remove(this.key);
	}
}
