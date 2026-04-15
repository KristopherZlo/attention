package dev.creas.attention.client;

import dev.creas.attention.client.command.AttentionClientCommands;
import dev.creas.attention.client.hud.AttentionMarkerController;
import dev.creas.attention.client.hud.AttentionMarkerRenderer;
import dev.creas.attention.client.threat.AttentionThreatTracker;
import dev.creas.attention.client.threat.LiveThreatDetector;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

public final class AttentionClient implements ClientModInitializer {
	private static final Identifier MARKER_LAYER = Identifier.of("attention", "crosshair_marker");
	private static final double DEFAULT_DETECTION_RADIUS = 24.0D;

	private final AttentionMarkerController markerController = new AttentionMarkerController();
	private final AttentionMarkerRenderer markerRenderer = new AttentionMarkerRenderer(markerController.getState());
	private final AttentionThreatTracker threatTracker = new AttentionThreatTracker(new LiveThreatDetector(), () -> DEFAULT_DETECTION_RADIUS);

	@Override
	public void onInitializeClient() {
		HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, MARKER_LAYER, markerRenderer::render);
		ClientTickEvents.END_CLIENT_TICK.register(this::onEndClientTick);
		ClientCommandRegistrationCallback.EVENT.register(
				(dispatcher, registryAccess) -> AttentionClientCommands.register(dispatcher, markerController)
		);
	}

	private void onEndClientTick(MinecraftClient client) {
		threatTracker.tick(client, markerController);
		markerController.tick();
	}
}
