package eu.praxios.blocklords.model;

public record Building(long id, long villageId, BlueprintType type, int level, BuildLocation location, long createdAt) {
}
