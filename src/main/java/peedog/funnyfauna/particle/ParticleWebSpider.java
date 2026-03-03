package peedog.funnyfauna.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.entity.particle.Particle;
import net.minecraft.client.render.LightmapHelper;
import net.minecraft.client.render.tessellator.Tessellator;
import net.minecraft.client.render.texture.stitcher.TextureRegistry;
import net.minecraft.core.block.Blocks;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.World;

import java.util.HashSet;
import java.util.Set;

@Environment(EnvType.CLIENT)
public class ParticleWebSpider extends Particle {

	private static final Set<Long> ACTIVE_WEBS = new HashSet<>();

	private final long key;
	private boolean registered = false;

	private final double baseX, baseY, baseZ;

	private float crawlTimer = 0f;
	private float idleTimer = 0f;

	private boolean crawling = false;

	private double dirX = 0;
	private double dirY = 0;

	private float animTimer = 0f;

	private static long webKey(int x, int y, int z) {
		return (((long)x & 0x3FFFFFF) << 38)
			| (((long)z & 0x3FFFFFF) << 12)
			| (y & 0xFFF);
	}

	public ParticleWebSpider(World world, double x, double y, double z) {
		super(world, x, y, z, 0, 0, 0);

		this.baseX = MathHelper.floor(x);
		this.baseY = MathHelper.floor(y);
		this.baseZ = MathHelper.floor(z);

		this.key = webKey((int) baseX, (int) baseY, (int) baseZ);

		this.noPhysics = true;
		this.gravity = 0;
		this.lifetime = Integer.MAX_VALUE;

		this.size = 0.6F;
		this.setPos(baseX + 0.5, baseY + 0.5, baseZ + 0.5);

		this.tex = TextureRegistry.getTexture("funnyfauna:particle/spider1");
	}

	@Override
	public void tick() {

		// ── Singleton per web ─────────────────────
		if (!registered) {
			if (ACTIVE_WEBS.contains(key)) {
				this.remove();
				return;
			}
			ACTIVE_WEBS.add(key);
			registered = true;
		}

		this.xo = this.x;
		this.yo = this.y;
		this.zo = this.z;

		// Remove if cobweb gone
		if (world.getBlockId((int) baseX, (int) baseY, (int) baseZ)
			!= Blocks.COBWEB.id()) {
			this.remove();
			return;
		}

		// ── Idle vs Crawling State ─────────────────

		if (!crawling) {
			idleTimer++;

			if (idleTimer > 40 + random.nextInt(80)) {
				crawling = true;
				idleTimer = 0;

				// Pick random crawl direction
				float angle = random.nextFloat() * (float)Math.PI * 2f;
				dirX = Math.cos(angle);
				dirY = Math.sin(angle);
			}

		} else {

			crawlTimer++;
			animTimer++;

			double speed = 0.01;

			this.x += dirX * speed;
			this.y += dirY * speed;

			// Stay inside block radius
			double cx = baseX + 0.5;
			double cy = baseY + 0.5;

			double dx = this.x - cx;
			double dy = this.y - cy;

			double radius = 0.25;

			if (dx*dx + dy*dy > radius*radius) {
				crawling = false;
				crawlTimer = 0;
			}
		}
	}

	@Override
	public void render(Tessellator t, float partialTick,
					   double xOff, double yOff, double zOff,
					   float xa, float ya, float za,
					   float xa2, float za2) {

		float x = (float)(xo + (this.x - xo) * partialTick - xOff);
		float y = (float)(yo + (this.y - yo) * partialTick - yOff);
		float z = (float)(zo + (this.z - zo) * partialTick - zOff);

		float r = 0.12F;

		// ── Alternate texture frames ──────────────
		int frame = (int)(animTimer / 6f) % 2;
		this.tex = TextureRegistry.getTexture(
			"funnyfauna:particle/spider" + (frame + 1)
		);

		float u0 = (float)tex.getIconUMin();
		float u1 = (float)tex.getIconUMax();
		float v0 = (float)tex.getIconVMin();
		float v1 = (float)tex.getIconVMax();

		float br = 1.0F;
		if (LightmapHelper.isLightmapEnabled()) {
			t.setLightmapCoord(this.getLightmapCoord(partialTick));
		} else {
			br = this.getBrightness(partialTick);
		}

		t.setColorOpaque_F(br, br, br);

		// ---- ROTATION ----
		float angle = (float)Math.atan2(dirY, dirX) + 90;
		float cos = MathHelper.cos(angle);
		float sin = MathHelper.sin(angle);

		// define quad corners in local particle plane
		float[][] corners = {
			{-r, -r}, {-r, r}, {r, r}, {r, -r}
		};

		float[] u = {u0, u0, u1, u1};
		float[] v = {v1, v0, v0, v1};

		for (int i = 0; i < 4; i++) {
			float rx = corners[i][0] * cos - corners[i][1] * sin;
			float ry = corners[i][0] * sin + corners[i][1] * cos;

			corners[i][0] = rx;
			corners[i][1] = ry;
		}

		// add vertices
		t.addVertexWithUV(x + corners[0][0]*xa + corners[0][1]*xa2,
			y + corners[0][1],
			z + corners[0][0]*za + corners[0][1]*za2, u[0], v[0]);

		t.addVertexWithUV(x + corners[1][0]*xa + corners[1][1]*xa2,
			y + corners[1][1],
			z + corners[1][0]*za + corners[1][1]*za2, u[1], v[1]);

		t.addVertexWithUV(x + corners[2][0]*xa + corners[2][1]*xa2,
			y + corners[2][1],
			z + corners[2][0]*za + corners[2][1]*za2, u[2], v[2]);

		t.addVertexWithUV(x + corners[3][0]*xa + corners[3][1]*xa2,
			y + corners[3][1],
			z + corners[3][0]*za + corners[3][1]*za2, u[3], v[3]);
	}
	@Override
	public void remove() {
		super.remove();
		if (registered) ACTIVE_WEBS.remove(key);
	}
}
