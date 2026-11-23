package eu.praxios.blocklords.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ClaimRegionTest {
    private final UUID world = UUID.randomUUID();

    @Test
    void containsCylinderButNotAnotherWorld() {
        ClaimRegion region = new ClaimRegion(world, 100, 64, -50, 48);
        assertTrue(region.contains(world, 100, -50));
        assertTrue(region.contains(world, 148, -50));
        assertFalse(region.contains(world, 148.01, -50));
        assertFalse(region.contains(UUID.randomUUID(), 100, -50));
        assertTrue(region.contains(world, 130, -20));
        assertFalse(region.contains(world, 160, -10));
    }

    @Test
    void rejectsInvalidRadius() {
        assertThrows(IllegalArgumentException.class, () -> new ClaimRegion(world, 0, 0, 0, 0));
    }
}
