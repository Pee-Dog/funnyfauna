package peedog.funnyfauna.block;

import java.util.Objects;

public class ServerBlockPos3D {
	public final int x, y, z;

	public ServerBlockPos3D(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ServerBlockPos3D pos = (ServerBlockPos3D) o;
		return x == pos.x && y == pos.y && z == pos.z;
	}

	@Override
	public int hashCode() {
		return Objects.hash(x, y, z);
	}

	@Override
	public String toString() {
		return "ServerBlockPos3D{x=" + x + ", y=" + y + ", z=" + z + "}";
	}
}
