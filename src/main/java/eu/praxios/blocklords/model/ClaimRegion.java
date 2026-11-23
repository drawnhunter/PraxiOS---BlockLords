package eu.praxios.blocklords.model;

import java.util.UUID;

/** Volle Höhe; nur die horizontale Zylinderform ist für die Claim-Prüfung relevant. */
public record ClaimRegion(UUID world, int bellX, int bellY, int bellZ, int radius) {
    public ClaimRegion {
        if (radius <= 0) {
            throw new IllegalArgumentException("Der Claim-Radius muss positiv sein.");
        }
    }

    public boolean contains(UUID candidateWorld, double x, double z) {
        if (!world.equals(candidateWorld)) {
            return false;
        }
        double dx = x - bellX;
        double dz = z - bellZ;
        return dx * dx + dz * dz <= (double) radius * radius;
    }

    public boolean containsBell(ClaimRegion other) {
        return contains(other.world, other.bellX, other.bellZ);
    }
}
