package peedog.funnyfauna.mixin.accessors;

import net.minecraft.core.entity.Mob;
import net.minecraft.core.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Mob.class)
public interface IMobAccessor {
	@Accessor("currentTarget")
	void setCurrentTarget(Entity target); // exposes the private field
}
