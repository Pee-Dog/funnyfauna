package peedog.funnyfauna.entity.util;

import net.minecraft.core.entity.Entity;
import net.minecraft.core.entity.Mob;
import net.minecraft.core.util.helper.MathHelper;

public class LookUtil {
	private static float rotationLerp(float f, float f1, float f2) {
		float f3;
		for(f3 = f1 - f; f3 < -180.0F; f3 += 360.0F) {
		}

		while(f3 >= 180.0F) {
			f3 -= 360.0F;
		}

		if (f3 > f2) {
			f3 = f2;
		}

		if (f3 < -f2) {
			f3 = -f2;
		}

		return f + f3;
	}

	public static void lookAt(Mob mob, Entity entity, float yRot, float xRot) {
		double x = entity.x - mob.x;
		double y = entity.z - mob.z;
		double y2;
		if (entity instanceof Mob) {
			Mob mob1 = (Mob)entity;
			y2 = mob1.y + (double)mob1.getHeadHeight() - (mob.y + (double)mob.getHeadHeight());
		} else {
			y2 = (entity.bb.minY + entity.bb.maxY) / (double)2.0F - (mob.y + (double)mob.getHeadHeight());
		}

		double d3 = (double) MathHelper.sqrt(x * x + y * y);
		float f2 = (float)(Math.atan2(y, x) * (double)180.0F / Math.PI) - 90.0F;
		float f3 = (float)(-(Math.atan2(-y2, d3) * (double)180.0F / Math.PI));
		mob.xRot = -rotationLerp(-mob.xRot, f3, xRot);
		mob.xRot = Math.min(40, Math.max(-40, mob.xRot));
		mob.yRot = rotationLerp(mob.yRot, f2, yRot);
	}
}
