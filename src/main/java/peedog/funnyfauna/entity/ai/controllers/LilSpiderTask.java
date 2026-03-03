package peedog.funnyfauna.entity.ai.controllers;

import net.minecraft.core.block.Blocks;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.path.WanderTask;
import peedog.funnyfauna.entity.lilspider.EntityLilSpider;

public class LilSpiderTask extends Task<EntityLilSpider> {

	private final WanderTask<EntityLilSpider> wanderTask;

	private enum State { WANDER, IDLE, SHOOT_STRING, RISE }
	private State state = State.WANDER;

	private int stateTimer = 0;
	private int idleTimer  = 0;
	private int ceilingX, ceilingY, ceilingZ;

	/** How many ticks must pass while wandering before a ceiling-check can fire. */
	private static final int WANDER_CEILING_CHECK_MIN = 100;
	/** 1-in-N chance per tick (after the minimum) of attempting a ceiling check. */
	private static final int CEILING_CHECK_CHANCE = 25;

	/** How many ticks without meaningful progress before the spider gives up rising. */
	private static final int STUCK_TIMEOUT = 40;
	/** Minimum distance the spider must close per STUCK_TIMEOUT window to count as progress. */
	private static final double STUCK_PROGRESS_THRESHOLD = 0.3;

	private double lastDistToTarget = Double.MAX_VALUE;
	private int    stuckTimer       = 0;

	public LilSpiderTask(EntityLilSpider mob) {
		super(mob);
		this.wanderTask = new WanderTask<>(mob);
	}

	// ─────────────────────────────────────────────────────────────────────────

	@Override
	protected void onStart() {
		state      = State.WANDER;
		stateTimer = 0;
		idleTimer  = 0;
	}

	@Override
	public Task onTick() {

		// While at home the entity's own tick() handles freezing – nothing to do.
		if (mob.hasHome()) return null;

		switch (state) {

			// ── WANDER ────────────────────────────────────────────────────────
			case WANDER: {
				stateTimer++;

				// Occasionally stop and rest
				if (stateTimer % 80 == 0 && mob.world.rand.nextInt(4) == 0) {
					state     = State.IDLE;
					idleTimer = 30 + mob.world.rand.nextInt(80);
					return null;
				}

				// Rarely look for a ceiling to string up to
				if (stateTimer > WANDER_CEILING_CHECK_MIN && mob.world.rand.nextInt(CEILING_CHECK_CHANCE) == 0) {
					if (findCeiling()) {
						state      = State.SHOOT_STRING;
						stateTimer = 0;
						mob.startShootingString(ceilingX, ceilingY, ceilingZ);
						return null;
					}
					// Back off a bit so we don't spam every tick
					stateTimer = WANDER_CEILING_CHECK_MIN - 100;
				}

				return wanderTask;
			}

			// ── IDLE ─────────────────────────────────────────────────────────
			case IDLE: {
				// Bleed off momentum and wait
				mob.xd *= 0.4;
				mob.zd *= 0.4;
				if (--idleTimer <= 0) {
					state      = State.WANDER;
					stateTimer = 0;
				}
				return null;
			}

			// ── SHOOT_STRING ─────────────────────────────────────────────────
			case SHOOT_STRING: {
				// Stand perfectly still while the silk thread animates upward.
				mob.xd = 0;
				mob.zd = 0;
				// Counteract gravity so the spider doesn't droop mid-animation.
				if (!mob.onGround) mob.yd = 0;

				// stringAnimTick is also incremented in EntityLilSpider.tick() on
				// the client side – here we only drive the server copy.
				stateTimer++;
				mob.stringAnimTick = stateTimer;

				if (stateTimer >= EntityLilSpider.STRING_DURATION) {
					state            = State.RISE;
					stateTimer       = 0;
					lastDistToTarget = Double.MAX_VALUE;
					stuckTimer       = 0;
					mob.setStringStateRising();
				}
				return null;
			}

			// ── RISE ─────────────────────────────────────────────────────────
			case RISE: {
				// Target the BOTTOM of the ceiling air block, not its centre.
				// ceilingY is already the air block directly below the solid ceiling,
				// so targeting (ceilingY + 0.15) keeps the spider safely in that
				// air block without clipping into the solid block above.
				double tx = ceilingX + 0.5;
				double ty = ceilingY + 0.15;
				double tz = ceilingZ + 0.5;

				double dx   = tx - mob.x;
				double dy   = ty - mob.y;
				double dz   = tz - mob.z;
				double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

				if (dist < 0.25) {
					// Arrived – place cobweb and lock in.
					if (mob.world.isAirBlock(ceilingX, ceilingY, ceilingZ)) {
						mob.world.setBlockWithNotify(ceilingX, ceilingY, ceilingZ, Blocks.COBWEB.id());
					}
					mob.setHome(ceilingX, ceilingY, ceilingZ);
					mob.stopString();
					lastDistToTarget = Double.MAX_VALUE;
					stuckTimer       = 0;
					state            = State.WANDER;
					stateTimer       = 0;
					return null;
				}

				// ── Stuck detection ───────────────────────────────────────────
				// Every STUCK_TIMEOUT ticks, check if the spider has actually closed
				// the gap. If it hasn't moved STUCK_PROGRESS_THRESHOLD closer it is
				// probably blocked by a wall – abort and return to wandering.
				stuckTimer++;
				if (stuckTimer >= STUCK_TIMEOUT) {
					stuckTimer = 0;
					if (lastDistToTarget - dist < STUCK_PROGRESS_THRESHOLD) {
						mob.stopString();
						mob.xd = 0; mob.yd = 0; mob.zd = 0;
						lastDistToTarget = Double.MAX_VALUE;
						state            = State.WANDER;
						stateTimer       = WANDER_CEILING_CHECK_MIN; // brief cooldown before next attempt
						return wanderTask;
					}
					lastDistToTarget = dist; // reset baseline for the next window
				}

				// Float upward along the silk thread.
				// The extra +0.08 counteracts Minecraft's standard gravity constant.
				double speed = 0.14;
				mob.xd = dx / dist * speed;
				mob.yd = dy / dist * speed + 0.08;
				mob.zd = dz / dist * speed;
				return null;
			}
		}

		return wanderTask;
	}

	@Override
	protected void onStop(Task interruptTask) {
		mob.stopString();
	}

	// ─────────────────────────────────────────────────────────────────────────

	/**
	 * Scans the surrounding volume for air blocks that are adjacent to a solid
	 * face (ceiling above OR wall on any side). Collects ALL candidates then
	 * picks one at random, so every spider chooses a different spot rather than
	 * always preferring the same direction.
	 */
	private boolean findCeiling() {
		int bx           = (int) mob.x;
		int by           = (int) mob.y;
		int bz           = (int) mob.z;
		int searchRadius = 4;
		int searchHeight = 10;

		java.util.List<int[]> candidates = new java.util.ArrayList<>();

		for (int ox = -searchRadius; ox <= searchRadius; ox++) {
			for (int oz = -searchRadius; oz <= searchRadius; oz++) {
				for (int dy = 1; dy <= searchHeight; dy++) {
					int cx = bx + ox;
					int cy = by + dy;
					int cz = bz + oz;

					if (!mob.world.isAirBlock(cx, cy, cz)) break; // hit solid, stop column

					// Qualify any air block with at least one solid face-neighbour.
					if (hasSolidFaceNeighbour(cx, cy, cz)) {
						candidates.add(new int[]{ cx, cy, cz });
					}
				}
			}
		}

		if (candidates.isEmpty()) return false;

		// Pick randomly so different spiders spread across available spots.
		int[] chosen = candidates.get(mob.world.rand.nextInt(candidates.size()));
		ceilingX = chosen[0];
		ceilingY = chosen[1];
		ceilingZ = chosen[2];
		return true;
	}

	/**
	 * Returns true if any of the 6 face-adjacent blocks is solid.
	 * Handles ceiling attachment (above) and wall attachment (sides).
	 */
	private boolean hasSolidFaceNeighbour(int x, int y, int z) {
		return isSolid(x, y + 1, z)  // ceiling
			|| isSolid(x + 1, y, z)  // east wall
			|| isSolid(x - 1, y, z)  // west wall
			|| isSolid(x, y, z + 1)  // south wall
			|| isSolid(x, y, z - 1); // north wall
	}

	private boolean isSolid(int x, int y, int z) {
		return !mob.world.isAirBlock(x, y, z)
			&& mob.world.getBlockMaterial(x, y, z).isSolid();
	}

	@Override
	protected boolean isEqual(Task other) { return other instanceof LilSpiderTask; }
}
