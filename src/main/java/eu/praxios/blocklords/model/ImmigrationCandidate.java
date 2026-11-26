package eu.praxios.blocklords.model;

import java.util.UUID;

public record ImmigrationCandidate(long id, long villageId, UUID villagerId, Status status,
                                   long createdAt, long decidedAt, int xpAwarded) {
    public enum Status { WAITING, ACCEPTED, REJECTED, LOST, CANCELLED }
}
