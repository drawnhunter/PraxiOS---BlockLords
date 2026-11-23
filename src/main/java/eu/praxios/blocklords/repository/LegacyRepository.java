package eu.praxios.blocklords.repository;

import eu.praxios.blocklords.database.Database;
import eu.praxios.blocklords.model.LegacyPlot;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class LegacyRepository {
    private final Database database;

    public LegacyRepository(Database database) {
        this.database = database;
    }

    public List<LegacyPlot> findAll() throws Exception {
        return database.read(connection -> {
            List<LegacyPlot> plots = new ArrayList<>();
            try (Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery("SELECT * FROM legacy_plots")) {
                while (result.next()) {
                    plots.add(map(result));
                }
            }
            return plots;
        });
    }

    public LegacyPlot register(String name, UUID owner, UUID world,
                               int minX, int minY, int minZ,
                               int maxX, int maxY, int maxZ,
                               UUID registeredBy) throws Exception {
        long now = System.currentTimeMillis();
        return database.inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO legacy_plots(name, owner_uuid, world_uuid, min_x, min_y, min_z, max_x, max_y, max_z, registered_by, created_at) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, name);
                if (owner == null) statement.setNull(2, java.sql.Types.VARCHAR); else statement.setString(2, owner.toString());
                statement.setString(3, world.toString());
                statement.setInt(4, Math.min(minX, maxX));
                statement.setInt(5, Math.min(minY, maxY));
                statement.setInt(6, Math.min(minZ, maxZ));
                statement.setInt(7, Math.max(minX, maxX));
                statement.setInt(8, Math.max(minY, maxY));
                statement.setInt(9, Math.max(minZ, maxZ));
                statement.setString(10, registeredBy.toString());
                statement.setLong(11, now);
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    long id = keys.getLong(1);
                    return new LegacyPlot(id, name, owner, world,
                            Math.min(minX, maxX), Math.min(minY, maxY), Math.min(minZ, maxZ),
                            Math.max(minX, maxX), Math.max(minY, maxY), Math.max(minZ, maxZ), registeredBy, now);
                }
            }
        });
    }

    public void delete(long id) throws Exception {
        database.inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM legacy_plots WHERE id = ?")) {
                statement.setLong(1, id);
                if (statement.executeUpdate() == 0) {
                    throw new IllegalArgumentException("Altparzelle " + id + " existiert nicht.");
                }
            }
            return null;
        });
    }

    static LegacyPlot map(ResultSet result) throws Exception {
        String owner = result.getString("owner_uuid");
        return new LegacyPlot(
                result.getLong("id"), result.getString("name"),
                owner == null ? null : UUID.fromString(owner), UUID.fromString(result.getString("world_uuid")),
                result.getInt("min_x"), result.getInt("min_y"), result.getInt("min_z"),
                result.getInt("max_x"), result.getInt("max_y"), result.getInt("max_z"),
                UUID.fromString(result.getString("registered_by")), result.getLong("created_at"));
    }
}
