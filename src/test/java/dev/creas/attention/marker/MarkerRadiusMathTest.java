package dev.creas.attention.marker;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarkerRadiusMathTest {
	@Test
	void farThreatUsesMaximumRadius() {
		float factor = MarkerRadiusMath.resolveDistanceFactor(576.0D, 24.0D);

		assertEquals(1.0F, factor, 0.001F);
		assertEquals(30.0F, MarkerRadiusMath.visibleRadius(12.0F, 30.0F, factor), 0.001F);
	}

	@Test
	void closeThreatUsesMinimumRadiusButNeverZero() {
		float factor = MarkerRadiusMath.resolveDistanceFactor(0.0D, 24.0D);

		assertEquals(0.0F, factor, 0.001F);
		assertEquals(12.0F, MarkerRadiusMath.visibleRadius(12.0F, 30.0F, factor), 0.001F);
	}

	@Test
	void midRangeThreatUsesEasedRadiusCurve() {
		float factor = MarkerRadiusMath.resolveDistanceFactor(144.0D, 24.0D);

		assertEquals(0.25F, factor, 0.001F);
		assertEquals(16.5F, MarkerRadiusMath.visibleRadius(12.0F, 30.0F, factor), 0.001F);
	}
}
