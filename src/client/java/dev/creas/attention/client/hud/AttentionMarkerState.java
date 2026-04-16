package dev.creas.attention.client.hud;

public final class AttentionMarkerState {
	private boolean visible;
	private float previousDisplayAngleDeg = 180.0F;
	private float displayAngleDeg = 180.0F;
	private float targetAngleDeg = 180.0F;
	private float previousAlpha;
	private float alpha;
	private float previousDisplayRadiusPx = 76.0F;
	private float displayRadiusPx = 76.0F;
	private float targetRadiusPx = 76.0F;

	public void capturePreviousFrame() {
		previousDisplayAngleDeg = displayAngleDeg;
		previousAlpha = alpha;
		previousDisplayRadiusPx = displayRadiusPx;
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public float getPreviousDisplayAngleDeg() {
		return previousDisplayAngleDeg;
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

	public float getPreviousAlpha() {
		return previousAlpha;
	}

	public float getAlpha() {
		return alpha;
	}

	public void setAlpha(float alpha) {
		this.alpha = alpha;
	}

	public float getPreviousDisplayRadiusPx() {
		return previousDisplayRadiusPx;
	}

	public float getDisplayRadiusPx() {
		return displayRadiusPx;
	}

	public void setDisplayRadiusPx(float displayRadiusPx) {
		this.displayRadiusPx = displayRadiusPx;
	}

	public float getTargetRadiusPx() {
		return targetRadiusPx;
	}

	public void setTargetRadiusPx(float targetRadiusPx) {
		this.targetRadiusPx = targetRadiusPx;
	}
}
