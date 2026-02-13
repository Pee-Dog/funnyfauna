package peedog.funnyfauna.world;

import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.World;
import net.minecraft.core.world.biome.Biome;
import net.minecraft.core.world.biome.Biomes;
import net.minecraft.core.world.weather.Weather;
import net.minecraft.core.world.weather.Weathers;
import peedog.funnyfauna.entity.bird.MobBird;
import peedog.funnyfauna.entity.bunny.MobBunny;
import peedog.funnyfauna.entity.cricket.EntityCricket;
import peedog.funnyfauna.entity.tumbleweed.EntityTumbleweed;
import peedog.funnyfauna.entity.worm.EntityWorm;

import java.util.HashMap;
import java.util.Map;

public class AmbientSpawn {

	/* ---------------- CONFIG ---------------- */

	private static final int SPAWN_RADIUS = 32;

	private static final int BASE_CRICKETS = 10;
	private static final int BASE_WORMS = 10;
	private static final int BASE_TUMBLEWEEDS = 3;
	private static final int BASE_BIRD_FLOCKS = 2;
	private static final int BASE_BUNNIES = 20;

	private static final int CRICKET_CHANCE = 70;
	private static final int WORM_CHANCE = 70;
	private static final int TUMBLEWEED_CHANCE = 300;
	private static final int BIRD_FLOCK_CHANCE = 140;
	private static final int BUNNY_CHANCE = 70;

	private static final int BIRD_MIN_FLOCK = 4;
	private static final int BIRD_MAX_FLOCK = 6;
	private static final int BUNNY_MIN_GROUP = 4;
	private static final int BUNNY_MAX_GROUP = 7;

	/* ---------------------------------------- */

	private static final Map<Class<? extends Entity>, Integer> spawnCounts = new HashMap<>();

	public static void tick(World world) {
		if (world.isClientSide) return;

		Weather weather = world.getCurrentWeather();
		int playerCount = Math.max(1, world.players.size());

		// Reset counts
		spawnCounts.clear();
		count(world, EntityCricket.class);
		count(world, EntityWorm.class);
		count(world, EntityTumbleweed.class);
		count(world, MobBird.class);
		count(world, MobBunny.class);

		for (Player player : world.players) {

			// --- Keep the existing per-player random skip for crickets/worms/tumbleweeds/birds ---
			double x = player.x + world.rand.nextInt(SPAWN_RADIUS * 2) - SPAWN_RADIUS;
			double z = player.z + world.rand.nextInt(SPAWN_RADIUS * 2) - SPAWN_RADIUS;
			int ix = MathHelper.floor(x);
			int iz = MathHelper.floor(z);
			int y = getSafeSpawnY(world, ix, iz);

			Biome biome = world.getBlockBiome(ix, y, iz);

			/* ---------- CAPS ---------- */
			int cricketCap = Math.max(BASE_CRICKETS, BASE_CRICKETS * playerCount);
			int wormCap = Math.max(BASE_WORMS, BASE_WORMS * playerCount);
			int tumbleweedCap = Math.max(BASE_TUMBLEWEEDS, BASE_TUMBLEWEEDS * playerCount);
			int birdCap = Math.max(BASE_BIRD_FLOCKS * BIRD_MAX_FLOCK, BASE_BIRD_FLOCKS * BIRD_MAX_FLOCK * playerCount);
			int bunnyCap = Math.max(BASE_BUNNIES, BASE_BUNNIES * playerCount);

			/* ---------- WORMS ---------- */
			if ((weather == Weathers.OVERWORLD_RAIN || weather == Weathers.OVERWORLD_STORM)
				&& world.rand.nextInt(WORM_CHANCE) == 0
				&& getCount(EntityWorm.class) < wormCap) {

				if (world.getClosestPlayer(x, y, z, getMinPlayerDistance(new EntityWorm(world))) != null) continue;
				spawnSingle(world, new EntityWorm(world), x, y, z);
				continue;
			}

			/* ---------- CRICKETS ---------- */
			if (!world.isDaytime()
				&& weather != Weathers.OVERWORLD_RAIN
				&& weather != Weathers.OVERWORLD_STORM
				&& world.rand.nextInt(CRICKET_CHANCE) == 0
				&& getCount(EntityCricket.class) < cricketCap) {

				if (world.getClosestPlayer(x, y, z, getMinPlayerDistance(new EntityCricket(world))) != null) continue;
				spawnSingle(world, new EntityCricket(world), x, y, z);
				continue;
			}

			/* ---------- TUMBLEWEEDS ---------- */
			if (world.rand.nextInt(TUMBLEWEED_CHANCE) == 0
				&& getCount(EntityTumbleweed.class) < tumbleweedCap
				&& isTumbleweedBiome(biome)) {

				if (world.getClosestPlayer(x, y, z, getMinPlayerDistance(new EntityTumbleweed(world))) != null) continue;
				spawnSingle(world, new EntityTumbleweed(world), x, y, z);
				continue;
			}

			/* ---------- BIRD FLOCKS ---------- */
			if (canSpawnBirds(biome, weather)
				&& world.rand.nextInt(BIRD_FLOCK_CHANCE) == 0
				&& getCount(MobBird.class) < birdCap) {

				if (world.getClosestPlayer(x, y, z, getMinPlayerDistance(new MobBird(world))) != null) continue;
				spawnBirdFlock(world, x, y, z, ix, iz);
			}

			/* ---------- BUNNIES (NEW) ---------- */
			if (world.isDaytime()
				&& weather != Weathers.OVERWORLD_RAIN
				&& weather != Weathers.OVERWORLD_STORM
				&& getCount(MobBunny.class) < bunnyCap
				&& isBunnyBiome(biome)) {

				// Use the same spawn attempt chance as birds
				if (world.rand.nextInt(BUNNY_CHANCE) == 0
					&& world.getClosestPlayer(x, y, z, getMinPlayerDistance(new MobBunny(world))) == null) {

					spawnBunnyFlock(world, x, z, ix, iz, biome);
				}
			}
		}
	}

	/* ================= HELPERS ================= */

	private static void spawnBirdFlock(World world, double x, int y, double z, int ix, int iz) {
		int flockSize = BIRD_MIN_FLOCK + world.rand.nextInt(BIRD_MAX_FLOCK - BIRD_MIN_FLOCK + 1);

		// 🔹 Pick ONE variant for the entire flock
		int flockVariant = world.rand.nextInt(2); // <-- number of variants

		for (int i = 0; i < flockSize; i++) {
			double ox = world.rand.nextGaussian() * 4;
			double oz = world.rand.nextGaussian() * 4;

			int safeY = getSafeSpawnYForBird(world, ix, iz, 2F);

			MobBird bird = new MobBird(world);

			// 🔹 Force variant
			bird.setSkinVariant(flockVariant);

			bird.moveTo(
				x + ox + 0.5,
				safeY,
				z + oz + 0.5,
				world.rand.nextFloat() * 360F,
				0
			);

			world.entityJoinedWorld(bird);
			increment(MobBird.class);
		}
	}


	private static void spawnSingle(World world, Entity entity, double x, int y, double z) {
		entity.moveTo(x + 0.5, y, z + 0.5, world.rand.nextFloat() * 360F, 0);
		world.entityJoinedWorld(entity);
		increment(entity.getClass());
	}

	private static int getSafeSpawnY(World world, int x, int z) {
		int y = world.getHeightValue(x, z);
		while (y > 1 && world.isAirBlock(x, y - 1, z)) y--;
		return y;
	}
	private static int getSafeSpawnYForBird(World world, int x, int z, float entityHeight) {
		int y = world.getHeightValue(x, z);

		// Scan downward to solid ground
		while (y > 1 && world.isAirBlock(x, y - 1, z)) y--;

		// Now scan upward to ensure enough clearance
		int clearance = MathHelper.ceil(entityHeight); // blocks needed
		boolean collision = true;
		while (collision && y + clearance < world.getHeightBlocks()) {
			collision = false;
			for (int yy = 0; yy < clearance; yy++) {
				if (!world.isAirBlock(x, y + yy, z)) {
					collision = true;
					break;
				}
			}
			if (collision) y++; // move up until clear
		}

		return y;
	}


	private static void count(World world, Class<? extends Entity> clazz) {
		int count = 0;
		for (Entity e : world.loadedEntityList) {
			if (clazz.isInstance(e)) count++;
		}
		spawnCounts.put(clazz, count);
	}

	private static int getCount(Class<? extends Entity> clazz) {
		return spawnCounts.getOrDefault(clazz, 0);
	}

	private static void increment(Class<? extends Entity> clazz) {
		spawnCounts.put(clazz, getCount(clazz) + 1);
	}

	private static boolean isTumbleweedBiome(Biome biome) {
		return biome == Biomes.OVERWORLD_OUTBACK
			|| biome == Biomes.OVERWORLD_DESERT
			|| biome == Biomes.OVERWORLD_OUTBACK_GRASSY
			|| biome == Biomes.OVERWORLD_CAATINGA_PLAINS
			|| biome == Biomes.OVERWORLD_CAATINGA;
	}

	private static boolean canSpawnBirds(Biome biome, Weather weather) {
		if (weather == Weathers.OVERWORLD_RAIN || weather == Weathers.OVERWORLD_STORM) return false;

		return biome == Biomes.OVERWORLD_PLAINS
			|| biome == Biomes.OVERWORLD_MEADOW
			|| biome == Biomes.OVERWORLD_SHRUBLAND
			|| biome == Biomes.OVERWORLD_BIRCH_FOREST
			|| biome == Biomes.OVERWORLD_BOREAL_FOREST
			|| biome == Biomes.OVERWORLD_FOREST
			|| biome == Biomes.OVERWORLD_SEASONAL_FOREST
			|| biome == Biomes.OVERWORLD_RAINFOREST
			|| biome == Biomes.OVERWORLD_GRASSLANDS;
	}

	private static void spawnBunnyFlock(World world, double x, double z, int ix, int iz, Biome biome) {
		int flockSize = BUNNY_MIN_GROUP + world.rand.nextInt(BUNNY_MAX_GROUP - BUNNY_MIN_GROUP + 1);

		// Pick ONE variant for the entire flock based on biome
		int variant;
		if (biome == Biomes.OVERWORLD_TUNDRA) {
			variant = 1;
		} else if (biome == Biomes.OVERWORLD_DESERT
			|| biome == Biomes.OVERWORLD_OUTBACK
			|| biome == Biomes.OVERWORLD_OUTBACK_GRASSY
			|| biome == Biomes.OVERWORLD_CAATINGA
			|| biome == Biomes.OVERWORLD_CAATINGA_PLAINS) {
			variant = world.rand.nextBoolean() ? 0 : 2;
		} else {
			variant = 0;
		}

		for (int i = 0; i < flockSize; i++) {
			double ox = world.rand.nextGaussian() * 4; // match birds’ spread
			double oz = world.rand.nextGaussian() * 4;

			int safeY = getSafeSpawnYForBunny(world, ix + MathHelper.floor(ox), iz + MathHelper.floor(oz));

			MobBunny bunny = new MobBunny(world);
			bunny.setSkinVariant(variant);
			bunny.moveTo(x + ox + 0.5, safeY, z + oz + 0.5, world.rand.nextFloat() * 360F, 0);
			world.entityJoinedWorld(bunny);
			increment(MobBunny.class);
		}
	}

	private static int getSafeSpawnYForBunny(World world, int x, int z) {
		int y = world.getHeightValue(x, z);

		// Scan downward to solid ground
		while (y > 1 && world.isAirBlock(x, y - 1, z)) y--;

		// Ensure clearance (bunny height ~1 block)
		int clearance = 1;
		boolean collision = true;
		while (collision && y + clearance < world.getHeightBlocks()) {
			collision = false;
			for (int yy = 0; yy < clearance; yy++) {
				if (!world.isAirBlock(x, y + yy, z)) {
					collision = true;
					break;
				}
			}
			if (collision) y++;
		}

		return y;
	}



	private static boolean isBunnyBiome(Biome biome) {
		return biome == Biomes.OVERWORLD_PLAINS
			|| biome == Biomes.OVERWORLD_MEADOW
			|| biome == Biomes.OVERWORLD_DESERT
			|| biome == Biomes.OVERWORLD_OUTBACK
			|| biome == Biomes.OVERWORLD_OUTBACK_GRASSY
			|| biome == Biomes.OVERWORLD_CAATINGA
			|| biome == Biomes.OVERWORLD_CAATINGA_PLAINS
			|| biome == Biomes.OVERWORLD_GRASSLANDS
			|| biome == Biomes.OVERWORLD_SHRUBLAND
			|| biome == Biomes.OVERWORLD_TUNDRA
			|| biome == Biomes.OVERWORLD_SEASONAL_FOREST
			|| biome == Biomes.OVERWORLD_BIRCH_FOREST;
	}

	/* Distance-from-player helper */
	private static int getMinPlayerDistance(Entity entity) {
		if (entity instanceof EntityCricket || entity instanceof EntityWorm) {
			return 12; // spawn closer for small critters
		} else if (entity instanceof MobBird || entity instanceof EntityTumbleweed || entity instanceof MobBunny) {
			return 30; // keep larger mobs away from player
		}
		return 20; // default fallback
	}

}
