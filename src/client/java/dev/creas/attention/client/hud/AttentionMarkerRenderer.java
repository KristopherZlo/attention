package dev.creas.attention.client.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import org.joml.Matrix3x2fStack;

public final class AttentionMarkerRenderer {
	private static final int SEGMENT_COUNT = 16;
	private static final float SEGMENT_STEP_DEGREES = 360.0F / SEGMENT_COUNT;
	private static final float SEGMENT_THICKNESS_PX = 2.0F;
	private static final float SEGMENT_HALF_THICKNESS_PX = SEGMENT_THICKNESS_PX * 0.5F;
	private static final float MIN_RENDER_ALPHA = 0.02F;
	private static final float HALF_EDGE_LENGTH_FACTOR = (float) Math.tan(Math.PI / SEGMENT_COUNT);
	private static final int MARKER_COLOR = 0xFFFFFFFF;

	private final AttentionMarkerState state;

	public AttentionMarkerRenderer(AttentionMarkerState state) {
		this.state = state;
	}

	public void render(DrawContext context, RenderTickCounter tickCounter) {
		MinecraftClient client = MinecraftClient.getInstance();

		if (client.player == null || client.options.hudHidden || state.getAlpha() < MIN_RENDER_ALPHA) {
			return;
		}

		int centerX = client.getWindow().getScaledWidth() / 2;
		int centerY = client.getWindow().getScaledHeight() / 2;
		float radius = state.getDisplayRadiusPx();
		float halfLength = Math.max(5.0F, radius * HALF_EDGE_LENGTH_FACTOR);
		SegmentBlend blend = SegmentBlend.forAngle(state.getDisplayAngleDeg());

		drawSegment(context, centerX, centerY, radius, halfLength, blend.primaryIndex(), state.getAlpha());

		if (blend.secondaryAlpha() > 0.0F) {
			drawSegment(context, centerX, centerY, radius, halfLength, blend.secondaryIndex(), state.getAlpha() * blend.secondaryAlpha());
		}
	}

	private static void drawSegment(DrawContext context, int centerX, int centerY, float radius, float halfLength, int segmentIndex, float alpha) {
		float markerAngleDeg = normalizeSigned(segmentIndex * SEGMENT_STEP_DEGREES);
		float screenAngleDeg = markerAngleDeg - 90.0F;
		double radialAngleRad = Math.toRadians(screenAngleDeg);
		float segmentCenterX = centerX + (float) (Math.cos(radialAngleRad) * radius);
		float segmentCenterY = centerY + (float) (Math.sin(radialAngleRad) * radius);
		float tangentAngleRad = (float) Math.toRadians(screenAngleDeg - 90.0F);
		Matrix3x2fStack matrices = context.getMatrices();

		matrices.pushMatrix();
		matrices.translate(segmentCenterX, segmentCenterY);
		matrices.rotate(tangentAngleRad);
		context.fill(
				Math.round(-halfLength),
				Math.round(-SEGMENT_HALF_THICKNESS_PX),
				Math.round(halfLength),
				Math.round(SEGMENT_HALF_THICKNESS_PX),
				colorWithAlpha(MARKER_COLOR, alpha)
		);
		matrices.popMatrix();
	}

	private static int colorWithAlpha(int color, float alphaMultiplier) {
		int alpha = Math.min(255, Math.max(0, Math.round(((color >>> 24) & 0xFF) * alphaMultiplier)));
		return (alpha << 24) | (color & 0x00FFFFFF);
	}

	private static float normalizePositive(float angleDeg) {
		float wrapped = angleDeg % 360.0F;
		return wrapped < 0.0F ? wrapped + 360.0F : wrapped;
	}

	private static float normalizeSigned(float angleDeg) {
		float wrapped = normalizePositive(angleDeg);
		return wrapped > 180.0F ? wrapped - 360.0F : wrapped;
	}

	private record SegmentBlend(int primaryIndex, int secondaryIndex, float secondaryAlpha) {
		private static SegmentBlend forAngle(float markerAngleDeg) {
			float wrappedAngle = normalizePositive(markerAngleDeg);
			float sector = wrappedAngle / SEGMENT_STEP_DEGREES;
			int lowerIndex = ((int) Math.floor(sector)) % SEGMENT_COUNT;
			int upperIndex = (lowerIndex + 1) % SEGMENT_COUNT;
			float fraction = sector - lowerIndex;
			float midpointBlend = (fraction <= 0.5F ? fraction : 1.0F - fraction) * 2.0F;
			float secondaryAlpha = midpointBlend <= 0.0F ? 0.0F : (float) Math.sqrt(midpointBlend);

			if (fraction <= 0.5F) {
				return new SegmentBlend(lowerIndex, upperIndex, secondaryAlpha);
			}

			return new SegmentBlend(upperIndex, lowerIndex, secondaryAlpha);
		}
	}
}
