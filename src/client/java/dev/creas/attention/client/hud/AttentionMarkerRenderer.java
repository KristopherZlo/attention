package dev.creas.attention.client.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import org.joml.Matrix3x2fStack;

public final class AttentionMarkerRenderer {
	private static final int ARC_HALF_ANGLE = 45;
	private static final int ARC_STEP_DEGREES = 6;
	private static final int INNER_RADIUS = 22;
	private static final int OUTER_RADIUS = 34;
	private static final int SEGMENT_HALF_THICKNESS = 2;
	private static final int SHADOW_COLOR = 0x65000000;
	private static final int HOT_COLOR = 0xEFFF4438;
	private static final int WARM_COLOR = 0xDFFFAC2F;

	private final AttentionMarkerState state;

	public AttentionMarkerRenderer(AttentionMarkerState state) {
		this.state = state;
	}

	public void render(DrawContext context, RenderTickCounter tickCounter) {
		MinecraftClient client = MinecraftClient.getInstance();

		if (client.player == null || client.options.hudHidden || state.getAlpha() < 0.02F) {
			return;
		}

		int centerX = client.getWindow().getScaledWidth() / 2;
		int centerY = client.getWindow().getScaledHeight() / 2;
		float pulseScale = state.getScale() + (state.getPulseTime() * 0.06F);
		float screenCenterAngle = state.getDisplayAngleDeg() - 90.0F;

		drawArc(context, centerX, centerY, screenCenterAngle, pulseScale, colorWithAlpha(SHADOW_COLOR, state.getAlpha() * 0.75F), INNER_RADIUS - 1, OUTER_RADIUS + 1, 0);
		drawArc(context, centerX, centerY, screenCenterAngle, pulseScale, colorWithAlpha(WARM_COLOR, state.getAlpha()), INNER_RADIUS, OUTER_RADIUS, 0);
		drawArc(context, centerX, centerY, screenCenterAngle, pulseScale, colorWithAlpha(HOT_COLOR, state.getAlpha()), INNER_RADIUS + 2, OUTER_RADIUS - 2, 10);
	}

	private static void drawArc(DrawContext context, int centerX, int centerY, float centerAngle, float scale, int color, int innerRadius, int outerRadius, int insetDegrees) {
		int start = -ARC_HALF_ANGLE + insetDegrees;
		int end = ARC_HALF_ANGLE - insetDegrees;

		for (int angle = start; angle <= end; angle += ARC_STEP_DEGREES) {
			drawSegment(context, centerX, centerY, centerAngle + angle, scale, color, innerRadius, outerRadius);
		}
	}

	private static void drawSegment(DrawContext context, int centerX, int centerY, float angleDegrees, float scale, int color, int innerRadius, int outerRadius) {
		Matrix3x2fStack matrices = context.getMatrices();
		matrices.pushMatrix();
		matrices.translate(centerX, centerY);
		matrices.scale(scale, scale);
		matrices.rotate((float) Math.toRadians(angleDegrees));
		context.fill(innerRadius, -SEGMENT_HALF_THICKNESS, outerRadius, SEGMENT_HALF_THICKNESS, color);
		matrices.popMatrix();
	}

	private static int colorWithAlpha(int color, float alphaMultiplier) {
		int alpha = Math.min(255, Math.max(0, Math.round(((color >>> 24) & 0xFF) * alphaMultiplier)));
		return (alpha << 24) | (color & 0x00FFFFFF);
	}
}

