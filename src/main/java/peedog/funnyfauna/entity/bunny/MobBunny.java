package peedog.funnyfauna.entity.bunny;

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
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import peedog.funnyfauna.entity.MobTaskrunner;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.controllers.BunnyTask;
import peedog.funnyfauna.entity.ai.interfaces.IFleeable;
import peedog.funnyfauna.entity.ai.interfaces.IFollower;
import turniplabs.halplibe.helper.EnvironmentHelper;

import java.util.UUID;

public class MobBunny extends MobTaskrunner implements IFleeable, IFollower {

	private static final int DATA_FLAGS = 16;
	private static final int DATA_OWNER_UUID = 17;
	private static final int DATA_SKIN_VARIANT = 18;

	private @Nullable String ownerUUID = null;

	// Fleeing Data
	private int fleeTimer = 0;
	private Entity fleeTarget = null;

	private int ridingCooldown = 20;
	private boolean wasEjected = false;

	public MobBunny(World world) {
		super(world);
		this.textureIdentifier = NamespaceID.getPermanent("funnyfauna", "bunny");
		// Smaller size for a bunny
		this.setSize(0.4F, 0.5F);
		// No drops defined in prompt, but you can add them here
	}

	@Override
	public void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(DATA_FLAGS, (byte)0, Byte.class);
		this.entityData.define(DATA_OWNER_UUID, (String)null, String.class);
		this.entityData.define(DATA_SKIN_VARIANT, 0, Integer.class);

	}

	@Override
	public Task<MobBunny> createTask() {
		return new BunnyTask(this);
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

	public @Nullable String getOwnerUUID() {
		return this.entityData.getString(DATA_OWNER_UUID);
	}

	public void setOwnerUUID(@Nullable String uuid) {
		this.entityData.set(DATA_OWNER_UUID, uuid);
	}

	@Override
	public int getMaxHealth() {
		return 4; // Less health than a lizard
	}

	public void setSkinVariant(int variant) {
		variant = Math.max(0, Math.min(2, variant)); // 3 variants: 0,1,2
		this.entityData.set(DATA_SKIN_VARIANT, variant);
	}

	public int getSkinVariant() {
		return this.entityData.getInt(DATA_SKIN_VARIANT);
	}

	public String getEntityTexture() {
		return "/assets/funnyfauna/textures/entity/bunny/" + getSkinVariant() + ".png";
	}



	@Override
	public boolean canSpawnHere() {
		int x = MathHelper.floor(this.x);
		int y = MathHelper.floor(this.bb.minY);
		int z = MathHelper.floor(this.z);
		int id = this.world.getBlockId(x, y - 1, z);
		// Standard grass spawn check
		return id != 0 && this.world.getBlockLightValue(x, y, z) > 8 && super.canSpawnHere();
	}

	// --- Fleeing Implementation (IFleeable) ---
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

	@Override
	public boolean hurt(Entity attacker, int damage, DamageType type) {
		boolean result = super.hurt(attacker, damage, type);

		if (result && !world.isClientSide) {
			// If hurt by an entity, run away!
			if (attacker != null) {
				this.setFleeTarget(attacker);
				this.setFleeTimer(200); // Flee for 10 seconds
			}
		}
		return result;
	}

	@Override
	public void updateAI() {
		if (world.isClientSide) return;
		// Skip AI if riding a player
		if (this.vehicle instanceof Player) return;
		super.updateAI();
	}

	@Override
	public void tick() {
		// Fix riding desync
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
			this.fallDistance = 0.0F;
		}
	}

	@Override
	public void causeFallDamage(float distance) {
		if (this.vehicle instanceof Player) return;
		super.causeFallDamage(distance);
	}

	@Override
	public boolean collidesWithBlock(net.minecraft.core.block.Block<?> block, int metadata) {
		if (this.vehicle instanceof Player) return false;
		return super.collidesWithBlock(block, metadata);
	}

	@Override
	public boolean collidesWith(Entity entity) {
		if (this.vehicle instanceof Player) return false;
		return super.collidesWith(entity);
	}

	@Override
	public boolean isSelectable() {
		if (this.vehicle instanceof Player && !((Player) this.vehicle).isSneaking()) return false;
		return super.isSelectable();
	}

	@Override
	public boolean isPickable() {
		if (this.vehicle instanceof Player && !((Player) this.vehicle).isSneaking()) return false;
		return super.isPickable();
	}

	@Override
	public boolean isPushable() {
		if (this.vehicle instanceof Player) return false;
		return super.isPushable();
	}
	@Override
	protected boolean canDespawn() {
		return !this.isTamed() && super.canDespawn();
	}

	@Override
	public void trySuffocate() {
		if (!(this.vehicle instanceof Player)) super.trySuffocate();
	}

	@Override
	public double getRidingHeight() {
		if (EnvironmentHelper.isClientWorld() && this.vehicle != Minecraft.getMinecraft().thePlayer) {
			return this.heightOffset + 0.5F;
		}
		return this.heightOffset - 1.1f;
	}

	@Override
	public void startRiding(IVehicle vehicle) {
		super.startRiding(vehicle);
		ridingCooldown = 20;

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

		// ---- Taming with Seeds ----
		if (!this.isTamed() && held != null) {
			// Get the internal item key (e.g., "tile.sapling.oak")
			String itemKey = held.getItem().getKey();

			// Check if the name exists and contains "sapling" (case-insensitive)
			if (itemKey != null && itemKey.toLowerCase().contains("sapling")) {
				// Consume the item
				if (player.getGamemode().consumeBlocks()) {
					player.swingItem();
					held.stackSize--;}
				if (held.stackSize <= 0) {
					held = null;
				}
			}

			if (!this.world.isClientSide) {
				if (this.random.nextInt(5) == 0) {
					this.setTamed(true);
					this.ownerUUID = player.uuid.toString();
					this.setHealthRaw(this.getMaxHealth());

					this.showHeartsOrSmokeFX(true);
					this.world.sendTrackedEntityStatusUpdatePacket(this, (byte)7);
				} else {
					this.showHeartsOrSmokeFX(false);
					this.world.sendTrackedEntityStatusUpdatePacket(this, (byte)6);
				}
			}
			return true;
		}

		// ---- Head placement (only if tamed and owned) ----
		if (this.isTamed() && this.ownerUUID != null && this.ownerUUID.equals(player.uuid.toString())) {
			if (this.vehicle == null && !player.isSneaking()) {
				if (!this.world.isClientSide) {
					this.startRiding(player);
				}
				return true;
			} else if (this.vehicle == player) {
				if (!this.world.isClientSide) {
					player.ejectRider();
				}
				return true;
			}
		}

		return super.interact(player);
	}

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

	@Override
	public void handleEntityEvent(byte event, float attackedAtYaw) {
		if (event == 7) this.showHeartsOrSmokeFX(true);
		else if (event == 6) this.showHeartsOrSmokeFX(false);
		else super.handleEntityEvent(event, attackedAtYaw);
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putBoolean("Tamed", isTamed());
		if (ownerUUID != null) tag.putString("Owner", ownerUUID);
		tag.putInt("Skin", getSkinVariant());

	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		setTamed(tag.getBoolean("Tamed"));
		if (tag.containsKey("Owner")) ownerUUID = tag.getString("Owner");
		setSkinVariant(tag.getInteger("Skin"));

	}

	// You may want to change these sound references if you have bunny sounds
	@Override
	public String getLivingSound() {
		return null; // Silent? or "random.click"?
	}

	@Override
	public String getHurtSound() {
		return "random.hurt";
	}

	@Override
	public String getDeathSound() {
		return "random.hurt";
	}

	@Override
	public @Nullable Entity leader() {
		String owner = this.getOwnerUUID();
		if (owner == null || owner.isEmpty()) {
			return null;
		}

		UUID uuid = UUID.fromString(owner);
		return this.world.getPlayerEntityByUUID(uuid);
	}
}
