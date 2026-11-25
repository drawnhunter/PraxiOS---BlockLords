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
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.plugin.PluginMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class WorkerServiceTest {
    @TempDir Path temp;
    private ServerMock server;
    private TaskRunner tasks;
    private Database database;
    private WorkerRepository repository;
    private WorkerService workers;
    private Village village;
    private Building building;
    private PlayerMock player;

    @BeforeEach
    void setup() throws Exception {
        server = MockBukkit.mock();
        PluginMock plugin = MockBukkit.createMockPlugin();
        tasks = new TaskRunner(plugin);
        database = new Database(temp.resolve("worker-service.db"));
        new MigrationRunner(database, Logger.getLogger("test")).migrate();
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.load(new File("src/main/resources/config.yml"));
        BlockLordsConfig settings = new BlockLordsConfig(yaml);
        LogService logs = new LogService(Logger.getLogger("test"), () -> settings);
        AuditService audit = new AuditService(new AuditRepository(database), tasks, logs);

        WorldMock world = server.addSimpleWorld("world");
        player = server.addPlayer();
        VillageRepository villageRepository = new VillageRepository(database);
        village = villageRepository.createVillage("Arbeiterdorf", player.getUniqueId(),
                new ClaimRegion(world.getUID(), 0, 64, 0, 48), 4, 20, false, 21);
        VillageService villages = new VillageService(villageRepository, new LegacyRepository(database), tasks);
        villages.loadInitial();

        ConstructionRepository constructionRepository = new ConstructionRepository(database);
        StorageRepository storageRepository = new StorageRepository(database);
        StorageService storage = new StorageService(() -> settings, storageRepository, constructionRepository, tasks, audit, villages);
        storage.loadInitial(villages.all());
        ConstructionService construction = new ConstructionService(constructionRepository, villageRepository, villages, storage,
                storageRepository, new FaweBuilder(plugin, tasks, logs, () -> settings), tasks, audit);

        BuildLocation origin = new BuildLocation(world.getUID(), 10, 65, 10, 0);
        BlockLink chest = new BlockLink(world.getUID(), 12, 66, 13);
        BuildSite site = constructionRepository.createSite(village.id(), BlueprintType.FARM, origin, player.getUniqueId(), chest);
        constructionRepository.contribute(site.id(), player.getUniqueId(), ResourceType.HOLZ, 16);
        constructionRepository.contribute(site.id(), player.getUniqueId(), ResourceType.STEIN, 8);
        constructionRepository.markBuilding(site.id());
        building = constructionRepository.completeSite(site.id());
        construction.loadInitial(villages.all());

        ResidentRepository residentRepository = new ResidentRepository(database);
        residentRepository.touchAll(village.id(), List.of(
                resident(village, "Astrid", 1), resident(village, "Bodo", 2), resident(village, "Clara", 3)));
        ResidentService residents = new ResidentService(plugin, () -> settings, residentRepository, villages, tasks, audit);
        residents.loadInitial(villages.all());

        repository = new WorkerRepository(database);
        workers = new WorkerService(repository, storageRepository, construction, villages, residents, tasks, audit, () -> settings);
        workers.loadInitial(villages.all());
    }

    @AfterEach
    void close() throws Exception {
        if (tasks != null) tasks.close();
        if (database != null) database.close();
        if (server != null) MockBukkit.unmock();
    }

    @Test
    void twoWorkersAreAllowedAndThirdIsRejected() throws Exception {
        workers.assign(player, village, building.id());
        await(() -> workers.workers(village.id()).size() == 1);
        workers.assign(player, village, building.id());
        await(() -> workers.workers(village.id()).size() == 2);
        workers.assign(player, village, building.id());
        await(() -> workers.workers(village.id()).size() == 2);

        assertEquals(2, workers.capacity());
        assertEquals(2, workers.workers(village.id()).size());
    }

    @Test
    void existingAssignmentAboveLimitIsNotRemoved() throws Exception {
        for (int ignored = 0; ignored < 3; ignored++) {
            repository.assign(village.id(), UUID.randomUUID(), building.id(), Worker.Job.FARMER);
        }
        workers.loadInitial(List.of(village));
        workers.assign(player, village, building.id());
        await(() -> workers.workers(village.id()).size() == 3);
        assertEquals(3, workers.workers(village.id()).size());
    }

    private Resident resident(Village village, String name, int number) {
        return new Resident(village.id(), UUID.randomUUID(), Resident.Status.CONFIRMED,
                false, null, null, 1, 1, name, number, false);
    }

    private void await(BooleanSupplier condition) throws Exception {
        for (int attempt = 0; attempt < 200; attempt++) {
            server.getScheduler().performTicks(1);
            if (condition.getAsBoolean()) return;
            Thread.sleep(25);
        }
        fail("Asynchrone Arbeiteraktion wurde nicht abgeschlossen");
    }
}
