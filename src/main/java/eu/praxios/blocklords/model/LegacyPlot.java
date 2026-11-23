package eu.praxios.blocklords.model;

import java.util.UUID;

public record LegacyPlot(
        long id,
        String name,
        UUID owner,
        UUID world,
        int minX,
        int minY,
        int minZ,
        int maxX,
        int maxY,
        int maxZ,
        UUID registeredBy,
        long createdAt) {

    public boolean contains(UUID candidateWorld, int x, int y, int z) {
        return world.equals(candidateWorld)
                && x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
    }

    public boolean overlaps(ClaimRegion region) {
        if (!world.equals(region.world())) {
            return false;
        }
        int closestX = Math.max(minX, Math.min(region.bellX(), maxX));
        int closestZ = Math.max(minZ, Math.min(region.bellZ(), maxZ));
        long dx = (long) region.bellX() - closestX;
        long dz = (long) region.bellZ() - closestZ;
        return dx * dx + dz * dz <= (long) region.radius() * region.radius();
    }
}
