package dev.creas.attention.marker;

public final class MarkerSegmentMath {
	public static final int SEGMENT_COUNT = 16;
	public static final float SEGMENT_STEP_DEGREES = 360.0F / SEGMENT_COUNT;
	private static final float HALF_EDGE_LENGTH_FACTOR = (float) Math.tan(Math.PI / SEGMENT_COUNT);

	private MarkerSegmentMath() {
	}

	public static float segmentHalfLength(float radiusPx) {
		return radiusPx * HALF_EDGE_LENGTH_FACTOR;
	}

	public static SegmentBlend blendForAngle(float markerAngleDeg) {
		float wrappedAngle = normalizePositive(markerAngleDeg);
		float sector = wrappedAngle / SEGMENT_STEP_DEGREES;
		int lowerIndex = ((int) Math.floor(sector)) % SEGMENT_COUNT;
		int upperIndex = (lowerIndex + 1) % SEGMENT_COUNT;
		float fraction = sector - lowerIndex;
		float secondaryAlpha = (fraction <= 0.5F ? fraction : 1.0F - fraction) * 2.0F;

		if (fraction <= 0.5F) {
			return new SegmentBlend(lowerIndex, upperIndex, secondaryAlpha);
		}

		return new SegmentBlend(upperIndex, lowerIndex, secondaryAlpha);
	}

	public static int nearestSegmentIndex(float markerAngleDeg) {
		float wrappedAngle = normalizePositive(markerAngleDeg);
		return ((int) Math.floor((wrappedAngle + (SEGMENT_STEP_DEGREES * 0.5F)) / SEGMENT_STEP_DEGREES)) % SEGMENT_COUNT;
	}

	public static float snappedAngleDegrees(float markerAngleDeg) {
		return normalizeSigned(nearestSegmentIndex(markerAngleDeg) * SEGMENT_STEP_DEGREES);
	}

	public static float normalizePositive(float angleDeg) {
		float wrapped = angleDeg % 360.0F;
		return wrapped < 0.0F ? wrapped + 360.0F : wrapped;
	}

	public static float normalizeSigned(float angleDeg) {
		float wrapped = normalizePositive(angleDeg);
		return wrapped > 180.0F ? wrapped - 360.0F : wrapped;
	}

	public record SegmentBlend(int primaryIndex, int secondaryIndex, float secondaryAlpha) {
	}
}
