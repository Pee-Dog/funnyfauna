package peedog.funnyfauna;

import com.mojang.nbt.NbtIo;
import com.mojang.nbt.tags.CompoundTag;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.particle.Particle;
import net.minecraft.client.entity.particle.ParticleDispatcher;
import net.minecraft.client.entity.particle.ParticleLambda;
import net.minecraft.client.gui.guidebook.mobs.MobInfoRegistry;
import net.minecraft.client.gui.hud.component.HudComponents;
import net.minecraft.client.render.texture.stitcher.AtlasStitcher;
import net.minecraft.client.render.texture.stitcher.TextureRegistry;
import net.minecraft.client.sound.SoundRepository;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.item.Items;
import net.minecraft.core.net.packet.PacketCustomPayload;
import net.minecraft.core.world.World;
import peedog.funnyfauna.entity.armadillo.MobArmadillo;
import peedog.funnyfauna.entity.boar.MobBoar;
import peedog.funnyfauna.entity.camel.MobCamel;
import peedog.funnyfauna.entity.emu.MobEmu;
import peedog.funnyfauna.entity.horse.MobHorse;
import peedog.funnyfauna.entity.lizard.MobLizard;
import peedog.funnyfauna.gui.HudEquippedSlot;
import peedog.funnyfauna.item.FunnyFaunaItems;
import peedog.funnyfauna.particle.*;
import turniplabs.halplibe.helper.TextureHelper;
import turniplabs.halplibe.util.ClientStartEntrypoint;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static peedog.funnyfauna.FunnyFauna.MOD_ID;

@Environment(EnvType.CLIENT)
public class FunnyFaunaClient implements ClientModInitializer, ClientStartEntrypoint {
	private static HudEquippedSlot equippedHud;
	// Make sure dispatcher is assigned correctly
	private static ParticleDispatcher dispatcher;

	@Override
	public void beforeClientStart() {
		SoundRepository.registerNamespace(MOD_ID);

		dispatcher = ParticleDispatcher.getInstance(); // assign to static field

		// Register custom particles
		dispatcher.addDispatch("bugs", (world, x, y, z, xa, ya, za, data) -> new ParticleBugs(world, x, y, z, 1.0F));
		dispatcher.addDispatch("cricket", (world, x, y, z, xa, ya, za, data) -> {
			int color = 0xFFFFFF; // default white
			try {
				color = (Integer) data;
			} catch (Exception ignored) {}
			return new ParticleCricket(world, x, y, z, color);
		});
		dispatcher.addDispatch("webspider", (world, x, y, z, xa, ya, za, data) -> new ParticleWebSpider(world, x, y, z));

		dispatcher.addDispatch("bug_squash", (world, x, y, z, xd, yd, zd, data) ->
			new ParticleBugSquash(world, x, y, z, xd, yd, zd, 0.4F)
		);
		dispatcher.addDispatch("glow", ParticleGlow::new);
		dispatcher.addDispatch("plume", ParticlePlume::new);



	}

	public static ParticleDispatcher getParticleDispatcher() {
		return dispatcher;
	}

	@Override
	public void afterClientStart() {
		Minecraft mc = Minecraft.getMinecraft();

		// Create HUD component
		equippedHud = new HudEquippedSlot();

		// Register HUD component safely after client start
		HudComponents.INSTANCE.register(equippedHud);
		MobInfoRegistry.register(MobBoar.class, "guidebook.section.mob.boar.name", "guidebook.section.mob.boar.desc",
			10, 10, new MobInfoRegistry.MobDrop[]{new MobInfoRegistry.MobDrop(new ItemStack(FunnyFaunaItems.COARSEHIDE), 1.0F, 1, 3), new MobInfoRegistry.MobDrop(new ItemStack(Items.FOOD_PORKCHOP_RAW), 0.66F, 0, 2)});
		MobInfoRegistry.register(MobArmadillo.class, "guidebook.section.mob.armadillo.name", "guidebook.section.mob.armadillo.desc",
			16, 10, new MobInfoRegistry.MobDrop[]{new MobInfoRegistry.MobDrop(new ItemStack(FunnyFaunaItems.SCUTE), 1.0F, 2, 3)});
		MobInfoRegistry.register(MobLizard.class, "guidebook.section.mob.lizard.name", "guidebook.section.mob.lizard.desc",
			6, 10, new MobInfoRegistry.MobDrop[]{new MobInfoRegistry.MobDrop(new ItemStack(FunnyFaunaItems.SCALES), 1.0F, 1, 3)});
		MobInfoRegistry.register(MobEmu.class, "guidebook.section.mob.emu.name", "guidebook.section.mob.emu.desc",
			10, 10, new MobInfoRegistry.MobDrop[]{new MobInfoRegistry.MobDrop(new ItemStack(Items.FEATHER_CHICKEN), 0.80F, 0, 4), new MobInfoRegistry.MobDrop(new ItemStack(FunnyFaunaItems.BIRDFOOT), 0.66F, 0, 2)});
		MobInfoRegistry.register(MobHorse.class, "guidebook.section.mob.horse.name", "guidebook.section.mob.horse.desc",
			15-25, 10, new MobInfoRegistry.MobDrop[]{new MobInfoRegistry.MobDrop(new ItemStack(Items.LEATHER), 1.0F, 2, 5)});
		MobInfoRegistry.register(MobCamel.class, "guidebook.section.mob.camel.name", "guidebook.section.mob.camel.desc",
			20-30, 10, new MobInfoRegistry.MobDrop[]{new MobInfoRegistry.MobDrop(new ItemStack(FunnyFaunaItems.COARSEHIDE), 1.0F, 2, 5)});
	}

	@Override
	public void onInitializeClient() {
		registerTextures();
	}
	public static HudEquippedSlot getEquippedHud() {
		return equippedHud;
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
