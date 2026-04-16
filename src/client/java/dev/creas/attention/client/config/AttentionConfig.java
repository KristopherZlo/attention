package dev.creas.attention.client.config;

public record AttentionConfig(double detectionRadiusBlocks, double indicatorRadiusPixels) {
	private static final double DEFAULT_DETECTION_RADIUS = 24.0D;
	private static final double DEFAULT_INDICATOR_RADIUS = 60.0D;
	private static final double MIN_INDICATOR_RADIUS = 24.0D;
	private static final double MAX_INDICATOR_RADIUS = 120.0D;

	public static AttentionConfig defaults() {
		return new AttentionConfig(DEFAULT_DETECTION_RADIUS, DEFAULT_INDICATOR_RADIUS);
	}

	public AttentionConfig sanitize() {
		double sanitizedDetectionRadius = detectionRadiusBlocks > 0.0D ? detectionRadiusBlocks : DEFAULT_DETECTION_RADIUS;
		double sanitizedIndicatorRadius = clamp(indicatorRadiusPixels, MIN_INDICATOR_RADIUS, MAX_INDICATOR_RADIUS);

		if (sanitizedDetectionRadius == detectionRadiusBlocks && sanitizedIndicatorRadius == indicatorRadiusPixels) {
			return this;
		}

		return new AttentionConfig(sanitizedDetectionRadius, sanitizedIndicatorRadius);
	}

	public AttentionConfig withIndicatorRadiusPixels(double indicatorRadiusPixels) {
		return new AttentionConfig(detectionRadiusBlocks, indicatorRadiusPixels).sanitize();
	}

	public static double minIndicatorRadiusPixels() {
		return MIN_INDICATOR_RADIUS;
	}

	public static double maxIndicatorRadiusPixels() {
		return MAX_INDICATOR_RADIUS;
	}

	private static double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}
}
