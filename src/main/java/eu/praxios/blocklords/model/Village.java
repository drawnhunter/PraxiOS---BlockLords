package eu.praxios.blocklords.model;

import java.util.UUID;

public record Village(
        long id,
        String name,
        UUID owner,
        ClaimRegion region,
        int level,
        int xp,
        VillageStatus status,
        long createdAt,
        long lastActivity) {
}
