package eu.praxios.blocklords.database;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/** Kleine, explizit versionierte Migrationen ohne zusätzliches Runtime-Framework. */
public final class MigrationRunner {
    private static final List<String> MIGRATIONS = List.of(
            "V1__phase1_foundation.sql",
            "V2__phase2_economy.sql",
            "V3__residents.sql",
            "V4__placement_sessions.sql");
    private final Database database;
    private final Logger logger;

    public MigrationRunner(Database database, Logger logger) {
        this.database = database;
        this.logger = logger;
    }

    public void migrate() throws Exception {
        database.read(connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE IF NOT EXISTS schema_migrations ("
                        + "version INTEGER PRIMARY KEY, name TEXT NOT NULL, applied_at INTEGER NOT NULL)");
            }
            return null;
        });

        for (int index = 0; index < MIGRATIONS.size(); index++) {
            int version = index + 1;
            String name = MIGRATIONS.get(index);
            if (isApplied(version)) {
                continue;
            }
            List<String> statements = loadStatements(name);
            database.inTransaction(connection -> {
                for (String sql : statements) {
                    try (Statement statement = connection.createStatement()) {
                        statement.execute(sql);
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO schema_migrations(version, name, applied_at) VALUES (?, ?, ?)")) {
                    statement.setInt(1, version);
                    statement.setString(2, name);
                    statement.setLong(3, Instant.now().toEpochMilli());
                    statement.executeUpdate();
                }
                logger.info("BlockLords-Datenbankmigration " + name + " angewendet.");
                return null;
            });
        }
    }

    private boolean isApplied(int version) throws Exception {
        return database.read(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT 1 FROM schema_migrations WHERE version = ?")) {
                statement.setInt(1, version);
                try (ResultSet result = statement.executeQuery()) {
                    return result.next();
                }
            }
        });
    }

    private List<String> loadStatements(String name) throws IOException {
        String resource = "/db/migration/" + name;
        try (InputStream input = MigrationRunner.class.getResourceAsStream(resource)) {
            if (input == null) {
                throw new IOException("Fehlende Datenbankmigration: " + resource);
            }
            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            List<String> statements = new ArrayList<>();
            for (String candidate : sql.split(";")) {
                String cleaned = candidate.lines()
                        .filter(line -> !line.stripLeading().startsWith("--"))
                        .reduce("", (left, right) -> left + "\n" + right)
                        .trim();
                if (!cleaned.isEmpty()) {
                    statements.add(cleaned);
                }
            }
            return statements;
        }
    }
}
