package peedog.funnyfauna.entity.ai;

import peedog.funnyfauna.entity.MobTaskrunner;

import java.util.Random;
import java.util.function.Predicate;

public abstract class Task<T extends MobTaskrunner> {

	private Task _sub = null;

	private boolean _first = true;
	private boolean _stopped = false;
	private boolean _active = false;

	public final T mob;
	public final Random random;
	public int time;

	public Task(T mob) {
		this.mob = mob;
		this.random = new Random();
		this.time = 0;
	}

	public void tick() {
		if (_first) {
			_active = true;
			onStart();
			_first = false;
			_stopped = false;
		}
		if (_stopped) return;

		Task newSub = onTick();
		// We have a sub task
		if (newSub != null) {
			if (!newSub.equals(_sub)) {
				// Our sub task is new
				if (_sub != null) {
					// Our previous sub must be interrupted.
					_sub.stop(newSub);
				}
			}
			_sub = newSub;

			// Run our child
			_sub.tick();
		} else {
			// We are null
			if (_sub != null) {
				// Our previous sub must be interrupted.
				_sub.stop();
				_sub = null;
			}
		}
		time++;
	}

	public void reset() {
		_first = true;
		_active = false;
		_stopped = false;
	}

	public void stop() {
		stop(null);
	}

	/**
	 * Stops the task. Next time it's run it will run `onStart`
	 */
	/**
	 * Stops the task. Next time it's run it will run `onStart`
	 */
	public void stop(Task interruptTask) {
		if (!_active) return;
		if (_stopped) return; // Already stopped, prevent recursion

		// Mark as stopped immediately to prevent recursion
		_stopped = true;

		if (!_first) {
			onStop(interruptTask);
		}

		// Stop child task if it exists and isn't already stopped
		if (_sub != null && _sub.isActive() && !_sub.stopped()) {
			_sub.stop(interruptTask);
		}

		_first = true;
		_active = false;
	}

	/**
	 * Lets the task know it's execution has been "suspended"
	 * <p>
	 * STILL RUNS `onStop`
	 * <p>
	 * Doesn't stop it all-together (meaning `isActive` still returns true)
	 */
	public void interrupt(Task interruptTask) {
		if (!_active) return;
		if (!_first) {
			onStop(interruptTask);
		}

		if (_sub != null && !_sub.stopped()) {
			_sub.interrupt(interruptTask);
		}

		_first = true;
	}
	// Virtual
	public boolean isFinished() {
		return false;
	}

	public boolean isActive() {
		return _active;
	}

	public boolean stopped() {
		return _stopped;
	}

	protected abstract void onStart();

	protected abstract Task onTick();

	// interruptTask = null if the task stopped cleanly
	protected abstract void onStop(Task interruptTask);

	protected abstract boolean isEqual(Task other);

	@Override
	public String toString() {
		return "Task";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj instanceof Task) {
			return isEqual((Task)obj);
		}
		return false;
	}

	public boolean thisOrChildSatisfies(Predicate<Task> pred) {
		Task t = this;
		while (t != null) {
			if (pred.test(t)) return true;
			t = t._sub;
		}
		return false;
	}

	public boolean thisOrChildAreTimedOut() {
		return false;
		//return thisOrChildSatisfies(task -> task instanceof TimeoutWanderTask);
	}

	/**
	 * Sometimes a task just can NOT be bothered to be interrupted right now.
	 * For instance, if we're in mid air and MUST complete the parkour movement.
	 */
	private boolean canBeInterrupted(Task subTask, Task toInterruptWith) {
		return true;
		//if (subTask == null) return true;
		// Our task can declare that is FORCES itself to be active NOW.
		//return (subTask.thisOrChildSatisfies(task -> {
		//	if (task instanceof ITaskCanForce canForce) {
		//		return !canForce.shouldForce(mod, toInterruptWith);
		//	}
		//	return true;
		//}));
	}
}
