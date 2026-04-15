package dev.creas.attention.client.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import dev.creas.attention.client.hud.AttentionMarkerController;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

public final class AttentionClientCommands {
	private AttentionClientCommands() {
	}

	public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, AttentionMarkerController markerController) {
		dispatcher.register(
				ClientCommandManager.literal("attention")
						.then(ClientCommandManager.literal("demo")
								.then(ClientCommandManager.literal("on")
										.executes(context -> {
											markerController.enableDemoMode();
											context.getSource().sendFeedback(Text.literal("Attention demo enabled."));
											return 1;
										}))
								.then(ClientCommandManager.literal("off")
										.executes(context -> {
											markerController.disableDemoMode();
											context.getSource().sendFeedback(Text.literal("Attention demo disabled."));
											return 1;
										}))
								.then(ClientCommandManager.literal("pulse")
										.executes(context -> {
											markerController.enableDemoMode();
											markerController.triggerPulse();
											context.getSource().sendFeedback(Text.literal("Attention demo pulse triggered."));
											return 1;
										}))
								.then(ClientCommandManager.literal("angle")
										.then(ClientCommandManager.argument("degrees", DoubleArgumentType.doubleArg(-360.0D, 360.0D))
												.executes(context -> {
													double degrees = DoubleArgumentType.getDouble(context, "degrees");
													markerController.enableDemoMode();
													markerController.setDemoAngle((float) degrees);
													context.getSource().sendFeedback(Text.literal("Attention demo angle set to " + degrees + " degrees."));
													return 1;
												}))))
		);
	}
}

