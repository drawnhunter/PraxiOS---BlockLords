package eu.praxios.blocklords.service;

import eu.praxios.blocklords.config.BlockLordsConfig;
import eu.praxios.blocklords.database.Database;
import eu.praxios.blocklords.database.MigrationRunner;
import eu.praxios.blocklords.model.ClaimRegion;
import eu.praxios.blocklords.model.ResourceType;
import eu.praxios.blocklords.model.Village;
import eu.praxios.blocklords.repository.*;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.plugin.PluginMock;

import java.io.File;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.function.BooleanSupplier;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class StorageServiceTest {
    @TempDir Path temp;
    private ServerMock server;
    private TaskRunner tasks;
    private Database database;
    private StorageRepository repository;
    private StorageService storage;
    private Village village;
    private PlayerMock player;

    @BeforeEach
    void setup() throws Exception {
        server = MockBukkit.mock();
        PluginMock plugin = MockBukkit.createMockPlugin();
        tasks = new TaskRunner(plugin);
        database = new Database(temp.resolve("storage-service.db"));
        new MigrationRunner(database, Logger.getLogger("test")).migrate();
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.load(new File("src/main/resources/config.yml"));
        BlockLordsConfig settings = new BlockLordsConfig(yaml);
        LogService logs = new LogService(Logger.getLogger("test"), () -> settings);
        AuditService audit = new AuditService(new AuditRepository(database), tasks, logs);

        server.addSimpleWorld("world");
        player = server.addPlayer();
        VillageRepository villageRepository = new VillageRepository(database);
        village = villageRepository.createVillage("Lagerdorf", player.getUniqueId(),
                new ClaimRegion(player.getWorld().getUID(), 0, 64, 0, 48), 4, 20, false, 21);
        VillageService villages = new VillageService(villageRepository, new LegacyRepository(database), tasks);
        villages.loadInitial();
        repository = new StorageRepository(database);
        storage = new StorageService(() -> settings, repository, new ConstructionRepository(database), tasks, audit, villages);
        storage.loadInitial(villages.all());
    }

    @AfterEach
    void close() throws Exception {
        if (tasks != null) tasks.close();
        if (database != null) database.close();
        if (server != null) MockBukkit.unmock();
    }

    @Test
    void fullStorageRefundsItemsAndReportsError() throws Exception {
        repository.deposit(village.id(), player.getUniqueId(), ResourceType.HOLZ, 1728, 1728);
        player.getInventory().addItem(new ItemStack(Material.OAK_LOG, 1));
        storage.depositAll(player, village);
        await(() -> player.getInventory().contains(Material.OAK_LOG, 1) && messageContains("voll"));
        assertTrue(player.getInventory().contains(Material.OAK_LOG, 1));
    }

    @Test
    void emptyWithdrawReportsError() throws Exception {
        storage.withdraw(player, village, ResourceType.HOLZ, 1);
        await(() -> messageContains("nicht im Dorfvorrat"));
    }

    @Test
    void normalDepositAndWithdrawWorks() throws Exception {
        player.getInventory().addItem(new ItemStack(Material.OAK_LOG, 32));
        storage.depositAll(player, village);
        await(() -> storage.balances(village.id()).getOrDefault(ResourceType.HOLZ, 0) == 32);
        assertEquals(32, repository.balances(village.id()).get(ResourceType.HOLZ));

        storage.withdraw(player, village, ResourceType.HOLZ, 64);
        await(() -> player.getInventory().contains(Material.OAK_LOG, 32));
        assertEquals(32, player.getInventory().getItem(0).getAmount());
        assertEquals(0, repository.balances(village.id()).getOrDefault(ResourceType.HOLZ, 0));
    }

    private boolean messageContains(String text) {
        try {
            net.kyori.adventure.text.Component message = player.nextComponentMessage();
            return message != null && PlainTextComponentSerializer.plainText().serialize(message).contains(text);
        } catch (NoSuchElementException exception) {
            return false;
        }
    }

    private void await(BooleanSupplier condition) throws Exception {
        for (int attempt = 0; attempt < 200; attempt++) {
            server.getScheduler().performTicks(1);
            if (condition.getAsBoolean()) return;
            Thread.sleep(25);
        }
        fail("Asynchrone Lageraktion wurde nicht abgeschlossen");
    }
}
