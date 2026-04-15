package dev.creas.attention.client.hud;

public final class AttentionMarkerState {
	private boolean visible;
	private float displayAngleDeg = 180.0F;
	private float targetAngleDeg = 180.0F;
	private float alpha;
	private float scale = 0.92F;
	private float pulseTime;

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public float getDisplayAngleDeg() {
		return displayAngleDeg;
	}

	public void setDisplayAngleDeg(float displayAngleDeg) {
		this.displayAngleDeg = displayAngleDeg;
	}

	public float getTargetAngleDeg() {
		return targetAngleDeg;
	}

	public void setTargetAngleDeg(float targetAngleDeg) {
		this.targetAngleDeg = targetAngleDeg;
	}

	public float getAlpha() {
		return alpha;
	}

	public void setAlpha(float alpha) {
		this.alpha = alpha;
	}

	public float getScale() {
		return scale;
	}

	public void setScale(float scale) {
		this.scale = scale;
	}

	public float getPulseTime() {
		return pulseTime;
	}

	public void setPulseTime(float pulseTime) {
		this.pulseTime = pulseTime;
	}
}
