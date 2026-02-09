package peedog.funnyfauna.entity;

import net.minecraft.core.entity.animal.MobAnimal;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.world.World;
import peedog.funnyfauna.entity.FunnyRideable;
import peedog.funnyfauna.entity.horse.MobHorse;
import peedog.funnyfauna.mixin.accessors.EntityAccessor;
import peedog.funnyfauna.net.message.FunnyRideableNetworkMessage;
import turniplabs.halplibe.helper.EnvironmentHelper;
import turniplabs.halplibe.helper.network.NetworkHandler;

public class MobFunnyRideable extends MobAnimal implements FunnyRideable {
	protected boolean jumpPressed;
	protected float rideFootSize;
	protected double xdChange = 0;
	protected double zdChange = 0;
	protected boolean playerUsedJump = false;

	public MobFunnyRideable(World world) {
		super(world);
	}

	@Override
	public void tick() {
		super.tick();
		if (this.passenger instanceof Player) {
			this.yRotO = this.yRot = this.passenger.yRot;
			this.xRotO = this.xRot = this.passenger.xRot;
		}
		this.onGround();

		if (canBeControlled()) {
			vehicleMovement();
		}
	}

	@Override
	public void controlEntity(float moveForward, float moveStrafe, boolean isJumping, float xRot, float yRot) {
		if (!canBeControlled()) return;
		if (EnvironmentHelper.isClientWorld()) {
			NetworkHandler.sendToServer(
				new FunnyRideableNetworkMessage(moveForward, moveStrafe, isJumping, xRot, yRot)
			);
		}

		float yawDeg = (float) (yRot * (Math.PI / 180));

		// movement step controlled by horse's movementSpeed
		float step = getRideMovementSpeed();

		if (moveForward > 0.1F || moveForward < -0.1F) {
			xdChange += moveForward * -Math.sin(yawDeg) * step;
			zdChange += moveForward * Math.cos(yawDeg) * step;
		}

		if (moveStrafe > 0.1F || moveStrafe < -0.1F) {
			xdChange += moveStrafe * Math.cos(yawDeg) * step;
			zdChange += moveStrafe * Math.sin(yawDeg) * step;
		}

		if (isJumping && !jumpPressed) {
			playerUsedJump = true;
			jumpPressed = true;
		}

		if (this.jumpPressed && !isJumping) {
			this.jumpPressed = false;
		}
	}

	public void vehicleMovement() {
		if (passenger == null || !(this.passenger instanceof Player)) return;
		Player player = (Player) this.passenger;

		player.handleSpecialVehicleControl();

		xd += xdChange;
		zd += zdChange;
		xdChange = 0.0;
		zdChange = 0.0;

		if (playerUsedJump && this.onGround) {
			yd = getRideJumpStrength();
			this.onGround = false;
		} else {
			if (isInWater()) yd = 0.5;
		}
		playerUsedJump = false;

		player.sendSpecialVehiclePacket();

		// Limit horizontal speed based on horse's movementSpeed
		double horizontalSpeed = Math.sqrt(this.xd * this.xd + this.zd * this.zd);
		double speedCap = getRideMovementSpeed();

		if (horizontalSpeed > speedCap) {
			double normal = speedCap / horizontalSpeed;
			this.xd *= normal;
			this.zd *= normal;
		}

	}

	@Override
	public void updateAI() {
		// If a player is riding and the mob can be controlled, skip wandering AI completely
		if (this.passenger instanceof Player && canBeControlled()) {
			// Disable all autonomous movement
			this.moveSpeed = 0.0F;
			this.moveStrafing = 0.0F;
			this.isJumping = false;
			this.footSize = rideFootSize;

			Player player = (Player) this.passenger;
			((EntityAccessor) player).setFallDistance(0.0F);
			return; // <--- EARLY RETURN prevents wandering AI
		}

		// Otherwise, run normal AI
		this.footSize = rideFootSize - 0.5f;
		super.updateAI();
	}


	public void onGround() {
		if (this.onGround) {
		}
	}

	protected boolean canBeControlled() {
		return true;
	}
	// MobFunnyRideable.java

	protected float getRideMovementSpeed() {
		return 0.175f;
	}

	// MobFunnyRideable
	protected float getRideJumpStrength() {
		return 1.4f;
	}


}
