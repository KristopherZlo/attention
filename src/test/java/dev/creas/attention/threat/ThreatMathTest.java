package dev.creas.attention.threat;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThreatMathTest {
	@Test
	void relativeYawMapsWorldDirectionsToHudAngles() {
		assertEquals(0.0F, ThreatMath.relativeYawDegrees(0.0F, 0.0D, 0.0D, 0.0D, 5.0D), 0.001F);
		assertEquals(90.0F, ThreatMath.relativeYawDegrees(0.0F, 0.0D, 0.0D, 5.0D, 0.0D), 0.001F);
		assertEquals(-90.0F, ThreatMath.relativeYawDegrees(0.0F, 0.0D, 0.0D, -5.0D, 0.0D), 0.001F);
		assertEquals(180.0F, ThreatMath.relativeYawDegrees(0.0F, 0.0D, 0.0D, 0.0D, -5.0D), 0.001F);
	}

	@Test
	void markerAnglePreservesWrappedRelativeYaw() {
		assertEquals(180.0F, ThreatMath.markerAngleDegrees(-180.0F), 0.001F);
		assertEquals(90.0F, ThreatMath.markerAngleDegrees(270.0F), 0.001F);
	}

	@Test
	void offscreenCheckUsesHalfFovPlusMargin() {
		assertFalse(ThreatMath.isOutsideView(40.0F, 90.0F, 8.0F));
		assertTrue(ThreatMath.isOutsideView(60.0F, 90.0F, 8.0F));
		assertFalse(ThreatMath.isOutsideView(-52.0F, 90.0F, 8.0F));
		assertTrue(ThreatMath.isOutsideView(-54.0F, 90.0F, 8.0F));
	}

	@Test
	void towardPlayerRequiresDirectionAndSpeed() {
		assertTrue(ThreatMath.isMovingTowardPlayer(0.0D, 0.0D, 0.0D, 0.3D, 0.0D, 5.0D, 0.6D));
		assertFalse(ThreatMath.isMovingTowardPlayer(0.0D, 0.0D, 0.3D, 0.0D, 0.0D, 5.0D, 0.6D));
		assertFalse(ThreatMath.isMovingTowardPlayer(0.0D, 0.0D, 0.0D, 0.01D, 0.0D, 5.0D, 0.6D));
	}

	@Test
	void facingPointUsesEntityYawTowardTarget() {
		assertTrue(ThreatMath.isFacingPoint(0.0F, 0.0D, 0.0D, 0.0D, 5.0D, 35.0F));
		assertTrue(ThreatMath.isFacingPoint(-90.0F, 0.0D, 0.0D, 5.0D, 0.0D, 35.0F));
		assertFalse(ThreatMath.isFacingPoint(90.0F, 0.0D, 0.0D, 5.0D, 0.0D, 35.0F));
	}

	@Test
	void primaryThreatPrefersNearestThreatRegardlessOfKind() {
		Optional<ThreatSelection> selection = ThreatMath.selectPrimaryThreat(Stream.of(
				new ThreatSnapshot(4, ThreatKind.HOSTILE_APPROACHING, 4.0D, 180.0F),
				new ThreatSnapshot(3, ThreatKind.OFFSCREEN_PLAYER, 1.0D, -90.0F),
				new ThreatSnapshot(2, ThreatKind.HOSTILE_TARGETING, 25.0D, 30.0F)
		));

		assertTrue(selection.isPresent());
		assertEquals(ThreatKind.OFFSCREEN_PLAYER, selection.get().snapshot().kind());
		assertEquals(90.0F, selection.get().markerAngleDeg(), 0.001F);
	}

	@Test
	void primaryThreatBreaksTiesByNearestDistance() {
		Optional<ThreatSelection> selection = ThreatMath.selectPrimaryThreat(Stream.of(
				new ThreatSnapshot(10, ThreatKind.OFFSCREEN_PLAYER, 16.0D, -90.0F),
				new ThreatSnapshot(11, ThreatKind.OFFSCREEN_PLAYER, 4.0D, 90.0F)
		));

		assertTrue(selection.isPresent());
		assertEquals(11, selection.get().snapshot().entityId());
		assertEquals(-90.0F, selection.get().markerAngleDeg(), 0.001F);
	}
}
