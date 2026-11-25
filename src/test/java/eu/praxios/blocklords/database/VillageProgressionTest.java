package eu.praxios.blocklords.database;

import eu.praxios.blocklords.repository.VillageRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VillageProgressionTest {
    @Test
    void thresholdsMatchConcept() {
        assertEquals(1, VillageRepository.levelForXp(0));
        assertEquals(1, VillageRepository.levelForXp(99));
        assertEquals(2, VillageRepository.levelForXp(100));
        assertEquals(3, VillageRepository.levelForXp(250));
        assertEquals(4, VillageRepository.levelForXp(450));
        assertEquals(5, VillageRepository.levelForXp(700));
        assertEquals(10, VillageRepository.levelForXp(99999));
        assertEquals(100, VillageRepository.xpForNextLevel(1));
        assertEquals(250, VillageRepository.xpForNextLevel(2));
        assertEquals(450, VillageRepository.xpForNextLevel(3));
        assertEquals(3300, VillageRepository.xpForNextLevel(9));
        assertEquals(-1, VillageRepository.xpForNextLevel(10));
    }
}
