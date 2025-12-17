package peedog.funnyfauna.client.render.entity;

import net.minecraft.client.render.model.Cube;
import net.minecraft.client.render.model.ModelBase;
import peedog.funnyfauna.entity.lizard.MobLizard;

public class ModelLizard extends ModelBase {
	public MobLizard entity;

	public Cube head;
	public Cube body;
	public Cube tail;
	public Cube leg1;
	public Cube leg2;
	public Cube leg3;
	public Cube leg4;

	public ModelLizard() {
		super();

		// Head
		this.head = new Cube(0, 0);
		this.head.addBox(-2.0F, -3.0F, -6.0F, 4, 4, 6);
		this.head.setRotationPoint(0.0F, 21.0F, -5.0F);

		// Body
		this.body = new Cube(1, 10);
		this.body.addBox(-2.0F, -2.0F, -4.0F, 4, 4, 9);
		this.body.setRotationPoint(0.0F, 21.0F, -1.0F);

		// Tail
		this.tail = new Cube(21, 0);
		this.tail.addBox(-2.0F, -1.5F, 0.0F, 4, 2, 9);
		this.tail.setRotationPoint(0.0F, 21.5F, 4.0F);

		// Legs
		this.leg2 = new Cube(0, 26);
		this.leg2.addBox(-2.0F, -1.0F, -1.0F, 2, 4, 2);
		this.leg2.setRotationPoint(-2.0F, 21.0F, -4.0F);

		this.leg1 = new Cube(0, 26);
		this.leg1.addBox(-2.0F, -1.0F, -1.0F, 2, 4, 2);
		this.leg1.setRotationPoint(4.0F, 21.0F, -4.0F);

		this.leg3 = new Cube(0, 26);
		this.leg3.addBox(-2.0F, -1.0F, -1.0F, 2, 4, 2);
		this.leg3.setRotationPoint(4.0F, 21.0F, 3.0F);

		this.leg4 = new Cube(0, 26);
		this.leg4.addBox(-2.0F, -1.0F, -1.0F, 2, 4, 2);
		this.leg4.setRotationPoint(-2.0F, 21.0F, 3.0F);
	}

	@Override
	public void render(float limbSwing, float limbYaw, float limbPitch,
					   float headYaw, float headPitch, float scale) {

		this.setRotationAngles(limbSwing, limbYaw, limbPitch, headYaw, headPitch, scale);

		this.head.render(scale);
		this.body.render(scale);

		// ONLY render tail if it still exists
		if (this.entity == null || this.entity.hasTail) {
			this.tail.render(scale);
		}

		this.leg1.render(scale);
		this.leg2.render(scale);
		this.leg3.render(scale);
		this.leg4.render(scale);
	}

	public void setRotationAngles(float limbSwing, float limbYaw, float limbPitch,
								  float headYaw, float headPitch, float scale) {
		// Head rotation
		this.head.yRot = headYaw * ((float) Math.PI / 180F);
		this.head.xRot = headPitch * ((float) Math.PI / 180F);

		// Tail sway
		this.tail.yRot = (float) Math.cos(limbSwing * 0.6F) * 0.25F * limbYaw;

		// Legs crawling
		this.leg1.xRot = (float) Math.cos(limbSwing * 0.6662F) * 0.5F * limbYaw;
		this.leg2.xRot = (float) Math.cos(limbSwing * 0.6662F + (float)Math.PI) * 0.5F * limbYaw;
		this.leg3.xRot = (float) Math.cos(limbSwing * 0.6662F + (float)Math.PI) * 0.5F * limbYaw;
		this.leg4.xRot = (float) Math.cos(limbSwing * 0.6662F) * 0.5F * limbYaw;
	}
}
