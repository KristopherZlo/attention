package dev.creas.attention.client.threat;

import dev.creas.attention.client.config.AttentionConfig;
import dev.creas.attention.threat.ThreatKind;
import dev.creas.attention.threat.ThreatMath;
import dev.creas.attention.threat.ThreatSelection;
import dev.creas.attention.threat.ThreatSnapshot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Optional;
import java.util.stream.Stream;

public final class LiveThreatDetector {
	private static final float FOV_MARGIN_DEGREES = 8.0F;
	private static final double APPROACH_DOT_THRESHOLD = 0.6D;

	public Optional<ThreatSelection> detect(MinecraftClient client, AttentionConfig config) {
		PlayerEntity localPlayer = client.player;

		if (localPlayer == null || client.world == null) {
			return Optional.empty();
		}

		double detectionRadiusBlocks = config.detectionRadiusBlocks();
		Stream<ThreatSnapshot> hostileThreats = Stream.empty();
		Stream<ThreatSnapshot> playerThreats = Stream.empty();

		if (config.reactToTargetingHostiles() || config.reactToApproachingHostiles()) {
			hostileThreats = client.world.getEntitiesByClass(
					HostileEntity.class,
					localPlayer.getBoundingBox().expand(detectionRadiusBlocks),
					HostileEntity::isAlive
			).stream().flatMap(hostile -> createHostileThreat(client, localPlayer, hostile, config).stream());
		}

		if (config.reactToPlayers()) {
			playerThreats = client.world.getPlayers().stream()
					.filter(otherPlayer -> otherPlayer.isAlive() && !otherPlayer.isSpectator() && otherPlayer != localPlayer)
					.filter(otherPlayer -> config.allowsPlayerName(otherPlayer.getGameProfile().name()))
					.flatMap(otherPlayer -> createPlayerThreat(client, localPlayer, otherPlayer, detectionRadiusBlocks).stream());
		}

		return ThreatMath.selectPrimaryThreat(Stream.concat(hostileThreats, playerThreats));
	}

	public boolean isStillRelevant(MinecraftClient client, AttentionConfig config, ThreatKind kind, Entity entity, float relativeYaw) {
		PlayerEntity localPlayer = client.player;
		double detectionRadiusBlocks = config.detectionRadiusBlocks();

		if (localPlayer == null || !config.reactsTo(kind) || entity.squaredDistanceTo(localPlayer) > (detectionRadiusBlocks * detectionRadiusBlocks)) {
			return false;
		}

		if (!ThreatMath.isOutsideView(relativeYaw, getHorizontalFov(client), FOV_MARGIN_DEGREES)) {
			return false;
		}

		return switch (kind) {
			case HOSTILE_TARGETING -> entity instanceof HostileEntity hostile && hostile.getTarget() == localPlayer;
			case HOSTILE_APPROACHING -> entity instanceof HostileEntity hostile && isHostileApproaching(localPlayer, hostile);
			case OFFSCREEN_PLAYER -> entity instanceof PlayerEntity otherPlayer
					&& otherPlayer.isAlive()
					&& !otherPlayer.isSpectator()
					&& config.allowsPlayerName(otherPlayer.getGameProfile().name());
		};
	}

	public boolean isStillTrackable(MinecraftClient client, AttentionConfig config, ThreatKind kind, Entity entity, float relativeYaw) {
		PlayerEntity localPlayer = client.player;
		double detectionRadiusBlocks = config.detectionRadiusBlocks();

		if (localPlayer == null || entity.squaredDistanceTo(localPlayer) > (detectionRadiusBlocks * detectionRadiusBlocks)) {
			return false;
		}

		if (!ThreatMath.isOutsideView(relativeYaw, getHorizontalFov(client), FOV_MARGIN_DEGREES)) {
			return false;
		}

		return switch (kind) {
			case HOSTILE_TARGETING, HOSTILE_APPROACHING -> entity instanceof HostileEntity
					&& (config.reactToTargetingHostiles() || config.reactToApproachingHostiles());
			case OFFSCREEN_PLAYER -> entity instanceof PlayerEntity otherPlayer
					&& otherPlayer.isAlive()
					&& !otherPlayer.isSpectator()
					&& config.allowsPlayerName(otherPlayer.getGameProfile().name());
		};
	}

	private Optional<ThreatSnapshot> createHostileThreat(MinecraftClient client, PlayerEntity localPlayer, HostileEntity hostile, AttentionConfig config) {
		double detectionRadiusBlocks = config.detectionRadiusBlocks();
		double distanceSq = hostile.squaredDistanceTo(localPlayer);

		if (distanceSq > (detectionRadiusBlocks * detectionRadiusBlocks)) {
			return Optional.empty();
		}

		float relativeYaw = relativeYawTo(client, localPlayer, hostile);

		if (!ThreatMath.isOutsideView(relativeYaw, getHorizontalFov(client), FOV_MARGIN_DEGREES)) {
			return Optional.empty();
		}

		if (config.reactToTargetingHostiles() && hostile.getTarget() == localPlayer) {
			return Optional.of(new ThreatSnapshot(hostile.getId(), ThreatKind.HOSTILE_TARGETING, distanceSq, relativeYaw));
		}

		if (config.reactToApproachingHostiles() && isHostileApproaching(localPlayer, hostile)) {
			return Optional.of(new ThreatSnapshot(hostile.getId(), ThreatKind.HOSTILE_APPROACHING, distanceSq, relativeYaw));
		}

		return Optional.empty();
	}

	private Optional<ThreatSnapshot> createPlayerThreat(MinecraftClient client, PlayerEntity localPlayer, PlayerEntity otherPlayer, double detectionRadiusBlocks) {
		double distanceSq = otherPlayer.squaredDistanceTo(localPlayer);

		if (distanceSq > (detectionRadiusBlocks * detectionRadiusBlocks)) {
			return Optional.empty();
		}

		float relativeYaw = relativeYawTo(client, localPlayer, otherPlayer);

		if (!ThreatMath.isOutsideView(relativeYaw, getHorizontalFov(client), FOV_MARGIN_DEGREES)) {
			return Optional.empty();
		}

		return Optional.of(new ThreatSnapshot(otherPlayer.getId(), ThreatKind.OFFSCREEN_PLAYER, distanceSq, relativeYaw));
	}

	private static boolean isHostileApproaching(PlayerEntity localPlayer, HostileEntity hostile) {
		return ThreatMath.isMovingTowardPlayer(
				hostile.getX(),
				hostile.getZ(),
				hostile.getVelocity().x,
				hostile.getVelocity().z,
				localPlayer.getX(),
				localPlayer.getZ(),
				APPROACH_DOT_THRESHOLD
		);
	}

	private static float relativeYawTo(MinecraftClient client, PlayerEntity localPlayer, Entity entity) {
		return ThreatMath.relativeYawDegrees(
				localPlayer.getYaw(),
				localPlayer.getX(),
				localPlayer.getZ(),
				entity.getX(),
				entity.getZ()
		);
	}

	private static float getHorizontalFov(MinecraftClient client) {
		return client.options.getFov().getValue().floatValue();
	}
}
