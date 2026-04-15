package dev.creas.attention.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AttentionConfigManager {
	private static final Logger LOGGER = LoggerFactory.getLogger("attention");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final Path configPath = FabricLoader.getInstance().getGameDir().resolve("config").resolve("attention.json");
	private AttentionConfig config = AttentionConfig.defaults();

	public AttentionConfig getConfig() {
		return config;
	}

	public AttentionConfig load() {
		try {
			Files.createDirectories(configPath.getParent());

			if (Files.notExists(configPath)) {
				config = AttentionConfig.defaults();
				save();
				return config;
			}

			try (Reader reader = Files.newBufferedReader(configPath)) {
				AttentionConfig loadedConfig = GSON.fromJson(reader, AttentionConfig.class);
				config = loadedConfig == null ? AttentionConfig.defaults() : loadedConfig.sanitize();
			}
		} catch (IOException | JsonParseException exception) {
			LOGGER.warn("Failed to load attention config from {}", configPath, exception);
			config = AttentionConfig.defaults();
		}

		return config;
	}

	public void save() {
		try {
			Files.createDirectories(configPath.getParent());

			try (Writer writer = Files.newBufferedWriter(configPath)) {
				GSON.toJson(config, writer);
			}
		} catch (IOException exception) {
			LOGGER.warn("Failed to save attention config to {}", configPath, exception);
		}
	}
}
