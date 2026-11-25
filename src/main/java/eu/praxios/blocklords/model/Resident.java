package eu.praxios.blocklords.model;

import java.util.UUID;

public record Resident(long villageId, UUID villager, Status status, boolean hasBed,
                       BlockLink bed, BlockLink meetingPoint, long firstSeen, long lastSeen,
                       String displayName, int number, boolean dead) {
    public enum Status { CONFIRMED, UNCONFIRMED }

    public String displayLabel() {
        return (displayName == null || displayName.isBlank() ? "Bewohner" : displayName) + " #" + number;
    }

    public boolean sameLinks(Resident other) {
        return status == other.status && hasBed == other.hasBed
                && java.util.Objects.equals(bed, other.bed)
                && java.util.Objects.equals(meetingPoint, other.meetingPoint);
    }
}
