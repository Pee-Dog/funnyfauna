package peedog.funnyfauna.entity.ai.path.follow;

import net.minecraft.core.util.helper.MathHelper;
import net.minecraft.core.world.pathfinder.Path;
import net.minecraft.core.world.pathfinder.Node;
import peedog.funnyfauna.entity.MobTaskrunner;
import peedog.funnyfauna.entity.ai.Task;
import peedog.funnyfauna.entity.ai.path.PathTask;
import peedog.funnyfauna.entity.ai.path.PheromonePathfinder;

import java.util.List;

public class FollowPheromoneTask extends PathTask<MobTaskrunner> {
	private final PheromonePathfinder pathfinder;
	private int pathRecalcTimer = 0;

	public FollowPheromoneTask(MobTaskrunner mob) {
		super(mob);
		this.pathfinder = new PheromonePathfinder(mob.world);
	}

	@Override
	protected void onStart() {
		// Optional: clear path on start to force a fresh search
		this.path = null;
	}

	@Override
	public Task onTick() {
		if (this.mob.world == null) return null;

		// 1. Only recalculate path every 10 ticks or if the path is done
		if (pathRecalcTimer-- <= 0 || this.path == null || this.path.isDone()) {
			pathRecalcTimer = 10;
			int mx = MathHelper.floor(this.mob.x);
			int my = MathHelper.floor(this.mob.y);
			int mz = MathHelper.floor(this.mob.z);

			List<PheromonePathfinder.ScentNode> nodes = pathfinder.findScentTrail(mx, my, mz, 16);

			if (nodes != null && nodes.size() > 2) {
				this.path = convertToMinecraftPath(nodes);
			} else {
				this.path = null;
				return null; // Stop and let WanderTask take over
			}
		}

		// 2. Perform movement but DO NOT call super.onTick() if it returns this.onTick()
		// Instead, handle the movement manually or ensure PathTask is fixed.
		super.onTick();

		if (this.path == null || this.path.isDone()) {
			return null;
		}

		return this; // Return this to indicate the task is still running for the NEXT tick
	}

	private Path convertToMinecraftPath(List<PheromonePathfinder.ScentNode> scentNodes) {
		// Convert our A* ScentNodes into Minecraft's native Path nodes
		Node[] nodes = new Node[scentNodes.size()];
		for (int i = 0; i < scentNodes.size(); i++) {
			PheromonePathfinder.ScentNode sn = scentNodes.get(i);
			nodes[i] = new Node(sn.x, sn.y, sn.z);
		}
		return new Path(nodes);
	}

	@Override
	protected boolean isEqual(Task other) {
		return other instanceof FollowPheromoneTask;
	}

	public boolean hasPath() {
		return this.path != null && !this.path.isDone();
	}
}
