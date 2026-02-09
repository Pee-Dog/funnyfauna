package peedog.funnyfauna.world;

import net.minecraft.core.world.World;

public class WorldWind {
	private static double windX = 1.0;
	private static double windZ = 0.0;
	private static float windAngle = 0.0f;

	private static final double WIND_CHANGE_SPEED = 0.0025; // angle change per tick

	public static void tick(World world) {
		// Slowly rotate wind direction
		windAngle += WIND_CHANGE_SPEED;
		windX = Math.cos(windAngle);
		windZ = Math.sin(windAngle);
	}

	public static double getWindX() { return windX; }
	public static double getWindZ() { return windZ; }
}
