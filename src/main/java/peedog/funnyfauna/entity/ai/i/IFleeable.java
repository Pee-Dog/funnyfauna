package peedog.funnyfauna.entity.ai.i;

import net.minecraft.core.entity.Entity;

public interface IFleeable {
	int getFleeTimer();
	void setFleeTimer(int ticks);

	Entity getFleeTarget();
	void setFleeTarget(Entity entity);
}
