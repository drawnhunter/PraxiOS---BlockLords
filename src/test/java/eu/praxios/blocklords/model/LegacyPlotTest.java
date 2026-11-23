package eu.praxios.blocklords.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LegacyPlotTest {
    @Test
    void detectsCuboidCylinderOverlapAndSeparation() {
        UUID world = UUID.randomUUID();
        LegacyPlot plot = new LegacyPlot(1, "Alt", null, world,
                90, 60, 90, 110, 80, 110, UUID.randomUUID(), 1L);
        assertTrue(plot.overlaps(new ClaimRegion(world, 100, 64, 100, 48)));
        assertTrue(plot.overlaps(new ClaimRegion(world, 150, 64, 100, 48)));
        assertFalse(plot.overlaps(new ClaimRegion(world, 200, 64, 100, 48)));
        assertFalse(plot.overlaps(new ClaimRegion(UUID.randomUUID(), 100, 64, 100, 48)));
        assertTrue(plot.contains(world, 100, 70, 100));
        assertFalse(plot.contains(world, 100, 50, 100));
    }
}
