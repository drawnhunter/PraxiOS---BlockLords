package eu.praxios.blocklords.repository;

import eu.praxios.blocklords.database.Database;
import eu.praxios.blocklords.model.BlockLink;
import eu.praxios.blocklords.model.Resident;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ResidentRepository {
    private final Database database;

    public ResidentRepository(Database database) {
        this.database = database;
    }

    public List<Resident> find(long villageId) throws Exception {
        return database.read(connection -> {
            List<Resident> residents = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM residents WHERE village_id = ? ORDER BY first_seen")) {
                statement.setLong(1, villageId);
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        residents.add(new Resident(result.getLong("village_id"), UUID.fromString(result.getString("villager_uuid")),
                                Resident.Status.valueOf(result.getString("status")), result.getInt("has_bed") == 1,
                                link(result, "bed"), link(result, "meeting"),
                                result.getLong("first_seen"), result.getLong("last_seen"),
                                result.getString("display_name"), result.getInt("resident_number")));
                    }
                }
            }
            return residents;
        });
    }

    public void touchAll(long villageId, List<Resident> residents) throws Exception {
        if (residents.isEmpty()) return;
        database.inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO residents(village_id, villager_uuid, status, has_bed, bed_world, bed_x, bed_y, bed_z, "
                            + "meeting_world, meeting_x, meeting_y, meeting_z, first_seen, last_seen, display_name, resident_number) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                            + "ON CONFLICT(villager_uuid) DO UPDATE SET village_id=excluded.village_id, status=excluded.status, "
                            + "has_bed=excluded.has_bed, bed_world=excluded.bed_world, bed_x=excluded.bed_x, bed_y=excluded.bed_y, "
                            + "bed_z=excluded.bed_z, meeting_world=excluded.meeting_world, meeting_x=excluded.meeting_x, "
                            + "meeting_y=excluded.meeting_y, meeting_z=excluded.meeting_z, last_seen=excluded.last_seen, "
                            + "display_name=COALESCE(excluded.display_name, residents.display_name), "
                            + "resident_number=CASE WHEN excluded.resident_number > 0 THEN excluded.resident_number ELSE residents.resident_number END")) {
                for (Resident resident : residents) {
                    statement.setLong(1, villageId);
                    statement.setString(2, resident.villager().toString());
                    statement.setString(3, resident.status().name());
                    statement.setInt(4, resident.hasBed() ? 1 : 0);
                    link(statement, 5, resident.bed());
                    link(statement, 9, resident.meetingPoint());
                    statement.setLong(13, resident.firstSeen());
                    statement.setLong(14, System.currentTimeMillis());
                    statement.setString(15, resident.displayName());
                    statement.setInt(16, resident.number());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
            return null;
        });
    }

    private static BlockLink link(ResultSet result, String prefix) throws Exception {
        String world = result.getString(prefix + "_world");
        if (world == null) return null;
        return new BlockLink(UUID.fromString(world), result.getInt(prefix + "_x"),
                result.getInt(prefix + "_y"), result.getInt(prefix + "_z"));
    }

    private static void link(PreparedStatement statement, int index, BlockLink link) throws Exception {
        if (link == null) {
            statement.setNull(index, java.sql.Types.VARCHAR);
            statement.setNull(index + 1, java.sql.Types.INTEGER);
            statement.setNull(index + 2, java.sql.Types.INTEGER);
            statement.setNull(index + 3, java.sql.Types.INTEGER);
        } else {
            statement.setString(index, link.world().toString());
            statement.setInt(index + 1, link.x());
            statement.setInt(index + 2, link.y());
            statement.setInt(index + 3, link.z());
        }
    }
}
