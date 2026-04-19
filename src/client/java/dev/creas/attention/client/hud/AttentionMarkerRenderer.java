package dev.creas.attention.client.hud;

import dev.creas.attention.marker.MarkerSegmentMath;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import org.joml.Matrix3x2fStack;

public final class AttentionMarkerRenderer {
	private static final float MIN_RENDER_ALPHA = 0.02F;
	private static final int MARKER_COLOR = 0xFFFFFFFF;
	private static final float SECONDARY_SEGMENT_SHARED_SHIFT_PX = 0.45F;

	private final AttentionMarkerState state;

	public AttentionMarkerRenderer(AttentionMarkerState state) {
		this.state = state;
	}

	public void render(DrawContext context, RenderTickCounter tickCounter) {
		MinecraftClient client = MinecraftClient.getInstance();
		float tickProgress = tickCounter.getTickProgress(true);
		float alpha = lerp(state.getPreviousAlpha(), state.getAlpha(), tickProgress);

		if (client.player == null || client.options.hudHidden || alpha < MIN_RENDER_ALPHA) {
			return;
		}

		int centerX = client.getWindow().getScaledWidth() / 2;
		int centerY = client.getWindow().getScaledHeight() / 2;
		float angleDeg = interpolateAngle(state.getPreviousDisplayAngleDeg(), state.getDisplayAngleDeg(), tickProgress);
		float radius = lerp(state.getPreviousDisplayRadiusPx(), state.getDisplayRadiusPx(), tickProgress);
		drawMarker(context, centerX, centerY, radius, angleDeg, alpha);
	}

	public static void drawMarker(DrawContext context, int centerX, int centerY, float radius, float angleDeg, float alpha) {
		float halfLength = MarkerSegmentMath.segmentHalfLength(radius);
		MarkerSegmentMath.SegmentBlend blend = MarkerSegmentMath.blendForAngle(angleDeg);
		float primaryAlpha = alpha;

		if (primaryAlpha > 0.0F) {
			drawSegment(context, centerX, centerY, radius, halfLength, halfLength, blend.primaryIndex(), primaryAlpha, 0.0F);
		}

		float secondaryAlpha = blend.secondaryAlpha() * alpha;
		if (secondaryAlpha > 0.0F) {
			float localShiftPx = blend.secondarySharesLowerBoundary()
					? -SECONDARY_SEGMENT_SHARED_SHIFT_PX
					: SECONDARY_SEGMENT_SHARED_SHIFT_PX;
			drawSegment(context, centerX, centerY, radius, halfLength, halfLength, blend.secondaryIndex(), secondaryAlpha, localShiftPx);
		}
	}

	private static void drawSegment(
			DrawContext context,
			int centerX,
			int centerY,
			float radius,
			float lowerSideLength,
			float upperSideLength,
			int segmentIndex,
			float alpha,
			float localShiftPx
	) {
		float markerAngleDeg = MarkerSegmentMath.normalizeSigned(segmentIndex * MarkerSegmentMath.SEGMENT_STEP_DEGREES);
		float screenAngleDeg = markerAngleDeg - 90.0F;
		double radialAngleRad = Math.toRadians(screenAngleDeg);
		float segmentCenterX = centerX + (float) (Math.cos(radialAngleRad) * radius);
		float segmentCenterY = centerY + (float) (Math.sin(radialAngleRad) * radius);
		float tangentAngleRad = (float) Math.toRadians(screenAngleDeg - 90.0F);
		Matrix3x2fStack matrices = context.getMatrices();

		matrices.pushMatrix();
		matrices.translate(segmentCenterX, segmentCenterY);
		matrices.rotate(tangentAngleRad);
		matrices.translate(localShiftPx, 0.0F);
		int left = Math.round(-upperSideLength);
		int right = Math.max(left + 1, Math.round(lowerSideLength));
		context.fill(
				left,
				-1,
				right,
				0,
				colorWithAlpha(MARKER_COLOR, alpha)
		);
		matrices.popMatrix();
	}

	private static float interpolateAngle(float previous, float current, float delta) {
		float angleDelta = AttentionMarkerController.normalizeRelativeAngle(current - previous);
		return AttentionMarkerController.normalizeRelativeAngle(previous + (angleDelta * delta));
	}

	private static float lerp(float previous, float current, float delta) {
		return previous + ((current - previous) * delta);
	}

	private static int colorWithAlpha(int color, float alphaMultiplier) {
		int alpha = Math.min(255, Math.max(0, Math.round(((color >>> 24) & 0xFF) * alphaMultiplier)));
		return (alpha << 24) | (color & 0x00FFFFFF);
	}
}
