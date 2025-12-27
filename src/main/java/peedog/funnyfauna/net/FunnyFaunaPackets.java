package peedog.funnyfauna.net;

import com.mojang.nbt.NbtIo;
import com.mojang.nbt.tags.CompoundTag;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class FunnyFaunaPackets {

	public static CompoundTag readTag(byte[] data) {
		try {
			return NbtIo.readCompressed(new ByteArrayInputStream(data));
		} catch (IOException e) {
			return null;
		}
	}
}
