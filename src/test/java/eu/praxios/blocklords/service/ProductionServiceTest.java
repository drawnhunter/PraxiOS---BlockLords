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
import eu.praxios.blocklords.ui.ActionMenuHolder;
import eu.praxios.blocklords.ui.UiAction;
import eu.praxios.blocklords.ui.UiService;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.io.File;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class ProductionServiceTest {
    @TempDir Path temp;
    private ServerMock server;
    private PluginMock plugin;
    private TaskRunner tasks;
    private Database database;
    private VillageRepository villageRepository;
    private StorageRepository storageRepository;
    private WorkerRepository workerRepository;
    private ProductionReportRepository reportRepository;
    private StorageService storageService;
    private ProductionService production;
    private Village village;
    private PlayerMock player;
    private UiService ui;
    private AtomicLong clock;
    private long start;

    @BeforeEach
    void setup() throws Exception {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin();
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
        server.addSimpleWorld("world");
        player = server.addPlayer();
        ui = new UiService(() -> settings, villages, null, null, tasks, null, storageService,
                null, null, null, null, null, null, production);
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
        assertTrue(report.pauses().contains("Weizen: 6 von 6 nicht eingelagert – Lager voll"));
        assertEquals(1_728, storageRepository.balances(village.id()).get(ResourceType.WEIZEN));
        assertEquals(1_728, storageRepository.balances(village.id()).get(ResourceType.NAHRUNG));
    }

    @Test
    void productionMenuExplainsAmountsAndReplacesLegacyCapacityText() throws Exception {
        saveExampleReport();
        storageRepository.deposit(village.id(), village.owner(), ResourceType.NAHRUNG, 1_728, 1_728);
        storageRepository.deposit(village.id(), village.owner(), ResourceType.WEIZEN, 1_728, 1_728);
        storageRepository.deposit(village.id(), village.owner(), ResourceType.STEIN, 64, 1_728);
        storageService.loadInitial(java.util.List.of(village));
        ui.openProduction(player, village);
        await(() -> action("Nahrung · 308 eingelagert") != null);

        UiAction food = action("Nahrung · 308 eingelagert");
        assertTrue(food.lore().containsAll(List.of("Geplant: 768", "Eingelagert: 308", "Nicht eingelagert: 460",
                "Bestand: 1728/1728 pro Ressource", "Teilweise eingelagert – Lager voll.")));
        var holder = (ActionMenuHolder) player.getOpenInventory().getTopInventory().getHolder();
        var foodItem = player.getOpenInventory().getTopInventory().getItem(holder.actions().indexOf(food));
        assertNotNull(foodItem);
        assertNotNull(foodItem.getItemMeta().displayName());
        assertTrue(PlainTextComponentSerializer.plainText().serialize(foodItem.getItemMeta().displayName())
                .contains("Nahrung · 308 eingelagert"));

        UiAction wheat = action("Weizen · 0 eingelagert");
        assertNotNull(wheat);
        assertTrue(wheat.lore().contains("Nichts eingelagert – Lager voll."));
        assertFalse(wheat.lore().contains("Nur teilweise eingelagert; Lagergrund prüfen."));
        UiAction stone = action("Stein · 64 eingelagert");
        assertNotNull(stone);
        assertTrue(stone.lore().contains("Vollständig eingelagert."));

        UiAction balance = action("Nahrungsbilanz · +116 netto");
        assertNotNull(balance);
        assertTrue(balance.lore().containsAll(List.of("Eingelagert: +308", "Verbrauch: -192",
                "Netto = eingelagert - Verbrauch")));
        UiAction pauses = action("Pausen und Gründe");
        assertNotNull(pauses);
        assertEquals(3, pauses.lore().size());
        assertTrue(pauses.lore().containsAll(List.of("Minenarbeiter: kein Arbeiter",
                "Nahrung: 460 von 768 nicht eingelagert – Lager voll",
                "Weizen: 1152 von 1152 nicht eingelagert – Lager voll")));
        assertFalse(pauses.lore().stream().anyMatch(reason -> reason.contains("Lager voll (")));
    }

    @Test
    void joinReportMirrorsSameTextAndSkipsMissingReport() throws Exception {
        List<String> logs = new ArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (record.getMessage() != null && record.getMessage().contains("DORFBERICHT")) logs.add(record.getMessage());
            }

            @Override public void flush() { }
            @Override public void close() { }
        };
        plugin.getLogger().addHandler(handler);
        try {
            production.sendJoinReport(player, village);
            settleDatabase();
            assertTrue(logs.isEmpty());
            assertFalse(messageContains("Dorfbericht"));

            saveExampleReport();
            production.sendJoinReport(player, village);
            await(() -> !logs.isEmpty());
            String prefix = "DORFBERICHT | " + player.getName() + " | ";
            int prefixStart = logs.getFirst().indexOf(prefix);
            assertTrue(prefixStart >= 0);
            String consoleText = logs.getFirst().substring(prefixStart + prefix.length());
            var playerMessage = player.nextComponentMessage();
            assertNotNull(playerMessage);
            assertTrue(PlainTextComponentSerializer.plainText().serialize(playerMessage).endsWith(consoleText));
            assertTrue(consoleText.contains("Dorfbericht 'Produktionsdorf':"));
            assertTrue(consoleText.contains("+308 Nahrung"));

            logs.clear();
            player.disconnect();
            production.sendJoinReport(player, village);
            settleDatabase();
            assertTrue(logs.isEmpty());
        } finally {
            plugin.getLogger().removeHandler(handler);
        }
    }

    private void saveExampleReport() throws Exception {
        reportRepository.save(new ProductionReport(village.id(), clock.get(), clock.get(), true, 64.0, 2, 0, 192,
                List.of("Minenarbeiter: kein Arbeiter", "Nahrung: Lager voll (308/768)",
                        "Weizen: Lager voll (0/1152)"),
                List.of(new ProductionReport.Entry(ResourceType.NAHRUNG, 768, 308),
                        new ProductionReport.Entry(ResourceType.WEIZEN, 1152, 0),
                        new ProductionReport.Entry(ResourceType.STEIN, 64, 64))));
    }

    private UiAction action(String name) {
        var top = player.getOpenInventory().getTopInventory();
        if (top == null || !(top.getHolder() instanceof ActionMenuHolder holder)) return null;
        return holder.actions().stream().filter(action -> action.name().startsWith(name)).findFirst().orElse(null);
    }

    private boolean messageContains(String text) {
        try {
            var message = player.nextComponentMessage();
            return message != null && PlainTextComponentSerializer.plainText().serialize(message).contains(text);
        } catch (java.util.NoSuchElementException exception) {
            return false;
        }
    }

    private void settleDatabase() throws Exception {
        for (int pass = 0; pass < 2; pass++) {
            AtomicLong completed = new AtomicLong();
            tasks.database(() -> null, ignored -> completed.set(1), error -> {
                throw new AssertionError(error);
            });
            await(() -> completed.get() == 1);
        }
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
