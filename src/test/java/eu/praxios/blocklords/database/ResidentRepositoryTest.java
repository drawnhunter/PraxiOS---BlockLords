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
            new MigrationRunner(database, Logger.getLogger("test")).migrate();
            UUID player = UUID.randomUUID();
            Village village = new VillageRepository(database).createVillage("Namensdorf", player,
                    new ClaimRegion(UUID.randomUUID(), 0, 64, 0, 48), 4, 20, false, 21);
            ResidentRepository repository = new ResidentRepository(database);
            Resident resident = new Resident(village.id(), UUID.randomUUID(), Resident.Status.CONFIRMED,
                    false, null, null, 1, 1, "Astrid", 1);
            repository.touchAll(village.id(), List.of(resident));
            Resident loaded = repository.find(village.id()).getFirst();
            assertEquals("Astrid #1", loaded.displayLabel());
            assertEquals("Astrid", loaded.displayName());
            assertEquals(1, loaded.number());
        }
    }
}
