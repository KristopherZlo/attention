package dev.creas.attention.marker;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarkerSegmentMathTest {
	@Test
	void midpointBetweenFacetsShowsBothAtFullOpacity() {
		MarkerSegmentMath.SegmentBlend blend = MarkerSegmentMath.blendForAngle(MarkerSegmentMath.SEGMENT_STEP_DEGREES * 0.5F);

		assertEquals(0, blend.primaryIndex());
		assertEquals(1, blend.secondaryIndex());
		assertEquals(1.0F, blend.secondaryAlpha(), 0.001F);
	}

	@Test
	void secondaryFacetFadesLinearlyAsThreatMovesCloserToPrimaryFacet() {
		MarkerSegmentMath.SegmentBlend blend = MarkerSegmentMath.blendForAngle(MarkerSegmentMath.SEGMENT_STEP_DEGREES * 0.25F);

		assertEquals(0, blend.primaryIndex());
		assertEquals(1, blend.secondaryIndex());
		assertEquals(0.5F, blend.secondaryAlpha(), 0.001F);
	}

	@Test
	void blendWrapsAcrossLastAndFirstFacet() {
		MarkerSegmentMath.SegmentBlend blend = MarkerSegmentMath.blendForAngle(360.0F - (MarkerSegmentMath.SEGMENT_STEP_DEGREES * 0.25F));

		assertEquals(0, blend.primaryIndex());
		assertEquals(MarkerSegmentMath.SEGMENT_COUNT - 1, blend.secondaryIndex());
		assertEquals(0.5F, blend.secondaryAlpha(), 0.001F);
	}
}
