package eu.praxios.blocklords.repository;

import eu.praxios.blocklords.database.Database;
import eu.praxios.blocklords.model.ResourceType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public final class StorageRepository {
    private final Database database;
    private final MissionRepository missions;

    public StorageRepository(Database database) {
        this(database, null);
    }

    public StorageRepository(Database database, MissionRepository missions) {
        this.database = database;
        this.missions = missions;
    }

    public Map<ResourceType, Integer> balances(long villageId) throws Exception {
        return database.read(connection -> balances(connection, villageId));
    }

    public int deposit(long villageId, UUID player, ResourceType resource, int amount, int capacity) throws Exception {
        return depositInternal(villageId, player, resource, amount, capacity, true);
    }

    public int refund(long villageId, UUID player, ResourceType resource, int amount, int capacity) throws Exception {
        return depositInternal(villageId, player, resource, amount, capacity, false);
    }

    private int depositInternal(long villageId, UUID player, ResourceType resource, int amount, int capacity,
                                boolean trackMission) throws Exception {
        return database.inTransaction(connection -> {
            int actual = Math.max(0, Math.min(amount, capacity - balances(connection, villageId).getOrDefault(resource, 0)));
            if (actual <= 0) return 0;
            change(connection, villageId, resource, actual);
            transaction(connection, villageId, player, resource, actual, "DEPOSIT");
            if (trackMission && missions != null) {
                missions.recordStorageDeposit(connection, villageId, player, resource, actual);
            }
            return actual;
        });
    }

    public int withdraw(long villageId, UUID player, ResourceType resource, int amount) throws Exception {
        return database.inTransaction(connection -> {
            int available = balances(connection, villageId).getOrDefault(resource, 0);
            int actual = Math.max(0, Math.min(amount, available));
            if (actual <= 0) return 0;
            change(connection, villageId, resource, -actual);
            transaction(connection, villageId, player, resource, -actual, "WITHDRAW");
            return actual;
        });
    }

    public boolean consumeCost(long villageId, UUID player, Map<ResourceType, Integer> cost, String reason) throws Exception {
        return database.inTransaction(connection -> consumeCost(connection, villageId, player, cost, reason));
    }

    public boolean consumeCost(Connection connection, long villageId, UUID player,
                               Map<ResourceType, Integer> cost, String reason) throws Exception {
        Map<ResourceType, Integer> balances = balances(connection, villageId);
        for (var entry : cost.entrySet()) {
            if (balances.getOrDefault(entry.getKey(), 0) < entry.getValue()) return false;
        }
        for (var entry : cost.entrySet()) {
            change(connection, villageId, entry.getKey(), -entry.getValue());
            transaction(connection, villageId, player, entry.getKey(), -entry.getValue(), reason);
        }
        return true;
    }

    public void pauseProduction(long villageId, long cycleAt, String reason) throws Exception {
        database.inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO production_state(village_id, last_cycle_at, last_report, last_report_at) VALUES (?, ?, ?, ?) "
                            + "ON CONFLICT(village_id) DO UPDATE SET last_cycle_at=excluded.last_cycle_at, "
                            + "last_report=excluded.last_report, last_report_at=excluded.last_report_at")) {
                statement.setLong(1, villageId);
                statement.setLong(2, cycleAt);
                statement.setString(3, reason);
                statement.setLong(4, System.currentTimeMillis());
                statement.executeUpdate();
            }
            return null;
        });
    }

    public ProductionResult applyProduction(long villageId, Map<ResourceType, Integer> produced,
                                            int foodConsumed, int capacity, long cycleAt, String report) throws Exception {
        return database.inTransaction(connection -> {
            Map<ResourceType, Integer> balances = balances(connection, villageId);
            int actualFood = Math.min(foodConsumed, balances.getOrDefault(ResourceType.NAHRUNG, 0));
            if (actualFood > 0) {
                change(connection, villageId, ResourceType.NAHRUNG, -actualFood);
                transaction(connection, villageId, null, ResourceType.NAHRUNG, -actualFood, "WORKER_UPKEEP");
                totals(connection, villageId, ResourceType.NAHRUNG, 0, actualFood);
                balances.merge(ResourceType.NAHRUNG, -actualFood, Integer::sum);
            }
            Map<ResourceType, Integer> accepted = new EnumMap<>(ResourceType.class);
            for (var entry : produced.entrySet()) {
                int actual = Math.max(0, Math.min(entry.getValue(),
                        capacity - balances.getOrDefault(entry.getKey(), 0)));
                if (actual <= 0) continue;
                change(connection, villageId, entry.getKey(), actual);
                transaction(connection, villageId, null, entry.getKey(), actual, "PRODUCTION");
                totals(connection, villageId, entry.getKey(), actual, 0);
                accepted.put(entry.getKey(), actual);
            }
            long now = System.currentTimeMillis();
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO production_state(village_id, last_cycle_at, last_report, last_report_at) VALUES (?, ?, ?, ?) "
                            + "ON CONFLICT(village_id) DO UPDATE SET last_cycle_at=excluded.last_cycle_at, "
                            + "last_report=excluded.last_report, last_report_at=excluded.last_report_at")) {
                statement.setLong(1, villageId);
                statement.setLong(2, cycleAt);
                statement.setString(3, report);
                statement.setLong(4, now);
                statement.executeUpdate();
            }
            return new ProductionResult(accepted, actualFood);
        });
    }

    public ProductionState productionState(long villageId) throws Exception {
        return database.read(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM production_state WHERE village_id = ?")) {
                statement.setLong(1, villageId);
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) return new ProductionState(0L, null, 0L);
                    return new ProductionState(result.getLong("last_cycle_at"), result.getString("last_report"),
                            result.getLong("last_report_at"));
                }
            }
        });
    }

    public void initializeProduction(long villageId, long cycleAt) throws Exception {
        database.inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO production_state(village_id, last_cycle_at) VALUES (?, ?) "
                            + "ON CONFLICT(village_id) DO NOTHING")) {
                statement.setLong(1, villageId);
                statement.setLong(2, cycleAt);
                statement.executeUpdate();
            }
            return null;
        });
    }

    private Map<ResourceType, Integer> balances(Connection connection, long villageId) throws Exception {
        Map<ResourceType, Integer> balances = new EnumMap<>(ResourceType.class);
        for (ResourceType resource : ResourceType.values()) balances.put(resource, 0);
        try (PreparedStatement statement = connection.prepareStatement("SELECT resource, amount FROM storage WHERE village_id = ?")) {
            statement.setLong(1, villageId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    balances.put(ResourceType.valueOf(result.getString(1)), result.getInt(2));
                }
            }
        }
        return balances;
    }

    private void change(Connection connection, long villageId, ResourceType resource, int delta) throws Exception {
        int updated;
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE storage SET amount = amount + ? WHERE village_id = ? AND resource = ?")) {
            statement.setInt(1, delta);
            statement.setLong(2, villageId);
            statement.setString(3, resource.name());
            updated = statement.executeUpdate();
        }
        if (updated == 0 && delta >= 0) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO storage(village_id, resource, amount) VALUES (?, ?, ?)")) {
                statement.setLong(1, villageId);
                statement.setString(2, resource.name());
                statement.setInt(3, delta);
                statement.executeUpdate();
            }
        }
    }

    private void transaction(Connection connection, long villageId, UUID player, ResourceType resource,
                             int delta, String reason) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO storage_transactions(village_id, player_uuid, resource, delta, reason, created_at) VALUES (?, ?, ?, ?, ?, ?)")) {
            statement.setLong(1, villageId);
            if (player == null) statement.setNull(2, java.sql.Types.VARCHAR); else statement.setString(2, player.toString());
            statement.setString(3, resource.name());
            statement.setInt(4, delta);
            statement.setString(5, reason);
            statement.setLong(6, System.currentTimeMillis());
            statement.executeUpdate();
        }
    }

    private void totals(Connection connection, long villageId, ResourceType resource, int produced, int consumed) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO production_totals(village_id, resource, produced, consumed) VALUES (?, ?, ?, ?) "
                        + "ON CONFLICT(village_id, resource) DO UPDATE SET produced=produced+?, consumed=consumed+?")) {
            statement.setLong(1, villageId);
            statement.setString(2, resource.name());
            statement.setInt(3, produced);
            statement.setInt(4, consumed);
            statement.setInt(5, produced);
            statement.setInt(6, consumed);
            statement.executeUpdate();
        }
    }

    public record ProductionResult(Map<ResourceType, Integer> accepted, int foodConsumed) {
    }

    public record ProductionState(long lastCycleAt, String lastReport, long lastReportAt) {
    }
}
