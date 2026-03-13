package peedog.funnyfauna.entity.sasquatch;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.util.collection.NamespaceID;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.World;
import peedog.funnyfauna.entity.MobTaskrunner;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.controllers.SasquatchTask;
import peedog.funnyfauna.entity.ai.interfaces.IFleeable;

public class MobSasquatch extends MobTaskrunner implements IFleeable {

	public static final double SAFE_DISTANCE    = 70.0;
	public static final double DESPAWN_DISTANCE = 50.0;

	/** 1.0 = fully opaque, 0.0 = fully invisible. Lerped each tick. */
	public float alpha = 1.0F;                                                                                                                

	private Entity fleeTarget  = null;
	private int    fleeTimer   = 0;
	private int    scanCounter = 0;

	public MobSasquatch(World world) {
		super(world);
		this.textureIdentifier = NamespaceID.getPermanent("funnyfauna", "sasquatch");
		this.setSize(1.2F, 2.8F);
	}

	// ------------------------------------------------------------------
	// IFleeable
	// ------------------------------------------------------------------

	@Override public Entity getFleeTarget()         { return fleeTarget; }
	@Override public void   setFleeTarget(Entity e) { this.fleeTarget = e; }
	@Override public int    getFleeTimer()           { return fleeTimer; }
	@Override public void   setFleeTimer(int ticks)  { this.fleeTimer = ticks; }

	// ------------------------------------------------------------------
	// AI
	// ------------------------------------------------------------------

	@Override
	public Task<MobSasquatch> createTask() {
		return new SasquatchTask(this);
	}

	// ------------------------------------------------------------------
	// Living update
	// ------------------------------------------------------------------

	@Override
	public void onLivingUpdate() {
		super.onLivingUpdate();

		if (world.isClientSide) return;

		if (this.fleeTimer > 0) {
			this.fleeTimer--;
		}

		// Deterministic scan every 5 ticks — no random gate so it never
		// accidentally skips for long stretches.
		if (++scanCounter >= 5) {
			scanCounter = 0;

			Player nearest = (Player) this.world.getClosestPlayerToEntity(this, SAFE_DISTANCE);

			if (nearest != null) {
				double dist = this.distanceTo(nearest);

				if (dist < SAFE_DISTANCE) {
					this.setFleeTarget(nearest);
					this.setFleeTimer(80);
				}

				// Fade out linearly between SAFE_DISTANCE and DESPAWN_DISTANCE.
				// Below DESPAWN_DISTANCE alpha clamps to 0 and the mob despawns.
				float targetAlpha = (float) ((dist - DESPAWN_DISTANCE) / (SAFE_DISTANCE - DESPAWN_DISTANCE));
				targetAlpha = Math.max(0.0F, Math.min(1.0F, targetAlpha));
				// Lerp toward target so the fade is smooth across scan gaps
				this.alpha += (targetAlpha - this.alpha) * 0.3F;

				if (this.alpha <= 0.01F) {
					this.remove();
					return;
				}
			} else {
				// No player nearby — fade back to fully opaque
				this.alpha += (1.0F - this.alpha) * 0.3F;
			}
		}
	}

	// ------------------------------------------------------------------
	// Stats / spawning
	// ------------------------------------------------------------------

	@Override
	public int getMaxHealth() { return 30; }

	@Override
	public boolean canSpawnHere() {
		int x  = MathHelper.floor(this.x);
		int y  = MathHelper.floor(this.bb.minY);
		int z  = MathHelper.floor(this.z);
		int id = this.world.getBlockId(x, y - 1, z);
		if (id == 0 || id == 8 || id == 9 || id == 10 || id == 11) return false;
		return this.world.getBlockLightValue(x, y, z) <= 7;
	}

	@Override
	public boolean interact(Player player) { return false; }

	// ------------------------------------------------------------------
	// Persistence
	// ------------------------------------------------------------------

	@Override
	public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); }

	@Override
	public void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag); }

	// ------------------------------------------------------------------
	// Sounds
	// ------------------------------------------------------------------

	@Override public String getLivingSound() { return "funnyfauna:mob.sasquatch.idle"; }
	@Override public String getHurtSound()   { return "funnyfauna:mob.sasquatch.hurt"; }
	@Override public String getDeathSound()  { return "funnyfauna:mob.sasquatch.death"; }
}
