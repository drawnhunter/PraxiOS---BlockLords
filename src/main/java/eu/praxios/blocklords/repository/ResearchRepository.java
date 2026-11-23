package eu.praxios.blocklords.repository;

import eu.praxios.blocklords.database.Database;
import eu.praxios.blocklords.model.ResearchProject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class ResearchRepository {
    private final Database database;

    public ResearchRepository(Database database) {
        this.database = database;
    }

    public ResearchProject start(long villageId, String key, long durationMillis) throws Exception {
        return database.inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT 1 FROM research WHERE village_id = ? AND research_key = ? AND status IN ('RUNNING', 'COMPLETED')")) {
                statement.setLong(1, villageId);
                statement.setString(2, key);
                try (ResultSet result = statement.executeQuery()) {
                    if (result.next()) throw new IllegalStateException("Diese Forschung ist bereits aktiv oder abgeschlossen.");
                }
            }
            long now = System.currentTimeMillis();
            long id;
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO research(village_id, research_key, status, started_at, finishes_at) VALUES (?, ?, 'RUNNING', ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                statement.setLong(1, villageId);
                statement.setString(2, key);
                statement.setLong(3, now);
                statement.setLong(4, now + durationMillis);
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    keys.next();
                    id = keys.getLong(1);
                }
            }
            return new ResearchProject(id, villageId, key, ResearchProject.Status.RUNNING, now, now + durationMillis, null);
        });
    }

    public List<ResearchProject> find(long villageId) throws Exception {
        return database.read(connection -> {
            List<ResearchProject> projects = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM research WHERE village_id = ? ORDER BY id")) {
                statement.setLong(1, villageId);
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) projects.add(map(result));
                }
            }
            return projects;
        });
    }

    public List<ResearchProject> due(long now) throws Exception {
        return database.read(connection -> {
            List<ResearchProject> projects = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM research WHERE status = 'RUNNING' AND finishes_at <= ?")) {
                statement.setLong(1, now);
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) projects.add(map(result));
                }
            }
            return projects;
        });
    }

    public void complete(long id) throws Exception {
        database.inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE research SET status = 'COMPLETED', completed_at = ? WHERE id = ? AND status = 'RUNNING'")) {
                statement.setLong(1, System.currentTimeMillis());
                statement.setLong(2, id);
                statement.executeUpdate();
            }
            return null;
        });
    }

    private static ResearchProject map(ResultSet result) throws Exception {
        Long completed = result.getLong("completed_at");
        if (result.wasNull()) completed = null;
        return new ResearchProject(result.getLong("id"), result.getLong("village_id"), result.getString("research_key"),
                ResearchProject.Status.valueOf(result.getString("status")), result.getLong("started_at"),
                result.getLong("finishes_at"), completed);
    }
}
