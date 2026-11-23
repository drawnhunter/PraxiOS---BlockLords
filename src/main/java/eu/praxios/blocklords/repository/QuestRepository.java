package eu.praxios.blocklords.repository;

import eu.praxios.blocklords.database.Database;
import eu.praxios.blocklords.model.ClaimRegion;
import eu.praxios.blocklords.model.QuestState;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

public final class QuestRepository {
    private final Database database;

    public QuestRepository(Database database) {
        this.database = database;
    }

    public QuestState find(UUID player) throws Exception {
        return database.read(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM founding_quests WHERE player_uuid = ?")) {
                statement.setString(1, player.toString());
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        return QuestState.fresh(player, System.currentTimeMillis());
                    }
                    String world = result.getString("bell_world");
                    ClaimRegion bell = world == null ? null : new ClaimRegion(UUID.fromString(world),
                            result.getInt("bell_x"), result.getInt("bell_y"), result.getInt("bell_z"), 1);
                    return new QuestState(player, QuestState.Stage.valueOf(result.getString("stage")), bell,
                            result.getInt("progress"), result.getInt("founder_completed") == 1, result.getLong("updated_at"));
                }
            }
        });
    }

    public void save(QuestState state) throws Exception {
        database.inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO founding_quests(player_uuid, stage, bell_world, bell_x, bell_y, bell_z, progress, founder_completed, updated_at) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) "
                            + "ON CONFLICT(player_uuid) DO UPDATE SET stage=excluded.stage, bell_world=excluded.bell_world, "
                            + "bell_x=excluded.bell_x, bell_y=excluded.bell_y, bell_z=excluded.bell_z, "
                            + "progress=excluded.progress, founder_completed=excluded.founder_completed, updated_at=excluded.updated_at")) {
                statement.setString(1, state.player().toString());
                statement.setString(2, state.stage().name());
                if (state.bell() == null) {
                    statement.setNull(3, java.sql.Types.VARCHAR);
                    statement.setNull(4, java.sql.Types.INTEGER);
                    statement.setNull(5, java.sql.Types.INTEGER);
                    statement.setNull(6, java.sql.Types.INTEGER);
                } else {
                    statement.setString(3, state.bell().world().toString());
                    statement.setInt(4, state.bell().bellX());
                    statement.setInt(5, state.bell().bellY());
                    statement.setInt(6, state.bell().bellZ());
                }
                statement.setInt(7, state.progress());
                statement.setInt(8, state.founderCompleted() ? 1 : 0);
                statement.setLong(9, state.updatedAt());
                statement.executeUpdate();
            }
            return null;
        });
    }
}
