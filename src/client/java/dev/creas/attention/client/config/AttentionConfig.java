package dev.creas.attention.client.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.creas.attention.threat.ThreatKind;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

public record AttentionConfig(
		double detectionRadiusBlocks,
		double minIndicatorRadiusPixels,
		double maxIndicatorRadiusPixels,
		boolean reactOnlyInSurvival,
		boolean reactToPlayers,
		boolean reactToTargetingHostiles,
		boolean reactToApproachingHostiles,
		PlayerFilterMode playerFilterMode,
		List<String> playerFilterNames
) {
	private static final double DEFAULT_DETECTION_RADIUS = 24.0D;
	private static final double MIN_DETECTION_RADIUS = 6.0D;
	private static final double MAX_DETECTION_RADIUS = 96.0D;
	private static final double DEFAULT_MIN_INDICATOR_RADIUS = 10.0D;
	private static final double DEFAULT_MAX_INDICATOR_RADIUS = 16.0D;
	private static final double MIN_INDICATOR_RADIUS = 8.0D;
	private static final double MAX_INDICATOR_RADIUS = 120.0D;
	private static final boolean DEFAULT_REACT_ONLY_IN_SURVIVAL = true;
	private static final boolean DEFAULT_REACT_TO_PLAYERS = true;
	private static final boolean DEFAULT_REACT_TO_TARGETING_HOSTILES = true;
	private static final boolean DEFAULT_REACT_TO_APPROACHING_HOSTILES = true;

	public static AttentionConfig defaults() {
		return new AttentionConfig(
				DEFAULT_DETECTION_RADIUS,
				DEFAULT_MIN_INDICATOR_RADIUS,
				DEFAULT_MAX_INDICATOR_RADIUS,
				DEFAULT_REACT_ONLY_IN_SURVIVAL,
				DEFAULT_REACT_TO_PLAYERS,
				DEFAULT_REACT_TO_TARGETING_HOSTILES,
				DEFAULT_REACT_TO_APPROACHING_HOSTILES,
				PlayerFilterMode.ALL_PLAYERS,
				List.of()
		);
	}

	public AttentionConfig sanitize() {
		double sanitizedDetectionRadius = clamp(detectionRadiusBlocks, MIN_DETECTION_RADIUS, MAX_DETECTION_RADIUS);
		double sanitizedMaxRadius = clamp(maxIndicatorRadiusPixels, MIN_INDICATOR_RADIUS, MAX_INDICATOR_RADIUS);
		double sanitizedMinRadius = clamp(minIndicatorRadiusPixels, MIN_INDICATOR_RADIUS, sanitizedMaxRadius);
		PlayerFilterMode sanitizedPlayerFilterMode = playerFilterMode == null ? PlayerFilterMode.ALL_PLAYERS : playerFilterMode;
		List<String> sanitizedPlayerFilterNames = sanitizePlayerFilterNames(playerFilterNames);

		if (sanitizedDetectionRadius == detectionRadiusBlocks
				&& sanitizedMinRadius == minIndicatorRadiusPixels
				&& sanitizedMaxRadius == maxIndicatorRadiusPixels
				&& sanitizedPlayerFilterMode == playerFilterMode
				&& sanitizedPlayerFilterNames.equals(playerFilterNames == null ? List.of() : playerFilterNames)) {
			return this;
		}

		return new AttentionConfig(
				sanitizedDetectionRadius,
				sanitizedMinRadius,
				sanitizedMaxRadius,
				reactOnlyInSurvival,
				reactToPlayers,
				reactToTargetingHostiles,
				reactToApproachingHostiles,
				sanitizedPlayerFilterMode,
				sanitizedPlayerFilterNames
		);
	}

	public boolean reactsTo(ThreatKind kind) {
		return switch (kind) {
			case OFFSCREEN_PLAYER -> reactToPlayers;
			case HOSTILE_TARGETING -> reactToTargetingHostiles;
			case HOSTILE_APPROACHING -> reactToApproachingHostiles;
		};
	}

	public boolean allowsPlayerName(String playerName) {
		if (!reactToPlayers) {
			return false;
		}

		String normalizedName = normalizePlayerName(playerName);
		boolean listed = playerFilterNames.contains(normalizedName);

		return switch (playerFilterMode) {
			case ALL_PLAYERS -> true;
			case ONLY_LISTED_PLAYERS -> listed;
			case ALL_EXCEPT_LISTED_PLAYERS -> !listed;
		};
	}

	public JsonObject toJson() {
		JsonObject root = new JsonObject();
		root.addProperty("detectionRadiusBlocks", detectionRadiusBlocks);
		root.addProperty("minIndicatorRadiusPixels", minIndicatorRadiusPixels);
		root.addProperty("maxIndicatorRadiusPixels", maxIndicatorRadiusPixels);
		root.addProperty("reactOnlyInSurvival", reactOnlyInSurvival);
		root.addProperty("reactToPlayers", reactToPlayers);
		root.addProperty("reactToTargetingHostiles", reactToTargetingHostiles);
		root.addProperty("reactToApproachingHostiles", reactToApproachingHostiles);
		root.addProperty("playerFilterMode", playerFilterMode.name());

		JsonArray names = new JsonArray();
		for (String playerFilterName : playerFilterNames) {
			names.add(playerFilterName);
		}
		root.add("playerFilterNames", names);
		return root;
	}

	public static AttentionConfig fromJson(JsonObject root) {
		AttentionConfig defaults = defaults();
		double legacyRadius = getDouble(root, "indicatorRadiusPixels", defaults.maxIndicatorRadiusPixels());
		double maxRadius = getDouble(root, "maxIndicatorRadiusPixels", legacyRadius);
		double minRadius = getDouble(root, "minIndicatorRadiusPixels", Math.max(defaults.minIndicatorRadiusPixels(), legacyRadius * 0.55D));

		return new AttentionConfig(
				getDouble(root, "detectionRadiusBlocks", defaults.detectionRadiusBlocks()),
				minRadius,
				maxRadius,
				getBoolean(root, "reactOnlyInSurvival", defaults.reactOnlyInSurvival()),
				getBoolean(root, "reactToPlayers", defaults.reactToPlayers()),
				getBoolean(root, "reactToTargetingHostiles", defaults.reactToTargetingHostiles()),
				getBoolean(root, "reactToApproachingHostiles", defaults.reactToApproachingHostiles()),
				getPlayerFilterMode(root, defaults.playerFilterMode()),
				getPlayerFilterNames(root)
		).sanitize();
	}

	public static List<String> parsePlayerFilterText(String rawText) {
		if (rawText == null || rawText.isBlank()) {
			return List.of();
		}

		String[] tokens = rawText.split("[,;\\n\\r]+");
		return sanitizePlayerFilterNames(List.of(tokens));
	}

	public static String formatPlayerFilterText(Collection<String> playerNames) {
		return String.join(", ", sanitizePlayerFilterNames(playerNames));
	}

	public static double minDetectionRadiusBlocks() {
		return MIN_DETECTION_RADIUS;
	}

	public static double maxDetectionRadiusBlocks() {
		return MAX_DETECTION_RADIUS;
	}

	public static double minimumIndicatorRadiusPixels() {
		return MIN_INDICATOR_RADIUS;
	}

	public static double maximumIndicatorRadiusPixels() {
		return MAX_INDICATOR_RADIUS;
	}

	private static List<String> sanitizePlayerFilterNames(Collection<String> playerNames) {
		if (playerNames == null || playerNames.isEmpty()) {
			return List.of();
		}

		LinkedHashSet<String> sanitizedNames = new LinkedHashSet<>();
		for (String playerName : playerNames) {
			String normalizedName = normalizePlayerName(playerName);
			if (!normalizedName.isEmpty()) {
				sanitizedNames.add(normalizedName);
			}
		}

		return List.copyOf(sanitizedNames);
	}

	private static String normalizePlayerName(String playerName) {
		return playerName == null ? "" : playerName.trim().toLowerCase(Locale.ROOT);
	}

	private static double getDouble(JsonObject root, String key, double fallback) {
		JsonElement element = root.get(key);
		return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber() ? element.getAsDouble() : fallback;
	}

	private static boolean getBoolean(JsonObject root, String key, boolean fallback) {
		JsonElement element = root.get(key);
		return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean() ? element.getAsBoolean() : fallback;
	}

	private static PlayerFilterMode getPlayerFilterMode(JsonObject root, PlayerFilterMode fallback) {
		JsonElement element = root.get("playerFilterMode");

		if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
			return fallback;
		}

		try {
			return PlayerFilterMode.valueOf(element.getAsString());
		} catch (IllegalArgumentException ignored) {
			return fallback;
		}
	}

	private static List<String> getPlayerFilterNames(JsonObject root) {
		JsonElement element = root.get("playerFilterNames");

		if (element == null || !element.isJsonArray()) {
			return List.of();
		}

		LinkedHashSet<String> names = new LinkedHashSet<>();
		for (JsonElement entry : element.getAsJsonArray()) {
			if (entry.isJsonPrimitive() && entry.getAsJsonPrimitive().isString()) {
				String normalizedName = normalizePlayerName(entry.getAsString());
				if (!normalizedName.isEmpty()) {
					names.add(normalizedName);
				}
			}
		}

		return List.copyOf(names);
	}

	private static double clamp(double value, double min, double max) {
		if (Double.isNaN(value) || Double.isInfinite(value)) {
			return min;
		}

		return Math.max(min, Math.min(max, value));
	}
}
