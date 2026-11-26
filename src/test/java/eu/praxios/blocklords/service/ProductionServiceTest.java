package eu.praxios.blocklords.service;

import eu.praxios.blocklords.config.BlockLordsConfig;
import eu.praxios.blocklords.database.Database;
import eu.praxios.blocklords.database.MigrationRunner;
import eu.praxios.blocklords.model.*;
import eu.praxios.blocklords.repository.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.plugin.PluginMock;

import java.io.File;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class ProductionServiceTest {
    @TempDir Path temp;
    private ServerMock server;
    private TaskRunner tasks;
    private Database database;
    private VillageRepository villageRepository;
    private StorageRepository storageRepository;
    private WorkerRepository workerRepository;
    private ProductionReportRepository reportRepository;
    private StorageService storageService;
    private ProductionService production;
    private Village village;
    private AtomicLong clock;
    private long start;

    @BeforeEach
    void setup() throws Exception {
        server = MockBukkit.mock();
        PluginMock plugin = MockBukkit.createMockPlugin();
        tasks = new TaskRunner(plugin);
        database = new Database(temp.resolve("production-service.db"));
        new MigrationRunner(database, Logger.getLogger("test")).migrate();
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.load(new File("src/main/resources/config.yml"));
        BlockLordsConfig settings = new BlockLordsConfig(yaml);
        AuditService audit = new AuditService(new AuditRepository(database), tasks,
                new LogService(Logger.getLogger("test"), () -> settings));
        clock = new AtomicLong(1_000_000L);
        start = clock.get();

        UUID owner = UUID.randomUUID();
        villageRepository = new VillageRepository(database);
        village = villageRepository.createVillage("Produktionsdorf", owner,
                new ClaimRegion(UUID.randomUUID(), 0, 64, 0, 48), 4, 20, false, 21);
        VillageService villages = new VillageService(villageRepository, new LegacyRepository(database), tasks);
        villages.loadInitial();
        storageRepository = new StorageRepository(database);
        workerRepository = new WorkerRepository(database);
        reportRepository = new ProductionReportRepository(database);
        storageService = new StorageService(() -> settings, storageRepository, new ConstructionRepository(database),
                tasks, audit, villages);
        storageService.loadInitial(villages.all());
        production = new ProductionService(plugin, () -> settings, workerRepository, storageRepository,
                villages, storageService, tasks, reportRepository, clock::get);
    }

    @AfterEach
    void close() throws Exception {
        if (tasks != null) tasks.close();
        if (database != null) database.close();
        if (server != null) MockBukkit.unmock();
    }

    @Test
    void offlineProductionStopsAfterFourHours() throws Exception {
        assignFarmer();
        storageRepository.deposit(village.id(), village.owner(), ResourceType.NAHRUNG, 1_000, 1_728);
        storageService.loadInitial(java.util.List.of(village));
        production.runVillage(withActivity(start, VillageStatus.ACTIVE));
        clock.set(start + 4 * 3_600_000L);
        production.runVillage(withActivity(start, VillageStatus.ACTIVE));
        await(() -> storageService.balances(village.id()).getOrDefault(ResourceType.WEIZEN, 0) == 144);
        ProductionReport report = reportRepository.find(village.id()).orElseThrow();
        assertEquals(24.0, report.effectiveCycles());
        assertEquals(144, accepted(report, ResourceType.WEIZEN));
        assertEquals(24, report.foodConsumed());

        int wheat = storageService.balances(village.id()).get(ResourceType.WEIZEN);
        clock.addAndGet(300_000L);
        production.runVillage(withActivity(start, VillageStatus.ACTIVE));
        await(() -> storageRepository.productionState(village.id()).lastReport().equals("Offline-Limit erreicht"));
        assertEquals(wheat, storageService.balances(village.id()).getOrDefault(ResourceType.WEIZEN, 0));
        clock.addAndGet(300_000L);
        production.runVillage(withActivity(start, VillageStatus.ACTIVE));
        assertEquals(wheat, storageService.balances(village.id()).getOrDefault(ResourceType.WEIZEN, 0));
    }

    @Test
    void restingVillagePausesWithoutBackpayOnReactivation() throws Exception {
        assignFarmer();
        storageRepository.deposit(village.id(), village.owner(), ResourceType.NAHRUNG, 1_000, 1_728);
        storageService.loadInitial(java.util.List.of(village));
        production.runVillage(withActivity(start, VillageStatus.RESTING));
        clock.addAndGet(300_000L);
        production.runVillage(withActivity(start, VillageStatus.RESTING));
        await(() -> reportRepository.find(village.id()).map(report -> report.pauses().contains("Dorf ruht")).orElse(false));
        assertEquals(0, storageService.balances(village.id()).getOrDefault(ResourceType.WEIZEN, 0));

        clock.addAndGet(300_000L);
        production.runVillage(withActivity(clock.get(), VillageStatus.ACTIVE));
        await(() -> storageService.balances(village.id()).getOrDefault(ResourceType.WEIZEN, 0) == 6);
        assertEquals(6, storageService.balances(village.id()).getOrDefault(ResourceType.WEIZEN, 0));
    }

    @Test
    void fullResourceOnlyStopsThatResourceAndRecordsPause() throws Exception {
        assignFarmer();
        storageRepository.deposit(village.id(), village.owner(), ResourceType.WEIZEN, 1_728, 1_728);
        storageRepository.deposit(village.id(), village.owner(), ResourceType.NAHRUNG, 1_728, 1_728);
        storageService.loadInitial(java.util.List.of(village));
        production.runVillage(withActivity(start, VillageStatus.ACTIVE));
        clock.addAndGet(300_000L);
        production.runVillage(withActivity(start, VillageStatus.ACTIVE));
        await(() -> reportRepository.find(village.id()).isPresent());
        ProductionReport report = reportRepository.find(village.id()).orElseThrow();
        assertEquals(0, accepted(report, ResourceType.WEIZEN));
        assertEquals(6, planned(report, ResourceType.WEIZEN));
        assertEquals(1, accepted(report, ResourceType.NAHRUNG));
        assertTrue(report.pauses().stream().anyMatch(reason -> reason.contains("WEIZEN") || reason.contains("Weizen")));
        assertEquals(1_728, storageRepository.balances(village.id()).get(ResourceType.WEIZEN));
        assertEquals(1_728, storageRepository.balances(village.id()).get(ResourceType.NAHRUNG));
    }

    private void assignFarmer() throws Exception {
        Building farm = createBuilding(BlueprintType.FARM);
        workerRepository.assign(village.id(), UUID.randomUUID(), farm.id(), Worker.Job.FARMER);
    }

    private Building createBuilding(BlueprintType type) throws Exception {
        database.inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO buildings(village_id, type, level, world_uuid, x, y, z, rotation, created_at) VALUES (?, ?, 1, ?, 0, 64, 0, 0, 1)")) {
                statement.setLong(1, village.id());
                statement.setString(2, type.name());
                statement.setString(3, village.region().world().toString());
                statement.executeUpdate();
            }
            return null;
        });
        return new ConstructionRepository(database).findBuildings(village.id()).getFirst();
    }

    private Village withActivity(long activity, VillageStatus status) {
        return new Village(village.id(), village.name(), village.owner(), village.region(), village.level(), village.xp(),
                status, village.createdAt(), activity);
    }

    private int accepted(ProductionReport report, ResourceType resource) {
        return report.resources().stream().filter(entry -> entry.resource() == resource)
                .mapToInt(ProductionReport.Entry::accepted).sum();
    }

    private int planned(ProductionReport report, ResourceType resource) {
        return report.resources().stream().filter(entry -> entry.resource() == resource)
                .mapToInt(ProductionReport.Entry::planned).sum();
    }

    private interface CheckedCondition {
        boolean getAsBoolean() throws Exception;
    }

    private void await(CheckedCondition condition) throws Exception {
        for (int attempt = 0; attempt < 200; attempt++) {
            server.getScheduler().performTicks(1);
            if (condition.getAsBoolean()) return;
            Thread.sleep(25);
        }
        fail("Asynchrone Produktionsaktion wurde nicht abgeschlossen");
    }
}
