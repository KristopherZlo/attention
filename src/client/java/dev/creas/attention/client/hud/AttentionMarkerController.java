package dev.creas.attention.client.hud;

import dev.creas.attention.marker.MarkerRadiusMath;

public final class AttentionMarkerController {
	private static final float ALPHA_LERP = 0.12F;
	private static final float ANGLE_LERP = 0.22F;
	private static final float RADIUS_LERP = 0.14F;
	private static final float BREATH_BASE_ALPHA = 0.94F;
	private static final float BREATH_ALPHA_AMPLITUDE = 0.04F;
	private static final float BREATH_SPEED = 0.115F;
	private static final float DEFAULT_MIN_RADIUS_PX = 10.0F;
	private static final float DEFAULT_MAX_RADIUS_PX = 16.0F;
	private static final float MIN_REVEAL_SLIDE_DISTANCE_PX = 8.0F;
	private static final float REVEAL_SLIDE_FACTOR = 0.42F;
	private static final float MIN_RADIUS_PX = 8.0F;
	private static final float HIDDEN_ALPHA_THRESHOLD = 0.02F;

	private final AttentionMarkerState state = new AttentionMarkerState();
	private boolean demoMode;
	private long tickCounter;
	private float configuredMinRadiusPx = DEFAULT_MIN_RADIUS_PX;
	private float configuredMaxRadiusPx = DEFAULT_MAX_RADIUS_PX;
	private float threatDistanceFactor = 1.0F;

	public AttentionMarkerController() {
		float hiddenRadius = hiddenRadiusFor(configuredMaxRadiusPx);
		state.setDisplayRadiusPx(hiddenRadius);
		state.setTargetRadiusPx(hiddenRadius);
	}

	public AttentionMarkerState getState() {
		return state;
	}

	public boolean hasFadedOut() {
		return !state.isVisible() && state.getAlpha() < HIDDEN_ALPHA_THRESHOLD;
	}

	public boolean isDemoModeEnabled() {
		return demoMode;
	}

	public void enableDemoMode() {
		demoMode = true;
		resetThreatDistance();
		setVisible(true);
	}

	public void disableDemoMode() {
		demoMode = false;
		setVisible(false);
	}

	public void setDemoAngle(float angleDegrees) {
		enableDemoMode();
		setTargetAngle(angleDegrees);
	}

	public void setConfiguredRadii(float minRadiusPx, float maxRadiusPx) {
		configuredMaxRadiusPx = Math.max(MIN_RADIUS_PX, maxRadiusPx);
		configuredMinRadiusPx = Math.min(Math.max(8.0F, minRadiusPx), configuredMaxRadiusPx);

		if (!state.isVisible() && state.getAlpha() < HIDDEN_ALPHA_THRESHOLD) {
			float hiddenRadius = hiddenRadiusFor(configuredMaxRadiusPx);
			state.setDisplayRadiusPx(hiddenRadius);
			state.setTargetRadiusPx(hiddenRadius);
		}
	}

	public void resetThreatDistance() {
		threatDistanceFactor = 1.0F;
	}

	public void setThreatDistance(double distanceSq, double detectionRadiusBlocks) {
		threatDistanceFactor = MarkerRadiusMath.resolveDistanceFactor(distanceSq, detectionRadiusBlocks);

		if (!state.isVisible() && state.getAlpha() < HIDDEN_ALPHA_THRESHOLD) {
			float hiddenRadius = hiddenRadiusFor(visibleRadiusForCurrentThreat());
			state.setDisplayRadiusPx(hiddenRadius);
			state.setTargetRadiusPx(hiddenRadius);
		}
	}

	public void setVisible(boolean visible) {
		if (visible && !state.isVisible()) {
			state.setDisplayRadiusPx(Math.max(state.getDisplayRadiusPx(), hiddenRadiusFor(visibleRadiusForCurrentThreat())));
		}

		state.setVisible(visible);
	}

	public void setTargetAngle(float angleDegrees) {
		state.setTargetAngleDeg(normalizeRelativeAngle(angleDegrees));
	}

	public void triggerPulse() {
		setVisible(true);
	}

	public void tick() {
		tickCounter++;
		state.capturePreviousFrame();
		float targetAlpha = state.isVisible() ? breathingAlpha() : 0.0F;
		float targetRadius = state.isVisible() ? visibleRadiusForCurrentThreat() : hiddenRadiusFor(configuredMaxRadiusPx);

		state.setDisplayAngleDeg(lerpAngle(state.getDisplayAngleDeg(), state.getTargetAngleDeg(), ANGLE_LERP));
		state.setAlpha(lerp(state.getAlpha(), targetAlpha, ALPHA_LERP));
		state.setTargetRadiusPx(targetRadius);
		state.setDisplayRadiusPx(lerp(state.getDisplayRadiusPx(), targetRadius, RADIUS_LERP));
	}

	public static float normalizeRelativeAngle(float angleDegrees) {
		float wrapped = angleDegrees % 360.0F;

		if (wrapped <= -180.0F) {
			wrapped += 360.0F;
		}

		if (wrapped > 180.0F) {
			wrapped -= 360.0F;
		}

		return wrapped;
	}

	private static float lerp(float current, float target, float factor) {
		return current + (target - current) * factor;
	}

	private static float lerpAngle(float current, float target, float factor) {
		float delta = normalizeRelativeAngle(target - current);
		return normalizeRelativeAngle(current + (delta * factor));
	}

	private static float hiddenRadiusFor(float radiusPx) {
		return radiusPx + Math.max(MIN_REVEAL_SLIDE_DISTANCE_PX, radiusPx * REVEAL_SLIDE_FACTOR);
	}

	private float breathingAlpha() {
		return BREATH_BASE_ALPHA + ((float) Math.sin(tickCounter * BREATH_SPEED) * BREATH_ALPHA_AMPLITUDE);
	}

	private float visibleRadiusForCurrentThreat() {
		return MarkerRadiusMath.visibleRadius(configuredMinRadiusPx, configuredMaxRadiusPx, threatDistanceFactor);
	}
}
