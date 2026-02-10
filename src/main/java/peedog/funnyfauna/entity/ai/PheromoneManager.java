package peedog.funnyfauna.entity.ai;

import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

public class PheromoneManager {
	// Map of Block Coordinate Hash -> Strength
	private static final Map<Integer, Integer> scentMap = new HashMap<>();
	private static final int MAX_STRENGTH = 150;
	private static int decayTimer = 0;

	/**
	 * Creates a unique hash for block coordinates.
	 * This version handles negative coordinates and prevents bit-overflow collisions.
	 */
	public static int createHash(int x, int y, int z) {
		int l = y & 255 | (x & 32767) << 8 | (z & 32767) << 24;
		if (x < 0) l |= Integer.MIN_VALUE;
		if (z < 0) l |= 32768;
		return l;
	}

	public static void addScent(int x, int y, int z) {
		int hash = createHash(x, y, z);
		int current = scentMap.getOrDefault(hash, 0);

		// Reinforcement: Increases strength up to the cap
		if (current < MAX_STRENGTH) {
			scentMap.put(hash, Math.min(MAX_STRENGTH, current + 15)); // Increased gain for better trail definition
		}
	}

	public static int getStrength(int x, int y, int z) {
		return scentMap.getOrDefault(createHash(x, y, z), 0);
	}

	public static void tick() {
		// Slow decay: Every 2 seconds (40 ticks), weaken all trails
		if (++decayTimer % 40 == 0) {
			Iterator<Map.Entry<Integer, Integer>> it = scentMap.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<Integer, Integer> entry = it.next();
				int newValue = entry.getValue() - 1;
				if (newValue <= 0) {
					it.remove();
				} else {
					entry.setValue(newValue);
				}
			}
		}
	}

	// Safety: Clear scents on world change or reload
	public static void clear() {
		scentMap.clear();
	}
}
