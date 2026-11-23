package eu.praxios.blocklords.repository;

import eu.praxios.blocklords.database.Database;
import eu.praxios.blocklords.model.AuditEntry;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class AuditRepository {
    private final Database database;

    public AuditRepository(Database database) {
        this.database = database;
    }

    public void log(UUID actor, String action, String details) {
        try {
            database.inTransaction(connection -> {
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO audit(actor_uuid, action, details, created_at) VALUES (?, ?, ?, ?)")) {
                    if (actor == null) statement.setNull(1, java.sql.Types.VARCHAR); else statement.setString(1, actor.toString());
                    statement.setString(2, action);
                    statement.setString(3, details.length() > 1000 ? details.substring(0, 1000) : details);
                    statement.setLong(4, System.currentTimeMillis());
                    statement.executeUpdate();
                }
                return null;
            });
        } catch (Exception exception) {
            throw new IllegalStateException("Audit-Eintrag konnte nicht geschrieben werden", exception);
        }
    }

    public List<AuditEntry> recent(int limit) throws Exception {
        return database.read(connection -> {
            List<AuditEntry> entries = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM audit ORDER BY id DESC LIMIT ?")) {
                statement.setInt(1, Math.max(1, Math.min(limit, 100)));
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        String actor = result.getString("actor_uuid");
                        entries.add(new AuditEntry(result.getLong("id"),
                                actor == null ? null : UUID.fromString(actor), result.getString("action"),
                                result.getString("details"), result.getLong("created_at")));
                    }
                }
            }
            return entries;
        });
    }
}
