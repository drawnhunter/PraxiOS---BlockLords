package eu.praxios.blocklords.database;

import eu.praxios.blocklords.model.ClaimRegion;
import eu.praxios.blocklords.model.Mission;
import eu.praxios.blocklords.model.MissionDefinition;
import eu.praxios.blocklords.model.MissionType;
import eu.praxios.blocklords.model.Resident;
import eu.praxios.blocklords.model.ResourceType;
import eu.praxios.blocklords.model.Village;
import eu.praxios.blocklords.repository.ConstructionRepository;
import eu.praxios.blocklords.repository.ImmigrationRepository;
import eu.praxios.blocklords.repository.MissionRepository;
import eu.praxios.blocklords.repository.ProductionReportRepository;
import eu.praxios.blocklords.repository.ResidentRepository;
import eu.praxios.blocklords.repository.StorageRepository;
import eu.praxios.blocklords.repository.VillageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class MigrationUpgradeTest {
    @TempDir Path temp;

    @Test
    void v5DatabaseUpgradesWithoutBreakingLegacyRows() throws Exception {
        try (Database database = new Database(temp.resolve("v5-upgrade.db"))) {
            applyV5(database);
            UUID owner = UUID.randomUUID();
            UUID world = UUID.randomUUID();
            Village village = new VillageRepository(database).createVillage("Altdorf", owner,
                    new ClaimRegion(world, 0, 64, 0, 48), 4, 20, false, 21);
            UUID villager = UUID.randomUUID();
            database.inTransaction(connection -> {
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO residents(village_id, villager_uuid, status, has_bed, first_seen, last_seen, display_name, resident_number) "
                                + "VALUES (?, ?, 'CONFIRMED', 0, 1, 1, 'Astrid', 1)")) {
                    statement.setLong(1, village.id());
                    statement.setString(2, villager.toString());
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO build_sites(village_id, type, world_uuid, x, y, z, rotation, status, created_by, created_at) "
                                + "VALUES (?, 'RATHAUS', ?, 10, 65, 10, 0, 'COLLECTING', ?, 1)")) {
                    statement.setLong(1, village.id());
                    statement.setString(2, world.toString());
                    statement.setString(3, owner.toString());
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO buildings(village_id, type, level, world_uuid, x, y, z, rotation, created_at) "
                                + "VALUES (?, 'FARM', 1, ?, 20, 65, 20, 0, 1)")) {
                    statement.setLong(1, village.id());
                    statement.setString(2, world.toString());
                    statement.executeUpdate();
                }
                return null;
            });

            new MigrationRunner(database, Logger.getLogger("test")).migrate();

            assertTrue(columnExists(database, "residents", "dead"));
            assertTrue(columnExists(database, "build_sites", "chest_world"));
            assertTrue(columnExists(database, "buildings", "chest_world"));
            assertFalse(new ResidentRepository(database).find(village.id()).getFirst().dead());
            ConstructionRepository construction = new ConstructionRepository(database);
            assertNull(construction.findSites(village.id()).getFirst().chest());
            assertNull(construction.findBuildings(village.id()).getFirst().chest());
        }
    }

    private void applyV5(Database database) throws Exception {
        database.inTransaction(connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE schema_migrations (version INTEGER PRIMARY KEY, name TEXT NOT NULL, applied_at INTEGER NOT NULL)");
            }
            return null;
        });
        List<String> migrations = List.of(
                "V1__phase1_foundation.sql",
                "V2__phase2_economy.sql",
                "V3__residents.sql",
                "V4__placement_sessions.sql",
                "V5__resident_names.sql");
        for (int index = 0; index < migrations.size(); index++) {
            int version = index + 1;
            String name = migrations.get(index);
            database.inTransaction(connection -> {
                for (String sql : loadStatements(name)) {
                    try (Statement statement = connection.createStatement()) {
                        statement.execute(sql);
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO schema_migrations(version, name, applied_at) VALUES (?, ?, 1)")) {
                    statement.setInt(1, version);
                    statement.setString(2, name);
                    statement.executeUpdate();
                }
                return null;
            });
        }
    }

    @Test
    void v6DatabaseUpgradesToMissionsWithoutRetroactiveRewards() throws Exception {
        try (Database database = new Database(temp.resolve("v6-missions-upgrade.db"))) {
            applyV6(database);
            UUID owner = UUID.randomUUID();
            VillageRepository villages = new VillageRepository(database);
            Village village = villages.createVillage("Bestandsdorf", owner,
                    new ClaimRegion(UUID.randomUUID(), 0, 64, 0, 48), 4, 20, false, 21);
            StorageRepository storage = new StorageRepository(database);
            storage.deposit(village.id(), owner, ResourceType.HOLZ, 64, 1_728);
            villages.addXp(village.id(), 40);

            new MigrationRunner(database, Logger.getLogger("test")).migrate();

            assertTrue(columnExists(database, "missions", "id"));
            assertTrue(columnExists(database, "mission_events", "id"));
            assertTrue(columnExists(database, "mission_event_links", "mission_id"));
            assertTrue(columnExists(database, "mission_xp_grants", "mission_id"));
            assertEquals(64, storage.balances(village.id()).get(ResourceType.HOLZ));
            assertEquals(40, villages.findById(village.id()).orElseThrow().xp());
            MissionRepository missions = new MissionRepository(database, villages);
            assertTrue(missions.current(village.id(), owner).isEmpty());
            var definitions = java.util.Arrays.stream(MissionType.values()).map(MissionDefinition::defaults).toList();
            var offers = missions.prepareOffers(village.id(), owner, 2, 1, definitions, 1_000L, 86_400_000L);
            assertEquals(3, offers.size());
            assertTrue(offers.stream().allMatch(view -> view.mission().status() == Mission.Status.OFFERED));
            assertTrue(offers.stream().allMatch(view -> view.mission().progress() == 0));
            assertEquals(40, villages.findById(village.id()).orElseThrow().xp());
        }
    }

    @Test
    void v7DatabaseUpgradesToImmigrationAndProductionReportsWithoutRetroactiveEffects() throws Exception {
        try (Database database = new Database(temp.resolve("v7-immigration-upgrade.db"))) {
            applyV7(database);
            UUID owner = UUID.randomUUID();
            VillageRepository villages = new VillageRepository(database);
            Village village = villages.createVillage("Bestandseinwanderung", owner,
                    new ClaimRegion(UUID.randomUUID(), 0, 64, 0, 48), 4, 20, false, 21);
            StorageRepository storage = new StorageRepository(database);
            storage.deposit(village.id(), owner, ResourceType.NAHRUNG, 64, 1_728);
            storage.applyProduction(village.id(), java.util.Map.of(ResourceType.WEIZEN, 2), 0, 1_728, 1_000L, "Legacy");
            villages.addXp(village.id(), 40);
            ResidentRepository residents = new ResidentRepository(database);
            residents.touchAll(village.id(), List.of(new Resident(village.id(), UUID.randomUUID(), Resident.Status.CONFIRMED, true,
                    new eu.praxios.blocklords.model.BlockLink(village.region().world(), 1, 64, 1), null,
                    1L, 1L, "Astrid", 1, false)));
            MissionRepository missions = new MissionRepository(database, villages);
            var definitions = java.util.Arrays.stream(MissionType.values()).map(MissionDefinition::defaults).toList();
            var offered = missions.prepareOffers(village.id(), owner, 2, 1, definitions, 1_000L, 86_400_000L).getFirst();
            missions.accept(village.id(), owner, offered.mission().id(), 1_000L);

            new MigrationRunner(database, Logger.getLogger("test")).migrate();

            assertTrue(columnExists(database, "immigration_state", "village_id"));
            assertTrue(columnExists(database, "immigration_candidates", "id"));
            assertTrue(columnExists(database, "production_reports", "village_id"));
            assertTrue(columnExists(database, "production_report_resources", "village_id"));
            assertEquals(64, storage.balances(village.id()).get(ResourceType.NAHRUNG));
            assertEquals(40, villages.findById(village.id()).orElseThrow().xp());
            assertEquals(1, residents.find(village.id()).size());
            assertEquals(Mission.Status.ACTIVE,
                    missions.accessible(village.id(), owner, offered.mission().id()).orElseThrow().mission().status());
            assertEquals("Legacy", storage.productionState(village.id()).lastReport());
            ImmigrationRepository immigration = new ImmigrationRepository(database, villages, storage, residents);
            assertTrue(immigration.snapshot(village.id()).waiting().isEmpty());
            assertTrue(new ProductionReportRepository(database).find(village.id()).isEmpty());
            assertEquals(40, villages.findById(village.id()).orElseThrow().xp());
        }
    }

    private void applyV7(Database database) throws Exception {
        applyV6(database);
        database.inTransaction(connection -> {
            for (String sql : loadStatements("V7__missions.sql")) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(sql);
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO schema_migrations(version, name, applied_at) VALUES (7, 'V7__missions.sql', 1)")) {
                statement.executeUpdate();
            }
            return null;
        });
    }

    private void applyV6(Database database) throws Exception {
        applyV5(database);
        database.inTransaction(connection -> {
            for (String sql : loadStatements("V6__resident_loss_and_chest_locations.sql")) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(sql);
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO schema_migrations(version, name, applied_at) VALUES (6, 'V6__resident_loss_and_chest_locations.sql', 1)")) {
                statement.executeUpdate();
            }
            return null;
        });
    }

    private List<String> loadStatements(String name) throws Exception {
        try (InputStream input = MigrationRunner.class.getResourceAsStream("/db/migration/" + name)) {
            assertNotNull(input);
            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            return java.util.Arrays.stream(sql.split(";"))
                    .map(candidate -> candidate.lines()
                            .filter(line -> !line.stripLeading().startsWith("--"))
                            .reduce("", (left, right) -> left + "\n" + right).trim())
                    .filter(candidate -> !candidate.isEmpty())
                    .toList();
        }
    }

    private boolean columnExists(Database database, String table, String column) throws Exception {
        return database.read(connection -> {
            try (Statement statement = connection.createStatement();
                 var result = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
                while (result.next()) {
                    if (column.equals(result.getString("name"))) return true;
                }
                return false;
            }
        });
    }
}
