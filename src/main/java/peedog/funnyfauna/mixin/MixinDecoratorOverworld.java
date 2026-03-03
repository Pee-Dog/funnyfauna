package peedog.funnyfauna.mixin;

import net.minecraft.core.world.biome.Biome;
import net.minecraft.core.world.biome.Biomes;
import net.minecraft.core.world.chunk.Chunk;
import net.minecraft.core.world.generate.chunk.perlin.overworld.ChunkDecoratorOverworld;
import net.minecraft.core.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import peedog.funnyfauna.block.FunnyFaunaBlocks;
import peedog.funnyfauna.world.features.WorldFeatureAntHill;

import java.util.Random;

@Mixin(value = ChunkDecoratorOverworld.class, remap = false)
public abstract class MixinDecoratorOverworld {
	@Shadow
	private World world;

	@Inject(method = "decorate", at = @At("TAIL"))
	private void injectAntHills(Chunk chunk, CallbackInfo ci) {
		// These local variables mirror the ones in the original decorate() method
		int chunkX = chunk.xPosition; // [cite: 244]
		int chunkZ = chunk.zPosition; // [cite: 244]
		int x = chunkX * 16; // [cite: 246]
		int z = chunkZ * 16; // [cite: 246]

		// Recreate the random seed logic used by the decorator
		Random rand = new Random(this.world.getRandomSeed());
		long l1 = rand.nextLong() / 2L * 2L + 1L;
		long l2 = rand.nextLong() / 2L * 2L + 1L;
		rand.setSeed((long)chunkX * l1 + (long)chunkZ * l2 ^ this.world.getRandomSeed());

		// Get the biome at the current chunk
		int yHeight = this.world.getHeightValue(x + 8, z + 8);
		Biome biome = this.world.getBlockBiome(x + 8, yHeight, z + 8);

		// Determine probability: 1 in 20 for deserts, 1 in 3 elsewhere
		int chance = (biome == Biomes.OVERWORLD_DESERT || biome == Biomes.OVERWORLD_OUTBACK) ? 10 : 2;

		if (rand.nextInt(chance) == 0) {
			int xPos = x + rand.nextInt(16) + 8;
			int zPos = z + rand.nextInt(16) + 8;
			int yPos = this.world.getHeightValue(xPos, zPos);

			// Call the feature [cite: 2]
			new WorldFeatureAntHill(FunnyFaunaBlocks.ANT_HILL.id()).place(this.world, rand, xPos, yPos, zPos);
		}
	}
}
