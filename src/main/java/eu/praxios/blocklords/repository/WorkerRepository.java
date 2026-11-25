package eu.praxios.blocklords.repository;

import eu.praxios.blocklords.database.Database;
import eu.praxios.blocklords.model.Worker;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class WorkerRepository {
    private final Database database;

    public WorkerRepository(Database database) {
        this.database = database;
    }

    public Worker assign(long villageId, UUID villager, long buildingId, Worker.Job job) throws Exception {
        long now = System.currentTimeMillis();
        return database.inTransaction(connection -> {
            long id;
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO workers(village_id, villager_uuid, building_id, job, assigned_at) VALUES (?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                statement.setLong(1, villageId);
                statement.setString(2, villager.toString());
                statement.setLong(3, buildingId);
                statement.setString(4, job.name());
                statement.setLong(5, now);
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    keys.next();
                    id = keys.getLong(1);
                }
            }
            return new Worker(id, villageId, villager, buildingId, job, now);
        });
    }

    public List<Worker> find(long villageId) throws Exception {
        return database.read(connection -> {
            List<Worker> workers = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM workers WHERE village_id = ? ORDER BY id")) {
                statement.setLong(1, villageId);
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        workers.add(new Worker(result.getLong("id"), result.getLong("village_id"),
                                UUID.fromString(result.getString("villager_uuid")), result.getLong("building_id"),
                                Worker.Job.valueOf(result.getString("job")), result.getLong("assigned_at")));
                    }
                }
            }
            return workers;
        });
    }

    public void remove(long workerId) throws Exception {
        database.inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM workers WHERE id = ?")) {
                statement.setLong(1, workerId);
                statement.executeUpdate();
            }
            return null;
        });
    }

    public void removeVillager(UUID villager) throws Exception {
        database.inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM workers WHERE villager_uuid = ?")) {
                statement.setString(1, villager.toString());
                statement.executeUpdate();
            }
            return null;
        });
    }
}
