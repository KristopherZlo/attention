package dev.creas.attention.client.threat;

import dev.creas.attention.client.hud.AttentionMarkerController;
import dev.creas.attention.threat.ThreatKind;
import dev.creas.attention.threat.ThreatMath;
import dev.creas.attention.threat.ThreatSelection;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;

import java.util.Optional;
import java.util.function.DoubleSupplier;

public final class AttentionThreatTracker {
	private static final int SCAN_INTERVAL_TICKS = 4;

	private final LiveThreatDetector detector;
	private final DoubleSupplier detectionRadiusSupplier;
	private int scanCooldown;
	private TrackedThreat trackedThreat;

	public AttentionThreatTracker(LiveThreatDetector detector, DoubleSupplier detectionRadiusSupplier) {
		this.detector = detector;
		this.detectionRadiusSupplier = detectionRadiusSupplier;
	}

	public void tick(MinecraftClient client, AttentionMarkerController markerController) {
		if (markerController.isDemoModeEnabled()) {
			clear();
			return;
		}

		if (client.player == null || client.world == null || client.player.isSpectator()) {
			markerController.setVisible(false);
			clear();
			return;
		}

		if (trackedThreat != null && !updateTrackedThreat(client, markerController)) {
			markerController.setVisible(false);
			clear();
		}

		if (scanCooldown > 0) {
			scanCooldown--;
			return;
		}

		Optional<ThreatSelection> selection = detector.detect(client, detectionRadiusSupplier.getAsDouble());
		scanCooldown = SCAN_INTERVAL_TICKS;

		if (selection.isEmpty()) {
			markerController.setVisible(false);
			clear();
			return;
		}

		ThreatSelection threatSelection = selection.get();
		TrackedThreat newThreat = new TrackedThreat(
				threatSelection.snapshot().entityId(),
				threatSelection.snapshot().kind()
		);
		boolean changed = !newThreat.equals(trackedThreat);

		trackedThreat = newThreat;
		markerController.setTargetAngle(threatSelection.markerAngleDeg());

		if (changed) {
			markerController.triggerPulse();
		} else {
			markerController.setVisible(true);
		}
	}

	private boolean updateTrackedThreat(MinecraftClient client, AttentionMarkerController markerController) {
		Entity entity = client.world.getEntityById(trackedThreat.entityId());

		if (entity == null || !entity.isAlive()) {
			return false;
		}

		float relativeYaw = ThreatMath.relativeYawDegrees(
				client.player.getYaw(),
				client.player.getX(),
				client.player.getZ(),
				entity.getX(),
				entity.getZ()
		);

		if (!detector.isStillRelevant(client, trackedThreat.kind(), entity, relativeYaw, detectionRadiusSupplier.getAsDouble())) {
			return false;
		}

		markerController.setTargetAngle(relativeYaw);
		markerController.setVisible(true);
		return true;
	}

	private void clear() {
		trackedThreat = null;
		scanCooldown = 0;
	}

	private record TrackedThreat(int entityId, ThreatKind kind) {
	}
}

