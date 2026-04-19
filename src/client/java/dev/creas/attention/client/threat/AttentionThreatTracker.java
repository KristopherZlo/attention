package dev.creas.attention.client.threat;

import dev.creas.attention.client.config.AttentionConfig;
import dev.creas.attention.client.hud.AttentionMarkerController;
import dev.creas.attention.marker.MarkerSegmentMath;
import dev.creas.attention.threat.ThreatKind;
import dev.creas.attention.threat.ThreatMath;
import dev.creas.attention.threat.ThreatSelection;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.GameMode;

import java.util.Optional;
import java.util.function.Supplier;

public final class AttentionThreatTracker {
	private static final int SCAN_INTERVAL_TICKS = 4;
	private static final int REACTION_DELAY_TICKS = 8;
	private static final int TRACKING_GRACE_TICKS = 40;
	private static final long APPEARANCE_SOUND_COOLDOWN_TICKS = 20L * 5L;
	private static final double DIRECTIONAL_SOUND_DISTANCE_BLOCKS = 4.0D;

	private final LiveThreatDetector detector;
	private final Supplier<AttentionConfig> configSupplier;
	private int scanCooldown;
	private int trackedThreatGraceTicks;
	private long tickCounter;
	private long pendingThreatSinceTick;
	private long lastSoundTick = Long.MIN_VALUE / 4;
	private boolean indicatorDisappearedSinceLastSound = true;
	private TrackedThreat trackedThreat;
	private TrackedThreat pendingThreat;

	public AttentionThreatTracker(LiveThreatDetector detector, Supplier<AttentionConfig> configSupplier) {
		this.detector = detector;
		this.configSupplier = configSupplier;
	}

	public void tick(MinecraftClient client, AttentionMarkerController markerController) {
		tickCounter++;
		AttentionConfig config = configSupplier.get();

		if (markerController.hasFadedOut()) {
			indicatorDisappearedSinceLastSound = true;
		}

		if (client.isPaused()) {
			return;
		}

		if (markerController.isDemoModeEnabled()) {
			clear();
			return;
		}

		if (!canReact(client, config)) {
			markerController.resetThreatDistance();
			markerController.setVisible(false);
			clear();
			return;
		}

		if (trackedThreat != null) {
			if (updateTrackedThreat(client, markerController, config, true)) {
				trackedThreatGraceTicks = 0;
			} else if (trackedThreatGraceTicks < TRACKING_GRACE_TICKS && updateTrackedThreat(client, markerController, config, false)) {
				trackedThreatGraceTicks++;
				return;
			} else {
				markerController.resetThreatDistance();
				markerController.setVisible(false);
				clearTrackedThreat();
			}
		}

		if (scanCooldown > 0) {
			scanCooldown--;
			return;
		}

		Optional<ThreatSelection> selection = detector.detect(client, config);
		scanCooldown = SCAN_INTERVAL_TICKS;

		if (selection.isEmpty()) {
			if (trackedThreat != null && trackedThreatGraceTicks < TRACKING_GRACE_TICKS && updateTrackedThreat(client, markerController, config, false)) {
				trackedThreatGraceTicks++;
				return;
			}

			pendingThreat = null;
			markerController.resetThreatDistance();
			markerController.setVisible(false);
			clearTrackedThreat();
			return;
		}

		applySelection(client, markerController, config, selection.get());
	}

	private void applySelection(MinecraftClient client, AttentionMarkerController markerController, AttentionConfig config, ThreatSelection selection) {
		double detectionRadiusBlocks = config.detectionRadiusBlocks();
		TrackedThreat selectedThreat = new TrackedThreat(
				selection.snapshot().entityId(),
				selection.snapshot().kind()
		);

		if (trackedThreat != null) {
			pendingThreat = null;
			trackedThreat = selectedThreat;
			trackedThreatGraceTicks = 0;
			markerController.setThreatDistance(selection.snapshot().distanceSq(), detectionRadiusBlocks);
			markerController.setTargetAngle(selection.markerAngleDeg());
			markerController.setVisible(true);
			return;
		}

		if (pendingThreat == null || !pendingThreat.sameEntity(selectedThreat)) {
			pendingThreat = selectedThreat;
			pendingThreatSinceTick = tickCounter;
			return;
		}

		if (tickCounter - pendingThreatSinceTick < REACTION_DELAY_TICKS) {
			return;
		}

		pendingThreat = null;
		trackedThreat = selectedThreat;
		trackedThreatGraceTicks = 0;
		markerController.setThreatDistance(selection.snapshot().distanceSq(), detectionRadiusBlocks);
		markerController.setTargetAngle(selection.markerAngleDeg());
		markerController.triggerPulse();
		playAppearanceSound(client, selection.markerAngleDeg());
	}

	private boolean updateTrackedThreat(MinecraftClient client, AttentionMarkerController markerController, AttentionConfig config, boolean requireRelevant) {
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
		double distanceSq = entity.squaredDistanceTo(client.player);
		double detectionRadiusBlocks = config.detectionRadiusBlocks();

		boolean trackable = requireRelevant
				? detector.isStillRelevant(client, config, trackedThreat.kind(), entity, relativeYaw)
				: detector.isStillTrackable(client, config, trackedThreat.kind(), entity, relativeYaw);

		if (!trackable) {
			return false;
		}

		markerController.setThreatDistance(distanceSq, detectionRadiusBlocks);
		markerController.setTargetAngle(ThreatMath.markerAngleDegrees(relativeYaw));
		markerController.setVisible(true);
		return true;
	}

	private boolean canReact(MinecraftClient client, AttentionConfig config) {
		if (client.player == null || client.world == null || client.player.isSpectator()) {
			return false;
		}

		return !config.reactOnlyInSurvival()
				|| (client.interactionManager != null && client.interactionManager.getCurrentGameMode() == GameMode.SURVIVAL);
	}

	private void clear() {
		pendingThreat = null;
		clearTrackedThreat();
	}

	private void clearTrackedThreat() {
		trackedThreat = null;
		trackedThreatGraceTicks = 0;
		scanCooldown = 0;
	}

	private void playAppearanceSound(MinecraftClient client, float markerAngleDeg) {
		if (client.player == null
				|| client.world == null
				|| !indicatorDisappearedSinceLastSound
				|| tickCounter - lastSoundTick < APPEARANCE_SOUND_COOLDOWN_TICKS) {
			return;
		}

		lastSoundTick = tickCounter;
		indicatorDisappearedSinceLastSound = false;

		float snappedMarkerAngleDeg = MarkerSegmentMath.snappedAngleDegrees(markerAngleDeg);
		double worldYawRad = Math.toRadians(client.player.getYaw() + snappedMarkerAngleDeg);
		double soundX = client.player.getX() - (Math.sin(worldYawRad) * DIRECTIONAL_SOUND_DISTANCE_BLOCKS);
		double soundZ = client.player.getZ() + (Math.cos(worldYawRad) * DIRECTIONAL_SOUND_DISTANCE_BLOCKS);
		client.world.playSoundClient(
				soundX,
				client.player.getEyeY(),
				soundZ,
				SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE,
				SoundCategory.PLAYERS,
				0.85F,
				1.0F,
				false
		);
	}

	private record TrackedThreat(int entityId, ThreatKind kind) {
		private boolean sameEntity(TrackedThreat other) {
			return other != null && entityId == other.entityId;
		}
	}
}
