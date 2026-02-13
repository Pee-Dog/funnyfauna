package peedog.funnyfauna.entity.ai.interfaces;

import net.minecraft.core.entity.Entity;
import org.jetbrains.annotations.Nullable;

public interface IFollower {

	@Nullable Entity leader();

	default float followStartDistance() { return 4.0F; }

	default float teleportDistance() { return 12.0F; }

	default float followSpeed() { return 1.0F; }
}
