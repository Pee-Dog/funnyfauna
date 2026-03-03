package peedog.funnyfauna.entity.ai.controllers;

import net.minecraft.core.entity.player.Player;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.compound.IdleTask;
import peedog.funnyfauna.entity.ai.path.flight.FlightTask;
import peedog.funnyfauna.entity.ai.path.flight.FlutterTask;
import peedog.funnyfauna.entity.moth.MobMoth;

public class MothTask extends Task<MobMoth> {

	private final FlutterTask<MobMoth> flutterTask;

	public MothTask(MobMoth moth) {
		super(moth);
		this.flutterTask = new FlutterTask<>(moth);
	}

	@Override
	protected void onStart() {

	}

	@Override
	protected Task onTick() {

		// If flying → flutter
		if (mob.isFlying()) {
			return flutterTask;
		}

		// Random takeoff — only at night; daytime moths stay put unless spooked
		if (!mob.isFlying() && !mob.shouldPerchInBrightDaylight() && random.nextInt(200) == 0) {
			mob.setPerched(false);
			mob.setFlying(true);
			mob.yd = .4;
			return flutterTask;
		}

		return null;
	}

	@Override
	protected void onStop(Task interruptTask) {

	}

	@Override
	protected boolean isEqual(Task other) {
		return other instanceof MothTask;
	}
}
