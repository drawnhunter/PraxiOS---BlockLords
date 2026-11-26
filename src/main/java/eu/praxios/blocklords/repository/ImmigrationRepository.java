package eu.praxios.blocklords.repository;

import eu.praxios.blocklords.database.Database;
import eu.praxios.blocklords.model.*;
import eu.praxios.blocklords.util.ResidentNames;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class ImmigrationRepository {
    private final Database database;
    private final VillageRepository villages;
    private final StorageRepository storage;
    private final ResidentRepository residents;

    public ImmigrationRepository(Database database, VillageRepository villages,
                                 StorageRepository storage, ResidentRepository residents) {
        this.database = database;
        this.villages = villages;
        this.storage = storage;
        this.residents = residents;
    }

    public Snapshot snapshot(long villageId) throws Exception {
        return database.read(connection -> new Snapshot(state(connection, villageId), waiting(connection, villageId)));
    }

    public ImmigrationState state(long villageId, long nextArrivalAt, String reason) throws Exception {
        return database.inTransaction(connection -> {
            ImmigrationState previous = state(connection, villageId);
            if (previous.nextArrivalAt() == nextArrivalAt && Objects.equals(previous.reason(), reason)) return previous;
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO immigration_state(village_id, next_arrival_at, reason, updated_at) VALUES (?, ?, ?, ?) "
                            + "ON CONFLICT(village_id) DO UPDATE SET next_arrival_at=excluded.next_arrival_at, "
                            + "reason=excluded.reason, updated_at=excluded.updated_at")) {
                statement.setLong(1, villageId);
                statement.setLong(2, nextArrivalAt);
                statement.setString(3, reason);
                statement.setLong(4, System.currentTimeMillis());
                statement.executeUpdate();
            }
            return new ImmigrationState(villageId, nextArrivalAt, reason);
        });
    }

    public ImmigrationCandidate createCandidate(long villageId, UUID villagerId, long now) throws Exception {
        return database.inTransaction(connection -> {
            if (waiting(connection, villageId).isPresent()) {
                throw new IllegalStateException("Es wartet bereits ein Wanderer für dieses Dorf.");
            }
            long id;
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO immigration_candidates(village_id, villager_uuid, status, created_at, xp_awarded) "
                            + "VALUES (?, ?, 'WAITING', ?, 0)", Statement.RETURN_GENERATED_KEYS)) {
                statement.setLong(1, villageId);
                statement.setString(2, villagerId.toString());
                statement.setLong(3, now);
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (!keys.next()) throw new IllegalStateException("SQLite hat keine Wanderer-ID zurückgegeben.");
                    id = keys.getLong(1);
                }
            }
            return new ImmigrationCandidate(id, villageId, villagerId, ImmigrationCandidate.Status.WAITING, now, 0L, 0);
        });
    }

    public Optional<ImmigrationCandidate> waitingById(long villageId, long candidateId) throws Exception {
        return database.read(connection -> waitingById(connection, villageId, candidateId));
    }

    public Optional<ImmigrationCandidate> reject(long villageId, long candidateId, long now) throws Exception {
        return completeWaiting(villageId, candidateId, ImmigrationCandidate.Status.REJECTED, now);
    }

    public Optional<ImmigrationCandidate> cancel(long villageId, long candidateId, long now) throws Exception {
        return completeWaiting(villageId, candidateId, ImmigrationCandidate.Status.CANCELLED, now);
    }

    public Optional<ImmigrationCandidate> markLost(UUID villagerId, long now) throws Exception {
        return database.inTransaction(connection -> {
            Optional<ImmigrationCandidate> found = waitingByVillager(connection, villagerId);
            if (found.isEmpty()) return Optional.empty();
            ImmigrationCandidate candidate = found.get();
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE immigration_candidates SET status='LOST', decided_at=? WHERE id=? AND status='WAITING'")) {
                statement.setLong(1, now);
                statement.setLong(2, candidate.id());
                if (statement.executeUpdate() != 1) return Optional.empty();
            }
            return Optional.of(new ImmigrationCandidate(candidate.id(), candidate.villageId(), candidate.villagerId(),
                    ImmigrationCandidate.Status.LOST, candidate.createdAt(), now, 0));
        });
    }

    public Acceptance accept(long villageId, long candidateId, Resident newcomer, int foodCost,
                             int firstXp, int repeatXp, long now) throws Exception {
        return database.inTransaction(connection -> {
            ImmigrationCandidate candidate = waitingById(connection, villageId, candidateId)
                    .orElseThrow(() -> new IllegalArgumentException("Dieser Wanderer ist nicht mehr verfügbar."));
            if (!storage.consumeCost(connection, villageId, newcomer.villager(),
                    java.util.Map.of(ResourceType.NAHRUNG, foodCost), "IMMIGRATION")) {
                throw new IllegalStateException("Der Dorfvorrat enthält nicht genug Nahrung für die Aufnahme.");
            }
            int previous = acceptedCount(connection, villageId);
            int xp = previous == 0 ? firstXp : repeatXp;
            Resident identified = identify(connection, newcomer, now);
            residents.touchAll(connection, villageId, List.of(identified));
            Village updatedVillage = villages.addXp(connection, villageId, xp);
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE immigration_candidates SET status='ACCEPTED', decided_at=?, xp_awarded=? "
                            + "WHERE id=? AND status='WAITING'")) {
                statement.setLong(1, now);
                statement.setInt(2, xp);
                statement.setLong(3, candidateId);
                if (statement.executeUpdate() != 1) {
                    throw new IllegalStateException("Der Wanderer wurde bereits entschieden.");
                }
            }
            ImmigrationCandidate accepted = new ImmigrationCandidate(candidateId, villageId, candidate.villagerId(),
                    ImmigrationCandidate.Status.ACCEPTED, candidate.createdAt(), now, xp);
            return new Acceptance(accepted, identified, updatedVillage);
        });
    }

    private Resident identify(Connection connection, Resident newcomer, long now) throws Exception {
        int number;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COALESCE(MAX(resident_number), 0) + 1 FROM residents WHERE village_id=?")) {
            statement.setLong(1, newcomer.villageId());
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                number = result.getInt(1);
            }
        }
        Set<String> names = new HashSet<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT display_name FROM residents WHERE village_id=?")) {
            statement.setLong(1, newcomer.villageId());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    String name = result.getString("display_name");
                    if (name != null) names.add(name.toLowerCase(Locale.ROOT));
                }
            }
        }
        String name = newcomer.displayName();
        if (name == null || name.isBlank() || names.contains(name.toLowerCase(Locale.ROOT))) {
            name = uniqueName(names);
        }
        return new Resident(newcomer.villageId(), newcomer.villager(), Resident.Status.CONFIRMED, true,
                newcomer.bed(), newcomer.meetingPoint(), newcomer.firstSeen(), now, name, number, false);
    }

    private String uniqueName(Set<String> used) {
        for (int attempt = 0; attempt < 50; attempt++) {
            String name = ResidentNames.random();
            if (used.add(name.toLowerCase(Locale.ROOT))) return name;
        }
        throw new IllegalStateException("Kein freier Bewohnername verfügbar.");
    }

    private int acceptedCount(Connection connection, long villageId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM immigration_candidates WHERE village_id=? AND status='ACCEPTED'")) {
            statement.setLong(1, villageId);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    private Optional<ImmigrationCandidate> completeWaiting(long villageId, long candidateId,
                                                           ImmigrationCandidate.Status target, long now) throws Exception {
        return database.inTransaction(connection -> {
            Optional<ImmigrationCandidate> candidate = waitingById(connection, villageId, candidateId);
            if (candidate.isEmpty()) return Optional.empty();
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE immigration_candidates SET status=?, decided_at=? WHERE id=? AND status='WAITING'")) {
                statement.setString(1, target.name());
                statement.setLong(2, now);
                statement.setLong(3, candidateId);
                if (statement.executeUpdate() != 1) return Optional.empty();
            }
            ImmigrationCandidate current = candidate.get();
            return Optional.of(new ImmigrationCandidate(candidateId, villageId, current.villagerId(), target,
                    current.createdAt(), now, 0));
        });
    }

    private ImmigrationState state(Connection connection, long villageId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM immigration_state WHERE village_id=?")) {
            statement.setLong(1, villageId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return new ImmigrationState(villageId, 0L, "");
                return new ImmigrationState(villageId, result.getLong("next_arrival_at"), result.getString("reason"));
            }
        }
    }

    private Optional<ImmigrationCandidate> waiting(Connection connection, long villageId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM immigration_candidates WHERE village_id=? AND status='WAITING' ORDER BY id LIMIT 1")) {
            statement.setLong(1, villageId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        }
    }

    private Optional<ImmigrationCandidate> waitingById(Connection connection, long villageId, long candidateId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM immigration_candidates WHERE id=? AND village_id=? AND status='WAITING'")) {
            statement.setLong(1, candidateId);
            statement.setLong(2, villageId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        }
    }

    private Optional<ImmigrationCandidate> waitingByVillager(Connection connection, UUID villagerId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM immigration_candidates WHERE villager_uuid=? AND status='WAITING'")) {
            statement.setString(1, villagerId.toString());
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        }
    }

    private ImmigrationCandidate map(ResultSet result) throws Exception {
        return new ImmigrationCandidate(result.getLong("id"), result.getLong("village_id"),
                UUID.fromString(result.getString("villager_uuid")),
                ImmigrationCandidate.Status.valueOf(result.getString("status")), result.getLong("created_at"),
                result.getLong("decided_at"), result.getInt("xp_awarded"));
    }

    public record Snapshot(ImmigrationState state, Optional<ImmigrationCandidate> waiting) {
    }

    public record Acceptance(ImmigrationCandidate candidate, Resident resident, Village village) {
    }
}
