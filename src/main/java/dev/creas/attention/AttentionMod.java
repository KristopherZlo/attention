package dev.creas.attention;

import net.fabricmc.api.ModInitializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AttentionMod implements ModInitializer {
	public static final String MOD_ID = "attention";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final Identifier THREAT_PING_ID = Identifier.of(MOD_ID, "threat_ping");
	public static final SoundEvent THREAT_PING = SoundEvent.of(THREAT_PING_ID);

	@Override
	public void onInitialize() {
		Registry.register(Registries.SOUND_EVENT, THREAT_PING_ID, THREAT_PING);
	}
}

