package eu.praxios.blocklords.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResidentNamesTest {
    @Test
    void namesAreLargeUniqueAndRandomFromList() {
        assertTrue(ResidentNames.NAMES.size() >= 80);
        assertEquals(ResidentNames.NAMES.size(), ResidentNames.NAMES.stream().distinct().count());
        assertTrue(ResidentNames.NAMES.contains(ResidentNames.random()));
    }
}
