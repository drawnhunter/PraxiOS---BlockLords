package eu.praxios.blocklords.service;

import eu.praxios.blocklords.config.BlockLordsConfig;
import eu.praxios.blocklords.database.Database;
import eu.praxios.blocklords.database.MigrationRunner;
import eu.praxios.blocklords.model.*;
import eu.praxios.blocklords.repository.*;
import org.bukkit.Location;
import org.bukkit.Material;
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
import java.util.function.BooleanSupplier;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class ConstructionServiceTest {
    @TempDir Path temp;
    private ServerMock server;
    private TaskRunner tasks;
    private Database database;
    private ConstructionRepository repository;
    private ConstructionService construction;
    private Village village;
    private PlayerMock player;
    private WorldMock world;

    @BeforeEach
    void setup() throws Exception {
        server = MockBukkit.mock();
        PluginMock plugin = MockBukkit.createMockPlugin();
        tasks = new TaskRunner(plugin);
        database = new Database(temp.resolve("construction-service.db"));
        new MigrationRunner(database, Logger.getLogger("test")).migrate();

        YamlConfiguration yaml = new YamlConfiguration();
        yaml.load(new File("src/main/resources/config.yml"));
        BlockLordsConfig settings = new BlockLordsConfig(yaml);
        LogService logs = new LogService(Logger.getLogger("test"), () -> settings);
        AuditService audit = new AuditService(new AuditRepository(database), tasks, logs);

        VillageRepository villageRepository = new VillageRepository(database);
        ConstructionRepository constructionRepository = new ConstructionRepository(database);
        StorageRepository storageRepository = new StorageRepository(database);
        repository = constructionRepository;

        world = server.addSimpleWorld("world");
        player = server.addPlayer();
        player.teleport(new Location(world, 0, 65, 0));
        village = villageRepository.createVillage("Testdorf", player.getUniqueId(),
                new ClaimRegion(world.getUID(), 0, 64, 0, 48), 4, 20, false, 21);
        VillageService villages = new VillageService(villageRepository, new LegacyRepository(database), tasks);
        villages.loadInitial();
        StorageService storage = new StorageService(() -> settings, storageRepository, constructionRepository, tasks, audit, villages);
        storage.loadInitial(villages.all());
        FaweBuilder builder = new FaweBuilder(plugin, tasks, logs, () -> settings);
        construction = new ConstructionService(constructionRepository, villageRepository, villages, storage,
                storageRepository, builder, tasks, audit);
        construction.loadInitial(villages.all());
    }

    @AfterEach
    void close() throws Exception {
        if (tasks != null) tasks.close();
        if (database != null) database.close();
        if (server != null) MockBukkit.unmock();
    }

    @Test
    void confirmCreatesChestAtPlayerPositionAndPersistsIt() throws Exception {
        BuildLocation origin = new BuildLocation(world.getUID(), 6, 65, 0, 0);
        assertTrue(construction.previewAt(player, village, BlueprintType.RATHAUS, origin));
        assertTrue(construction.confirmPreview(player));
        await(() -> world.getBlockAt(0, 65, 0).getType() == Material.CHEST);

        List<BuildSite> sites = repository.findSites(village.id());
        assertEquals(1, sites.size());
        assertEquals(new BlockLink(world.getUID(), 0, 65, 0), sites.getFirst().chest());
        await(() -> construction.siteAtChest(world.getBlockAt(0, 65, 0)).isPresent());
    }

    @Test
    void confirmRejectsPlayerInsideBuildingBeforeCreatingSite() throws Exception {
        player.teleport(new Location(world, 6, 65, 0));
        BuildLocation origin = new BuildLocation(world.getUID(), 6, 65, 0, 0);
        assertTrue(construction.previewAt(player, village, BlueprintType.RATHAUS, origin));
        assertFalse(construction.confirmPreview(player));
        assertTrue(repository.findSites(village.id()).isEmpty());
        assertNotEquals(Material.CHEST, world.getBlockAt(6, 65, 0).getType());
    }

    private void await(BooleanSupplier condition) throws Exception {
        for (int attempt = 0; attempt < 200; attempt++) {
            server.getScheduler().performTicks(1);
            if (condition.getAsBoolean()) return;
            Thread.sleep(25);
        }
        fail("Asynchrone Baustellenaktion wurde nicht abgeschlossen");
    }
}
