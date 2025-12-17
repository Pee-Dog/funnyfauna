package peedog.funnyfauna;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.sound.SoundRepository;
import turniplabs.halplibe.util.ClientStartEntrypoint;

import static peedog.funnyfauna.FunnyFauna.MOD_ID;

@Environment(EnvType.CLIENT)
public class FunnyFaunaClient implements ClientModInitializer, ClientStartEntrypoint {
	@Override
	public void beforeClientStart() {
		SoundRepository.registerNamespace(MOD_ID);
	}
	@Override
	public void afterClientStart() {

	}

	@Override
	public void onInitializeClient() {

	}
}
