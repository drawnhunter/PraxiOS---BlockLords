package eu.praxios.blocklords.model;

import java.util.UUID;

public record PlacementSnapshot(UUID player, long villageId, BlueprintType blueprint,
                                byte[] contents, byte[] armor, byte[] extras, int selectedSlot, long createdAt) {
}
