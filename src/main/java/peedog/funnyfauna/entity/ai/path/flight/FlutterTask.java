package peedog.funnyfauna.entity.ai.path.flight;

import net.minecraft.core.util.helper.MathHelper;
import peedog.funnyfauna.entity.MobTaskrunner;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.interfaces.IFlyable;
import peedog.funnyfauna.entity.moth.MobMoth;

import java.util.Random;

public class FlutterTask<T extends MobTaskrunner & IFlyable> extends Task<T> {

	private static final double MIN_HEIGHT = 0.0; // was 2.0 — moths shouldn't skim the ground
	private static final double MAX_HEIGHT = 3.0;

	// Raised from 0.35 — moths should dart quickly
	private static final float MAX_SPEED = 0.35F;

	// How close the moth tries to orbit a light source before the tangential
	// force dominates and starts circling it
	private static final double ORBIT_RADIUS = 2.5;

	// Chance per tick to perch when bumping a wall (1 in N)
	private static final int WALL_PERCH_CHANCE = 2;

	private final Random rand = new Random();

	private double hoverTargetY;
	private int hoverChangeTimer = 0;

	// Cached best light position this tick
	private boolean hasLightTarget = false;
	private double lightX, lightY, lightZ;

	public FlutterTask(T mob) {
		super(mob);
	}

	@Override
	protected void onStart() {
		mob.setFlying(true);
		mob.setFlightTime(0);
		mob.setGroundY(getGroundHeight());
		chooseNewHoverHeight();
		mob.yRot = rand.nextFloat() * 360F;
	}

	@Override
	protected Task onTick() {
		mob.setFlightTime(mob.getFlightTime() + 1);
		mob.setGroundY(getGroundHeight());

		hoverChangeTimer--;
		if (hoverChangeTimer <= 0) chooseNewHoverHeight();

		// Find the best nearby light source once per tick so all methods share it
		scanForLight();

		applyFlutter();
		applyLightAttraction();
		applyHeightControl();
		handleCollisionLanding();

		return null;
	}

	@Override
	protected void onStop(Task interruptTask) {
	}

	private void chooseNewHoverHeight() {
		double ground = mob.getGroundY();
		hoverTargetY = ground + MIN_HEIGHT + rand.nextDouble() * (MAX_HEIGHT - MIN_HEIGHT);
		// Vary interval more — short bursts followed by longer drifts
		hoverChangeTimer = 20 + rand.nextInt(80);
	}

	// ------------------------------------------------------------------
	// Flutter — erratic directional jitter
	// ------------------------------------------------------------------

	private void applyFlutter() {
		if (mob instanceof MobMoth && ((MobMoth) mob).isPerched()) {
			mob.xd = 0;
			mob.yd = 0;
			mob.zd = 0;
			return;
		}

		// Larger yaw swing — moths change direction sharply
		mob.yRot += rand.nextFloat() * 60F - 30F; // ±30° per tick (was ±20°)
		double rad = Math.toRadians(mob.yRot);

		// Stronger forward impulse
		mob.xd += -Math.sin(rad) * 0.10; // was 0.05
		mob.zd +=  Math.cos(rad) * 0.10;

		// More pronounced side/vertical jitter
		mob.xd += (rand.nextFloat() - 0.5) * 0.06; // was 0.03
		mob.zd += (rand.nextFloat() - 0.5) * 0.06;
		mob.yd += (rand.nextFloat() - 0.5) * 0.04; // was 0.02

		// Clamp horizontal speed
		double speed = Math.sqrt(mob.xd * mob.xd + mob.zd * mob.zd);
		if (speed > MAX_SPEED) {
			mob.xd = (mob.xd / speed) * MAX_SPEED;
			mob.zd = (mob.zd / speed) * MAX_SPEED;
		}

		// Slightly less drag so momentum carries through — feels more purposeful
		mob.xd *= 0.92; // was 0.96
		mob.zd *= 0.92;
		mob.yd *= 0.90; // was 0.96
	}

	// ------------------------------------------------------------------
	// Light scanning — called once per tick, results shared below
	// ------------------------------------------------------------------

	private void scanForLight() {
		int searchRadius = 10;
		int bestX = 0, bestY = 0, bestZ = 0, bestEmission = 0;
		int mobX = MathHelper.floor(mob.x);
		int mobY = MathHelper.floor(mob.y);
		int mobZ = MathHelper.floor(mob.z);

		for (int x = -searchRadius; x <= searchRadius; x++) {
			for (int y = -5; y <= 5; y++) {
				for (int z = -searchRadius; z <= searchRadius; z++) {
					int lx = mobX + x, ly = mobY + y, lz = mobZ + z;
					int blockId = mob.world.getBlockId(lx, ly, lz);
					if (blockId <= 0) continue;
					// Use emitted light, not received — immune to daytime sky light
					int emission = net.minecraft.core.block.Blocks.lightEmission[blockId];
					if (emission >= 10 && emission > bestEmission) {
						bestEmission = emission;
						bestX = lx;
						bestY = ly;
						bestZ = lz;
					}
				}
			}
		}

		hasLightTarget = bestEmission >= 10;
		if (hasLightTarget) {
			lightX = bestX + 0.5;
			lightY = bestY + 0.5;
			lightZ = bestZ + 0.5;
		}
	}

	// ------------------------------------------------------------------
	// Light attraction — spiral/orbit behaviour
	//
	// When far away  → pull strongly toward the source
	// Near ORBIT_RADIUS → apply a tangential (perpendicular) force so the
	//                      moth circles the light rather than flying into it
	// Very close      → gentle repulsion keeps it from clipping inside
	// ------------------------------------------------------------------

	private void applyLightAttraction() {
		if (!hasLightTarget) return;

		double dx = lightX - mob.x;
		double dy = lightY - mob.y;
		double dz = lightZ - mob.z;
		double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

		if (dist < 0.3) return; // already basically on top

		// Normalised radial direction (toward light)
		double nx = dx / dist;
		double ny = dy / dist;
		double nz = dz / dist;

		// Tangential direction (perpendicular to radial in the XZ plane).
		// (-nz, 0, nx) is always 90° sideways — this makes it orbit.
		double tx = -nz;
		double tz =  nx;

		if (dist > ORBIT_RADIUS) {
			// Outside orbit radius: pull toward the light, still add a little
			// tangential spin so it spirals inward rather than beelining
			double pull = 0.06;
			double spin = 0.03;
			mob.xd += nx * pull + tx * spin;
			mob.yd += ny * pull * 0.5; // vertical pull is gentler
			mob.zd += nz * pull + tz * spin;
		} else if (dist > 0.8) {
			// Inside orbit radius: tangential dominates → circles the light
			double spin = 0.07;
			double radial = (dist - ORBIT_RADIUS) * 0.04; // slight inward/outward correction
			mob.xd += tx * spin + nx * radial;
			mob.yd += ny * 0.02;
			mob.zd += tz * spin + nz * radial;
		}
		// else: too close, let flutter push it away naturally

		mob.setFlying(true);
	}

	// ------------------------------------------------------------------
	// Height control
	// ------------------------------------------------------------------

	private void applyHeightControl() {
		double targetY = hasLightTarget ? lightY : hoverTargetY;

		// Emergency floor: if the moth is skimming too close to the ground, shove it
		// upward hard regardless of the spring target. This prevents the moth from
		// drifting back to ground level before the spring has time to work.
		if (mob.y < mob.getGroundY() + 1.5) {
			mob.yd += 0.12;
		}

		double dy = targetY - mob.y;
		mob.yd += dy * 0.05; // slightly stronger spring than before (was 0.04)
		mob.yd *= 0.90;
		mob.yd = MathHelper.clamp(mob.yd, -0.35, 0.35);
	}

	// ------------------------------------------------------------------
	// Wall collision — bounce direction, randomly perch
	// ------------------------------------------------------------------

	private void handleCollisionLanding() {
		// Already perched — hard stop
		if (mob instanceof MobMoth) {
			MobMoth moth = (MobMoth) mob;
			if (moth.isPerched()) {
				mob.xd = 0;
				mob.yd = 0;
				mob.zd = 0;
				return;
			}
		}

		// Ground landing — skip when attracted to a light source (moth should keep orbiting).
		// During daytime, land quickly after a short panic flutter (20 ticks ≈ 1 second).
		// At night, stay airborne longer (60 ticks ≈ 3 seconds).
		boolean isDaytime = (mob instanceof MobMoth) && ((MobMoth) mob).shouldPerchInBrightDaylight();
		int landingGuard = isDaytime ? 20 : 60;
		if (mob.onGround && mob.getFlightTime() > landingGuard && !hasLightTarget) {
			mob.setFlying(false);
			if (mob instanceof MobMoth) {
				MobMoth moth = (MobMoth) mob;
				moth.setPerched(true);
				moth.setVerticalLanding(false);
			}
			mob.xd = mob.yd = mob.zd = 0;
			return;
		}

		if (!(mob instanceof MobMoth)) return;
		MobMoth moth = (MobMoth) mob;

		int bx = MathHelper.floor(mob.x);
		int by = MathHelper.floor(mob.y);
		int bz = MathHelper.floor(mob.z);

		int signX = (int) Math.signum(mob.xd);
		int signZ = (int) Math.signum(mob.zd);

		// The wall block the moth is heading into
		int wallBlockX = bx + signX;
		int wallBlockZ = bz + signZ;

		boolean wallX = signX != 0 && moth.world.isBlockOpaqueCube(wallBlockX, by, bz);
		boolean wallZ = signZ != 0 && moth.world.isBlockOpaqueCube(bx, by, wallBlockZ);

		if (wallX || wallZ) {
			if (rand.nextInt(WALL_PERCH_CHANCE) == 0) {
				// --- PERCH on the wall ---
				// Pin is computed from the wall BLOCK's face, not from floor(mob.x).
				// halfW = 0.2 (half the 0.4 hitbox). Centre sits halfW inside the face.
				//   signX > 0: moth moving +x, wall face is the -x side of wallBlockX
				//              → face at x = wallBlockX,     pin = wallBlockX - halfW
				//   signX < 0: moth moving -x, wall face is the +x side of wallBlockX
				//              → face at x = wallBlockX + 1, pin = wallBlockX + 1 + halfW
				final double halfW = 0.05;
				double pinX = mob.x;
				double pinZ = mob.z;

				if (wallX) {
					pinX = (signX > 0) ? (wallBlockX - halfW) : (wallBlockX + 1 + halfW);
				}
				if (wallZ) {
					pinZ = (signZ > 0) ? (wallBlockZ - halfW) : (wallBlockZ + 1 + halfW);
				}

				moth.setPerchedPin(pinX, pinZ);
				mob.x = pinX;
				mob.z = pinZ;
				moth.setFlying(false);
				moth.setPerched(true);
				moth.setVerticalLanding(true);
				mob.xd = 0;
				mob.yd = 0;
				mob.zd = 0;
			} else {
				// --- BOUNCE off the wall ---
				// Reflect whichever velocity component caused the hit and add
				// a little random scatter so it doesn't just ping-pong back.
				if (wallX) {
					mob.xd = -mob.xd * 0.6 + (rand.nextFloat() - 0.5F) * 0.1F;
				}
				if (wallZ) {
					mob.zd = -mob.zd * 0.6 + (rand.nextFloat() - 0.5F) * 0.1F;
				}
				// Give a slight upward nudge — moths flutter up when startled
				mob.yd += 0.05 + rand.nextFloat() * 0.05;

				// Steer yaw away from the wall so the flutter logic keeps it clear
				mob.yRot += 90F + rand.nextFloat() * 90F;
			}
		}
	}

	// ------------------------------------------------------------------
	// Helpers
	// ------------------------------------------------------------------

	private double getGroundHeight() {
		int bx = MathHelper.floor(mob.x);
		int bz = MathHelper.floor(mob.z);
		int by = MathHelper.floor(mob.y);

		while (by > 0) {
			if (mob.world.isBlockOpaqueCube(bx, by, bz)) {
				return by + 1.0;
			}
			by--;
		}
		return 1.0;
	}

	@Override
	protected boolean isEqual(Task other) {
		return other instanceof FlutterTask;
	}
}
