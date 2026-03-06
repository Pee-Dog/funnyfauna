package peedog.funnyfauna.entity.ai.path.flight;

import net.minecraft.core.block.Block;
import net.minecraft.core.block.material.Material;
import peedog.funnyfauna.entity.pigeon.MobPigeon;

public class PigeonFlightTask extends FlightTask<MobPigeon> {

	public PigeonFlightTask(MobPigeon mob) {
		super(mob);
	}

	@Override
	protected boolean prefersLeaves() {
		// Pigeons prefer the ground, so we skip the early-flight leaf seeking
		return false;
	}

	@Override
	protected boolean isValidLandingBlock(Block block) {
		// Explicitly reject water, lava, and leaves
		if (block.getMaterial() == Material.water ||
			block.getMaterial() == Material.lava ||
			block.getMaterial() == Material.leaves) {
			return false;
		}

		// Only allow landing on solid, cube-shaped ground blocks
		return block.isCubeShaped();
	}
}
