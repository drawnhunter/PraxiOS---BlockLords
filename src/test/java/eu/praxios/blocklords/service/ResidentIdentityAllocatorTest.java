package eu.praxios.blocklords.service;

import eu.praxios.blocklords.model.Resident;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ResidentIdentityAllocatorTest {
    @Test
    void allocatesSequentialNumbersWithinAndAcrossBatches() {
        ResidentIdentityAllocator allocator = new ResidentIdentityAllocator(List.of());
        assertEquals(1, allocator.next(null).number());
        assertEquals(2, allocator.next(null).number());
        assertEquals(3, allocator.next(null).number());
        assertEquals(4, allocator.next(null).number());
    }

    @Test
    void preservesExistingIdentityAndContinuesAfterRestart() {
        Resident existing = resident("Astrid", 4);
        ResidentIdentityAllocator allocator = new ResidentIdentityAllocator(List.of(existing));
        ResidentIdentityAllocator.Identity preserved = allocator.next(existing);
        assertEquals("Astrid", preserved.name());
        assertEquals(4, preserved.number());
        assertEquals(5, allocator.next(null).number());

        Resident numbered = resident("Bodo", 5);
        ResidentIdentityAllocator reloaded = new ResidentIdentityAllocator(List.of(existing, numbered));
        assertEquals(6, reloaded.next(null).number());
    }

    @Test
    void repairsMissingNamesZeroAndDuplicateNumbers() {
        List<Resident> repaired = ResidentService.repairIdentities(List.of(
                resident(null, 0), resident("Astrid", 1), resident("Bodo", 1), resident("Clara", 0)));
        assertEquals(4, repaired.size());
        assertEquals(4, repaired.stream().map(Resident::number).distinct().count());
        assertTrue(repaired.stream().allMatch(resident -> resident.number() > 0));
        assertTrue(repaired.stream().allMatch(resident -> resident.displayName() != null && !resident.displayName().isBlank()));
    }

    private Resident resident(String name, int number) {
        return new Resident(1, UUID.randomUUID(), Resident.Status.CONFIRMED,
                false, null, null, 1, 1, name, number, false);
    }
}
