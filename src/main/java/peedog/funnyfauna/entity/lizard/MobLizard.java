package peedog.funnyfauna.entity.lizard;

import com.mojang.nbt.tags.CompoundTag;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Global;
import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.net.packet.PacketSetRiding;
import net.minecraft.core.util.collection.NamespaceID;
import net.minecraft.core.util.helper.DamageType;
import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.IVehicle;
import net.minecraft.core.world.World;
import net.minecraft.core.world.pathfinder.Path;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import peedog.funnyfauna.entity.MobTaskrunner;
import peedog.funnyfauna.entity.ai.controllers.LizardTask;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.interfaces.IFleeable;
import peedog.funnyfauna.item.FunnyFaunaItems;
import turniplabs.halplibe.helper.EnvironmentHelper;

import java.util.List;
import java.util.Objects;

public class MobLizard extends MobTaskrunner implements IFleeable {

	private static final int DATA_FLAGS = 16;
	private static final int DATA_OWNER_UUID = 17;


	private @Nullable Entity preyTarget = null;
	private @Nullable Path pathToEntity = null;
	private @Nullable String ownerUUID = null;

	// Fleeing data
	private Entity fleeTarget = null;
	private int fleeTimer = 0;

	private int ridingCooldown = 20;
	private boolean wasEjected = false;

	public MobLizard(World world) {
		super(world);
		this.textureIdentifier = NamespaceID.getPermanent("funnyfauna", "lizard");
		this.setSize(1F, 0.5F);
		this.mobDrops.add(new net.minecraft.core.WeightedRandomLootObject(
			FunnyFaunaItems.SCALES.getDefaultStack(), 1, 3));

	}


	@Override
	public void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(DATA_FLAGS, (byte)0, Byte.class);
		this.entityData.define(DATA_OWNER_UUID, (String)null, String.class);

		this.setHasTail(true);
	}

	@Override
	public Task<MobLizard> createTask() {
		return new LizardTask(this);
	}

	private byte getFlags() {
		return this.entityData.getByte(DATA_FLAGS);
	}

	private void setFlags(byte flags) {
		this.entityData.set(DATA_FLAGS, flags);
	}

	public boolean isTamed() {
		return (getFlags() & 1) != 0;
	}

	public void setTamed(boolean tamed) {
		byte f = getFlags();
		setFlags(tamed ? (byte)(f | 1) : (byte)(f & ~1));
	}

	public boolean hasTail() {
		return (getFlags() & 2) != 0;
	}

	public void setHasTail(boolean tail) {
		byte f = getFlags();
		setFlags(tail ? (byte)(f | 2) : (byte)(f & ~2));
	}

	public @Nullable String getOwnerUUID() {
		return this.entityData.getString(DATA_OWNER_UUID);
	}

	public void setOwnerUUID(@Nullable String uuid) {
		this.entityData.set(DATA_OWNER_UUID, uuid);
	}

	@Override
	public int getMaxHealth() {
		return 6;
	}

	// --- IFleeable implementation ---
	@Override
	public Entity getFleeTarget() {
		return this.fleeTarget;
	}

	@Override
	public void setFleeTarget(Entity entity) {
		this.fleeTarget = entity;
	}

	@Override
	public int getFleeTimer() {
		return this.fleeTimer;
	}

	@Override
	public void setFleeTimer(int i) {
		this.fleeTimer = i;
	}




	public boolean canSpawnHere() {
		int x = MathHelper.floor(this.x);
		int y = MathHelper.floor(this.bb.minY);
		int z = MathHelper.floor(this.z);

		int id = this.world.getBlockId(x, y - 1, z);

		// Prevent spawning on air, water, lava
		if (id == 0 || id == 8 || id == 9 || id == 10 || id == 11 || y < 128) {
			return false;
		}

		// Allow spawning on any other block
		return true;
	}

	@Override
	public boolean hurt(Entity attacker, int damage, DamageType type) {
		boolean result = super.hurt(attacker, damage, type);

		if (result && !world.isClientSide && hasTail()) {
			setHasTail(false);
			dropItem(FunnyFaunaItems.FOOD_LIZARDTAIL.id, 1);
		}

		return result;
	}


	@Override
	public void updateAI() {
		if (world.isClientSide) return;

		// Skip AI if riding a player
		if (this.vehicle instanceof Player) return;

		super.updateAI(); // run tasks only if not riding
	}

	@Override
	public void tick() {
		// Yes, this is stupid. Yes, it fixes riding desync.
		if (EnvironmentHelper.isServerEnvironment()
			&& ridingCooldown-- <= 0
			&& this.vehicle != null
			&& !wasEjected) {


			ridingCooldown = Global.TICKS_PER_SECOND * 2;

			MinecraftServer.getInstance().playerList.sendPacketToPlayersAroundPoint(
				this.x, this.y, this.z,
				32,
				this.world.dimension.id,
				new PacketSetRiding(this, (Entity) this.vehicle)
			);
		}
		if (this.vehicle == null) {
			wasEjected = false;
		}


		super.tick();

		// Disable physics/collision when riding player's head
		this.noPhysics = this.vehicle instanceof Player;

		if (this.vehicle instanceof Player) {
			Player player = (Player) this.vehicle;
			this.fallDistance = 0.0F;
		}
	}

	@Override
	public void causeFallDamage(float distance) {
		// Don't take fall damage when riding a player
		if (this.vehicle instanceof Player) {
			return;
		}
		super.causeFallDamage(distance);
	}

	@Override
	public boolean collidesWithBlock(net.minecraft.core.block.Block<?> block, int metadata) {
		// No block collision when riding player
		if (this.vehicle instanceof Player) {
			return false;
		}
		return super.collidesWithBlock(block, metadata);
	}

	@Override
	public boolean collidesWith(Entity entity) {
		// No entity collision when riding player
		if (this.vehicle instanceof Player) {
			return false;
		}
		return super.collidesWith(entity);
	}

	@Override
	public boolean isSelectable() {
		// Can't select lizard when on player's head (unless sneaking)
		if (this.vehicle instanceof Player && !((Player) this.vehicle).isSneaking()) {
			return false;
		}
		return super.isSelectable();
	}

	@Override
	public boolean isPickable() {
		// Can't pick lizard when on player's head (unless sneaking)
		if (this.vehicle instanceof Player && !((Player) this.vehicle).isSneaking()) {
			return false;
		}
		return super.isPickable();
	}

	@Override
	public boolean isPushable() {
		// Can't push lizard when riding player
		if (this.vehicle instanceof Player) {
			return false;
		}
		return super.isPushable();
	}

	@Override
	protected boolean canDespawn() {
		return !this.isTamed() && super.canDespawn();
	}

	@Override
	public void trySuffocate() {
		// Don't suffocate in blocks when riding player
		if (!(this.vehicle instanceof Player)) {
			super.trySuffocate();
		}
	}

	@Override
	public double getRidingHeight() {
		// Position on player's head
		if (EnvironmentHelper.isClientWorld() && this.vehicle != Minecraft.getMinecraft().thePlayer) {
			return this.heightOffset + 0.5F;
		}
		return this.heightOffset - 1.1f;
	}

	@Override
	public void startRiding(IVehicle vehicle) {
		super.startRiding(vehicle);
		ridingCooldown = 20; // force near-immediate sync

		if (EnvironmentHelper.isServerEnvironment()) {
			MinecraftServer.getInstance().playerList.sendPacketToPlayersAroundPoint(
				x, y, z, 32, world.dimension.id,
				new PacketSetRiding(this, (Entity) this.vehicle)
			);
		}
	}



	@Override
	public boolean interact(@NotNull Player player) {
		ItemStack held = player.inventory.getCurrentItem();

		// ---- Taming ----
		if (!this.isTamed() && held != null && held.itemID == FunnyFaunaItems.JAR_CRICKET.id) {

			// Consume the cricket jar
			if (player.getGamemode().consumeBlocks()) {
				held.consumeItem(player);
				if (held.stackSize <= 0) {
					player.inventory.setItem(player.inventory.getCurrentItemIndex(), null);
				}
			}

			if (!this.world.isClientSide) {
				if (this.random.nextInt(3) == 0) { // 33% tame chance
					this.setTamed(true);
					this.ownerUUID = player.uuid.toString();
					this.setHealthRaw(this.getMaxHealth());

					// Show hearts
					this.showHeartsOrSmokeFX(true);
					this.world.sendTrackedEntityStatusUpdatePacket(this, (byte)7);

					// Play success sound
					this.world.playSoundAtEntity(null, this, "random.orb", 1.0F, 1.2F);
				} else {
					// Show smoke on failure
					this.showHeartsOrSmokeFX(false);
					this.world.sendTrackedEntityStatusUpdatePacket(this, (byte)6);

					// Play failure sound
					this.world.playSoundAtEntity(null, this, "random.pop", 1.0F, 0.8F);
				}
			}

			return true;
		}

		// ---- Head placement (only if tamed and owned by player) ----
		if (this.isTamed() && this.ownerUUID != null && this.ownerUUID.equals(player.uuid.toString())) {
			if (this.vehicle == null && !player.isSneaking()) {
				// Mount on player's head
				if (!this.world.isClientSide) {
					this.startRiding(player);
				}
				return true;
			} else if (this.vehicle == player) {
				// Dismount from player's head
				if (!this.world.isClientSide) {
					player.ejectRider();
				}
				return true;
			}
		}

		return super.interact(player);
	}

	// Add this helper method for showing hearts/smoke particles
	private void showHeartsOrSmokeFX(boolean showHearts) {
		String particle = showHearts ? "heart" : "smoke";

		for (int i = 0; i < 7; i++) {
			double motionX = this.random.nextGaussian() * 0.02;
			double motionY = this.random.nextGaussian() * 0.02;
			double motionZ = this.random.nextGaussian() * 0.02;

			this.world.spawnParticle(
				particle,
				this.x + (this.random.nextFloat() * this.bbWidth * 2.0F) - this.bbWidth,
				this.y + 0.5 + (this.random.nextFloat() * this.bbHeight),
				this.z + (this.random.nextFloat() * this.bbWidth * 2.0F) - this.bbWidth,
				motionX, motionY, motionZ, 0
			);
		}
	}

	// Add this to handle the particle event
	@Override
	public void handleEntityEvent(byte event, float attackedAtYaw) {
		if (event == 7) {
			this.showHeartsOrSmokeFX(true);
		} else if (event == 6) {
			this.showHeartsOrSmokeFX(false);
		} else {
			super.handleEntityEvent(event, attackedAtYaw);
		}
	}



	@Override
	public void onLivingUpdate() {
		super.onLivingUpdate();

		// Regrow tail occasionally
		if (!this.hasTail() && this.random.nextInt(1200) == 0) {
			this.setHasTail(true);
		}

		// When tailless, scan for nearby mobs to flee from
		if (!world.isClientSide && !this.hasTail()) {
			// Decrement existing flee timer
			if (this.fleeTimer > 0) {
				this.fleeTimer--;
			}

			// Periodically scan for threats (every ~10 ticks)
			if (this.random.nextInt(10) == 0) {
				List<Entity> nearby = this.world.getEntitiesWithinAABBExcludingEntity(
					this,
					this.bb.expand(8.0, 4.0, 8.0)
				);

				Entity closest = null;
				double closestDist = Double.MAX_VALUE;

				for (Entity e : nearby) {
					// Ignore players (owner can be trusted), items, and other lizards
					if ((e instanceof Player && Objects.equals(((Player) e).uuid.toString(), ownerUUID)) || e == this) continue;
					if (!(e instanceof net.minecraft.core.entity.Mob)) continue;

					double dist = this.distanceTo(e);
					if (dist < closestDist) {
						closestDist = dist;
						closest = e;
					}
				}

				if (closest != null) {
					this.setFleeTarget(closest);
					this.setFleeTimer(100); // flee for 5 seconds
				}
			}
		}
	}
	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putBoolean("Tamed", isTamed());
		if (ownerUUID != null) tag.putString("Owner", ownerUUID);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		setTamed(tag.getBoolean("Tamed"));
		if (tag.containsKey("Owner")) ownerUUID = tag.getString("Owner");
	}



	@Override
	public String getLivingSound() {
		return "funnyfauna:mob.lizard.idle";
	}

	@Override
	public String getHurtSound() {
		return "funnyfauna:mob.lizard.hurt";
	}

	@Override
	public String getDeathSound() {
		return "funnyfauna:mob.lizard.death";
	}

}
