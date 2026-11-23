package eu.praxios.blocklords.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VillageNamesTest {
    @Test
    void acceptsReadableVillageNames() {
        assertNull(VillageNames.validate("Eichhain"));
        assertNull(VillageNames.validate("Sankt Marien-Furt"));
        assertNull(VillageNames.validate("Dorf am Brückenbach_2"));
    }

    @Test
    void normalizesWhitespace() {
        assertEquals("Dorf am Fluss", VillageNames.normalize("  Dorf   am  Fluss  "));
    }

    @Test
    void rejectsUnsafeOrUnusableNames() {
        assertNotNull(VillageNames.validate("Ab"));
        assertNotNull(VillageNames.validate("-Dorf-"));
        assertNotNull(VillageNames.validate("<red>Dorf</red>"));
        assertNotNull(VillageNames.validate("Ein viel zu langer Dorfname mit Zusatz"));
    }
}
