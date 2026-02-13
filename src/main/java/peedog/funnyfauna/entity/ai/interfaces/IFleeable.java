package peedog.funnyfauna.entity.ai.interfaces;

import net.minecraft.core.entity.Entity;

public interface IFleeable {
	int getFleeTimer();
	void setFleeTimer(int ticks);

	Entity getFleeTarget();
	void setFleeTarget(Entity entity);
}
