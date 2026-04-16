package dev.creas.attention.marker;

public final class MarkerRadiusMath {
	private MarkerRadiusMath() {
	}

	public static float resolveDistanceFactor(double distanceSq, double detectionRadiusBlocks) {
		if (detectionRadiusBlocks <= 0.0D) {
			return 1.0F;
		}

		double distanceBlocks = Math.sqrt(Math.max(0.0D, distanceSq));
		float normalizedDistance = clamp01((float) (distanceBlocks / detectionRadiusBlocks));
		return normalizedDistance * normalizedDistance;
	}

	public static float visibleRadius(float minRadiusPx, float maxRadiusPx, float distanceFactor) {
		float clampedMin = Math.min(minRadiusPx, maxRadiusPx);
		float clampedMax = Math.max(minRadiusPx, maxRadiusPx);
		return lerp(clampedMin, clampedMax, clamp01(distanceFactor));
	}

	private static float clamp01(float value) {
		return Math.max(0.0F, Math.min(1.0F, value));
	}

	private static float lerp(float start, float end, float delta) {
		return start + ((end - start) * delta);
	}
}
