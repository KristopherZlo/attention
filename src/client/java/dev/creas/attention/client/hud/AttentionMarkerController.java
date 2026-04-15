package dev.creas.attention.client.hud;

public final class AttentionMarkerController {
	private static final float ROTATION_LERP = 0.24F;
	private static final float ALPHA_LERP = 0.22F;
	private static final float SCALE_LERP = 0.28F;
	private static final float HIDDEN_SCALE = 0.92F;
	private static final float VISIBLE_SCALE = 1.0F;
	private static final float SHOW_BOOST = 1.12F;
	private static final float PULSE_BOOST = 0.12F;
	private static final float PULSE_DECAY = 0.14F;

	private final AttentionMarkerState state = new AttentionMarkerState();
	private boolean demoMode;

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

	public void setVisible(boolean visible) {
		if (visible && !state.isVisible()) {
			state.setScale(SHOW_BOOST);
			state.setPulseTime(1.0F);
		}

		state.setVisible(visible);
	}

	public void setTargetAngle(float angleDegrees) {
		state.setTargetAngleDeg(normalizeRelativeAngle(angleDegrees));
	}

	public void triggerPulse() {
		setVisible(true);
		state.setPulseTime(1.0F);
		state.setScale(Math.max(state.getScale(), SHOW_BOOST));
	}

	public void tick() {
		float targetAlpha = state.isVisible() ? 1.0F : 0.0F;
		float baseScale = state.isVisible() ? VISIBLE_SCALE : HIDDEN_SCALE;

		state.setDisplayAngleDeg(approachAngle(state.getDisplayAngleDeg(), state.getTargetAngleDeg(), ROTATION_LERP));
		state.setAlpha(lerp(state.getAlpha(), targetAlpha, ALPHA_LERP));
		state.setScale(lerp(state.getScale(), baseScale, SCALE_LERP));
		state.setPulseTime(Math.max(0.0F, state.getPulseTime() - PULSE_DECAY));
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
}

