package eu.praxios.blocklords.database;

import eu.praxios.blocklords.model.*;
import eu.praxios.blocklords.repository.ResidentRepository;
import eu.praxios.blocklords.repository.VillageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class ResidentRepositoryTest {
    @TempDir Path temp;

    @Test
    void residentNameAndNumberPersist() throws Exception {
        try (Database database = new Database(temp.resolve("residents.db"))) {
            ResidentRepository repository = repository(database);
            Village village = village(database, "Namensdorf");
            Resident resident = resident(village, "Astrid", 1);
            repository.touchAll(village.id(), List.of(resident));
            Resident loaded = repository.find(village.id()).getFirst();
            assertEquals("Astrid #1", loaded.displayLabel());
            assertEquals("Astrid", loaded.displayName());
            assertEquals(1, loaded.number());
            assertFalse(loaded.dead());
        }
    }

    @Test
    void deathSurvivesTouchAndOnlyDeadResidentsArePurged() throws Exception {
        try (Database database = new Database(temp.resolve("resident-loss.db"))) {
            ResidentRepository repository = repository(database);
            Village village = village(database, "Verlustdorf");
            Resident dead = resident(village, "Astrid", 1);
            Resident living = resident(village, "Bodo", 2);
            repository.touchAll(village.id(), List.of(dead, living));
            repository.markDead(dead.villager());
            repository.touchAll(village.id(), List.of(dead));

            List<Resident> loaded = repository.find(village.id());
            Resident marked = loaded.stream().filter(resident -> resident.villager().equals(dead.villager())).findFirst().orElseThrow();
            assertTrue(marked.dead());
            assertFalse(loaded.stream().filter(resident -> resident.villager().equals(living.villager())).findFirst().orElseThrow().dead());

            assertEquals(1, repository.deleteDead(village.id()));
            List<Resident> remaining = repository.find(village.id());
            assertEquals(1, remaining.size());
            assertEquals(living.villager(), remaining.getFirst().villager());
        }
    }

    private ResidentRepository repository(Database database) throws Exception {
        new MigrationRunner(database, Logger.getLogger("test")).migrate();
        return new ResidentRepository(database);
    }

    private Village village(Database database, String name) throws Exception {
        return new VillageRepository(database).createVillage(name, UUID.randomUUID(),
                new ClaimRegion(UUID.randomUUID(), 0, 64, 0, 48), 4, 20, false, 21);
    }

    private Resident resident(Village village, String name, int number) {
        return new Resident(village.id(), UUID.randomUUID(), Resident.Status.CONFIRMED,
                false, null, null, 1, 1, name, number, false);
    }
}
