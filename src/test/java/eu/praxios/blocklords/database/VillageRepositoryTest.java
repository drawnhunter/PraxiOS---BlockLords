package eu.praxios.blocklords.database;

import eu.praxios.blocklords.model.ClaimRegion;
import eu.praxios.blocklords.model.LegacyPlot;
import eu.praxios.blocklords.model.Role;
import eu.praxios.blocklords.model.Village;
import eu.praxios.blocklords.repository.ClaimException;
import eu.praxios.blocklords.repository.LegacyRepository;
import eu.praxios.blocklords.repository.OwnershipRepository;
import eu.praxios.blocklords.repository.VillageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class VillageRepositoryTest {
    @TempDir
    Path temp;
    private Database database;
    private VillageRepository villages;
    private final UUID world = UUID.randomUUID();
    private final UUID owner = UUID.randomUUID();

    @BeforeEach
    void setup() throws Exception {
        database = new Database(temp.resolve("test.db"));
        new MigrationRunner(database, Logger.getLogger("test")).migrate();
        villages = new VillageRepository(database);
    }

    @Test
    void migratesIdempotentlyAndPersistsFirstVillage() throws Exception {
        new MigrationRunner(database, Logger.getLogger("test")).migrate();
        Village village = villages.createVillage("Eichhain", owner, region(0, 0, 48), 4, 20, false, 21);
        assertTrue(village.id() > 0);
        assertEquals(Role.LANDHERR, villages.findAllMembers().get(village.id()).get(owner));
        assertEquals(1, villages.findAll().size());
        Path backup = temp.resolve("backup.db");
        database.backup(backup);
        assertTrue(Files.size(backup) > 0);
        database.close();
    }

    @Test
    void rejectsDuplicateNameOverlapAndSecondVillageDuringPhaseOne() throws Exception {
        villages.createVillage("Eichhain", owner, region(0, 0, 48), 4, 20, false, 21);
        assertReason(ClaimException.Reason.NAME_TAKEN, () -> villages.createVillage("eichhain", UUID.randomUUID(), region(500, 0, 48), 4, 20, false, 21));
        assertReason(ClaimException.Reason.MULTI_DISABLED, () -> villages.createVillage("Neudorf", owner, region(500, 0, 48), 4, 20, false, 21));
        assertReason(ClaimException.Reason.OVERLAP, () -> villages.createVillage("Zu nah", UUID.randomUUID(), region(50, 0, 48), 4, 20, false, 21));
        database.close();
    }

    @Test
    void respectsLegacyAndTrackedOwnership() throws Exception {
        LegacyRepository legacy = new LegacyRepository(database);
        legacy.register("Altbesitz", UUID.randomUUID(), world, -10, 60, -10, 10, 80, 10, UUID.randomUUID());
        assertReason(ClaimException.Reason.ALT_CONFLICT,
                () -> villages.createVillage("Eichhain", owner, region(0, 0, 48), 4, 20, false, 21));

        LegacyPlot own = legacy.register("Eigener Altbesitz", owner, world, 200, 60, 200, 210, 80, 210, owner);
        assertDoesNotThrow(() -> villages.createVillage("Eigendorf", owner, region(205, 205, 48), 4, 20, false, 21));
        assertTrue(own.id() > 0);
        database.close();
    }

    @Test
    void rejectsAnotherPlayersTrackedBlockInsideClaim() throws Exception {
        OwnershipRepository ownership = new OwnershipRepository(database);
        ownership.recordPlacements(List.of(new OwnershipRepository.BlockPosition(world, 5, 64, 5)), UUID.randomUUID());
        assertReason(ClaimException.Reason.ALT_CONFLICT,
                () -> villages.createVillage("Eichhain", owner, region(0, 0, 48), 4, 20, false, 21));
        database.close();
    }

    private ClaimRegion region(int x, int z, int radius) {
        return new ClaimRegion(world, x, 64, z, radius);
    }

    private void assertReason(ClaimException.Reason reason, org.junit.jupiter.api.function.Executable executable) {
        ClaimException exception = assertThrows(ClaimException.class, executable);
        assertEquals(reason, exception.reason());
    }
}
