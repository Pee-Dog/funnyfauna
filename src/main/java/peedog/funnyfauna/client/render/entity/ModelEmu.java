package peedog.funnyfauna.client.render.entity;

import net.minecraft.client.render.model.Cube;
import net.minecraft.client.render.model.ModelBase;

public class ModelEmu extends ModelBase {

	// Body parts (names preserved)
	public Cube head;
	public Cube tuft;
	public Cube beak;
	public Cube neck;
	public Cube body;
	public Cube tail;
	public Cube left_leg;
	public Cube left_foot;
	public Cube right_leg;
	public Cube right_foot;

	public ModelEmu() {
		super();

		// === Head ===
		this.head = new Cube(0, 0);
		this.head.addBox(-2.0F, -16.0F, -4.0F, 4, 10, 4);
		this.head.setRotationPoint(0.0F, 9.0F, -6.0F);

		// === Tuft ===
		this.tuft = new Cube(0, 9);
		this.tuft.addBox(0.0F, -18.0F, -4.0F, 0, 2, 5);
		this.tuft.setRotationPoint(0.0F, 9.0F, -6.0F);

		// === Beak ===
		this.beak = new Cube(16, 0);
		this.beak.addBox(-2.0F, -14.0F, -7.0F, 4, 2, 3);
		this.beak.setRotationPoint(0.0F, 9.0F, -6.0F);

		// === Neck ===
		this.neck = new Cube(16, 16);
		this.neck.addBox(-3.0F, -6.0F, -5.0F, 6, 10, 6);
		this.neck.setRotationPoint(0.0F, 9.0F, -6.0F);

		// === Body ===
		this.body = new Cube(1, 8);
		this.body.addBox(-5.0F, -5.0F, -7.0F, 10, 9, 15);
		this.body.setRotationPoint(0.0F, 10.0F, 0.0F);

		// === Left Leg ===
		this.left_leg = new Cube(56, 0);
		this.left_leg.addBox(-1.0F, 0.0F, -1.0F, 2, 10, 2);
		this.left_leg.setRotationPoint(2.0F, 14.0F, 0.0F);

		// === Left Foot ===
		this.left_foot = new Cube(52, 12);
		this.left_foot.addBox(-2.0F, 10.0F, -4.0F, 4, 0, 4);
		this.left_foot.setRotationPoint(2.0F, 14.0F, 0.0F);

		// === Right Leg ===
		this.right_leg = new Cube(56, 0);
		this.right_leg.addBox(-1.0F, 0.0F, -1.0F, 2, 10, 2);
		this.right_leg.setRotationPoint(-2.0F, 14.0F, 0.0F);

		// === Right Foot ===
		this.right_foot = new Cube(52, 12);
		this.right_foot.addBox(-2.0F, 10.0F, -4.0F, 4, 0, 4);
		this.right_foot.setRotationPoint(-2.0F, 14.0F, 0.0F);
	}

	@Override
	public void render(float limbSwing, float limbYaw, float limbPitch,
					   float headYaw, float headPitch, float scale) {
		this.setRotationAngles(limbSwing, limbYaw, limbPitch, headYaw, headPitch, scale);

		this.head.render(scale);
		this.tuft.render(scale);
		this.beak.render(scale);
		this.neck.render(scale);
		this.body.render(scale);
		this.left_leg.render(scale);
		this.left_foot.render(scale);
		this.right_leg.render(scale);
		this.right_foot.render(scale);
	}

	public void setRotationAngles(float limbSwing, float limbYaw, float limbPitch,
								  float headYaw, float headPitch, float scale) {

		// Head + neck rotation
		this.head.yRot = headYaw * ((float)Math.PI / 180F);
		this.head.xRot = headPitch * ((float)Math.PI / 180F);
		this.neck.yRot = this.head.yRot;
		this.neck.xRot = this.head.xRot;
		this.beak.yRot = this.head.yRot;
		this.beak.xRot = this.head.xRot;
		this.tuft.yRot = this.head.yRot;
		this.tuft.xRot = this.head.xRot;

		// Walking animation
		this.left_leg.xRot =
			(float)Math.cos(limbSwing * 0.6662F) * 1.2F * limbYaw;
		this.right_leg.xRot =
			(float)Math.cos(limbSwing * 0.6662F + Math.PI) * 1.2F * limbYaw;

		this.left_foot.xRot = this.left_leg.xRot;
		this.right_foot.xRot = this.right_leg.xRot;
	}
}
