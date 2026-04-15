package dev.creas.attention.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2fStack;

public final class AttentionClient implements ClientModInitializer {
	private static final String MOD_ID = "attention";
	private static final Identifier MARKER_LAYER = Identifier.of(MOD_ID, "crosshair_marker");

	private static final int ARC_START_DEGREES = 45;
	private static final int ARC_END_DEGREES = 135;
	private static final int ARC_STEP_DEGREES = 6;
	private static final int INNER_RADIUS = 22;
	private static final int OUTER_RADIUS = 34;
	private static final int SEGMENT_HALF_THICKNESS = 2;
	private static final int SHADOW_COLOR = 0x65000000;
	private static final int HOT_COLOR = 0xEFFF4438;
	private static final int WARM_COLOR = 0xDFFFAC2F;

	@Override
	public void onInitializeClient() {
		HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, MARKER_LAYER, AttentionClient::renderMarker);
	}

	private static void renderMarker(DrawContext context, RenderTickCounter tickCounter) {
		MinecraftClient client = MinecraftClient.getInstance();

		if (client.player == null || client.options.hudHidden) {
			return;
		}

		int centerX = client.getWindow().getScaledWidth() / 2;
		int centerY = client.getWindow().getScaledHeight() / 2;

		drawArc(context, centerX, centerY, 0, SHADOW_COLOR, INNER_RADIUS - 1, OUTER_RADIUS + 1);
		drawArc(context, centerX, centerY, 0, WARM_COLOR, INNER_RADIUS, OUTER_RADIUS);
		drawArc(context, centerX, centerY, 10, HOT_COLOR, INNER_RADIUS + 2, OUTER_RADIUS - 2);
	}

	private static void drawArc(DrawContext context, int centerX, int centerY, int insetDegrees, int color, int innerRadius, int outerRadius) {
		int start = ARC_START_DEGREES + insetDegrees;
		int end = ARC_END_DEGREES - insetDegrees;

		for (int angle = start; angle <= end; angle += ARC_STEP_DEGREES) {
			drawSegment(context, centerX, centerY, angle, color, innerRadius, outerRadius);
		}
	}

	private static void drawSegment(DrawContext context, int centerX, int centerY, int angleDegrees, int color, int innerRadius, int outerRadius) {
		Matrix3x2fStack matrices = context.getMatrices();
		matrices.pushMatrix();
		matrices.translate(centerX, centerY);
		matrices.rotate((float) Math.toRadians(angleDegrees));
		context.fill(innerRadius, -SEGMENT_HALF_THICKNESS, outerRadius, SEGMENT_HALF_THICKNESS, color);
		matrices.popMatrix();
	}
}

