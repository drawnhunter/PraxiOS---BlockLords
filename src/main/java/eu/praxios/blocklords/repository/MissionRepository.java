package eu.praxios.blocklords.repository;

import eu.praxios.blocklords.database.Database;
import eu.praxios.blocklords.model.Mission;
import eu.praxios.blocklords.model.MissionAction;
import eu.praxios.blocklords.model.MissionDefinition;
import eu.praxios.blocklords.model.MissionType;
import eu.praxios.blocklords.model.MissionView;
import eu.praxios.blocklords.model.ResourceType;
import eu.praxios.blocklords.model.Village;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class MissionRepository {
    private final Database database;
    private final VillageRepository villages;

    public MissionRepository(Database database, VillageRepository villages) {
        this.database = database;
        this.villages = villages;
    }

    public List<MissionView> prepareOffers(long villageId, UUID playerId, int personalOffers, int villageOffers,
                                           List<MissionDefinition> definitions, long now, long refreshMillis) throws Exception {
        return database.inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE missions SET status='REPLACED' WHERE status='OFFERED' AND created_at < ? "
                            + "AND (scope='DORF' OR player_uuid=?)")) {
                statement.setLong(1, now - refreshMillis);
                statement.setString(2, playerId.toString());
                statement.executeUpdate();
            }
            List<MissionView> existing = current(connection, villageId, playerId);
            Set<MissionType> used = EnumSet.noneOf(MissionType.class);
            existing.forEach(view -> used.add(view.mission().type()));
            long personal = existing.stream().filter(view -> view.mission().personal()).count();
            long shared = existing.size() - personal;
            for (MissionDefinition definition : definitions) {
                MissionType type = definition.type();
                if (used.contains(type)) continue;
                boolean needed = type.scope() == Mission.Scope.PERSONAL ? personal < personalOffers : shared < villageOffers;
                if (!needed) continue;
                insertOffer(connection, villageId, playerId, definition, now);
                used.add(type);
                if (type.scope() == Mission.Scope.PERSONAL) personal++;
                else shared++;
            }
            return current(connection, villageId, playerId);
        });
    }

    public List<MissionView> current(long villageId, UUID playerId) throws Exception {
        return database.read(connection -> current(connection, villageId, playerId));
    }

    public Optional<MissionView> accessible(long villageId, UUID playerId, long missionId) throws Exception {
        return database.read(connection -> accessible(connection, villageId, playerId, missionId));
    }

    public Mission accept(long villageId, UUID playerId, long missionId, long now) throws Exception {
        return database.inTransaction(connection -> {
            Mission mission = accessible(connection, villageId, playerId, missionId)
                    .map(MissionView::mission).orElseThrow(() -> new IllegalArgumentException("Diese Mission gehört nicht zu deinem Dorf."));
            if (mission.status() == Mission.Status.ACTIVE) return mission;
            if (mission.status() != Mission.Status.OFFERED) {
                throw new IllegalArgumentException("Diese Mission kann nicht mehr angenommen werden.");
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE missions SET status='ACTIVE', accepted_at=? WHERE id=? AND status='OFFERED'")) {
                statement.setLong(1, now);
                statement.setLong(2, missionId);
                if (statement.executeUpdate() != 1) throw new IllegalArgumentException("Die Mission wurde bereits geändert.");
            }
            return findById(connection, missionId).orElseThrow();
        });
    }

    public Mission replace(long villageId, UUID playerId, long missionId) throws Exception {
        return database.inTransaction(connection -> {
            Mission mission = accessible(connection, villageId, playerId, missionId)
                    .map(MissionView::mission).orElseThrow(() -> new IllegalArgumentException("Diese Mission gehört nicht zu deinem Dorf."));
            if (mission.status() != Mission.Status.OFFERED && mission.status() != Mission.Status.ACTIVE) {
                throw new IllegalArgumentException("Diese Mission ist bereits abgeschlossen oder ersetzt.");
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE missions SET status='REPLACED', pinned=0 WHERE id=? AND status IN ('OFFERED', 'ACTIVE')")) {
                statement.setLong(1, missionId);
                if (statement.executeUpdate() != 1) throw new IllegalArgumentException("Die Mission wurde bereits geändert.");
            }
            return findById(connection, missionId).orElseThrow();
        });
    }

    public Mission pin(long villageId, UUID playerId, long missionId) throws Exception {
        return database.inTransaction(connection -> {
            Mission mission = accessible(connection, villageId, playerId, missionId)
                    .map(MissionView::mission).orElseThrow(() -> new IllegalArgumentException("Diese Mission gehört nicht zu deinem Dorf."));
            if (!mission.personal() || mission.status() != Mission.Status.ACTIVE) {
                throw new IllegalArgumentException("Nur deine angenommenen persönlichen Missionen kannst du anpinnen.");
            }
            try (PreparedStatement clear = connection.prepareStatement(
                    "UPDATE missions SET pinned=0 WHERE village_id=? AND player_uuid=?")) {
                clear.setLong(1, villageId);
                clear.setString(2, playerId.toString());
                clear.executeUpdate();
            }
            try (PreparedStatement pin = connection.prepareStatement(
                    "UPDATE missions SET pinned=1 WHERE id=? AND player_uuid=? AND status='ACTIVE'")) {
                pin.setLong(1, missionId);
                pin.setString(2, playerId.toString());
                if (pin.executeUpdate() != 1) throw new IllegalArgumentException("Die Mission wurde bereits geändert.");
            }
            return findById(connection, missionId).orElseThrow();
        });
    }

    public Optional<Mission> pinned(long villageId, UUID playerId) throws Exception {
        return database.read(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM missions WHERE village_id=? AND player_uuid=? AND scope='PERSONAL' "
                            + "AND status='ACTIVE' AND pinned=1")) {
                statement.setLong(1, villageId);
                statement.setString(2, playerId.toString());
                try (ResultSet result = statement.executeQuery()) {
                    return result.next() ? Optional.of(map(result)) : Optional.empty();
                }
            }
        });
    }

    public List<Completion> takeUnnotified(long villageId, long now) throws Exception {
        return database.inTransaction(connection -> {
            List<Mission> pending = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM missions WHERE village_id=? AND status='COMPLETED' "
                            + "AND completion_notified_at IS NULL ORDER BY id")) {
                statement.setLong(1, villageId);
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) pending.add(map(result));
                }
            }
            List<Completion> completions = new ArrayList<>();
            for (Mission mission : pending) {
                try (PreparedStatement mark = connection.prepareStatement(
                        "UPDATE missions SET completion_notified_at=? WHERE id=? AND completion_notified_at IS NULL")) {
                    mark.setLong(1, now);
                    mark.setLong(2, mission.id());
                    if (mark.executeUpdate() != 1) continue;
                }
                Village village = villages.findById(connection, villageId)
                        .orElseThrow(() -> new IllegalArgumentException("Dorf " + villageId + " fehlt."));
                completions.add(new Completion(mission, village, awardedXp(connection, mission.id())));
            }
            return completions;
        });
    }

    private int awardedXp(Connection connection, long missionId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("SELECT xp FROM mission_xp_grants WHERE mission_id=?")) {
            statement.setLong(1, missionId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getInt(1) : 0;
            }
        }
    }

    public void recordStorageDeposit(Connection connection, long villageId, UUID playerId,
                                     ResourceType resource, int amount) throws Exception {
        recordStorageDeposit(connection, villageId, playerId, resource, amount, UUID.randomUUID());
    }

    public void recordStorageDeposit(Connection connection, long villageId, UUID playerId,
                                     ResourceType resource, int amount, UUID eventKey) throws Exception {
        recordEvent(connection, villageId, playerId, MissionAction.STORAGE_DEPOSIT, resource, amount,
                eventKey, "storage");
    }

    public void recordBuildContribution(Connection connection, long villageId, UUID playerId,
                                        ResourceType resource, int amount, long siteId) throws Exception {
        recordBuildContribution(connection, villageId, playerId, resource, amount, siteId, UUID.randomUUID());
    }

    public void recordBuildContribution(Connection connection, long villageId, UUID playerId,
                                        ResourceType resource, int amount, long siteId, UUID eventKey) throws Exception {
        recordEvent(connection, villageId, playerId, MissionAction.BUILD_CONTRIBUTION, resource, amount,
                eventKey, "build-site:" + siteId);
    }

    private void recordEvent(Connection connection, long villageId, UUID playerId, MissionAction action,
                             ResourceType resource, int amount, UUID eventKey, String sourceReference) throws Exception {
        if (amount <= 0 || !isMember(connection, villageId, playerId)) return;
        long eventId;
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO mission_events(event_key, village_id, player_uuid, action, resource, amount, source_reference, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT(event_key) DO NOTHING", Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, eventKey.toString());
            statement.setLong(2, villageId);
            statement.setString(3, playerId.toString());
            statement.setString(4, action.name());
            statement.setString(5, resource == null ? null : resource.name());
            statement.setInt(6, amount);
            statement.setString(7, sourceReference);
            statement.setLong(8, System.currentTimeMillis());
            if (statement.executeUpdate() == 0) return;
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) throw new IllegalStateException("SQLite hat keine Missionsereignis-ID zurückgegeben.");
                eventId = keys.getLong(1);
            }
        }
        for (Mission mission : activeForEvent(connection, villageId, playerId, action, resource)) {
            int applied = Math.min(amount, mission.remaining());
            if (applied <= 0) continue;
            try (PreparedStatement link = connection.prepareStatement(
                    "INSERT INTO mission_event_links(event_id, mission_id, amount, created_at) VALUES (?, ?, ?, ?) "
                            + "ON CONFLICT(event_id, mission_id) DO NOTHING")) {
                link.setLong(1, eventId);
                link.setLong(2, mission.id());
                link.setInt(3, applied);
                link.setLong(4, System.currentTimeMillis());
                if (link.executeUpdate() == 0) continue;
            }
            int total = mission.progress() + applied;
            boolean completed = total >= mission.target();
            long now = System.currentTimeMillis();
            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE missions SET progress=?, status=?, completed_at=?, pinned=? "
                            + "WHERE id=? AND status='ACTIVE'")) {
                update.setInt(1, Math.min(total, mission.target()));
                update.setString(2, completed ? Mission.Status.COMPLETED.name() : Mission.Status.ACTIVE.name());
                if (completed) update.setLong(3, now); else update.setNull(3, java.sql.Types.INTEGER);
                update.setInt(4, completed ? 0 : mission.pinned() ? 1 : 0);
                update.setLong(5, mission.id());
                if (update.executeUpdate() != 1) continue;
            }
            if (completed) awardFirstXp(connection, mission, now);
        }
    }

    private void awardFirstXp(Connection connection, Mission mission, long now) throws Exception {
        String recipient = mission.personal() ? mission.playerId().toString() : "DORF";
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO mission_xp_grants(village_id, recipient_key, mission_type, mission_id, xp, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT(village_id, recipient_key, mission_type) DO NOTHING")) {
            statement.setLong(1, mission.villageId());
            statement.setString(2, recipient);
            statement.setString(3, mission.type().name());
            statement.setLong(4, mission.id());
            statement.setInt(5, mission.xp());
            statement.setLong(6, now);
            if (statement.executeUpdate() == 1) {
                villages.addXp(connection, mission.villageId(), mission.xp());
            }
        }
    }

    private boolean isMember(Connection connection, long villageId, UUID playerId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM members WHERE village_id=? AND player_uuid=? "
                        + "AND role IN ('LANDHERR', 'GEFOLGSMANN', 'BUERGER')")) {
            statement.setLong(1, villageId);
            statement.setString(2, playerId.toString());
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private List<Mission> activeForEvent(Connection connection, long villageId, UUID playerId,
                                         MissionAction action, ResourceType resource) throws Exception {
        List<Mission> matches = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM missions WHERE village_id=? AND status='ACTIVE' "
                        + "AND (scope='DORF' OR player_uuid=?) ORDER BY id")) {
            statement.setLong(1, villageId);
            statement.setString(2, playerId.toString());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    Mission mission = map(result);
                    if (mission.action() != action) continue;
                    if (action == MissionAction.STORAGE_DEPOSIT && mission.resource() != resource) continue;
                    matches.add(mission);
                }
            }
        }
        return matches;
    }

    private List<MissionView> current(Connection connection, long villageId, UUID playerId) throws Exception {
        List<MissionView> views = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM missions WHERE village_id=? AND status IN ('OFFERED', 'ACTIVE') "
                        + "AND (scope='DORF' OR player_uuid=?) "
                        + "ORDER BY CASE scope WHEN 'PERSONAL' THEN 0 ELSE 1 END, id")) {
            statement.setLong(1, villageId);
            statement.setString(2, playerId.toString());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    Mission mission = map(result);
                    views.add(new MissionView(mission, ownContribution(connection, mission, playerId)));
                }
            }
        }
        return views;
    }

    private Optional<MissionView> accessible(Connection connection, long villageId, UUID playerId,
                                             long missionId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM missions WHERE id=? AND village_id=? AND (scope='DORF' OR player_uuid=?)")) {
            statement.setLong(1, missionId);
            statement.setLong(2, villageId);
            statement.setString(3, playerId.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return Optional.empty();
                Mission mission = map(result);
                return Optional.of(new MissionView(mission, ownContribution(connection, mission, playerId)));
            }
        }
    }

    private int ownContribution(Connection connection, Mission mission, UUID playerId) throws Exception {
        if (mission.personal()) return mission.progress();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COALESCE(SUM(l.amount), 0) FROM mission_event_links l "
                        + "JOIN mission_events e ON e.id=l.event_id WHERE l.mission_id=? AND e.player_uuid=?")) {
            statement.setLong(1, mission.id());
            statement.setString(2, playerId.toString());
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    private void insertOffer(Connection connection, long villageId, UUID playerId,
                             MissionDefinition definition, long now) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO missions(village_id, player_uuid, mission_type, scope, target, xp, status, progress, created_at, pinned) "
                        + "VALUES (?, ?, ?, ?, ?, ?, 'OFFERED', 0, ?, 0)")) {
            statement.setLong(1, villageId);
            if (definition.type().scope() == Mission.Scope.PERSONAL) statement.setString(2, playerId.toString());
            else statement.setNull(2, java.sql.Types.VARCHAR);
            statement.setString(3, definition.type().name());
            statement.setString(4, definition.type().scope().name());
            statement.setInt(5, definition.target());
            statement.setInt(6, definition.xp());
            statement.setLong(7, now);
            statement.executeUpdate();
        }
    }

    private Optional<Mission> findById(Connection connection, long missionId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM missions WHERE id=?")) {
            statement.setLong(1, missionId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        }
    }

    private Mission map(ResultSet result) throws Exception {
        return new Mission(result.getLong("id"), result.getLong("village_id"),
                result.getString("player_uuid") == null ? null : UUID.fromString(result.getString("player_uuid")),
                MissionType.valueOf(result.getString("mission_type")), result.getInt("target"), result.getInt("xp"),
                Mission.Status.valueOf(result.getString("status")), result.getInt("progress"),
                result.getLong("created_at"), result.getLong("accepted_at"), result.getLong("completed_at"),
                result.getInt("pinned") == 1);
    }

    public record Completion(Mission mission, Village village, int awardedXp) {
    }
}
