package peedog.funnyfauna.entity.ai.interfaces;

import net.minecraft.core.entity.Entity;

public interface IAttacker {
	float getAttackRange();
	boolean isAttacking();
	int getAttackCooldown();         // ticks until next attack
	void setAttackCooldown(int ticks);
	void startAttack(Entity target); // triggers animation + damage
}
