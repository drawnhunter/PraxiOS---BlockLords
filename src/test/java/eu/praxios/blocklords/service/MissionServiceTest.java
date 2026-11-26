package eu.praxios.blocklords.service;

import eu.praxios.blocklords.config.BlockLordsConfig;
import eu.praxios.blocklords.database.Database;
import eu.praxios.blocklords.database.MigrationRunner;
import eu.praxios.blocklords.model.*;
import eu.praxios.blocklords.repository.*;
import eu.praxios.blocklords.ui.ActionMenuHolder;
import eu.praxios.blocklords.ui.UiAction;
import eu.praxios.blocklords.ui.UiService;
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
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class MissionServiceTest {
    @TempDir Path temp;
    private ServerMock server;
    private TaskRunner tasks;
    private Database database;
    private VillageRepository villageRepository;
    private MissionRepository missionRepository;
    private VillageService villages;
    private Village village;
    private PlayerMock player;
    private MissionService missions;
    private StorageService storage;
    private UiService ui;

    @BeforeEach
    void setup() throws Exception {
        server = MockBukkit.mock();
        PluginMock plugin = MockBukkit.createMockPlugin();
        tasks = new TaskRunner(plugin);
        database = new Database(temp.resolve("mission-service.db"));
        new MigrationRunner(database, Logger.getLogger("test")).migrate();
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.load(new File("src/main/resources/config.yml"));
        yaml.set("missions.personal-offers", 3);
        BlockLordsConfig settings = new BlockLordsConfig(yaml);
        AuditService audit = new AuditService(new AuditRepository(database), tasks,
                new LogService(Logger.getLogger("test"), () -> settings));

        server.addSimpleWorld("world");
        player = server.addPlayer();
        player.teleport(player.getWorld().getSpawnLocation());
        villageRepository = new VillageRepository(database);
        village = villageRepository.createVillage("Spieledorf", player.getUniqueId(),
                new ClaimRegion(player.getWorld().getUID(), 0, 64, 0, 48), 4, 20, false, 21);
        villages = new VillageService(villageRepository, new LegacyRepository(database), tasks);
        villages.loadInitial();
        missionRepository = new MissionRepository(database, villageRepository);
        missions = new MissionService(() -> settings, missionRepository, villages, tasks, audit);
        StorageRepository storageRepository = new StorageRepository(database, missionRepository);
        storage = new StorageService(() -> settings, storageRepository,
                new ConstructionRepository(database, missionRepository), tasks, audit, villages, missions::announceCompletions);
        storage.loadInitial(villages.all());
        ui = new UiService(() -> settings, villages, null, null, tasks,
                null, null, null, null, null, null, missions);
    }

    @AfterEach
    void close() throws Exception {
        if (tasks != null) tasks.close();
        if (database != null) database.close();
        if (server != null) MockBukkit.unmock();
    }

    @Test
    void menuDepositCompletionAndRepeatUseRealPlayerActions() throws Exception {
        prepareOffers();
        ui.openMissions(player, village);
        await(() -> action("Holz beisteuern") != null);
        action("Holz beisteuern").action().run();
        await(() -> action("Mission annehmen") != null);
        action("Mission annehmen").action().run();
        await(() -> mission(MissionType.HOLZ_32).status() == Mission.Status.ACTIVE);
        long acceptedId = mission(MissionType.HOLZ_32).id();

        player.getInventory().addItem(new ItemStack(Material.OAK_LOG, 16));
        storage.deposit(player, village, ResourceType.HOLZ, 16);
        await(() -> missionRepository.accessible(village.id(), player.getUniqueId(), acceptedId)
                .orElseThrow().mission().progress() == 16);
        assertEquals(0, villageRepository.findById(village.id()).orElseThrow().xp());
        ui.openMissions(player, village);
        await(() -> action("Holz beisteuern · 16/32") != null);

        player.getInventory().addItem(new ItemStack(Material.OAK_LOG, 16));
        storage.deposit(player, village, ResourceType.HOLZ, 16);
        await(() -> missionRepository.accessible(village.id(), player.getUniqueId(), acceptedId)
                .orElseThrow().mission().status() == Mission.Status.COMPLETED
                && villages.byId(village.id()).orElseThrow().xp() == 20);
        await(() -> messageContains("+20 Dorf-XP"));
        assertTrue(missionRepository.takeUnnotified(village.id(), 2_000L).isEmpty());

        Mission repeated = accept(MissionType.HOLZ_32);
        player.getInventory().addItem(new ItemStack(Material.OAK_LOG, 32));
        storage.deposit(player, village, ResourceType.HOLZ, 32);
        await(() -> missionRepository.accessible(village.id(), player.getUniqueId(), repeated.id())
                .orElseThrow().mission().status() == Mission.Status.COMPLETED);
        await(() -> messageContains("keine weiteren Dorf-XP"));
        assertEquals(20, villageRepository.findById(village.id()).orElseThrow().xp());
        assertTrue(missionRepository.takeUnnotified(village.id(), 3_000L).isEmpty());
    }

    @Test
    void replacementNeedsConfirmationAndKeepsStoredResources() throws Exception {
        Mission active = accept(MissionType.STEIN_32);
        player.getInventory().addItem(new ItemStack(Material.COBBLESTONE, 16));
        storage.deposit(player, village, ResourceType.STEIN, 16);
        await(() -> missionRepository.accessible(village.id(), player.getUniqueId(), active.id())
                .orElseThrow().mission().progress() == 16);
        await(() -> storage.balances(village.id()).getOrDefault(ResourceType.STEIN, 0) == 16);

        missions.replace(player, village, active.id(), false, () -> { });
        await(() -> messageContains("Bestätige"));
        assertEquals(Mission.Status.ACTIVE, missionRepository.accessible(village.id(), player.getUniqueId(), active.id())
                .orElseThrow().mission().status());
        assertEquals(16, storage.balances(village.id()).get(ResourceType.STEIN));

        missions.replace(player, village, active.id(), true, () -> { });
        await(() -> missionRepository.accessible(village.id(), player.getUniqueId(), active.id())
                .orElseThrow().mission().status() == Mission.Status.REPLACED);
        assertEquals(16, storage.balances(village.id()).get(ResourceType.STEIN));
        assertEquals(0, villageRepository.findById(village.id()).orElseThrow().xp());
    }

    @Test
    void pinnedJoinHintAndStrangerRejectionWork() throws Exception {
        Mission active = accept(MissionType.STEIN_32);
        missions.pin(player, village, active.id(), () -> { });
        await(() -> missionRepository.pinned(village.id(), player.getUniqueId()).isPresent());
        drainMessages();
        missions.sendPinnedHint(player);
        await(() -> messageContains("Weiter mit 'Stein beisteuern'"));

        PlayerMock stranger = server.addPlayer();
        ui.openMissions(stranger, village);
        var rejection = stranger.nextComponentMessage();
        assertNotNull(rejection);
        assertTrue(PlainTextComponentSerializer.plainText().serialize(rejection).contains("Missionen sind"));
    }

    private void prepareOffers() throws Exception {
        List<MissionDefinition> definitions = Arrays.stream(MissionType.values()).map(MissionDefinition::defaults).toList();
        missionRepository.prepareOffers(village.id(), player.getUniqueId(), 3, 1, definitions,
                System.currentTimeMillis(), 86_400_000L);
    }

    private Mission accept(MissionType type) throws Exception {
        Mission offered = missionRepository.prepareOffers(village.id(), player.getUniqueId(), 3, 1,
                        Arrays.stream(MissionType.values()).map(MissionDefinition::defaults).toList(),
                        System.currentTimeMillis(), 86_400_000L)
                .stream().map(MissionView::mission).filter(mission -> mission.type() == type).findFirst().orElseThrow();
        missions.accept(player, village, offered.id(), () -> { });
        await(() -> missionRepository.accessible(village.id(), player.getUniqueId(), offered.id())
                .orElseThrow().mission().status() == Mission.Status.ACTIVE);
        return missionRepository.accessible(village.id(), player.getUniqueId(), offered.id()).orElseThrow().mission();
    }

    private Mission mission(MissionType type) throws Exception {
        return missionRepository.current(village.id(), player.getUniqueId()).stream().map(MissionView::mission)
                .filter(mission -> mission.type() == type).findFirst().orElseThrow();
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
        } catch (NoSuchElementException exception) {
            return false;
        }
    }

    private void drainMessages() {
        try {
            while (player.nextComponentMessage() != null) {
            }
        } catch (NoSuchElementException ignored) {
        }
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
        fail("Asynchrone Missionsaktion wurde nicht abgeschlossen");
    }
}
