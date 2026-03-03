package peedog.funnyfauna.entity.ai.interfaces;

public interface   IFlyable {
	boolean isFlying();
	void setFlying(boolean flying);

	int getFlightTime();
	void setFlightTime(int ticks);

	double getGroundY();
	void setGroundY(double y);

	boolean isPerched();
	void setPerched(boolean perched);

	boolean isLanding();
	void setLanding(boolean landing);

	// Wing animation fields
	float getFlap();
	void setFlap(float flap);

	float getFlapSpeed();
	void setFlapSpeed(float speed);

	float getFlapping();
	void setFlapping(float flapping);

	void setLandingSize(boolean landing);
}
