package eu.praxios.blocklords.database;

import eu.praxios.blocklords.model.*;
import eu.praxios.blocklords.repository.ConstructionRepository;
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

class ConstructionRepositoryTest {
    @TempDir Path temp;
    private Database database;
    private ConstructionRepository construction;
    private StorageRepository storage;
    private Village village;
    private final UUID player = UUID.randomUUID();

    @BeforeEach
    void setup() throws Exception {
        database = new Database(temp.resolve("construction.db"));
        new MigrationRunner(database, Logger.getLogger("test")).migrate();
        VillageRepository villages = new VillageRepository(database);
        village = villages.createVillage("Testdorf", player,
                new ClaimRegion(UUID.randomUUID(), 0, 64, 0, 48), 4, 20, false, 21);
        construction = new ConstructionRepository(database);
        storage = new StorageRepository(database);
    }

    @AfterEach
    void close() throws Exception { database.close(); }

    @Test
    void contributionsBecomeReadyAndCompleteBuilding() throws Exception {
        BuildSite site = construction.createSite(village.id(), BlueprintType.RATHAUS, location(), player, chest());
        assertEquals(chest(), construction.findSite(site.id()).orElseThrow().chest());
        assertEquals(32, construction.contribute(site.id(), player, ResourceType.HOLZ, 40));
        assertEquals(BuildSite.Status.COLLECTING, construction.findSite(site.id()).orElseThrow().status());
        assertEquals(16, construction.contribute(site.id(), player, ResourceType.STEIN, 20));
        BuildSite ready = construction.findSite(site.id()).orElseThrow();
        assertTrue(ready.complete());
        assertEquals(BuildSite.Status.READY, ready.status());
        construction.markBuilding(site.id());
        Building building = construction.completeSite(site.id());
        assertEquals(BlueprintType.RATHAUS, building.type());
        assertEquals(chest(), building.chest());
        assertTrue(construction.findSites(village.id()).isEmpty());
        assertEquals(1, construction.findBuildings(village.id()).size());
        assertEquals(chest(), construction.findBuildings(village.id()).getFirst().chest());
    }

    @Test
    void storageFundingIsAtomicAndCountedOnce() throws Exception {
        storage.deposit(village.id(), player, ResourceType.HOLZ, 20, 100);
        storage.deposit(village.id(), player, ResourceType.STEIN, 10, 100);
        BuildSite site = construction.createSite(village.id(), BlueprintType.FARM, location(), player, chest());
        assertEquals(16, construction.fundFromStorage(site.id(), player, ResourceType.HOLZ, 99));
        assertEquals(8, construction.fundFromStorage(site.id(), player, ResourceType.STEIN, 99));
        assertEquals(4, storage.balances(village.id()).get(ResourceType.HOLZ));
        assertEquals(2, storage.balances(village.id()).get(ResourceType.STEIN));
        assertTrue(construction.findSite(site.id()).orElseThrow().complete());
    }

    @Test
    void chestInventoryControlsReadyState() throws Exception {
        BuildSite site = construction.createSite(village.id(), BlueprintType.FARM, location(), player, chest());
        construction.reconcileFromChest(site.id(), Map.of(ResourceType.HOLZ, 16));
        assertEquals(BuildSite.Status.COLLECTING, construction.findSite(site.id()).orElseThrow().status());
        construction.reconcileFromChest(site.id(), Map.of(ResourceType.HOLZ, 16, ResourceType.STEIN, 8));
        BuildSite ready = construction.findSite(site.id()).orElseThrow();
        assertEquals(BuildSite.Status.READY, ready.status());
        assertTrue(ready.complete());
        construction.recordContribution(site.id(), player, ResourceType.HOLZ, 16);
        construction.reconcileFromChest(site.id(), Map.of());
        assertEquals(BuildSite.Status.COLLECTING, construction.findSite(site.id()).orElseThrow().status());
    }

    private BuildLocation location() {
        return new BuildLocation(village.region().world(), 10, 65, 10, 0);
    }

    private BlockLink chest() {
        return new BlockLink(village.region().world(), 8, 65, 8);
    }
}
