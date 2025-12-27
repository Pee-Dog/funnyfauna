package peedog.funnyfauna.client.render.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.model.Cube;

@Environment(EnvType.CLIENT)
public class ModelEmuEgg {

	public Cube egg;

	public ModelEmuEgg() {
		egg = new Cube(0, 0);

		/*
		 * Converted from Blockbench entity model:
		 * Size: 6 x 9 x 6
		 * Centered and grounded for block rendering
		 */
		egg.addBox(
			-3.0F,  // X (centered)
			-1.0F,  // Y (bottom touches ground after translate)
			-3.0F,  // Z (centered)
			6,      // width
			9,      // height
			6,      // depth
			0.0F
		);
	}

	public void render() {
		// 1/16 scale factor = correct block scale
		egg.render(0.0625F);
	}
}
