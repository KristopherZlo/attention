package dev.creas.attention.client.hud;

public final class AttentionMarkerController {
	private static final float ROTATION_LERP = 0.24F;
	private static final float ALPHA_LERP = 0.22F;
	private static final float RADIUS_LERP = 0.28F;
	private static final float DEFAULT_RADIUS_PX = 60.0F;
	private static final float REVEAL_SLIDE_DISTANCE_PX = 18.0F;
	private static final float PULSE_SLIDE_DISTANCE_PX = 8.0F;
	private static final float MIN_RADIUS_PX = 24.0F;

	private final AttentionMarkerState state = new AttentionMarkerState();
	private boolean demoMode;
	private float configuredRadiusPx = DEFAULT_RADIUS_PX;

	public AttentionMarkerController() {
		float hiddenRadius = hiddenRadiusFor(configuredRadiusPx);
		state.setDisplayRadiusPx(hiddenRadius);
		state.setTargetRadiusPx(hiddenRadius);
	}

	public AttentionMarkerState getState() {
		return state;
	}

	public boolean isDemoModeEnabled() {
		return demoMode;
	}

	public void enableDemoMode() {
		demoMode = true;
		setVisible(true);
	}

	public void disableDemoMode() {
		demoMode = false;
		setVisible(false);
	}

	public void setDemoAngle(float angleDegrees) {
		enableDemoMode();
		state.setTargetAngleDeg(normalizeRelativeAngle(angleDegrees));
	}

	public void setConfiguredRadius(float radiusPx) {
		configuredRadiusPx = Math.max(MIN_RADIUS_PX, radiusPx);

		if (!state.isVisible() && state.getAlpha() < 0.02F) {
			float hiddenRadius = hiddenRadiusFor(configuredRadiusPx);
			state.setDisplayRadiusPx(hiddenRadius);
			state.setTargetRadiusPx(hiddenRadius);
		}
	}

	public void setVisible(boolean visible) {
		if (visible && !state.isVisible()) {
			state.setDisplayRadiusPx(Math.max(state.getDisplayRadiusPx(), hiddenRadiusFor(configuredRadiusPx)));
		}

		state.setVisible(visible);
	}

	public void setTargetAngle(float angleDegrees) {
		state.setTargetAngleDeg(normalizeRelativeAngle(angleDegrees));
	}

	public void triggerPulse() {
		setVisible(true);
		state.setDisplayRadiusPx(Math.max(state.getDisplayRadiusPx(), configuredRadiusPx + PULSE_SLIDE_DISTANCE_PX));
	}

	public void tick() {
		float targetAlpha = state.isVisible() ? 1.0F : 0.0F;
		float targetRadius = state.isVisible() ? configuredRadiusPx : hiddenRadiusFor(configuredRadiusPx);

		state.setDisplayAngleDeg(approachAngle(state.getDisplayAngleDeg(), state.getTargetAngleDeg(), ROTATION_LERP));
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

	private static float approachAngle(float current, float target, float factor) {
		float delta = normalizeRelativeAngle(target - current);
		return normalizeRelativeAngle(current + delta * factor);
	}

	private static float lerp(float current, float target, float factor) {
		return current + (target - current) * factor;
	}

	private static float hiddenRadiusFor(float radiusPx) {
		return radiusPx + REVEAL_SLIDE_DISTANCE_PX;
	}
}
