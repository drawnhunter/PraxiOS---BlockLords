package eu.praxios.blocklords.model;

import java.util.UUID;

public record Worker(long id, long villageId, UUID villager, long buildingId, Job job, long assignedAt) {
    public enum Job { FARMER, MINER }
}
