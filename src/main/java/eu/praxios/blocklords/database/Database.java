package eu.praxios.blocklords.database;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/** Eine einzige, kontrolliert genutzte SQLite-Verbindung. Zugriffe laufen über den DB-Executor. */
public final class Database implements AutoCloseable {
    private final Path path;
    private final Connection connection;

    public Database(Path path) throws Exception {
        this.path = path.toAbsolutePath();
        Files.createDirectories(this.path.getParent());
        Class.forName("org.sqlite.JDBC");
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + this.path);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");
            statement.execute("PRAGMA journal_mode = WAL");
            statement.execute("PRAGMA synchronous = NORMAL");
        }
    }

    public Path path() {
        return path;
    }

    public synchronized <T> T inTransaction(SqlWork<T> work) throws Exception {
        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            T result = work.apply(connection);
            connection.commit();
            return result;
        } catch (Exception exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    public synchronized <T> T read(SqlWork<T> work) throws Exception {
        return work.apply(connection);
    }

    public synchronized void backup(Path target) throws Exception {
        Path absolute = target.toAbsolutePath();
        Files.createDirectories(absolute.getParent());
        Files.deleteIfExists(absolute);
        try (PreparedStatement statement = connection.prepareStatement("VACUUM INTO ?")) {
            statement.setString(1, absolute.toString());
            statement.execute();
        }
    }

    @Override
    public synchronized void close() throws SQLException {
        if (!connection.isClosed()) {
            connection.close();
        }
    }

    @FunctionalInterface
    public interface SqlWork<T> {
        T apply(Connection connection) throws Exception;
    }
}
