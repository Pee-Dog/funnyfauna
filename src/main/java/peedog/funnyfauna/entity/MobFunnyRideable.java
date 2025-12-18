package peedog.funnyfauna.entity;

import net.minecraft.core.entity.animal.MobAnimal;
import net.minecraft.core.entity.player.Player;
import net.minecraft.core.world.World;
import peedog.funnyfauna.mixin.accessors.EntityAccessor;
import peedog.funnyfauna.net.message.FunnyRideableNetworkMessage;
import turniplabs.halplibe.helper.EnvironmentHelper;
import turniplabs.halplibe.helper.network.NetworkHandler;

public class MobFunnyRideable extends MobAnimal implements FunnyRideable { ;
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
		vehicleMovement();
	}

	@Override
	public void controlEntity(float moveForward, float moveStrafe, boolean isJumping, float xRot, float yRot) {
		if (EnvironmentHelper.isClientWorld()) {
			NetworkHandler.sendToServer(
				new FunnyRideableNetworkMessage(moveForward, moveStrafe, isJumping, xRot, yRot)
			);
		}

		float yawDeg = (float) (yRot * (Math.PI / 180));
		float step = 0.175F;

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
		if (!(passenger instanceof Player)) return;

		if (!canBeControlled()) return;

		Player player = (Player) passenger;
		player.handleSpecialVehicleControl();

		// Apply forward/strafe movement
		xd += xdChange;
		zd += zdChange;
		xdChange = 0.0;
		zdChange = 0.0;

		// Only allow jumping if on the ground
		if (playerUsedJump && this.onGround) {
			this.yd = 0.9F; // horse jump strength
			this.onGround = false;
		}

		playerUsedJump = false;

		player.sendSpecialVehiclePacket();

		// Clamp horizontal speed
		double horizontalSpeed = Math.sqrt(this.xd * this.xd + this.zd * this.zd);
		if (horizontalSpeed > 0.375) {
			double normal = 0.375 / horizontalSpeed;
			this.xd *= normal;
			this.zd *= normal;
		}
	}



	@Override
	public void updateAI() {
		if (this.passenger instanceof Player) {
			this.moveSpeed = 0.0F;
			this.moveStrafing = 0.0F;
			this.isJumping = false;
			this.footSize = rideFootSize;

			this.yRotO = this.yRot = this.passenger.yRot;
			this.xRotO = this.xRot = this.passenger.xRot;

			Player player = (Player) this.passenger;
			((EntityAccessor) player).setFallDistance(0.0F);

			return; // ← THIS IS THE IMPORTANT LINE
		}

		this.footSize = rideFootSize - 0.5f;
		super.updateAI();
	}
	protected boolean canBeControlled() {
		return true;
	}

}
