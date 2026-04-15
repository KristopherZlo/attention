package dev.creas.attention.threat;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

public final class ThreatMath {
	private static final double MIN_APPROACH_SPEED_SQ = 0.0004D;

	private ThreatMath() {
	}

	public static float normalizeAngle(float angleDegrees) {
		float wrapped = angleDegrees % 360.0F;

		if (wrapped <= -180.0F) {
			wrapped += 360.0F;
		}

		if (wrapped > 180.0F) {
			wrapped -= 360.0F;
		}

		return wrapped;
	}

	public static float relativeYawDegrees(float playerYawDeg, double playerX, double playerZ, double threatX, double threatZ) {
		double deltaX = threatX - playerX;
		double deltaZ = threatZ - playerZ;
		double worldYaw = Math.toDegrees(Math.atan2(-deltaX, deltaZ));
		return normalizeAngle((float) (playerYawDeg - worldYaw));
	}

	public static float markerAngleDegrees(float relativeYawDeg) {
		return normalizeAngle(relativeYawDeg);
	}

	public static boolean isOutsideView(float relativeYawDeg, float horizontalFovDeg, float marginDeg) {
		return Math.abs(normalizeAngle(relativeYawDeg)) > ((horizontalFovDeg * 0.5F) + marginDeg);
	}

	public static boolean isMovingTowardPlayer(double entityX, double entityZ, double velocityX, double velocityZ, double playerX, double playerZ, double minDot) {
		double speedSq = (velocityX * velocityX) + (velocityZ * velocityZ);

		if (speedSq < MIN_APPROACH_SPEED_SQ) {
			return false;
		}

		double toPlayerX = playerX - entityX;
		double toPlayerZ = playerZ - entityZ;
		double toPlayerLengthSq = (toPlayerX * toPlayerX) + (toPlayerZ * toPlayerZ);

		if (toPlayerLengthSq < 1.0E-6D) {
			return false;
		}

		double dot = ((velocityX * toPlayerX) + (velocityZ * toPlayerZ))
				/ (Math.sqrt(speedSq) * Math.sqrt(toPlayerLengthSq));

		return dot >= minDot;
	}

	public static Optional<ThreatSelection> selectPrimaryThreat(Stream<ThreatSnapshot> threats) {
		return threats
				.min(Comparator
						.comparingInt((ThreatSnapshot threat) -> switch (threat.kind()) {
							case HOSTILE_TARGETING -> 0;
							case OFFSCREEN_PLAYER -> 1;
							case HOSTILE_APPROACHING -> 2;
						})
						.thenComparingDouble(ThreatSnapshot::distanceSq)
						.thenComparingInt(ThreatSnapshot::entityId))
				.map(threat -> new ThreatSelection(threat, markerAngleDegrees(threat.relativeYawDeg())));
	}
}

