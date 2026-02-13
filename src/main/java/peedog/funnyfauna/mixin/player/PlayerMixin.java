package peedog.funnyfauna.mixin.player;

import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.Mob;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.entity.projectile.ProjectileArrow;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.player.inventory.container.ContainerInventory;
import net.minecraft.core.util.helper.DamageType;
import net.minecraft.core.world.chunk.ChunkCoordinates;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import peedog.funnyfauna.PlayerInventoryDisplay;
import peedog.funnyfauna.entity.bunny.MobBunny;
import peedog.funnyfauna.entity.camel.MobCamel;
import peedog.funnyfauna.entity.fox.FoxDistractionHandler;

@Mixin(value = Player.class, remap = false)
public abstract class PlayerMixin extends Mob implements PlayerInventoryDisplay {

	public PlayerMixin(net.minecraft.core.world.World world) {
		super(world);
	}

	// Shadowed fields needed for movement logic
	@Shadow protected float baseSpeed;
	@Shadow protected float baseFlySpeed;

	@Shadow public ContainerInventory inventory;
	@Shadow public float cameraVelocity;
	@Shadow public float cameraVelocityOld;
	@Shadow private ChunkCoordinates lastDeathCoordinate;
	@Shadow protected abstract void collideWithPlayer(Entity entity);
	/* ----------------------------
			   B-Hop State Variables
			   ---------------------------- */
	@Unique private int funnyfauna$chainedJumps = 0;
	@Unique private int funnyfauna$ticksOnGround = 0;

	/* ----------------------------
	   Equipped slot state
	   ---------------------------- */
	@Unique private ItemStack funnyfauna$equippedSlot = null;
	@Unique private boolean funnyfauna$equippedEnabled = false;

	/* ----------------------------
	   Interface: state access
	   ---------------------------- */
	@Override
	public ItemStack funnyfauna$getEquippedSlot() {
		return funnyfauna$equippedSlot;
	}

	@Override
	public boolean funnyfauna$isEquippedEnabled() {
		return funnyfauna$equippedEnabled;
	}

	@Override
	public void funnyfauna$setEquippedSlot(ItemStack stack) {
		this.funnyfauna$equippedSlot = stack;
	}

	@Override
	public void funnyfauna$setEquippedEnabled(boolean enabled) {
		this.funnyfauna$equippedEnabled = enabled;
	}

	/* ----------------------------
	   GUI methods (NO-OP here)
	   ---------------------------- */
	@Override
	public void funnyfauna$displayGUISatchel(ItemStack stack) {}

	@Override
	public void funnyfauna$displayGUICamel(MobCamel camel) {}

	/* ----------------------------
	   Bunny Hopping Logic
	   ---------------------------- */
	@Inject(method = "onLivingUpdate", at = @At("HEAD"), cancellable = true)
	private void funnyfauna$phoppingExact(CallbackInfo ci) {

		// Only replace vanilla update if bunny is on head (passenger)
		if (!(this.passenger instanceof MobBunny)) {
			return; // let vanilla run normally
		}

		// ===== BEGIN EXACT PHOPPING CODE =====

		// Peaceful healing logic (Standard in Player.onLivingUpdate)
		if (this.world.getDifficulty() == net.minecraft.core.enums.Difficulty.PEACEFUL
			&& this.getHealth() < this.getMaxHealth()
			&& this.tickCount % (20 * 12) == 0) {
			this.heal(1);
		}

		this.inventory.decrementAnimations();
		this.cameraVelocityOld = this.cameraVelocity;

		// Call super (Mob) to handle basic physics and AI
		super.onLivingUpdate();

		// Handle flight speed (Standard player logic)
		this.flySpeed = this.baseFlySpeed;
		if (this.isSprinting()) {
			this.flySpeed = (float)((double)this.flySpeed + (double)this.baseFlySpeed * 0.3);
		}

		// ---- PHOPPING SPEED LOGIC ----
		this.speed = this.baseSpeed;

		if (this.isSprinting()) {
			// If we jump within a 5-tick window of landing, increment the jump chain
			if (funnyfauna$ticksOnGround > 0 && this.isJumping) {
				funnyfauna$ticksOnGround = 0;
				funnyfauna$chainedJumps++;
			}

			// If we stay on the ground too long, the chain is broken
			if (funnyfauna$ticksOnGround > 0.5) {
				funnyfauna$chainedJumps = 0;
			}

			// Apply Multiplicative Logarithmic Boost: (Base + SprintBoost) * multiplier
			// This formula provides the exponential speed feel of bhopping
			this.speed = (float)(((double)this.speed + (double)this.baseSpeed * 0.2)
				* (Math.log(funnyfauna$chainedJumps + 1) + 1));

			// Increment ground counter to track landing window

		} else {
			// Immediately reset chain if we stop sprinting
			funnyfauna$chainedJumps = 0;
		}
		if (this.onGround) {
			funnyfauna$ticksOnGround++;
		}
		// Camera Velocity & Bobbing Logic (from PHOPPING example)
		double velocity = (double) net.minecraft.core.util.helper.MathHelper.sqrt(this.xd * this.xd + this.zd * this.zd);
		double pitch = (double)((float)Math.atan(-this.yd * 0.2) * 15.0F);

		if (velocity > 0.1D) velocity = 0.1D;
		if (!this.onGround || this.getHealth() <= 0) velocity = 0.0D;
		if (this.onGround || this.getHealth() <= 0) pitch = 0.0D;

		this.cameraVelocity += (float)((velocity - this.cameraVelocity) * 0.4F);
		this.cameraPitch += (float)((pitch - this.cameraPitch) * 0.8F);

		// Clean up death coordinates
		if (!this.dead && this.lastDeathCoordinate != null
			&& this.distanceTo(
			(double)this.lastDeathCoordinate.x,
			(double)this.lastDeathCoordinate.y,
			(double)this.lastDeathCoordinate.z) < 8.0D) {
			this.lastDeathCoordinate = null;
		}

		// Player-to-Entity collision handling
		if (this.getHealth() > 0) {
			java.util.List<Entity> list = this.world.getEntitiesWithinAABBExcludingEntity(this, this.bb.grow(1.0D, 0.5D, 1.0D));
			if (list != null) {
				for (Entity entity : list) {
					if (!entity.removed) {
						this.collideWithPlayer(entity);
					}
				}
			}
		}

		// Cancel the original vanilla Player.onLivingUpdate() to let our custom logic take over
		ci.cancel();
	}



	/* ----------------------------
	   Existing Logic
	   ---------------------------- */
	@Inject(method = "hurt", at = @At("RETURN"))
	private void funnyfauna$onPlayerHurt(Entity attacker, int damage, DamageType type, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValue()) return;
		if (attacker == null) return;
		if (((Player)(Object)this).world.isClientSide) return;

		Player player = (Player)(Object)this;
		Entity blamed = attacker;

		if (attacker instanceof ProjectileArrow && ((ProjectileArrow) attacker).owner != null) {
			ProjectileArrow arrow = (ProjectileArrow) attacker;
			blamed = arrow.owner;
		}

		if (!(blamed instanceof Mob)) return;

		FoxDistractionHandler.alertFoxes(player, (Mob)blamed);
	}
}
