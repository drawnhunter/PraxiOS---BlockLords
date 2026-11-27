package eu.praxios.blocklords.database;

import eu.praxios.blocklords.model.*;
import eu.praxios.blocklords.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class ImmigrationRepositoryTest {
    @TempDir Path temp;
    private Database database;
    private VillageRepository villages;
    private ResidentRepository residents;
    private StorageRepository storage;
    private ImmigrationRepository immigration;
    private Village village;
    private final UUID owner = UUID.randomUUID();

    @BeforeEach
    void setup() throws Exception {
        database = new Database(temp.resolve("immigration.db"));
        new MigrationRunner(database, Logger.getLogger("test")).migrate();
        villages = new VillageRepository(database);
        residents = new ResidentRepository(database);
        storage = new StorageRepository(database);
        immigration = new ImmigrationRepository(database, villages, storage, residents);
        village = villages.createVillage("Wanderdorf", owner,
                new ClaimRegion(UUID.randomUUID(), 0, 64, 0, 48), 4, 20, false, 21);
        storage.deposit(village.id(), owner, ResourceType.NAHRUNG, 100, 1_728);
    }

    @AfterEach
    void close() throws Exception { database.close(); }

    @Test
    void acceptanceIsAtomicAndAwardsFirstAndRepeatXpOnce() throws Exception {
        ImmigrationCandidate first = immigration.createCandidate(village.id(), UUID.randomUUID(), 1_000L);
        ImmigrationRepository.Acceptance accepted = immigration.accept(village.id(), first.id(), newcomer(first),
                16, 30, 5, 2_000L, 302_000L, "Nächster Wartezyklus läuft");
        assertEquals(30, accepted.candidate().xpAwarded());
        assertEquals(30, xp());
        assertEquals(84, storage.balances(village.id()).get(ResourceType.NAHRUNG));
        assertEquals(1, accepted.resident().number());
        assertEquals(first.villagerId(), accepted.resident().villager());
        assertTrue(accepted.resident().hasBed());
        assertState(302_000L, "Nächster Wartezyklus läuft");

        assertThrows(IllegalArgumentException.class, () -> immigration.accept(village.id(), first.id(), newcomer(first),
                16, 30, 5, 2_100L, 302_100L, "Nächster Wartezyklus läuft"));
        assertEquals(30, xp());
        assertEquals(84, storage.balances(village.id()).get(ResourceType.NAHRUNG));
        assertEquals(1, residents.find(village.id()).size());
        assertState(302_000L, "Nächster Wartezyklus läuft");

        ImmigrationCandidate second = immigration.createCandidate(village.id(), UUID.randomUUID(), 3_000L);
        ImmigrationRepository.Acceptance secondAccepted = immigration.accept(village.id(), second.id(), newcomer(second),
                16, 30, 5, 4_000L, 304_000L, "Nächster Wartezyklus läuft");
        assertEquals(5, secondAccepted.candidate().xpAwarded());
        assertEquals(35, xp());
        assertEquals(68, storage.balances(village.id()).get(ResourceType.NAHRUNG));
        assertEquals(2, secondAccepted.resident().number());
        assertNotEquals(accepted.resident().displayName(), secondAccepted.resident().displayName());
        assertState(304_000L, "Nächster Wartezyklus läuft");
    }

    @Test
    void insufficientFoodRollsBackCandidateResidentAndCosts() throws Exception {
        ImmigrationCandidate candidate = immigration.createCandidate(village.id(), UUID.randomUUID(), 1_000L);
        assertThrows(IllegalStateException.class, () -> immigration.accept(village.id(), candidate.id(), newcomer(candidate),
                200, 30, 5, 2_000L, 302_000L, "Nächster Wartezyklus läuft"));
        assertEquals(100, storage.balances(village.id()).get(ResourceType.NAHRUNG));
        assertEquals(0, xp());
        assertTrue(immigration.waitingById(village.id(), candidate.id()).isPresent());
        assertTrue(residents.find(village.id()).isEmpty());
        assertState(0L, "Wanderer wartet an der Glocke auf Aufnahme");
    }

    @Test
    void rejectionAndLossHaveNoCostsAndAllowAnotherCandidate() throws Exception {
        ImmigrationCandidate rejected = immigration.createCandidate(village.id(), UUID.randomUUID(), 1_000L);
        assertTrue(immigration.reject(village.id(), rejected.id(), 2_000L, 62_000L, "Wanderer abgelehnt").isPresent());
        assertEquals(100, storage.balances(village.id()).get(ResourceType.NAHRUNG));
        assertEquals(0, xp());
        assertTrue(residents.find(village.id()).isEmpty());
        assertState(62_000L, "Wanderer abgelehnt");

        ImmigrationCandidate lost = immigration.createCandidate(village.id(), UUID.randomUUID(), 3_000L);
        assertTrue(immigration.markLost(lost.villagerId(), 4_000L, 64_000L, "Wanderer verstorben").isPresent());
        assertEquals(100, storage.balances(village.id()).get(ResourceType.NAHRUNG));
        assertEquals(0, xp());
        assertState(64_000L, "Wanderer verstorben");
        assertTrue(immigration.createCandidate(village.id(), UUID.randomUUID(), 5_000L).id() > 0);
    }

    @Test
    void decisionAndRetryAreVisibleInOneCommittedSnapshot() throws Exception {
        ImmigrationCandidate candidate = immigration.createCandidate(village.id(), UUID.randomUUID(), 1_000L);
        ImmigrationRepository.Snapshot before = immigration.snapshot(village.id());
        assertTrue(before.waiting().isPresent());

        assertTrue(immigration.reject(village.id(), candidate.id(), 2_000L, 62_000L, "Wanderer abgelehnt").isPresent());
        ImmigrationRepository.Snapshot after = immigration.snapshot(village.id());
        assertTrue(before.waiting().isPresent());
        assertTrue(after.waiting().isEmpty());
        assertEquals(62_000L, after.state().nextArrivalAt());
        assertEquals("Wanderer abgelehnt", after.state().reason());
    }

    @Test
    void identityContinuesExistingNumberAndAvoidsExistingName() throws Exception {
        UUID existingId = UUID.randomUUID();
        residents.touchAll(village.id(), List.of(new Resident(village.id(), existingId, Resident.Status.CONFIRMED, true,
                new BlockLink(village.region().world(), 1, 64, 1), new BlockLink(village.region().world(), 0, 64, 0),
                1L, 1L, "Astrid", 7, false)));
        ImmigrationCandidate candidate = immigration.createCandidate(village.id(), UUID.randomUUID(), 1_000L);
        Resident accepted = immigration.accept(village.id(), candidate.id(), newcomer(candidate),
                16, 30, 5, 2_000L, 302_000L, "Nächster Wartezyklus läuft").resident();
        assertEquals(8, accepted.number());
        assertNotEquals("Astrid", accepted.displayName());
        assertEquals(2, residents.find(village.id()).size());
    }

    @Test
    void waitingCandidatesAreUniquePerVillageButIndependentAcrossVillages() throws Exception {
        ImmigrationCandidate first = immigration.createCandidate(village.id(), UUID.randomUUID(), 1_000L);
        assertThrows(IllegalStateException.class, () -> immigration.createCandidate(village.id(), UUID.randomUUID(), 1_100L));
        Village other = villages.createVillage("Anderes Dorf", UUID.randomUUID(),
                new ClaimRegion(UUID.randomUUID(), 0, 64, 0, 48), 4, 20, false, 21);
        storage.deposit(other.id(), other.owner(), ResourceType.NAHRUNG, 32, 1_728);
        ImmigrationCandidate second = immigration.createCandidate(other.id(), UUID.randomUUID(), 1_200L);
        assertNotEquals(first.id(), second.id());
        immigration.accept(other.id(), second.id(), newcomer(second),
                16, 30, 5, 2_000L, 302_000L, "Nächster Wartezyklus läuft");
        assertEquals(0, xp());
        assertTrue(immigration.waitingById(village.id(), first.id()).isPresent());
    }

    private Resident newcomer(ImmigrationCandidate candidate) {
        return new Resident(candidate.villageId(), candidate.villagerId(), Resident.Status.CONFIRMED, true,
                new BlockLink(village.region().world(), 1, 64, 1), new BlockLink(village.region().world(), 0, 64, 0),
                candidate.createdAt(), candidate.createdAt(), null, 0, false);
    }

    private int xp() throws Exception {
        return villages.findById(village.id()).orElseThrow().xp();
    }

    private void assertState(long nextArrivalAt, String reason) throws Exception {
        ImmigrationState state = immigration.snapshot(village.id()).state();
        assertEquals(nextArrivalAt, state.nextArrivalAt());
        assertEquals(reason, state.reason());
    }
}
