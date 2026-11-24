package eu.praxios.blocklords.repository;

import eu.praxios.blocklords.database.Database;
import eu.praxios.blocklords.model.BlueprintType;
import eu.praxios.blocklords.model.PlacementSnapshot;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;
import java.util.UUID;

public final class PlacementRepository {
    private final Database database;

    public PlacementRepository(Database database) {
        this.database = database;
    }

    public void saveActive(PlacementSnapshot snapshot) throws Exception {
        database.inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO placement_sessions(player_uuid, village_id, blueprint, contents, armor, extras, selected_slot, status, created_at, updated_at) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?, ?) "
                            + "ON CONFLICT(player_uuid) DO UPDATE SET village_id=excluded.village_id, blueprint=excluded.blueprint, "
                            + "contents=excluded.contents, armor=excluded.armor, extras=excluded.extras, selected_slot=excluded.selected_slot, "
                            + "status='ACTIVE', created_at=excluded.created_at, updated_at=excluded.updated_at")) {
                statement.setString(1, snapshot.player().toString());
                statement.setLong(2, snapshot.villageId());
                statement.setString(3, snapshot.blueprint().name());
                statement.setBytes(4, snapshot.contents());
                statement.setBytes(5, snapshot.armor());
                statement.setBytes(6, snapshot.extras());
                statement.setInt(7, snapshot.selectedSlot());
                statement.setLong(8, snapshot.createdAt());
                statement.setLong(9, System.currentTimeMillis());
                statement.executeUpdate();
            }
            return null;
        });
    }

    public Optional<PlacementSnapshot> findActive(UUID player) throws Exception {
        return database.read(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM placement_sessions WHERE player_uuid = ? AND status = 'ACTIVE'")) {
                statement.setString(1, player.toString());
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) return Optional.empty();
                    return Optional.of(new PlacementSnapshot(player, result.getLong("village_id"),
                            BlueprintType.valueOf(result.getString("blueprint")), result.getBytes("contents"),
                            result.getBytes("armor"), result.getBytes("extras"), result.getInt("selected_slot"),
                            result.getLong("created_at")));
                }
            }
        });
    }

    public void close(UUID player) throws Exception {
        database.inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE placement_sessions SET status = 'CLOSED', updated_at = ? WHERE player_uuid = ? AND status = 'ACTIVE'")) {
                statement.setLong(1, System.currentTimeMillis());
                statement.setString(2, player.toString());
                statement.executeUpdate();
            }
            return null;
        });
    }
}
