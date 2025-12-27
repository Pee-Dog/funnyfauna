package peedog.funnyfauna;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.block.entity.TileEntityDispatcher;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.SpawnListEntry;
import net.minecraft.core.entity.animal.MobChicken;
import net.minecraft.core.entity.animal.MobCow;
import net.minecraft.core.entity.animal.MobPig;
import net.minecraft.core.entity.animal.MobSheep;
import net.minecraft.core.enums.MobCategory;
import net.minecraft.core.util.collection.NamespaceID;
import net.minecraft.core.world.World;
import net.minecraft.core.world.biome.Biome;
import net.minecraft.core.world.biome.Biomes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import peedog.funnyfauna.block.FunnyFaunaBlocks;
import peedog.funnyfauna.block.entity.TileEntityEmuEgg;
import peedog.funnyfauna.entity.FunnyFaunaEntities;
import peedog.funnyfauna.entity.armadillo.MobArmadillo;
import peedog.funnyfauna.entity.camel.MobCamel;
import peedog.funnyfauna.entity.emu.MobEmu;
import peedog.funnyfauna.entity.lizard.MobLizard;
import peedog.funnyfauna.entity.penguin.MobPenguin;
import peedog.funnyfauna.item.FunnyFaunaItems;
import peedog.funnyfauna.net.message.FunnyRideableNetworkMessage;
import turniplabs.halplibe.helper.network.NetworkHandler;
import turniplabs.halplibe.util.ConfigHandler;
import turniplabs.halplibe.util.GameStartEntrypoint;
import turniplabs.halplibe.util.RecipeEntrypoint;

import java.util.Properties;

public class FunnyFauna implements ModInitializer, RecipeEntrypoint, GameStartEntrypoint {
	public static final String MOD_ID = "funnyfauna";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static int GUI_LABEL_COLOR = 0x404040;
	public static int GUI_SATCHEL_ID;
	public static int itemID;

	private static void removeVanillaPassives(Biome biome) {
		biome.getSpawnableList(MobCategory.creature)
			.removeIf(entry ->
				entry.entityClass == MobPig.class ||
					entry.entityClass == MobCow.class ||
					entry.entityClass == MobSheep.class ||
					entry.entityClass == MobChicken.class
			);
	}
	static {
		final Properties prop = new Properties();
		prop.setProperty("gui_satchel_id","10");
		final ConfigHandler config = new ConfigHandler(MOD_ID, prop);

		config.updateConfig();

		GUI_SATCHEL_ID = config.getInt("gui_satchel_id");

	}


	@Override
	public void onInitialize() {
		new FunnyFaunaItems().initializeItems();
		new FunnyFaunaBlocks().initializeBlocks();
		NamespaceID emuEggId = NamespaceID.getPermanent(MOD_ID, "emu_egg");
		TileEntityDispatcher.addMapping(TileEntityEmuEgg.class, emuEggId);
		removeVanillaPassives(Biomes.OVERWORLD_TUNDRA);
		Biomes.OVERWORLD_TUNDRA.getSpawnableList(MobCategory.creature).add(new SpawnListEntry(MobPenguin.class, 10));
		removeVanillaPassives(Biomes.OVERWORLD_GLACIER);
		Biomes.OVERWORLD_GLACIER.getSpawnableList(MobCategory.creature).add(new SpawnListEntry(MobPenguin.class, 10));
		removeVanillaPassives(Biomes.OVERWORLD_TAIGA);
		Biomes.OVERWORLD_TAIGA.getSpawnableList(MobCategory.creature).add(new SpawnListEntry(MobPenguin.class, 10));
		Biomes.OVERWORLD_DESERT.getSpawnableList(MobCategory.creature).add(new SpawnListEntry(MobLizard.class, 10));
		Biomes.OVERWORLD_CAATINGA.getSpawnableList(MobCategory.creature).add(new SpawnListEntry(MobLizard.class, 10));
		Biomes.OVERWORLD_CAATINGA_PLAINS.getSpawnableList(MobCategory.creature).add(new SpawnListEntry(MobLizard.class, 10));
		Biomes.OVERWORLD_OUTBACK.getSpawnableList(MobCategory.creature).add(new SpawnListEntry(MobLizard.class, 10));
		Biomes.OVERWORLD_OUTBACK_GRASSY.getSpawnableList(MobCategory.creature).add(new SpawnListEntry(MobLizard.class, 10));
		Biomes.OVERWORLD_DESERT.getSpawnableList(MobCategory.creature).add(new SpawnListEntry(MobEmu.class, 10));
		Biomes.OVERWORLD_CAATINGA.getSpawnableList(MobCategory.creature).add(new SpawnListEntry(MobEmu.class, 10));
		Biomes.OVERWORLD_CAATINGA_PLAINS.getSpawnableList(MobCategory.creature).add(new SpawnListEntry(MobEmu.class, 10));
		Biomes.OVERWORLD_OUTBACK.getSpawnableList(MobCategory.creature).add(new SpawnListEntry(MobEmu.class, 10));
		Biomes.OVERWORLD_OUTBACK_GRASSY.getSpawnableList(MobCategory.creature).add(new SpawnListEntry(MobEmu.class, 10));
		Biomes.OVERWORLD_DESERT.getSpawnableList(MobCategory.creature).add(new SpawnListEntry(MobCamel.class, 10));
		Biomes.OVERWORLD_CAATINGA.getSpawnableList(MobCategory.creature).add(new SpawnListEntry(MobCamel.class, 10));
		Biomes.OVERWORLD_CAATINGA_PLAINS.getSpawnableList(MobCategory.creature).add(new SpawnListEntry(MobCamel.class, 10));
		Biomes.OVERWORLD_OUTBACK.getSpawnableList(MobCategory.creature).add(new SpawnListEntry(MobCamel.class, 10));
		Biomes.OVERWORLD_OUTBACK_GRASSY.getSpawnableList(MobCategory.creature).add(new SpawnListEntry(MobCamel.class, 10));
		Biomes.OVERWORLD_DESERT.getSpawnableList(MobCategory.creature).add(new SpawnListEntry(MobArmadillo.class, 10));
		Biomes.OVERWORLD_CAATINGA.getSpawnableList(MobCategory.creature).add(new SpawnListEntry(MobArmadillo.class, 10));
		Biomes.OVERWORLD_CAATINGA_PLAINS.getSpawnableList(MobCategory.creature).add(new SpawnListEntry(MobArmadillo.class, 10));
		Biomes.OVERWORLD_OUTBACK.getSpawnableList(MobCategory.creature).add(new SpawnListEntry(MobArmadillo.class, 10));
		Biomes.OVERWORLD_OUTBACK_GRASSY.getSpawnableList(MobCategory.creature).add(new SpawnListEntry(MobArmadillo.class, 10));
		NetworkHandler.registerNetworkMessage(FunnyRideableNetworkMessage::new);
		LOGGER.info("Funny Fauna initialized.");
	}

	@Override
	public void onRecipesReady() {}

	@Override
	public void initNamespaces() {}

	@Override
	public void beforeGameStart() {
		FunnyFaunaEntities.init();
	}



	@Override
	public void afterGameStart() {}

	public static Entity createEntity(Class<? extends Entity> entityClass, World world) {
		try {
			return entityClass.getConstructor(World.class).newInstance(world);
		} catch (Exception e) {
			throw new RuntimeException("Could not create Entity!");
		}
	}
}
