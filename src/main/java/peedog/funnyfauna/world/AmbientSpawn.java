package peedog.funnyfauna.world;

import net.minecraft.core.block.material.Material;
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
import peedog.funnyfauna.entity.chipmunk.MobChipmunk;
import peedog.funnyfauna.entity.cricket.EntityCricket;
import peedog.funnyfauna.entity.duck.MobDuck;
import peedog.funnyfauna.entity.lilspider.EntityLilSpider;
import peedog.funnyfauna.entity.moth.MobMoth;
import peedog.funnyfauna.entity.scorvid.MobScorvid;
import peedog.funnyfauna.entity.tumbleweed.EntityTumbleweed;
import peedog.funnyfauna.entity.worm.EntityWorm;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AmbientSpawn {

	/* ---------------- CONFIG ---------------- */

	private static final int SPAWN_RADIUS          = 32;

	// Minimum distance from any player before a group mob may spawn.
	// Prevents bunnies, birds, ducks, etc. from appearing right beside the player.
	private static final int MIN_GROUP_SPAWN_DIST  = 16;

	private static final int BASE_CRICKETS         = 10;
	private static final int BASE_WORMS            = 10;
	private static final int BASE_TUMBLEWEEDS      = 3;
	private static final int BASE_BIRD_FLOCKS      = 2;
	private static final int BASE_BUNNIES          = 10;
	private static final int BASE_LIL_SPIDERS      = 12;
	private static final int BASE_MOTHS            = 6;
	private static final int BASE_CHIPMUNKS        = 8;
	private static final int BASE_CORVID_FLOCKS    = 2;
	private static final int BASE_DUCKS            = 8;

	private static final int CRICKET_CHANCE        = 70;
	private static final int WORM_CHANCE           = 70;
	private static final int TUMBLEWEED_CHANCE     = 300;
	private static final int BIRD_FLOCK_CHANCE     = 100;
	private static final int BUNNY_CHANCE          = 200;
	private static final int LIL_SPIDER_CHANCE     = 4;
	private static final int MOTH_CHANCE           = 4;
	private static final int CHIPMUNK_CHANCE       = 70;
	private static final int CORVID_FLOCK_CHANCE   = 100;
	private static final int DUCK_CHANCE           = 100;

	private static final int BIRD_MIN_FLOCK        = 4;
	private static final int BIRD_MAX_FLOCK        = 6;
	private static final int BUNNY_MIN_GROUP       = 3;
	private static final int BUNNY_MAX_GROUP       = 5;
	private static final int SPIDER_MIN_GROUP      = 1;
	private static final int SPIDER_MAX_GROUP      = 2;
	private static final int MOTH_MIN_GROUP        = 3;
	private static final int MOTH_MAX_GROUP        = 5;
	private static final int CORVID_MIN_FLOCK      = 4;
	private static final int CORVID_MAX_FLOCK      = 6;
	private static final int DUCK_MIN_GROUP        = 3;
	private static final int DUCK_MAX_GROUP        = 5;

	private static final int SEALEVEL              = 128;

	/* ---------------- DEBUG ---------------- */

	private static final boolean DEBUG             = true;
	private static final int DEBUG_INTERVAL        = 200;
	private static int debugTickCounter            = 0;

	// Spider gate counters
	private static int dbg_sp_attempts   = 0;
	private static int dbg_sp_validFloor = 0;
	private static int dbg_sp_noSky      = 0;
	private static int dbg_sp_dark       = 0;
	private static int dbg_sp_belowSea   = 0;
	private static int dbg_sp_rng        = 0;
	private static int dbg_sp_cap        = 0;
	private static int dbg_sp_noPlayer   = 0;
	private static int dbg_sp_spawned    = 0;

	// Moth gate counters
	private static int dbg_mo_attempts   = 0;
	private static int dbg_mo_validFloor = 0;
	private static int dbg_mo_dark       = 0;
	private static int dbg_mo_rng        = 0;
	private static int dbg_mo_cap        = 0;
	private static int dbg_mo_noPlayer   = 0;
	private static int dbg_mo_spawned    = 0;

	private static float   dbg_sp_lastLight  = -1;
	private static float   dbg_mo_lastLight  = -1;
	private static int     dbg_sp_lastCaveY  = -1;
	private static int     dbg_mo_lastCaveY  = -1;
	private static boolean dbg_sp_lastSky    = true;

	/* ---------------- BIOME SETS ---------------- */

	private static final Set<Biome> TUMBLEWEED_BIOMES = new HashSet<>(Arrays.asList(
		Biomes.OVERWORLD_OUTBACK,
		Biomes.OVERWORLD_DESERT,
		Biomes.OVERWORLD_OUTBACK_GRASSY,
		Biomes.OVERWORLD_CAATINGA_PLAINS,
		Biomes.OVERWORLD_CAATINGA
	));

	private static final Set<Biome> BIRD_BIOMES = new HashSet<>(Arrays.asList(
		Biomes.OVERWORLD_PLAINS,
		Biomes.OVERWORLD_MEADOW,
		Biomes.OVERWORLD_SHRUBLAND,
		Biomes.OVERWORLD_BIRCH_FOREST,
		Biomes.OVERWORLD_BOREAL_FOREST,
		Biomes.OVERWORLD_FOREST,
		Biomes.OVERWORLD_SEASONAL_FOREST,
		Biomes.OVERWORLD_RAINFOREST,
		Biomes.OVERWORLD_GRASSLANDS
	));

	private static final Set<Biome> BUNNY_BIOMES = new HashSet<>(Arrays.asList(
		Biomes.OVERWORLD_PLAINS,
		Biomes.OVERWORLD_MEADOW,
		Biomes.OVERWORLD_DESERT,
		Biomes.OVERWORLD_OUTBACK,
		Biomes.OVERWORLD_OUTBACK_GRASSY,
		Biomes.OVERWORLD_CAATINGA,
		Biomes.OVERWORLD_CAATINGA_PLAINS,
		Biomes.OVERWORLD_GRASSLANDS,
		Biomes.OVERWORLD_SHRUBLAND,
		Biomes.OVERWORLD_TUNDRA,
		Biomes.OVERWORLD_SEASONAL_FOREST,
		Biomes.OVERWORLD_BIRCH_FOREST
	));

	private static final Set<Biome> CHIPMUNK_BIOMES = new HashSet<>(Arrays.asList(
		Biomes.OVERWORLD_FOREST,
		Biomes.OVERWORLD_BIRCH_FOREST,
		Biomes.OVERWORLD_BOREAL_FOREST,
		Biomes.OVERWORLD_SEASONAL_FOREST,
		Biomes.OVERWORLD_RAINFOREST
	));

	// Used for bunny variant selection
	private static final Set<Biome> DESERT_BIOMES = new HashSet<>(Arrays.asList(
		Biomes.OVERWORLD_DESERT,
		Biomes.OVERWORLD_OUTBACK,
		Biomes.OVERWORLD_OUTBACK_GRASSY,
		Biomes.OVERWORLD_CAATINGA,
		Biomes.OVERWORLD_CAATINGA_PLAINS
	));

	/* ---------------------------------------- */

	private static final Map<Class<? extends Entity>, Integer> spawnCounts = new HashMap<>();

	public static void tick(World world) {
		if (world.isClientSide) return;

		boolean isNether = world.dimension.id == -1;

		// ── Nether: only scorvids spawn here ──────────────────────────────────
		if (isNether) {
			int playerCount  = Math.max(1, world.players.size());
			int corvidCap    = BASE_CORVID_FLOCKS * CORVID_MAX_FLOCK * playerCount;
			count(world, MobScorvid.class);

			for (Player player : world.players) {
				double x = player.x + world.rand.nextInt(SPAWN_RADIUS * 2) - SPAWN_RADIUS;
				double z = player.z + world.rand.nextInt(SPAWN_RADIUS * 2) - SPAWN_RADIUS;
				int ix   = MathHelper.floor(x);
				int iz   = MathHelper.floor(z);
				int y    = getSafeSpawnY(world, ix, iz);
				if (y == -1) continue;

				if (world.rand.nextInt(CORVID_FLOCK_CHANCE) == 0
					&& getCount(MobScorvid.class) < corvidCap
					&& world.getClosestPlayer(x, y, z, MIN_GROUP_SPAWN_DIST) == null) {
					spawnScorvidFlock(world, x, y, z, ix, iz);
				}
			}
			return;
		}

		// ── Overworld ────────────────────────────────────────────────────────
		Weather weather  = world.getCurrentWeather();
		boolean isRainy  = weather == Weathers.OVERWORLD_RAIN || weather == Weathers.OVERWORLD_STORM;
		int playerCount  = Math.max(1, world.players.size());

		spawnCounts.clear();
		count(world, EntityCricket.class);
		count(world, EntityWorm.class);
		count(world, EntityTumbleweed.class);
		count(world, MobBird.class);
		count(world, MobBunny.class);
		count(world, EntityLilSpider.class);
		count(world, MobMoth.class);
		count(world, MobChipmunk.class);
		count(world, MobDuck.class);

		int cricketCap    = BASE_CRICKETS * playerCount;
		int wormCap       = BASE_WORMS * playerCount;
		int tumbleweedCap = BASE_TUMBLEWEEDS * playerCount;
		int birdCap       = BASE_BIRD_FLOCKS * BIRD_MAX_FLOCK * playerCount;
		int bunnyCap      = BASE_BUNNIES * playerCount;
		int lilSpiderCap  = BASE_LIL_SPIDERS * playerCount;
		int mothCap       = BASE_MOTHS * playerCount;
		int chipmunkCap   = BASE_CHIPMUNKS * playerCount;
		int duckCap       = BASE_DUCKS * playerCount;

		for (Player player : world.players) {
			double x = player.x + world.rand.nextInt(SPAWN_RADIUS * 2) - SPAWN_RADIUS;
			double z = player.z + world.rand.nextInt(SPAWN_RADIUS * 2) - SPAWN_RADIUS;
			int ix   = MathHelper.floor(x);
			int iz   = MathHelper.floor(z);
			int y    = getSafeSpawnY(world, ix, iz);

			// Cave Y: pick a random underground floor for spider/moth spawning
			int caveY = -1;
			boolean validCaveFloor = false;
			for (int attempt = 0; attempt < 16; attempt++) {
				int tryY = world.rand.nextInt(world.getHeightBlocks() - 1) + 1;
				if (world.isBlockNormalCube(ix, tryY - 1, iz)
					&& !world.isBlockNormalCube(ix, tryY, iz)
					&& !world.getBlockMaterial(ix, tryY, iz).isLiquid()) {
					caveY = tryY;
					validCaveFloor = true;
					break;
				}
			}

			Biome biome = world.getBlockBiome(ix, y, iz);

			/* ---------- WORMS – rain only ---------- */
			if (isRainy
				&& world.rand.nextInt(WORM_CHANCE) == 0
				&& getCount(EntityWorm.class) < wormCap
				&& world.getClosestPlayer(x, y, z, 16D) != null) {
				spawnSingle(world, new EntityWorm(world), x, y, z);
			}

			/* ---------- CRICKETS – night, no rain ---------- */
			if (!world.isDaytime() && !isRainy
				&& world.rand.nextInt(CRICKET_CHANCE) == 0
				&& getCount(EntityCricket.class) < cricketCap
				&& world.getClosestPlayer(x, y, z, 16D) != null) {
				spawnSingle(world, new EntityCricket(world), x, y, z);
			}

			/* ---------- TUMBLEWEEDS ---------- */
			if (TUMBLEWEED_BIOMES.contains(biome)
				&& world.rand.nextInt(TUMBLEWEED_CHANCE) == 0
				&& getCount(EntityTumbleweed.class) < tumbleweedCap
				&& world.getClosestPlayer(x, y, z, MIN_GROUP_SPAWN_DIST) == null) {
				spawnSingle(world, new EntityTumbleweed(world), x, y, z);
			}

			/* ---------- BIRD FLOCKS – no rain, leaf surface ---------- */
			if (!isRainy && BIRD_BIOMES.contains(biome)
				&& hasLeafSurface(world, ix, iz)
				&& world.rand.nextInt(BIRD_FLOCK_CHANCE) == 0
				&& getCount(MobBird.class) < birdCap
				&& world.getClosestPlayer(x, y, z, MIN_GROUP_SPAWN_DIST) == null) {
				spawnBirdFlock(world, x, y, z, ix, iz);
			}

			/* ---------- BUNNIES – daytime, no rain ---------- */
			if (world.isDaytime() && !isRainy && BUNNY_BIOMES.contains(biome)
				&& world.rand.nextInt(BUNNY_CHANCE) == 0
				&& getCount(MobBunny.class) < bunnyCap
				&& world.getClosestPlayer(x, y, z, MIN_GROUP_SPAWN_DIST) == null) {
				spawnBunnyGroup(world, x, z, ix, iz, biome);
			}

			/* ---------- CHIPMUNKS – daytime, no rain, leaf surface, singular ---------- */
			if (world.isDaytime() && !isRainy && CHIPMUNK_BIOMES.contains(biome)
				&& hasLeafSurface(world, ix, iz)
				&& world.rand.nextInt(CHIPMUNK_CHANCE) == 0
				&& getCount(MobChipmunk.class) < chipmunkCap
				&& world.getClosestPlayer(x, y, z, MIN_GROUP_SPAWN_DIST) == null) {
				spawnSingle(world, new MobChipmunk(world), x, y, z);
			}

			/* ---------- DUCKS – daytime, shallow water (1–3 blocks deep) ---------- */
			int duckY = getDuckSpawnY(world, ix, iz);
			if (world.isDaytime() && !isRainy
				&& duckY != -1 && isShallowWater(world, ix, iz, duckY)
				&& world.rand.nextInt(DUCK_CHANCE) == 0
				&& getCount(MobDuck.class) < duckCap
				&& world.getClosestPlayer(x, duckY, z, MIN_GROUP_SPAWN_DIST) == null) {
				spawnDuckGroup(world, x, duckY, z, ix, iz);
			}

			/* ---------- LIL SPIDERS – caves, dark, below sea level ---------- */
			if (DEBUG) {
				dbg_sp_attempts++;
				boolean gate_floor  = validCaveFloor;
				boolean gate_sea    = gate_floor  && caveY < SEALEVEL;
				boolean gate_sky    = gate_sea    && !world.canBlockSeeTheSky(ix, caveY, iz);
				float   lightVal    = gate_sky    ? world.getLightBrightness(ix, caveY, iz) : -1f;
				boolean gate_dark   = gate_sky    && lightVal < 0.5F;
				boolean gate_rng    = gate_dark   && world.rand.nextInt(LIL_SPIDER_CHANCE) == 0;
				boolean gate_cap    = gate_rng    && getCount(EntityLilSpider.class) < lilSpiderCap;
				boolean gate_player = gate_cap    && world.getClosestPlayer(x, (double)caveY, z, 24.0D) == null;

				if (gate_floor)  dbg_sp_validFloor++;
				if (gate_sea)    dbg_sp_belowSea++;
				if (gate_sky)    dbg_sp_noSky++;
				if (gate_dark)   dbg_sp_dark++;
				if (gate_rng)    dbg_sp_rng++;
				if (gate_cap)    dbg_sp_cap++;
				if (gate_player) { dbg_sp_noPlayer++; dbg_sp_spawned++; spawnLilSpiderGroup(world, x, caveY, z, ix, iz); }

				if (gate_floor) { dbg_sp_lastCaveY = caveY; dbg_sp_lastSky = world.canBlockSeeTheSky(ix, caveY, iz); }
				if (gate_sky)   dbg_sp_lastLight = lightVal;
			} else {
				if (validCaveFloor && caveY < SEALEVEL
					&& !world.canBlockSeeTheSky(ix, caveY, iz)
					&& world.getLightBrightness(ix, caveY, iz) < 0.5F
					&& world.rand.nextInt(LIL_SPIDER_CHANCE) == 0
					&& getCount(EntityLilSpider.class) < lilSpiderCap
					&& world.getClosestPlayer(x, (double)caveY, z, 24.0D) == null) {
					spawnLilSpiderGroup(world, x, caveY, z, ix, iz);
				}
			}

			/* ---------- MOTHS – anywhere dark ---------- */
			if (DEBUG) {
				dbg_mo_attempts++;
				boolean gate_floor  = validCaveFloor;
				float   lightVal    = gate_floor ? world.getLightBrightness(ix, caveY, iz) : -1f;
				boolean gate_dark   = gate_floor  && lightVal < 0.5F;
				boolean gate_rng    = gate_dark   && world.rand.nextInt(MOTH_CHANCE) == 0;
				boolean gate_cap    = gate_rng    && getCount(MobMoth.class) < mothCap;
				boolean gate_player = gate_cap    && world.getClosestPlayer(x, (double)caveY, z, 24.0D) == null;

				if (gate_floor)  dbg_mo_validFloor++;
				if (gate_dark)   dbg_mo_dark++;
				if (gate_rng)    dbg_mo_rng++;
				if (gate_cap)    dbg_mo_cap++;
				if (gate_player) { dbg_mo_noPlayer++; dbg_mo_spawned++; spawnMothGroup(world, x, caveY, z, ix, iz); }

				if (gate_floor) { dbg_mo_lastCaveY = caveY; dbg_mo_lastLight = lightVal; }
			} else {
				if (validCaveFloor
					&& world.getLightBrightness(ix, caveY, iz) < 0.5F
					&& world.rand.nextInt(MOTH_CHANCE) == 0
					&& getCount(MobMoth.class) < mothCap
					&& world.getClosestPlayer(x, (double)caveY, z, 24.0D) == null) {
					spawnMothGroup(world, x, caveY, z, ix, iz);
				}
			}
		}

		/* ---------- DEBUG PRINT ---------- */
		if (DEBUG) {
			debugTickCounter++;
			if (debugTickCounter >= DEBUG_INTERVAL) {
				debugTickCounter = 0;
				System.out.println("=== AmbientSpawn DEBUG (last " + DEBUG_INTERVAL + " ticks) ===");
				System.out.println("[SPIDER] attempts=" + dbg_sp_attempts
					+ " | validFloor=" + dbg_sp_validFloor
					+ " | belowSea=" + dbg_sp_belowSea
					+ " | noSky=" + dbg_sp_noSky
					+ " | dark=" + dbg_sp_dark
					+ " | rng=" + dbg_sp_rng
					+ " | underCap=" + dbg_sp_cap
					+ " | noPlayer=" + dbg_sp_noPlayer
					+ " | SPAWNED=" + dbg_sp_spawned);
				System.out.println("[SPIDER] lastCaveY=" + dbg_sp_lastCaveY
					+ " canSeeSky=" + dbg_sp_lastSky
					+ " lastLight=" + dbg_sp_lastLight);
				System.out.println("[SPIDER] currentCount=" + getCount(EntityLilSpider.class)
					+ " cap=" + (BASE_LIL_SPIDERS * Math.max(1, world.players.size())));
				System.out.println("[MOTH]   attempts=" + dbg_mo_attempts
					+ " | validFloor=" + dbg_mo_validFloor
					+ " | dark=" + dbg_mo_dark
					+ " | rng=" + dbg_mo_rng
					+ " | underCap=" + dbg_mo_cap
					+ " | noPlayer=" + dbg_mo_noPlayer
					+ " | SPAWNED=" + dbg_mo_spawned);
				System.out.println("[MOTH]   lastCaveY=" + dbg_mo_lastCaveY
					+ " lastLight=" + dbg_mo_lastLight);
				System.out.println("[MOTH]   currentCount=" + getCount(MobMoth.class)
					+ " cap=" + (BASE_MOTHS * Math.max(1, world.players.size())));
				System.out.println("==================================================");

				dbg_sp_attempts = dbg_sp_validFloor = dbg_sp_belowSea = dbg_sp_noSky = dbg_sp_dark = 0;
				dbg_sp_rng = dbg_sp_cap = dbg_sp_noPlayer = dbg_sp_spawned = 0;
				dbg_mo_attempts = dbg_mo_validFloor = dbg_mo_dark = 0;
				dbg_mo_rng = dbg_mo_cap = dbg_mo_noPlayer = dbg_mo_spawned = 0;
			}
		}
	}

	/* ================= SPAWN METHODS ================= */

	private static void spawnSingle(World world, Entity entity, double x, int y, double z) {
		entity.moveTo(x + 0.5, y, z + 0.5, world.rand.nextFloat() * 360F, 0);
		world.entityJoinedWorld(entity);
		increment(entity.getClass());
	}

	private static void spawnBirdFlock(World world, double x, int y, double z, int ix, int iz) {
		int flockSize = randomRange(world, BIRD_MIN_FLOCK, BIRD_MAX_FLOCK);
		int variant   = world.rand.nextInt(3);
		for (int i = 0; i < flockSize; i++) {
			double ox = world.rand.nextGaussian() * 4;
			double oz = world.rand.nextGaussian() * 4;
			MobBird bird = new MobBird(world);
			bird.setSkinVariant(variant);
			bird.moveTo(x + ox + 0.5, getSafeSpawnYForBird(world, ix, iz, 2F),
				z + oz + 0.5, world.rand.nextFloat() * 360F, 0);
			world.entityJoinedWorld(bird);
			increment(MobBird.class);
		}
	}

	/** Like spawnBirdFlock but no skin variants — used for nether corvids. */
	private static void spawnScorvidFlock(World world, double x, int y, double z, int ix, int iz) {
		int flockSize = randomRange(world, CORVID_MIN_FLOCK, CORVID_MAX_FLOCK);
		for (int i = 0; i < flockSize; i++) {
			double ox = world.rand.nextGaussian() * 4;
			double oz = world.rand.nextGaussian() * 4;
			MobScorvid corvid = new MobScorvid(world);
			corvid.moveTo(x + ox + 0.5, getSafeSpawnYForBird(world, ix, iz, 2F),
				z + oz + 0.5, world.rand.nextFloat() * 360F, 0);
			world.entityJoinedWorld(corvid);
			increment(MobScorvid.class);
		}
	}

	private static void spawnBunnyGroup(World world, double x, double z, int ix, int iz, Biome biome) {
		int groupSize = randomRange(world, BUNNY_MIN_GROUP, BUNNY_MAX_GROUP);
		for (int i = 0; i < groupSize; i++) {
			double ox = world.rand.nextGaussian() * 4;
			double oz = world.rand.nextGaussian() * 4;
			int spawnY = getSafeSpawnY(world, ix + MathHelper.floor(ox), iz + MathHelper.floor(oz));
			if (spawnY == -1) continue;

			MobBunny bunny = new MobBunny(world);
			bunny.setSkinVariant(getBunnyVariantForBiome(world, biome));
			bunny.moveTo(x + ox + 0.5, spawnY, z + oz + 0.5, world.rand.nextFloat() * 360F, 0);
			world.entityJoinedWorld(bunny);
			increment(MobBunny.class);
		}
	}

	private static void spawnDuckGroup(World world, double x, int y, double z, int ix, int iz) {
		int groupSize = randomRange(world, DUCK_MIN_GROUP, DUCK_MAX_GROUP);
		for (int i = 0; i < groupSize; i++) {
			double ox = world.rand.nextGaussian() * 4;
			double oz = world.rand.nextGaussian() * 4;
			int memberX = ix + MathHelper.floor(ox);
			int memberZ = iz + MathHelper.floor(oz);
			int memberY = getDuckSpawnY(world, memberX, memberZ);
			if (memberY == -1) continue;

			MobDuck duck = new MobDuck(world);
			duck.moveTo(x + ox + 0.5, memberY, z + oz + 0.5, world.rand.nextFloat() * 360F, 0);
			world.entityJoinedWorld(duck);
			increment(MobDuck.class);
		}
	}

	private static void spawnLilSpiderGroup(World world, double x, int y, double z, int ix, int iz) {
		int groupSize = randomRange(world, SPIDER_MIN_GROUP, SPIDER_MAX_GROUP);
		for (int i = 0; i < groupSize; i++) {
			double ox = world.rand.nextGaussian() * 3;
			double oz = world.rand.nextGaussian() * 3;
			int memberX = ix + MathHelper.floor(ox);
			int memberZ = iz + MathHelper.floor(oz);

			int memberY = -1;
			for (int tryY = y + 3; tryY > Math.max(1, y - 10); tryY--) {
				if (world.isBlockNormalCube(memberX, tryY - 1, memberZ)
					&& !world.isBlockNormalCube(memberX, tryY, memberZ)
					&& !world.getBlockMaterial(memberX, tryY, memberZ).isLiquid()) {
					memberY = tryY;
					break;
				}
			}
			if (memberY == -1) continue;

			EntityLilSpider spider = new EntityLilSpider(world);
			spider.moveTo(x + ox + 0.5, memberY, z + oz + 0.5, world.rand.nextFloat() * 360F, 0);
			if (!spider.canSpawnHere()) {
				System.out.println("[SPIDER-GROUP] canSpawnHere() returned false at Y=" + memberY);
				continue;
			}
			world.entityJoinedWorld(spider);
			increment(EntityLilSpider.class);
		}
	}

	private static void spawnMothGroup(World world, double x, int y, double z, int ix, int iz) {
		int groupSize = randomRange(world, MOTH_MIN_GROUP, MOTH_MAX_GROUP);
		for (int i = 0; i < groupSize; i++) {
			double ox = world.rand.nextGaussian() * 3;
			double oz = world.rand.nextGaussian() * 3;
			int memberX = ix + MathHelper.floor(ox);
			int memberZ = iz + MathHelper.floor(oz);

			int memberY = -1;
			for (int tryY = y + 3; tryY > Math.max(1, y - 10); tryY--) {
				if (world.isBlockNormalCube(memberX, tryY - 1, memberZ)
					&& !world.isBlockNormalCube(memberX, tryY, memberZ)
					&& !world.getBlockMaterial(memberX, tryY, memberZ).isLiquid()) {
					memberY = tryY;
					break;
				}
			}
			if (memberY == -1) continue;

			MobMoth moth = new MobMoth(world);
			moth.moveTo(x + ox + 0.5, memberY, z + oz + 0.5, world.rand.nextFloat() * 360F, 0);
			if (!moth.canSpawnHere()) {
				System.out.println("[MOTH-GROUP] canSpawnHere() returned false at Y=" + memberY);
				continue;
			}
			world.entityJoinedWorld(moth);
			increment(MobMoth.class);
		}
	}

	/* ================= HELPERS ================= */

	private static int randomRange(World world, int min, int max) {
		return min + world.rand.nextInt(max - min + 1);
	}

	/**
	 * Returns the Y at which a ground-dwelling mob should stand,
	 * or -1 if the surface is liquid (ocean/river/lake).
	 */
	private static int getSafeSpawnY(World world, int x, int z) {
		int y = world.getHeightValue(x, z);
		while (y > 1 && world.isAirBlock(x, y - 1, z)) y--;
		if (world.getBlockMaterial(x, y - 1, z).isLiquid()) return -1;
		return y;
	}

	/**
	 * Like getSafeSpawnY but returns the water-surface Y for ducks,
	 * or -1 if the top surface is not liquid.
	 */
	private static int getDuckSpawnY(World world, int x, int z) {
		int y = world.getHeightValue(x, z);
		while (y > 1 && world.isAirBlock(x, y - 1, z)) y--;
		if (!world.getBlockMaterial(x, y - 1, z).isLiquid()) return -1;
		return y;
	}

	/**
	 * Returns true if the water column at (x, z) is between 1 and 3 blocks deep —
	 * shallow enough for ducks but not open-ocean.
	 */
	private static boolean isShallowWater(World world, int x, int z, int duckY) {
		int depth = 0;
		for (int checkY = duckY - 1; checkY >= Math.max(1, duckY - 3); checkY--) {
			if (world.getBlockMaterial(x, checkY, z).isLiquid()) depth++;
			else break;
		}
		return depth >= 1 && depth <= 3;
	}

	/**
	 * Returns true if the top surface block at (x, z) is leaves material —
	 * used to gate bird and chipmunk spawns.
	 */
	private static boolean hasLeafSurface(World world, int x, int z) {
		int y = world.getHeightValue(x, z);
		while (y > 1 && world.isAirBlock(x, y - 1, z)) y--;
		return world.getBlockMaterial(x, y - 1, z) == Material.leaves;
	}

	private static int getSafeSpawnYForBird(World world, int x, int z, float entityHeight) {
		int y = world.getHeightValue(x, z);
		while (y > 1 && world.isAirBlock(x, y - 1, z)) y--;

		int clearance = MathHelper.ceil(entityHeight);
		boolean collision = true;
		while (collision && y + clearance < world.getHeightBlocks()) {
			collision = false;
			for (int yy = 0; yy < clearance; yy++) {
				if (!world.isAirBlock(x, y + yy, z)) { collision = true; break; }
			}
			if (collision) y++;
		}
		return y;
	}

	private static int getBunnyVariantForBiome(World world, Biome biome) {
		if (biome == Biomes.OVERWORLD_TUNDRA) return 1;
		if (DESERT_BIOMES.contains(biome)) {
			int[] desertVariants = {0, 2};
			return desertVariants[world.rand.nextInt(desertVariants.length)];
		}
		int[] normalVariants = {0, 3, 4};
		return normalVariants[world.rand.nextInt(normalVariants.length)];
	}

	private static void count(World world, Class<? extends Entity> clazz) {
		int n = 0;
		for (Entity e : world.loadedEntityList) if (clazz.isInstance(e)) n++;
		spawnCounts.put(clazz, n);
	}

	private static int getCount(Class<? extends Entity> clazz) {
		return spawnCounts.getOrDefault(clazz, 0);
	}

	private static void increment(Class<? extends Entity> clazz) {
		spawnCounts.put(clazz, getCount(clazz) + 1);
	}
}
