package eu.praxios.blocklords.service;

import eu.praxios.blocklords.config.BlockLordsConfig;
import eu.praxios.blocklords.database.Database;
import eu.praxios.blocklords.database.MigrationRunner;
import eu.praxios.blocklords.model.ClaimRegion;
import eu.praxios.blocklords.model.Resident;
import eu.praxios.blocklords.model.Village;
import eu.praxios.blocklords.repository.LegacyRepository;
import eu.praxios.blocklords.repository.ResidentRepository;
import eu.praxios.blocklords.repository.VillageRepository;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class ResidentServiceTest {
    @TempDir Path temp;
    private ServerMock server;
    private TaskRunner tasks;
    private Database database;
    private ResidentRepository repository;
    private ResidentService residents;
    private ResidentRepository residentRepository;
    private Village village;
    private PlayerMock player;

    @BeforeEach
    void setup() throws Exception {
        server = MockBukkit.mock();
        PluginMock plugin = MockBukkit.createMockPlugin();
        tasks = new TaskRunner(plugin);
        database = new Database(temp.resolve("resident-service.db"));
        new MigrationRunner(database, Logger.getLogger("test")).migrate();
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.load(new File("src/main/resources/config.yml"));
        BlockLordsConfig settings = new BlockLordsConfig(yaml);

        WorldMock world = server.addSimpleWorld("world");
        player = server.addPlayer();
        VillageRepository villageRepository = new VillageRepository(database);
        village = villageRepository.createVillage("Versammlung", player.getUniqueId(),
                new ClaimRegion(world.getUID(), 0, 64, 0, 48), 4, 20, false, 21);
        VillageService villages = new VillageService(villageRepository, new LegacyRepository(database), tasks);
        villages.loadInitial();
        residentRepository = new ResidentRepository(database);
        repository = residentRepository;
        residents = new ResidentService(plugin, () -> settings, residentRepository, villages, tasks);
        residents.loadInitial(villages.all());

        world.spawnEntity(new Location(world, 1, 65, 1), EntityType.VILLAGER);
        world.spawnEntity(new Location(world, 2, 65, 1), EntityType.VILLAGER);
        world.spawnEntity(new Location(world, 3, 65, 1), EntityType.VILLAGER);
    }

    @AfterEach
    void close() throws Exception {
        if (tasks != null) tasks.close();
        if (database != null) database.close();
        if (server != null) MockBukkit.unmock();
    }

    @Test
    void assemblyAllocatesUniqueNumbersInOneBatch() throws Exception {
        residents.assemble(player, village);
        await(() -> residents.residents(village.id()).size() == 3);
        List<Integer> numbers = repository.find(village.id()).stream().map(Resident::number).sorted().toList();
        assertEquals(List.of(1, 2, 3), numbers);
    }

    private void await(BooleanSupplier condition) throws Exception {
        for (int attempt = 0; attempt < 200; attempt++) {
            server.getScheduler().performTicks(1);
            if (condition.getAsBoolean()) return;
            Thread.sleep(25);
        }
        fail("Asynchrone Bewohneraktion wurde nicht abgeschlossen");
    }
}
