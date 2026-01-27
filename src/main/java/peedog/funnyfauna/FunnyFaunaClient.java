package peedog.funnyfauna;

import com.mojang.nbt.NbtIo;
import com.mojang.nbt.tags.CompoundTag;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.particle.ParticleDispatcher;
import net.minecraft.client.render.texture.stitcher.AtlasStitcher;
import net.minecraft.client.render.texture.stitcher.TextureRegistry;
import net.minecraft.client.sound.SoundRepository;
import net.minecraft.core.net.packet.PacketCustomPayload;
import peedog.funnyfauna.particle.ParticleBugSquash;
import peedog.funnyfauna.particle.ParticleBugs;
import peedog.funnyfauna.particle.ParticleCricket;
import turniplabs.halplibe.helper.TextureHelper;
import turniplabs.halplibe.util.ClientStartEntrypoint;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static peedog.funnyfauna.FunnyFauna.MOD_ID;

@Environment(EnvType.CLIENT)
public class FunnyFaunaClient implements ClientModInitializer, ClientStartEntrypoint {

	// Make sure dispatcher is assigned correctly
	private static ParticleDispatcher dispatcher;

	@Override
	public void beforeClientStart() {
		SoundRepository.registerNamespace(MOD_ID);

		dispatcher = ParticleDispatcher.getInstance(); // assign to static field

		// Register custom particles
		dispatcher.addDispatch("bugs", (world, x, y, z, xa, ya, za, data) -> new ParticleBugs(world, x, y, z, 1.0F));
		dispatcher.addDispatch("cricket", (world, x, y, z, xa, ya, za, data) -> new ParticleCricket(world, x, y, z));
		dispatcher.addDispatch("bug_squash", (world, x, y, z, xd, yd, zd, data) ->
			new ParticleBugSquash(world, x, y, z, xd, yd, zd, 0.4F)
		);



	}

	public static ParticleDispatcher getParticleDispatcher() {
		return dispatcher;
	}

	@Override
	public void afterClientStart() {
		// no-op
	}

	@Override
	public void onInitializeClient() {
		registerTextures();
	}

	public static void sendStackUpdate(final CompoundTag tag){
		final Minecraft mc = Minecraft.getMinecraft();
		if (!mc.currentWorld.isClientSide) return;

		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			NbtIo.writeCompressed(tag, bos);
		} catch (final IOException e) {
			return;
		}

		final byte[] buffer = bos.toByteArray();
		mc.getSendQueue().addToSendQueue(new PacketCustomPayload("funnyfauna$setItems", buffer));
	}

	public static void registerTextures() {
		for (final AtlasStitcher stitcher : TextureRegistry.stitcherMap.values()) {
			try {
				TextureHelper.initializeAllFiles(MOD_ID, stitcher, Integer.MAX_VALUE);
			} catch (Exception e) {
				FunnyFauna.LOGGER.error("Failed to initialize texture files!", e);
			}
		}
	}
}
