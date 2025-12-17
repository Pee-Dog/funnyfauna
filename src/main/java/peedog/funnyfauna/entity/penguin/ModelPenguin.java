package peedog.funnyfauna.entity.penguin;

import net.minecraft.client.render.model.Cube;
import net.minecraft.client.render.model.ModelBase;

public class ModelPenguin extends ModelBase {

	public Cube body, leg_l, leg_r, head, beak, wing_l, wing_r;

	public MobPenguin entity; // the penguin currently being rendered

	public ModelPenguin() {
		super();

		// Body
		this.body = new Cube(0, 0);
		this.body.addBox(-3.0F, -4.0F, -3.0F, 6, 11, 6, 0.0F);
		this.body.setRotationPoint(0.0F, 15.0F, 0.0F);

		// Legs
		this.leg_l = new Cube(0, 28);
		this.leg_l.addBox(-1.5F, 0.0F, -2.0F, 3, 2, 2);
		this.leg_l.setRotationPoint(1.5F, 22.0F, 0.0F);

		this.leg_r = new Cube(0, 28);
		this.leg_r.addBox(-1.5F, 0.0F, -2.0F, 3, 2, 2);
		this.leg_r.setRotationPoint(-1.5F, 22.0F, 0.0F);

		// Head and beak
		this.head = new Cube(10, 17);
		this.head.addBox(-2.0F, -4.0F, -2.0F, 4, 4, 4);
		this.head.setRotationPoint(0.0F, 11.0F, 0.0F);

		this.beak = new Cube(10, 25);
		this.beak.addBox(-1.0F, -2.0F, -5.0F, 2, 2, 3);
		this.beak.setRotationPoint(0.0F, 11.0F, 0.0F);

		// Wings
		this.wing_l = new Cube(0, 17);
		this.wing_l.addBox(0.0F, 0.0F, -2.0F, 1, 7, 4);
		this.wing_l.setRotationPoint(3.0F, 11.0F, 0.0F);

		this.wing_r = new Cube(0, 17);
		this.wing_r.addBox(-1.0F, 0.0F, -2.0F, 1, 7, 4);
		this.wing_r.setRotationPoint(-3.0F, 11.0F, 0.0F);
	}

	@Override
	public void render(float limbSwing, float limbYaw, float limbPitch,
					   float headYaw, float headPitch, float scale) {
		this.setRotationAngles(limbSwing, limbYaw, limbPitch, headYaw, headPitch, scale);
		this.body.render(scale);
		this.leg_l.render(scale);
		this.leg_r.render(scale);
		this.head.render(scale);
		this.beak.render(scale);
		this.wing_l.render(scale);
		this.wing_r.render(scale);
	}

	public void setRotationAngles(float limbSwing, float limbYaw, float limbPitch,
								  float headYaw, float headPitch, float scale) {

		// Head rotation
		this.head.yRot = headYaw * ((float)Math.PI / 180F);
		this.head.xRot = headPitch * ((float)Math.PI / 180F);
		this.beak.yRot = this.head.yRot;
		this.beak.xRot = this.head.xRot;

		// Walking animation
		this.leg_l.xRot = (float)Math.cos(limbSwing * 0.6662F) * 1.2F * limbYaw;
		this.leg_r.xRot = (float)Math.cos(limbSwing * 0.6662F + Math.PI) * 1.2F * limbYaw;

		this.wing_l.zRot = -limbPitch;
		this.wing_r.zRot = limbPitch;
	}
}
