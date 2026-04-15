package dev.creas.attention.client.config;

public record AttentionConfig(double detectionRadiusBlocks) {
	private static final double DEFAULT_DETECTION_RADIUS = 24.0D;

	public static AttentionConfig defaults() {
		return new AttentionConfig(DEFAULT_DETECTION_RADIUS);
	}

	public AttentionConfig sanitize() {
		if (detectionRadiusBlocks <= 0.0D) {
			return defaults();
		}

		return this;
	}
}

