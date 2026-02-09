package peedog.funnyfauna.entity.ai;

import net.minecraft.core.entity.animal.MobAnimal;

public interface Behavior {
	/** Called every tick. Returns true if behavior is still active. */
	boolean tick(MobAnimal mob);

	/** Called when the behavior starts */
	default void start(MobAnimal mob) {}

	/** Called when the behavior ends */
	default void stop(MobAnimal mob) {}

	/** Optional: priority for behavior selection */
	default int getPriority() { return 0; }
}

