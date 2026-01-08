package peedog.funnyfauna.world;

import net.minecraft.core.block.Blocks;
import net.minecraft.core.block.tag.BlockTags;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.World;
import net.minecraft.core.world.biome.Biome;
import net.minecraft.core.world.biome.Biomes;
import net.minecraft.core.world.season.Seasons;
import peedog.funnyfauna.entity.cricket.EntityCricket;

public class AmbientSpawn {

	private static final int MAX_CRICKETS_PER_PLAYER = 8;
	private static final int SPAWN_RADIUS = 16;
	private static final int SPAWN_CHANCE = 100;

	public static void tick(World world) {
		if (world.isClientSide) return;

		// 🌙 Night-only
		if (world.isDaytime()) return;

		// ❄ No winter seasons
		if (isWinter(world)) return;

		for (Player player : world.players) {

			if (world.rand.nextInt(SPAWN_CHANCE) != 0) continue;

			double x = player.x + world.rand.nextInt(SPAWN_RADIUS * 2) - SPAWN_RADIUS;
			double z = player.z + world.rand.nextInt(SPAWN_RADIUS * 2) - SPAWN_RADIUS;

			int ix = MathHelper.floor(x);
			int iz = MathHelper.floor(z);
			int y = world.getHeightValue(ix, iz);

			// 🌍 Biome gate
			Biome biome = world.getBiomeProvider().getBiome(ix, y, iz);
			if (isColdBiome(biome)) continue;

			// 🌱 Passive-mob-valid block check
			if (!canSpawnOnBlock(world, ix, y, iz)) continue;

			int nearby = world.getEntitiesWithinAABB(
				EntityCricket.class,
				player.bb.expand(32, 16, 32)
			).size();

			if (nearby >= MAX_CRICKETS_PER_PLAYER) continue;

			EntityCricket cricket = new EntityCricket(world);
			cricket.moveTo(
				x + 0.5,
				y,
				z + 0.5,
				world.rand.nextFloat() * 360F,
				0
			);

			world.entityJoinedWorld(cricket);
		}
	}

	/* ---------------- Helpers ---------------- */

	private static boolean isWinter(World world) {
		return world.getSeasonManager().getCurrentSeason() == Seasons.OVERWORLD_WINTER
			|| world.getSeasonManager().getCurrentSeason() == Seasons.OVERWORLD_WINTER_ENDLESS;
	}

	private static boolean isColdBiome(Biome biome) {
		return biome == Biomes.OVERWORLD_TUNDRA
			|| biome == Biomes.OVERWORLD_TAIGA
			|| biome == Biomes.OVERWORLD_GLACIER;
	}

	private static boolean canSpawnOnBlock(World world, int x, int y, int z) {
		int id = world.getBlockId(x, y - 1, z);
		if (id <= 0 || Blocks.blocksList[id] == null) return false;
		return Blocks.blocksList[id].hasTag(BlockTags.PASSIVE_MOBS_SPAWN);
	}
}
