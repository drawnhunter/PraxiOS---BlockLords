package eu.praxios.blocklords.repository;

import eu.praxios.blocklords.database.Database;
import eu.praxios.blocklords.model.ProductionReport;
import eu.praxios.blocklords.model.ResourceType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class ProductionReportRepository {
    private final Database database;

    public ProductionReportRepository(Database database) {
        this.database = database;
    }

    public Optional<ProductionReport> find(long villageId) throws Exception {
        return database.read(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM production_reports WHERE village_id=?")) {
                statement.setLong(1, villageId);
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) return Optional.empty();
                    List<String> pauses = result.getString("pauses").isBlank() ? List.of()
                            : Arrays.asList(result.getString("pauses").split("\\R"));
                    ProductionReport report = new ProductionReport(villageId, result.getLong("recorded_at"),
                            result.getLong("cycle_at"), result.getInt("active") == 1,
                            result.getDouble("effective_cycles"), result.getInt("farmers"), result.getInt("miners"),
                            result.getInt("food_consumed"), pauses, resources(connection, villageId));
                    return Optional.of(report);
                }
            }
        });
    }

    public void save(ProductionReport report) throws Exception {
        database.inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO production_reports(village_id, recorded_at, cycle_at, active, effective_cycles, farmers, miners, food_consumed, pauses) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT(village_id) DO UPDATE SET "
                            + "recorded_at=excluded.recorded_at, cycle_at=excluded.cycle_at, active=excluded.active, "
                            + "effective_cycles=excluded.effective_cycles, farmers=excluded.farmers, miners=excluded.miners, "
                            + "food_consumed=excluded.food_consumed, pauses=excluded.pauses")) {
                statement.setLong(1, report.villageId());
                statement.setLong(2, report.recordedAt());
                statement.setLong(3, report.cycleAt());
                statement.setInt(4, report.active() ? 1 : 0);
                statement.setDouble(5, report.effectiveCycles());
                statement.setInt(6, report.farmers());
                statement.setInt(7, report.miners());
                statement.setInt(8, report.foodConsumed());
                statement.setString(9, String.join("\n", report.pauses()));
                statement.executeUpdate();
            }
            try (PreparedStatement delete = connection.prepareStatement("DELETE FROM production_report_resources WHERE village_id=?")) {
                delete.setLong(1, report.villageId());
                delete.executeUpdate();
            }
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO production_report_resources(village_id, resource, planned, accepted) VALUES (?, ?, ?, ?)")) {
                for (ProductionReport.Entry entry : report.resources()) {
                    insert.setLong(1, report.villageId());
                    insert.setString(2, entry.resource().name());
                    insert.setInt(3, entry.planned());
                    insert.setInt(4, entry.accepted());
                    insert.addBatch();
                }
                insert.executeBatch();
            }
            return null;
        });
    }

    private List<ProductionReport.Entry> resources(java.sql.Connection connection, long villageId) throws Exception {
        List<ProductionReport.Entry> entries = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT resource, planned, accepted FROM production_report_resources WHERE village_id=? ORDER BY resource")) {
            statement.setLong(1, villageId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    entries.add(new ProductionReport.Entry(ResourceType.valueOf(result.getString("resource")),
                            result.getInt("planned"), result.getInt("accepted")));
                }
            }
        }
        return entries;
    }
}
