package dev.creas.attention.client;

import dev.creas.attention.client.command.AttentionClientCommands;
import dev.creas.attention.client.config.AttentionConfigManager;
import dev.creas.attention.client.hud.AttentionMarkerController;
import dev.creas.attention.client.hud.AttentionMarkerRenderer;
import dev.creas.attention.client.screen.AttentionSettingsScreen;
import dev.creas.attention.client.threat.AttentionThreatTracker;
import dev.creas.attention.client.threat.LiveThreatDetector;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class AttentionClient implements ClientModInitializer {
	private static final Identifier MARKER_LAYER = Identifier.of("attention", "crosshair_marker");
	private static final AttentionConfigManager CONFIG_MANAGER = new AttentionConfigManager();
	private static final AttentionMarkerController MARKER_CONTROLLER = new AttentionMarkerController();
	private static final AttentionMarkerRenderer MARKER_RENDERER = new AttentionMarkerRenderer(MARKER_CONTROLLER.getState());

	private KeyBinding openSettingsKeyBinding;
	private final AttentionThreatTracker threatTracker = new AttentionThreatTracker(
			new LiveThreatDetector(),
			CONFIG_MANAGER::getConfig
	);

	@Override
	public void onInitializeClient() {
		CONFIG_MANAGER.load();
		applyConfiguredRadii();
		HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, MARKER_LAYER, MARKER_RENDERER::render);
		ClientTickEvents.END_CLIENT_TICK.register(this::onEndClientTick);
		ClientCommandRegistrationCallback.EVENT.register(
				(dispatcher, registryAccess) -> AttentionClientCommands.register(dispatcher, MARKER_CONTROLLER, CONFIG_MANAGER)
		);
		openSettingsKeyBinding = KeyBindingHelper.registerKeyBinding(
				createOpenSettingsKeyBinding()
		);
	}

	private void onEndClientTick(MinecraftClient client) {
		applyConfiguredRadii();
		openSettingsIfRequested(client);
		threatTracker.tick(client, MARKER_CONTROLLER);
		MARKER_CONTROLLER.tick();
	}

	public static AttentionConfigManager getConfigManager() {
		return CONFIG_MANAGER;
	}

	public static Screen createSettingsScreen(Screen parent) {
		return new AttentionSettingsScreen(parent, CONFIG_MANAGER);
	}

	private static void applyConfiguredRadii() {
		var config = CONFIG_MANAGER.getConfig();
		MARKER_CONTROLLER.setConfiguredRadii((float) config.minIndicatorRadiusPixels(), (float) config.maxIndicatorRadiusPixels());
	}

	private void openSettingsIfRequested(MinecraftClient client) {
		if (client == null || openSettingsKeyBinding == null) {
			return;
		}

		while (openSettingsKeyBinding.wasPressed()) {
			if (!(client.currentScreen instanceof AttentionSettingsScreen)) {
				client.setScreen(createSettingsScreen(client.currentScreen));
			}
		}
	}

	private static KeyBinding createOpenSettingsKeyBinding() {
		try {
			Class<?> categoryClass = Class.forName("net.minecraft.client.option.KeyBinding$Category");
			Object category = categoryClass.getMethod("create", Identifier.class).invoke(null, Identifier.of("attention", "settings"));
			return KeyBinding.class
					.getConstructor(String.class, InputUtil.Type.class, int.class, categoryClass)
					.newInstance("key.attention.open_settings", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, category);
		} catch (ClassNotFoundException exception) {
			return createLegacyOpenSettingsKeyBinding();
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException("Failed to create settings key binding", exception);
		}
	}

	private static KeyBinding createLegacyOpenSettingsKeyBinding() {
		try {
			return KeyBinding.class
					.getConstructor(String.class, InputUtil.Type.class, int.class, String.class)
					.newInstance("key.attention.open_settings", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "key.categories.attention");
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException("Failed to create legacy settings key binding", exception);
		}
	}
}
