package eu.praxios.blocklords.model;

import java.util.UUID;

public record Mission(long id, long villageId, UUID playerId, MissionType type, int target, int xp,
                      Status status, int progress, long createdAt, long acceptedAt, long completedAt,
                      boolean pinned) {
    public enum Scope { PERSONAL, DORF }
    public enum Status { OFFERED, ACTIVE, COMPLETED, REPLACED }

    public Scope scope() { return type.scope(); }
    public MissionAction action() { return type.action(); }
    public ResourceType resource() { return type.resource(); }
    public String title() { return type.displayName(); }
    public int remaining() { return Math.max(0, target - progress); }
    public boolean personal() { return scope() == Scope.PERSONAL; }
    public MissionDefinition definition() { return new MissionDefinition(type, target, xp); }
}
