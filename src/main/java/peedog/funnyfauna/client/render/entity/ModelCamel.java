package peedog.funnyfauna.client.render.entity;

import net.minecraft.client.render.model.Cube;
import net.minecraft.client.render.model.ModelBase;

public class ModelCamel extends ModelBase {

	// === Head ===
	public Cube head;
	public Cube ear2;
	public Cube snout;
	public Cube ear1;
	public Cube neck;

	// === Body ===
	public Cube body1;
	public Cube body2;
	public Cube hump;
	public Cube tail;

	// === Legs ===
	public Cube leg1;
	public Cube foot1;
	public Cube leg2;
	public Cube foot2;
	public Cube leg3;
	public Cube foot3;
	public Cube leg4;
	public Cube foot4;

	public ModelCamel() {
		super();

		// === Head ===
		this.head = new Cube(0, 0);
		this.head.addBox(-4.0F, -16.0F, -9.0F, 8, 13, 6);
		this.head.setRotationPoint(0.0F, 2.0F, -11.0F);

		this.ear2 = new Cube(30, 14);
		this.ear2.addBox(-4.0F, -18.0F, -5.0F, 1, 2, 2);
		this.ear2.setRotationPoint(0.0F, 2.0F, -11.0F);

		this.ear1 = new Cube(30, 14);
		this.ear1.addBox(3.0F, -18.0F, -5.0F, 1, 2, 2);
		this.ear1.setRotationPoint(0.0F, 2.0F, -11.0F);

		this.snout = new Cube(1, 21);
		this.snout.addBox(-2.0F, -15.0F, -15.0F, 4, 5, 6);
		this.snout.setRotationPoint(0.0F, 2.0F, -11.0F);

		this.neck = new Cube(23, 16);
		this.neck.addBox(-4.0F, -3.0F, -9.0F, 8, 7, 9);
		this.neck.setRotationPoint(0.0F, 2.0F, -11.0F);

		// === Body ===
		this.body1 = new Cube(14, 10);
		this.body1.addBox(-7.0F, -5.0F, -11.0F, 14, 11, 11);
		this.body1.setRotationPoint(0.0F, 2.0F, 0.0F);

		this.body2 = new Cube(14, 10);
		this.body2.addBox(-7.0F, -5.0F, 0.0F, 14, 11, 11);
		this.body2.setRotationPoint(0.0F, 2.0F, 0.0F);

		this.hump = new Cube(18, 14);
		this.hump.addBox(-4.0F, -9.0F, -6.0F, 8, 4, 12);
		this.hump.setRotationPoint(0.0F, 2.0F, 0.0F);

		this.tail = new Cube(31, 0);
		this.tail.addBox(-2.0F, -4.0F, 11.1F, 4, 10, 0);
		this.tail.setRotationPoint(0.0F, 2.0F, 0.0F);

		// === Legs ===
		this.leg1 = new Cube(27, 12);
		this.leg1.addBox(-2.0F, 0.0F, -2.0F, 4, 13, 4);
		this.leg1.setRotationPoint(-5.0F, 8.0F, 7.0F);

		this.foot1 = new Cube(45, 1);
		this.foot1.addBox(-2.0F, 13.0F, -2.0F, 4, 3, 4);
		this.foot1.setRotationPoint(-5.0F, 8.0F, 7.0F);

		this.leg2 = new Cube(27, 12);
		this.leg2.addBox(-2.0F, 0.0F, -2.0F, 4, 13, 4);
		this.leg2.setRotationPoint(5.0F, 8.0F, 7.0F);

		this.foot2 = new Cube(45, 1);
		this.foot2.addBox(-2.0F, 13.0F, -2.0F, 4, 3, 4);
		this.foot2.setRotationPoint(5.0F, 8.0F, 7.0F);

		this.leg3 = new Cube(27, 12);
		this.leg3.addBox(-2.0F, 0.0F, -2.0F, 4, 13, 4);
		this.leg3.setRotationPoint(-5.0F, 8.0F, -8.0F);

		this.foot3 = new Cube(45, 1);
		this.foot3.addBox(-2.0F, 13.0F, -2.0F, 4, 3, 4);
		this.foot3.setRotationPoint(-5.0F, 8.0F, -8.0F);

		this.leg4 = new Cube(29, 12);
		this.leg4.addBox(-2.0F, 0.0F, -2.0F, 4, 13, 4);
		this.leg4.setRotationPoint(5.0F, 8.0F, -8.0F);

		this.foot4 = new Cube(45, 1);
		this.foot4.addBox(-2.0F, 13.0F, -2.0F, 4, 3, 4);
		this.foot4.setRotationPoint(5.0F, 8.0F, -8.0F);
	}

	@Override
	public void render(float limbSwing, float limbYaw, float limbPitch,
					   float headYaw, float headPitch, float scale) {

		this.setRotationAngles(limbSwing, limbYaw, limbPitch, headYaw, headPitch, scale);

		this.head.render(scale);
		this.ear1.render(scale);
		this.ear2.render(scale);
		this.snout.render(scale);
		this.neck.render(scale);

		this.body1.render(scale);
		this.body2.render(scale);
		this.hump.render(scale);
		this.tail.render(scale);

		this.leg1.render(scale);
		this.foot1.render(scale);
		this.leg2.render(scale);
		this.foot2.render(scale);
		this.leg3.render(scale);
		this.foot3.render(scale);
		this.leg4.render(scale);
		this.foot4.render(scale);
	}

	public void setRotationAngles(float limbSwing, float limbYaw, float limbPitch,
								  float headYaw, float headPitch, float scale) {

		// Head rotation
		this.head.yRot = headYaw * ((float)Math.PI / 180F);
		this.head.xRot = headPitch * ((float)Math.PI / 180F);

		this.neck.yRot = this.head.yRot;
		this.neck.xRot = this.head.xRot;
		this.snout.yRot = this.head.yRot;
		this.snout.xRot = this.head.xRot;
		this.ear1.yRot = this.head.yRot;
		this.ear1.xRot = this.head.xRot;
		this.ear2.yRot = this.head.yRot;
		this.ear2.xRot = this.head.xRot;


		// Walking animation
		this.leg1.xRot = (float)Math.cos(limbSwing * 0.6662F) * 1.4F * limbYaw;
		this.leg2.xRot = (float)Math.cos(limbSwing * 0.6662F + Math.PI) * 1.4F * limbYaw;
		this.leg3.xRot = this.leg2.xRot;
		this.leg4.xRot = this.leg1.xRot;

		this.foot1.xRot = this.leg1.xRot;
		this.foot2.xRot = this.leg2.xRot;
		this.foot3.xRot = this.leg3.xRot;
		this.foot4.xRot = this.leg4.xRot;
	}
}
