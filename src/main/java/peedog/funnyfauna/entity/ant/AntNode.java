package peedog.funnyfauna.entity.ant;

public class AntNode {

	public enum Face {
		UP, DOWN, NORTH, SOUTH, EAST, WEST
	}

	public final int x, z;
	public int y;
	public final Face face;

	float g, h, f;
	AntNode parent;
	boolean closed;
	int heapIdx = -1;

	private final int hash;

	public AntNode(int x, int y, int z, Face face) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.face = face;
		this.hash = computeHash(x, y, z, face);
	}

	private static int computeHash(int x, int y, int z, Face face) {
		int h = (x & 32767) | ((y & 255) << 15) | ((z & 32767) << 23);
		return h ^ (face.ordinal() << 29);
	}

	@Override
	public int hashCode() {
		return hash;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof AntNode)) return false;
		AntNode n = (AntNode)o;
		return n.x == x && n.y == y && n.z == z && n.face == face;
	}
}
