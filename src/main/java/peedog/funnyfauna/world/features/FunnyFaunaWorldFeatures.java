package peedog.funnyfauna.world.features;

import net.minecraft.core.net.command.util.CommandHelper;

public class FunnyFaunaWorldFeatures {
	private static boolean hasInit = false;

	public static void init() {
		if (!hasInit) {
			hasInit = true;
			initializeWorldFeatures();
		}
	}

	private static void initializeWorldFeatures() {
		// Registers your Ant Hill so the game knows it exists
		CommandHelper.registerWorldFeatureClass(WorldFeatureAntHill.class, "AntHill");

	}
}
