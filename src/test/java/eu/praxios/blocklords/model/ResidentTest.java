package eu.praxios.blocklords.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ResidentTest {
    @Test
    void detectsChangedVanillaLinks() {
        UUID villageWorld = UUID.randomUUID();
        Resident first = new Resident(1, UUID.randomUUID(), Resident.Status.UNCONFIRMED, true,
                new BlockLink(villageWorld, 1, 64, 1), null, 1, 1, "Astrid", 1, false);
        Resident same = new Resident(1, first.villager(), Resident.Status.UNCONFIRMED, true, first.bed(), null, 1, 2,
                "Astrid", 1, false);
        Resident confirmed = new Resident(1, first.villager(), Resident.Status.CONFIRMED, true, first.bed(),
                new BlockLink(villageWorld, 0, 64, 0), 1, 3, "Astrid", 1, false);
        assertTrue(first.sameLinks(same));
        assertFalse(first.sameLinks(confirmed));
    }
}
