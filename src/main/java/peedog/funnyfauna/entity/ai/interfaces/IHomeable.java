package peedog.funnyfauna.entity.ai.interfaces;

public interface IHomeable {
	boolean hasHome();
	void setHome(int x, int y, int z);
	int getHomeX();
	int getHomeY();
	int getHomeZ();
	double getDistanceToHomeSq(double x, double y, double z);

	// Add this convenience method
	default double getDistanceToHomeSq() {
		return getDistanceToHomeSq(0, 0, 0); // Will be overridden
	}
}
