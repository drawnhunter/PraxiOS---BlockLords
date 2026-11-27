package eu.praxios.blocklords.service;

import eu.praxios.blocklords.config.BlockLordsConfig;
import eu.praxios.blocklords.database.Database;
import eu.praxios.blocklords.database.MigrationRunner;
import eu.praxios.blocklords.model.*;
import eu.praxios.blocklords.repository.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.type.Bed;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Villager;
import org.bukkit.entity.memory.MemoryKey;
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
import java.sql.PreparedStatement;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class ImmigrationServiceTest {
    @TempDir Path temp;
    private ServerMock server;
    private TaskRunner tasks;
    private Database database;
    private VillageRepository villageRepository;
    private ResidentRepository residentRepository;
    private StorageRepository storageRepository;
    private ImmigrationRepository immigrationRepository;
    private ResidentService residents;
    private StorageService storage;
    private VillageScanner scanner;
    private ImmigrationService immigration;
    private Village village;
    private PlayerMock player;
    private WorldMock world;
    private AtomicLong clock;

    @BeforeEach
    void setup() throws Exception {
        server = MockBukkit.mock();
        PluginMock plugin = MockBukkit.createMockPlugin();
        tasks = new TaskRunner(plugin);
        database = new Database(temp.resolve("immigration-service.db"));
        new MigrationRunner(database, Logger.getLogger("test")).migrate();
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.load(new File("src/main/resources/config.yml"));
        BlockLordsConfig settings = new BlockLordsConfig(yaml);
        AuditService audit = new AuditService(new AuditRepository(database), tasks,
                new LogService(Logger.getLogger("test"), () -> settings));
        clock = new AtomicLong(1_000_000L);

        world = server.addSimpleWorld("world");
        player = server.addPlayer();
        player.teleport(new Location(world, 0, 65, 0));
        villageRepository = new VillageRepository(database);
        village = villageRepository.createVillage("Einwanderungsdorf", player.getUniqueId(),
                new ClaimRegion(world.getUID(), 0, 64, 0, 48), 4, 20, false, 21);
        village = villageRepository.addXp(village.id(), 100);
        VillageService villages = new VillageService(villageRepository, new LegacyRepository(database), tasks);
        villages.loadInitial();
        residentRepository = new ResidentRepository(database);
        scanner = new VillageScanner(plugin, () -> settings, tasks);
        residents = new ResidentService(plugin, () -> settings, residentRepository, villages, tasks, audit, scanner);
        residents.loadInitial(villages.all());
        storageRepository = new StorageRepository(database);
        ConstructionRepository constructionRepository = new ConstructionRepository(database);
        storage = new StorageService(() -> settings, storageRepository, constructionRepository, tasks, audit, villages);
        storage.loadInitial(villages.all());
        ConstructionService construction = new ConstructionService(constructionRepository, villageRepository, villages,
                storage, storageRepository, new FaweBuilder(plugin, tasks,
                new LogService(Logger.getLogger("test"), () -> settings), () -> settings), tasks, audit);
        createFarm();
        construction.loadInitial(villages.all());
        WorkerService workers = new WorkerService(new WorkerRepository(database), storageRepository, construction,
                villages, residents, tasks, audit, () -> settings);
        workers.loadInitial(villages.all());
        immigrationRepository = new ImmigrationRepository(database, villageRepository, storageRepository, residentRepository);
        immigration = new ImmigrationService(plugin, () -> settings, immigrationRepository, villages, residents, workers,
                storage, scanner, tasks, audit, clock::get);

        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) world.getBlockAt(x, 63, z).setType(Material.STONE);
        }
        world.getBlockAt(4, 65, 0).setType(Material.RED_BED);
        var bed = (Bed) world.getBlockAt(4, 65, 0).getBlockData();
        bed.setPart(Bed.Part.FOOT);
        world.getBlockAt(4, 65, 0).setBlockData(bed);
        world.getChunkAt(0, 0).load();
        scanner.request(village, ignored -> { });
        await(() -> scanner.fresh(village).isPresent() && residents.freeBed(village).isPresent());
        storageRepository.deposit(village.id(), player.getUniqueId(), ResourceType.NAHRUNG, 64, 1_728);
        storage.loadInitial(villages.all());
    }

    @AfterEach
    void close() throws Exception {
        if (tasks != null) tasks.close();
        if (database != null) database.close();
        if (server != null) MockBukkit.unmock();
    }

    @Test
    void wandererArrivesOnceAndAcceptanceCreatesConfirmedResident() throws Exception {
        immigration.runVillage(village);
        await(() -> immigrationRepository.snapshot(village.id()).state().nextArrivalAt() > clock.get());
        clock.addAndGet(300_000L);
        immigration.runVillage(village);
        await(() -> immigrationRepository.snapshot(village.id()).waiting().isPresent());
        assertEquals(1, world.getEntitiesByClass(Villager.class).size());
        immigration.runVillage(village);
        assertEquals(1, world.getEntitiesByClass(Villager.class).size());

        long candidateId = immigrationRepository.snapshot(village.id()).waiting().orElseThrow().id();
        immigration.accept(player, village, candidateId, () -> { });
        await(() -> residents.living(village.id()).size() == 1);
        Resident resident = residents.living(village.id()).getFirst();
        assertEquals(1, resident.number());
        assertNotNull(resident.bed());
        assertNotNull(resident.meetingPoint());
        assertNotNull(((Villager) world.getEntity(resident.villager())).getMemory(MemoryKey.HOME));
        assertNotNull(((Villager) world.getEntity(resident.villager())).getMemory(MemoryKey.MEETING_POINT));
        assertEquals(48, storageRepository.balances(village.id()).get(ResourceType.NAHRUNG));
        assertEquals(130, villageRepository.findById(village.id()).orElseThrow().xp());
        assertTrue(immigrationRepository.snapshot(village.id()).waiting().isEmpty());

        immigration.accept(player, village, candidateId, () -> { });
        assertEquals(48, storageRepository.balances(village.id()).get(ResourceType.NAHRUNG));
        assertEquals(130, villageRepository.findById(village.id()).orElseThrow().xp());
        assertEquals(1, residents.living(village.id()).size());

        world.getBlockAt(5, 65, 0).setType(Material.RED_BED);
        var secondBed = (Bed) world.getBlockAt(5, 65, 0).getBlockData();
        secondBed.setPart(Bed.Part.FOOT);
        world.getBlockAt(5, 65, 0).setBlockData(secondBed);
        Villager vanilla = (Villager) world.spawnEntity(new Location(world, 2, 65, 1), org.bukkit.entity.EntityType.VILLAGER);
        vanilla.setMemory(MemoryKey.HOME, new Location(world, 5, 65, 0));
        residents.assemble(player, village);
        await(() -> residents.living(village.id()).size() == 2);
        assertEquals(java.util.List.of(1, 2), residents.living(village.id()).stream()
                .map(Resident::number).sorted().toList());
    }

    @Test
    void missingFoodBlocksArrivalWithoutCosts() throws Exception {
        storageRepository.withdraw(village.id(), player.getUniqueId(), ResourceType.NAHRUNG, 64);
        storage.loadInitial(java.util.List.of(village));
        immigration.runVillage(village);
        await(() -> immigrationRepository.snapshot(village.id()).state().reason().contains("Nahrung"));
        clock.addAndGet(300_000L);
        immigration.runVillage(village);
        assertTrue(immigrationRepository.snapshot(village.id()).waiting().isEmpty());
        assertEquals(0, world.getEntitiesByClass(Villager.class).size());
        assertEquals(100, villageRepository.findById(village.id()).orElseThrow().xp());
    }

    @Test
    void deathAllowsRetryWithoutDuplicateOrFoodCost() throws Exception {
        immigration.runVillage(village);
        await(() -> immigrationRepository.snapshot(village.id()).state().nextArrivalAt() > clock.get());
        clock.addAndGet(300_000L);
        immigration.runVillage(village);
        await(() -> immigrationRepository.snapshot(village.id()).waiting().isPresent());
        UUID oldId = immigrationRepository.snapshot(village.id()).waiting().orElseThrow().villagerId();
        world.getEntity(oldId).remove();
        immigration.onDeath(oldId);
        await(() -> immigrationRepository.snapshot(village.id()).waiting().isEmpty());
        assertEquals(64, storageRepository.balances(village.id()).get(ResourceType.NAHRUNG));

        await(() -> immigrationRepository.snapshot(village.id()).state().nextArrivalAt() > clock.get());
        clock.set(immigrationRepository.snapshot(village.id()).state().nextArrivalAt());
        immigration.runVillage(village);
        await(() -> world.getEntitiesByClass(Villager.class).size() == 1);
        awaitDatabase(() -> immigrationRepository.snapshot(village.id()).waiting().isPresent());
        UUID newId = immigrationRepository.snapshot(village.id()).waiting().orElseThrow().villagerId();
        assertNotEquals(oldId, newId);
        assertEquals(1, world.getEntitiesByClass(Villager.class).size());
        long retryAt = clock.get() + 60_000L;
        immigration.reject(player, village, immigrationRepository.snapshot(village.id()).waiting().orElseThrow().id(), () -> { });
        await(() -> immigrationRepository.snapshot(village.id()).waiting().isEmpty()
                && world.getEntitiesByClass(Villager.class).isEmpty()
                && immigrationRepository.snapshot(village.id()).state().nextArrivalAt() == retryAt,
                () -> " · erwarteterRetry=" + retryAt + " · Snapshot=" + immigrationRepository.snapshot(village.id())
                        + " · NPCs=" + world.getEntitiesByClass(Villager.class).stream()
                        .map(entity -> entity.getUniqueId().toString()).toList());
        assertEquals(0, world.getEntitiesByClass(Villager.class).size(),
                () -> "Verbleibende Wanderer: " + world.getEntitiesByClass(Villager.class).stream()
                        .map(entity -> entity.getUniqueId().toString()).toList() + " · alt=" + oldId + " · neu=" + newId);
        assertEquals(64, storageRepository.balances(village.id()).get(ResourceType.NAHRUNG));
        assertTrue(immigrationRepository.snapshot(village.id()).state().reason().contains("abgelehnt"));

        immigration.runVillage(village);
        settleDatabase();
        assertEquals(retryAt, immigrationRepository.snapshot(village.id()).state().nextArrivalAt());
        assertTrue(immigrationRepository.snapshot(village.id()).state().reason().contains("abgelehnt"));
        assertTrue(immigrationRepository.snapshot(village.id()).waiting().isEmpty());
    }

    @Test
    void restingVillageDoesNotSpawnImmigrants() throws Exception {
        village = new Village(village.id(), village.name(), village.owner(), village.region(), village.level(), village.xp(),
                VillageStatus.RESTING, village.createdAt(), village.lastActivity());
        immigration.runVillage(village);
        await(() -> immigrationRepository.snapshot(village.id()).state().reason().contains("ruht"));
        clock.addAndGet(300_000L);
        immigration.runVillage(village);
        assertTrue(immigrationRepository.snapshot(village.id()).waiting().isEmpty());
        assertEquals(0, world.getEntitiesByClass(Villager.class).size());
    }

    private void createFarm() throws Exception {
        database.inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO buildings(village_id, type, level, world_uuid, x, y, z, rotation, created_at) VALUES (?, 'FARM', 1, ?, 5, 64, 5, 0, 1)")) {
                statement.setLong(1, village.id());
                statement.setString(2, world.getUID().toString());
                statement.executeUpdate();
            }
            return null;
        });
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

    private void awaitDatabase(CheckedCondition condition) throws Exception {
        for (int attempt = 0; attempt < 200; attempt++) {
            if (condition.getAsBoolean()) return;
            Thread.sleep(25);
        }
        fail("Datenbankzustand wurde nicht erreicht · Snapshot=" + immigrationRepository.snapshot(village.id())
                + " · NPCs=" + world.getEntitiesByClass(Villager.class).stream()
                .map(entity -> entity.getUniqueId().toString()).toList());
    }

    private void await(CheckedCondition condition) throws Exception {
        await(condition, () -> " · Snapshot=" + immigrationRepository.snapshot(village.id())
                + " · NPCs=" + world.getEntitiesByClass(Villager.class).stream()
                .map(entity -> entity.getUniqueId().toString()).toList());
    }

    private void await(CheckedCondition condition, CheckedSupplier<String> details) throws Exception {
        for (int attempt = 0; attempt < 200; attempt++) {
            server.getScheduler().performTicks(1);
            if (condition.getAsBoolean()) return;
            Thread.sleep(25);
        }
        fail("Asynchrone Einwanderungsaktion wurde nicht abgeschlossen" + details.get());
    }

    private interface CheckedCondition {
        boolean getAsBoolean() throws Exception;
    }

    private interface CheckedSupplier<T> {
        T get() throws Exception;
    }
}
