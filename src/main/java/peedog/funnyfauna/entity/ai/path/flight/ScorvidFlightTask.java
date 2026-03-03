package peedog.funnyfauna.entity.ai.path.flight;

import net.minecraft.core.block.Block;
import net.minecraft.core.block.material.Material;
import peedog.funnyfauna.entity.MobTaskrunner;
import peedog.funnyfauna.entity.ai.interfaces.IFlyable;

/**
 * Flight task for Scorvids.
 * <ul>
 *   <li>Lands on any solid block that is not lava (water is also excluded since
 *       Scorvids are Nether-adapted and should never land on water either).</li>
 *   <li>Never prefers leaves — Scorvids are ground/ledge perchers, not tree birds.</li>
 *   <li>Never delegates to {@link FlightSoloPerchTask} — Scorvids are always flock-oriented.</li>
 * </ul>
 */
public class ScorvidFlightTask<T extends MobTaskrunner & IFlyable> extends FlightTask<T> {

	public ScorvidFlightTask(T mob) {
		super(mob);
	}

	/**
	 * Scorvids can land on any solid cube that is neither lava nor water.
	 * Leaves are intentionally excluded — they don't perch in trees.
	 */
	@Override
	protected boolean isValidLandingBlock(Block block) {
		if (block.getMaterial() == Material.lava) return false;
		if (block.getMaterial() == Material.water) return false;
		return block.isCubeShaped();
	}

	/** Scorvids do not favour leaves — skip the early-phase leaf preference entirely. */
	@Override
	protected boolean prefersLeaves() {
		return false;
	}

	/** Scorvids are always flock-oriented and never seek solo perches. */
	@Override
	protected boolean canUseSoloPerch() {
		return false;
	}
}
