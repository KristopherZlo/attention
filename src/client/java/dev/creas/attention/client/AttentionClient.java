package dev.creas.attention.client;

import dev.creas.attention.client.command.AttentionClientCommands;
import dev.creas.attention.client.hud.AttentionMarkerController;
import dev.creas.attention.client.hud.AttentionMarkerRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.util.Identifier;

public final class AttentionClient implements ClientModInitializer {
	private static final Identifier MARKER_LAYER = Identifier.of("attention", "crosshair_marker");

	private final AttentionMarkerController markerController = new AttentionMarkerController();
	private final AttentionMarkerRenderer markerRenderer = new AttentionMarkerRenderer(markerController.getState());

	@Override
	public void onInitializeClient() {
		HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, MARKER_LAYER, markerRenderer::render);
		ClientTickEvents.END_CLIENT_TICK.register(client -> markerController.tick());
		ClientCommandRegistrationCallback.EVENT.register(
				(dispatcher, registryAccess) -> AttentionClientCommands.register(dispatcher, markerController)
		);
	}
}

