package peedog.funnyfauna.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.entity.particle.Particle;
import net.minecraft.client.render.texture.stitcher.TextureRegistry;
import net.minecraft.core.block.material.Material;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.World;

@Environment(EnvType.CLIENT)
public class ParticleBugs extends Particle {

	private static final double GROUND_OFFSET = 0.05;
	private static final double HOME_RADIUS = 0.15; // Radius around home to despawn

	private final float rBase, gBase, bBase;
	private final float oR, oG, oB;

	private final double homeX, homeY, homeZ;
	private double targetXd, targetZd;
	private int changeTimer;

	private boolean returningHome = false;
	private double returnSpeed = 0.05;

	// Animation
	private final int totalFrames = 2;
	private int currentFrame = 0;
	private int frameTimer = 0;
	private final int frameDelay = 5;

	public ParticleBugs(World world, double x, double y, double z, float scale) {
		super(world, x, y, z, 0, 0, 0);
		this.size = scale;
		this.tex = TextureRegistry.getTexture("funnyfauna:particle/bug1");

		// Base color
		this.rBase = 0.8f;
		this.gBase = 0.6f;
		this.bBase = 0.2f;

		this.rCol = rBase;
		this.gCol = gBase;
		this.bCol = bBase;

		this.oR = world.rand.nextFloat() * 0.06f - 0.03f;
		this.oG = world.rand.nextFloat() * 0.06f - 0.03f;
		this.oB = world.rand.nextFloat() * 0.06f - 0.03f;

		// Store home location
		this.homeX = x;
		this.homeY = y;
		this.homeZ = z;

		// Random initial horizontal direction
		double angle = world.rand.nextDouble() * 2 * Math.PI;
		double wanderSpeed = 0.03;
		this.targetXd = Math.cos(angle) * wanderSpeed;
		this.targetZd = Math.sin(angle) * wanderSpeed;
		this.changeTimer = 0;

		// Lifetime long enough to wander and return
		this.lifetime = 1200 + world.rand.nextInt(200); // very long to ensure return
	}

	@Override
	public void tick() {
		super.tick();
		if (this.removed) return;

		// Check if it's time to start returning home
		if (!returningHome && age > lifetime * 0.6) {
			returningHome = true;
		}

		if (returningHome) {
			// Compute vector to home
			double dx = homeX - x;
			double dz = homeZ - z;
			double dist = Math.sqrt(dx * dx + dz * dz);

			if (dist > HOME_RADIUS) {
				// Linear movement toward home at fixed speed
				this.xd = (dx / dist) * returnSpeed;
				this.zd = (dz / dist) * returnSpeed;
			} else {
				// Reached home — remove particle
				this.remove();
				return;
			}
		} else {
			// Randomized wandering with linear speed
			if (changeTimer-- <= 0) {
				double angle = world.rand.nextDouble() * 2 * Math.PI;
				double wanderSpeed = 0.03;
				this.targetXd = Math.cos(angle) * wanderSpeed;
				this.targetZd = Math.sin(angle) * wanderSpeed;
				this.changeTimer = 20 + world.rand.nextInt(20);
			}
			this.xd = targetXd;
			this.zd = targetZd;
		}

		// Move particle horizontally
		this.yd = 0;
		this.move(this.xd, this.yd, this.zd);

		// Smooth vertical snap to ground
		smoothSnapToGround();

		// Oscillate color slightly
		float brightness = (float)(Math.sin(this.age * 0.25f) + 1.0f) / 2.0f;
		this.rCol = MathHelper.clamp(rBase + (brightness - 0.5f) * 0.25f + oR, 0, 1);
		this.gCol = MathHelper.clamp(gBase + (brightness - 0.5f) * 0.25f + oG, 0, 1);
		this.bCol = MathHelper.clamp(bBase + (brightness - 0.5f) * 0.25f + oB, 0, 1);

		// Animate texture frames
		if (++frameTimer >= frameDelay) {
			frameTimer = 0;
			currentFrame = (currentFrame + 1) % totalFrames;
			this.tex = TextureRegistry.getTexture("funnyfauna:particle/bug" + (currentFrame + 1));
		}

		age++;
	}

	private void smoothSnapToGround() {
		int bx = MathHelper.floor(x);
		int bz = MathHelper.floor(z);
		double bestY = y;
		for (int by = MathHelper.floor(y); by >= 0; by--) {
			Material mat = world.getBlockMaterial(bx, by, bz);
			if (mat.isSolidBlocking()) {
				bestY = by + 1.0 + GROUND_OFFSET;
				break;
			}
		}
		// Smooth vertical interpolation
		this.y += (bestY - y) * 0.3;
	}
}
