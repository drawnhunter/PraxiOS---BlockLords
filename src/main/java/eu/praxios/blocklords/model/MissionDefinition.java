package eu.praxios.blocklords.model;

public record MissionDefinition(MissionType type, int target, int xp) {
    public MissionDefinition {
        if (target <= 0) throw new IllegalArgumentException("Missionsziel muss positiv sein.");
        if (xp < 0) throw new IllegalArgumentException("Missions-XP darf nicht negativ sein.");
    }

    public static MissionDefinition defaults(MissionType type) {
        return new MissionDefinition(type, type.defaultTarget(), type.defaultXp());
    }
}
