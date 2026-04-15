package dev.creas.attention.threat;

public record ThreatSnapshot(int entityId, ThreatKind kind, double distanceSq, float relativeYawDeg) {
}

