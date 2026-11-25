package eu.praxios.blocklords.repository;

import eu.praxios.blocklords.database.Database;
import eu.praxios.blocklords.model.ClaimRegion;
import eu.praxios.blocklords.model.LegacyPlot;
import eu.praxios.blocklords.model.Role;
import eu.praxios.blocklords.model.Village;
import eu.praxios.blocklords.model.VillageStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class VillageRepository {
    private final Database database;

    public VillageRepository(Database database) {
        this.database = database;
    }

    public List<Village> findAll() throws Exception {
        return database.read(connection -> {
            List<Village> villages = new ArrayList<>();
            try (Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery("SELECT * FROM villages ORDER BY id")) {
                while (result.next()) {
                    villages.add(mapVillage(result));
                }
            }
            return villages;
        });
    }

    public Map<Long, Map<UUID, Role>> findAllMembers() throws Exception {
        return database.read(connection -> {
            Map<Long, Map<UUID, Role>> members = new HashMap<>();
            try (Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery("SELECT * FROM members")) {
                while (result.next()) {
                    members.computeIfAbsent(result.getLong("village_id"), ignored -> new HashMap<>())
                            .put(UUID.fromString(result.getString("player_uuid")), Role.valueOf(result.getString("role")));
                }
            }
            return members;
        });
    }

    public Village createVillage(String name, UUID owner, ClaimRegion region,
                                 int maxPerOwner, int maxPerWorld, boolean multiEnabled,
                                 int decayBlockDays) throws Exception {
        return database.inTransaction(connection -> {
            List<Village> existing = findAll(connection);
            for (Village village : existing) {
                if (village.name().equalsIgnoreCase(name)) {
                    throw new ClaimException(ClaimException.Reason.NAME_TAKEN, "Dieser Dorfname ist bereits vergeben.");
                }
                if (village.region().world().equals(region.world())
                        && village.region().bellX() == region.bellX()
                        && village.region().bellY() == region.bellY()
                        && village.region().bellZ() == region.bellZ()) {
                    throw new ClaimException(ClaimException.Reason.BELL_CLAIMED, "Diese Glocke ist bereits beansprucht.");
                }
                if (regionsOverlap(village.region(), region)) {
                    throw new ClaimException(ClaimException.Reason.OVERLAP, "Der neue Claim würde einen bestehenden Claim überschneiden.");
                }
            }

            List<Village> owned = existing.stream().filter(village -> village.owner().equals(owner)).toList();
            if (owned.size() >= maxPerOwner) {
                throw new ClaimException(ClaimException.Reason.OWNER_LIMIT, "Du hast bereits die maximale Dorfanzahl erreicht.");
            }
            long inWorld = existing.stream().filter(village -> village.region().world().equals(region.world())).count();
            if (inWorld >= maxPerWorld) {
                throw new ClaimException(ClaimException.Reason.WORLD_LIMIT, "Das Dorflimit dieser Welt ist erreicht.");
            }
            if (!owned.isEmpty()) {
                if (!multiEnabled) {
                    throw new ClaimException(ClaimException.Reason.MULTI_DISABLED,
                            "Weitere Dörfer werden mit den Wirtschafts- und Herrschaftssystemen der späteren Phasen freigeschaltet.");
                }
                int requiredLevel = switch (owned.size()) {
                    case 1 -> 3;
                    case 2 -> 6;
                    default -> 8;
                };
                boolean progress = false;
                for (Village village : owned) {
                    if (village.level() >= requiredLevel && coreFeaturesReady(connection, village.id())) {
                        progress = true;
                        break;
                    }
                }
                if (!progress) {
                    throw new ClaimException(ClaimException.Reason.PROGRESS_MISSING,
                            "Für dieses weitere Dorf fehlen Dorf-Level " + requiredLevel + " und die vier Kernbedingungen.");
                }
                long cutoff = decayBlockDays > 0 ? System.currentTimeMillis() - (long) decayBlockDays * 86_400_000L : Long.MIN_VALUE;
                boolean blocked = decayBlockDays > 0 && owned.stream().anyMatch(village -> village.lastActivity() < cutoff);
                if (blocked) {
                    throw new ClaimException(ClaimException.Reason.PROGRESS_MISSING,
                            "Reaktiviere zuerst dein ruhendes Dorf oder übertrage es geordnet.");
                }
            }

            checkLegacyConflicts(connection, owner, region);
            checkBlockOwnershipConflicts(connection, owner, region);

            long now = System.currentTimeMillis();
            long id;
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO villages(name, owner_uuid, world_uuid, bell_x, bell_y, bell_z, radius, level, xp, status, created_at, last_activity) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, 1, 0, 'ACTIVE', ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, name);
                statement.setString(2, owner.toString());
                statement.setString(3, region.world().toString());
                statement.setInt(4, region.bellX());
                statement.setInt(5, region.bellY());
                statement.setInt(6, region.bellZ());
                statement.setInt(7, region.radius());
                statement.setLong(8, now);
                statement.setLong(9, now);
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new IllegalStateException("SQLite hat keine Dorf-ID zurückgegeben.");
                    }
                    id = keys.getLong(1);
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO members(village_id, player_uuid, role, joined_at) VALUES (?, ?, 'LANDHERR', ?)")) {
                statement.setLong(1, id);
                statement.setString(2, owner.toString());
                statement.setLong(3, now);
                statement.executeUpdate();
            }
            return new Village(id, name, owner, region, 1, 0, VillageStatus.ACTIVE, now, now);
        });
    }

    public Village addXp(long villageId, int amount) throws Exception {
        return database.inTransaction(connection -> {
            Village current;
            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM villages WHERE id = ?")) {
                statement.setLong(1, villageId);
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) throw new IllegalArgumentException("Dorf " + villageId + " existiert nicht.");
                    current = mapVillage(result);
                }
            }
            int xp = current.xp() + Math.max(0, amount);
            int level = levelForXp(xp);
            Village updated = new Village(current.id(), current.name(), current.owner(), current.region(), level, xp,
                    current.status(), current.createdAt(), current.lastActivity());
            try (PreparedStatement statement = connection.prepareStatement("UPDATE villages SET xp = ?, level = ? WHERE id = ?")) {
                statement.setInt(1, xp);
                statement.setInt(2, level);
                statement.setLong(3, villageId);
                statement.executeUpdate();
            }
            return updated;
        });
    }

    public void setFeature(long villageId, String key, boolean enabled) throws Exception {
        database.inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO village_features(village_id, feature_key, enabled, updated_at) VALUES (?, ?, ?, ?) "
                            + "ON CONFLICT(village_id, feature_key) DO UPDATE SET enabled=excluded.enabled, updated_at=excluded.updated_at")) {
                statement.setLong(1, villageId);
                statement.setString(2, key);
                statement.setInt(3, enabled ? 1 : 0);
                statement.setLong(4, System.currentTimeMillis());
                statement.executeUpdate();
            }
            return null;
        });
    }

    public Map<String, Boolean> features(long villageId) throws Exception {
        return database.read(connection -> {
            Map<String, Boolean> features = new HashMap<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT feature_key, enabled FROM village_features WHERE village_id = ?")) {
                statement.setLong(1, villageId);
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) features.put(result.getString(1), result.getInt(2) == 1);
                }
            }
            return features;
        });
    }

    public void updateActivity(long villageId, long time) throws Exception {
        database.inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE villages SET last_activity = ?, status = 'ACTIVE' WHERE id = ?")) {
                statement.setLong(1, time);
                statement.setLong(2, villageId);
                statement.executeUpdate();
            }
            return null;
        });
    }

    public void updateStatus(long villageId, VillageStatus status) throws Exception {
        database.inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("UPDATE villages SET status = ? WHERE id = ?")) {
                statement.setString(1, status.name());
                statement.setLong(2, villageId);
                statement.executeUpdate();
            }
            return null;
        });
    }

    public void transfer(long villageId, UUID currentOwner, UUID newOwner) throws Exception {
        database.inTransaction(connection -> {
            long now = System.currentTimeMillis();
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE villages SET owner_uuid = ? WHERE id = ? AND owner_uuid = ?")) {
                statement.setString(1, newOwner.toString());
                statement.setLong(2, villageId);
                statement.setString(3, currentOwner.toString());
                if (statement.executeUpdate() != 1) {
                    throw new IllegalArgumentException("Dorf oder aktueller Landherr stimmen nicht überein.");
                }
            }
            try (PreparedStatement delete = connection.prepareStatement(
                    "DELETE FROM members WHERE village_id = ? AND player_uuid IN (?, ?)")) {
                delete.setLong(1, villageId);
                delete.setString(2, currentOwner.toString());
                delete.setString(3, newOwner.toString());
                delete.executeUpdate();
            }
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO members(village_id, player_uuid, role, joined_at) VALUES (?, ?, 'LANDHERR', ?)")) {
                insert.setLong(1, villageId);
                insert.setString(2, newOwner.toString());
                insert.setLong(3, now);
                insert.executeUpdate();
            }
            return null;
        });
    }

    public void disband(long villageId) throws Exception {
        database.inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM villages WHERE id = ?")) {
                statement.setLong(1, villageId);
                if (statement.executeUpdate() == 0) {
                    throw new IllegalArgumentException("Dorf " + villageId + " existiert nicht.");
                }
            }
            return null;
        });
    }

    private boolean coreFeaturesReady(Connection connection, long villageId) throws java.sql.SQLException {
        List<String> required = List.of("TOWN_HALL", "HOUSING", "PRODUCTION", "FOOD_STABLE");
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT feature_key, enabled FROM village_features WHERE village_id = ?")) {
            statement.setLong(1, villageId);
            Map<String, Boolean> features = new HashMap<>();
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    features.put(result.getString(1), result.getInt(2) == 1);
                }
            }
            return required.stream().allMatch(key -> features.getOrDefault(key, false));
        }
    }

    private void checkLegacyConflicts(Connection connection, UUID owner, ClaimRegion region) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT * FROM legacy_plots")) {
            while (result.next()) {
                LegacyPlot plot = LegacyRepository.map(result);
                if (plot.overlaps(region) && (plot.owner() == null || !plot.owner().equals(owner))) {
                    throw new ClaimException(ClaimException.Reason.ALT_CONFLICT,
                            "Altparzelle '" + plot.name() + "' (ID " + plot.id() + ") überschneidet diesen Claim.");
                }
            }
        }
    }

    private void checkBlockOwnershipConflicts(Connection connection, UUID owner, ClaimRegion region) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT owner_uuid, x, y, z FROM block_ownership WHERE world_uuid = ? AND owner_uuid <> ? AND x BETWEEN ? AND ? AND z BETWEEN ? AND ?")) {
            statement.setString(1, region.world().toString());
            statement.setString(2, owner.toString());
            statement.setInt(3, region.bellX() - region.radius());
            statement.setInt(4, region.bellX() + region.radius());
            statement.setInt(5, region.bellZ() - region.radius());
            statement.setInt(6, region.bellZ() + region.radius());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    UUID blockOwner = UUID.fromString(result.getString("owner_uuid"));
                    int x = result.getInt("x");
                    int z = result.getInt("z");
                    if (!blockOwner.equals(owner) && region.contains(region.world(), x, z)) {
                        throw new ClaimException(ClaimException.Reason.ALT_CONFLICT,
                                "Registrierter Altbesitz bei " + x + "/" + result.getInt("y") + "/" + z
                                        + " verhindert einen automatischen Claim. Zustimmung oder Admin-Prüfung erforderlich.");
                    }
                }
            }
        }
    }

    private List<Village> findAll(Connection connection) throws Exception {
        List<Village> villages = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT * FROM villages")) {
            while (result.next()) {
                villages.add(mapVillage(result));
            }
        }
        return villages;
    }

    private static final int[] LEVEL_THRESHOLDS = {0, 100, 250, 450, 700, 1000, 1400, 1900, 2500, 3300};

    public static int levelForXp(int xp) {
        int level = 1;
        for (int threshold : LEVEL_THRESHOLDS) {
            if (xp >= threshold) level++;
        }
        return Math.min(10, level - 1);
    }

    public static int xpForNextLevel(int currentLevel) {
        return currentLevel < 1 || currentLevel >= 10 ? -1 : LEVEL_THRESHOLDS[currentLevel];
    }

    private static boolean regionsOverlap(ClaimRegion first, ClaimRegion second) {
        if (!first.world().equals(second.world())) {
            return false;
        }
        long dx = (long) first.bellX() - second.bellX();
        long dz = (long) first.bellZ() - second.bellZ();
        long radii = (long) first.radius() + second.radius();
        return dx * dx + dz * dz < radii * radii;
    }

    private static Village mapVillage(ResultSet result) throws Exception {
        return new Village(
                result.getLong("id"), result.getString("name"), UUID.fromString(result.getString("owner_uuid")),
                new ClaimRegion(UUID.fromString(result.getString("world_uuid")),
                        result.getInt("bell_x"), result.getInt("bell_y"), result.getInt("bell_z"), result.getInt("radius")),
                result.getInt("level"), result.getInt("xp"), VillageStatus.valueOf(result.getString("status")),
                result.getLong("created_at"), result.getLong("last_activity"));
    }
}
