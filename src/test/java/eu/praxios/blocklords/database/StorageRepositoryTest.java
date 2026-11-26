package eu.praxios.blocklords.database;

import eu.praxios.blocklords.model.ClaimRegion;
import eu.praxios.blocklords.model.ResourceType;
import eu.praxios.blocklords.model.Village;
import eu.praxios.blocklords.repository.StorageRepository;
import eu.praxios.blocklords.repository.VillageRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class StorageRepositoryTest {
    @TempDir Path temp;
    private Database database;
    private StorageRepository storage;
    private Village village;
    private final UUID player = UUID.randomUUID();

    @BeforeEach
    void setup() throws Exception {
        database = new Database(temp.resolve("storage.db"));
        new MigrationRunner(database, Logger.getLogger("test")).migrate();
        storage = new StorageRepository(database);
        village = new VillageRepository(database).createVillage("Lagerdorf", player,
                new ClaimRegion(UUID.randomUUID(), 0, 64, 0, 48), 4, 20, false, 21);
    }

    @AfterEach
    void close() throws Exception { database.close(); }

    @Test
    void depositStopsAtCapacityWithoutLoss() throws Exception {
        assertEquals(10, storage.deposit(village.id(), player, ResourceType.HOLZ, 20, 10));
        assertEquals(5, storage.deposit(village.id(), player, ResourceType.STEIN, 5, 10));
        assertEquals(10, storage.balances(village.id()).get(ResourceType.HOLZ));
        assertEquals(5, storage.balances(village.id()).get(ResourceType.STEIN));
        assertEquals(4, storage.withdraw(village.id(), player, ResourceType.HOLZ, 4));
        assertEquals(6, storage.balances(village.id()).get(ResourceType.HOLZ));
    }

    @Test
    void failedCostConsumptionHasNoPartialDebit() throws Exception {
        storage.deposit(village.id(), player, ResourceType.HOLZ, 5, 100);
        assertFalse(storage.consumeCost(village.id(), player,
                Map.of(ResourceType.HOLZ, 6, ResourceType.STEIN, 1), "TEST"));
        assertEquals(5, storage.balances(village.id()).get(ResourceType.HOLZ));
        assertEquals(0, storage.balances(village.id()).get(ResourceType.STEIN));
    }

    @Test
    void productionUsesPerResourceCapacity() throws Exception {
        storage.deposit(village.id(), player, ResourceType.HOLZ, 8, 10);
        StorageRepository.ProductionResult result = storage.applyProduction(village.id(),
                Map.of(ResourceType.WEIZEN, 5), 0, 10, 1L, "Test");
        assertEquals(5, result.accepted().get(ResourceType.WEIZEN));
        assertEquals(13, storage.balances(village.id()).values().stream().mapToInt(Integer::intValue).sum());
    }
}
