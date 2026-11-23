package eu.praxios.blocklords.repository;

import eu.praxios.blocklords.database.Database;
import eu.praxios.blocklords.model.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

public final class ConstructionRepository {
    private final Database database;

    public ConstructionRepository(Database database) {
        this.database = database;
    }

    public BuildSite createSite(long villageId, BlueprintType type, BuildLocation location, UUID creator) throws Exception {
        long now = System.currentTimeMillis();
        return database.inTransaction(connection -> {
            long id;
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO build_sites(village_id, type, world_uuid, x, y, z, rotation, status, created_by, created_at) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, 'COLLECTING', ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                statement.setLong(1, villageId);
                statement.setString(2, type.name());
                statement.setString(3, location.world().toString());
                statement.setInt(4, location.x());
                statement.setInt(5, location.y());
                statement.setInt(6, location.z());
                statement.setInt(7, location.rotation());
                statement.setString(8, creator.toString());
                statement.setLong(9, now);
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    keys.next();
                    id = keys.getLong(1);
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO build_site_resources(site_id, resource, required, contributed) VALUES (?, ?, ?, 0)")) {
                for (var entry : type.cost().entrySet()) {
                    statement.setLong(1, id);
                    statement.setString(2, entry.getKey().name());
                    statement.setInt(3, entry.getValue());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
            return new BuildSite(id, villageId, type, location, BuildSite.Status.COLLECTING, creator, now,
                    type.cost(), Map.of());
        });
    }

    public List<BuildSite> findSites(long villageId) throws Exception {
        return database.read(connection -> {
            List<BuildSite> sites = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM build_sites WHERE village_id = ? ORDER BY id")) {
                statement.setLong(1, villageId);
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) sites.add(mapSite(connection, result));
                }
            }
            return sites;
        });
    }

    public Optional<BuildSite> findSite(long id) throws Exception {
        return database.read(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM build_sites WHERE id = ?")) {
                statement.setLong(1, id);
                try (ResultSet result = statement.executeQuery()) {
                    return result.next() ? Optional.of(mapSite(connection, result)) : Optional.empty();
                }
            }
        });
    }

    public int contribute(long siteId, UUID player, ResourceType resource, int amount) throws Exception {
        return database.inTransaction(connection -> contributeInternal(connection, siteId, player, resource, amount));
    }

    public int fundFromStorage(long siteId, UUID player, ResourceType resource, int amount) throws Exception {
        return database.inTransaction(connection -> {
            BuildSite site = findSite(connection, siteId);
            int actual = Math.min(amount, site.remaining(resource));
            if (actual <= 0) return 0;
            Integer available;
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT amount FROM storage WHERE village_id = ? AND resource = ?")) {
                statement.setLong(1, site.villageId());
                statement.setString(2, resource.name());
                try (ResultSet result = statement.executeQuery()) {
                    available = result.next() ? result.getInt(1) : 0;
                }
            }
            actual = Math.min(actual, available);
            if (actual <= 0) return 0;
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE storage SET amount = amount - ? WHERE village_id = ? AND resource = ?")) {
                statement.setInt(1, actual);
                statement.setLong(2, site.villageId());
                statement.setString(3, resource.name());
                statement.executeUpdate();
            }
            insertStorageTransaction(connection, site.villageId(), player, resource, -actual, "BUILD_SITE");
            contributeInternal(connection, siteId, player, resource, actual);
            return actual;
        });
    }

    public void markBuilding(long siteId) throws Exception {
        database.inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE build_sites SET status = 'BUILDING' WHERE id = ? AND status = 'READY'")) {
                statement.setLong(1, siteId);
                if (statement.executeUpdate() != 1) {
                    throw new IllegalStateException("Baustelle ist nicht bereit oder wird bereits gebaut.");
                }
            }
            return null;
        });
    }

    public void markReady(long siteId) throws Exception {
        database.inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE build_sites SET status = 'READY' WHERE id = ? AND status = 'BUILDING'")) {
                statement.setLong(1, siteId);
                statement.executeUpdate();
            }
            return null;
        });
    }

    public Building completeSite(long siteId) throws Exception {
        return database.inTransaction(connection -> {
            BuildSite site;
            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM build_sites WHERE id = ? AND status = 'BUILDING'")) {
                statement.setLong(1, siteId);
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) throw new IllegalStateException("Baustelle ist nicht im Bauzustand.");
                    site = mapSite(connection, result);
                }
            }
            long now = System.currentTimeMillis();
            long id;
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO buildings(village_id, type, level, world_uuid, x, y, z, rotation, created_at) "
                            + "VALUES (?, ?, 1, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                statement.setLong(1, site.villageId());
                statement.setString(2, site.type().name());
                statement.setString(3, site.location().world().toString());
                statement.setInt(4, site.location().x());
                statement.setInt(5, site.location().y());
                statement.setInt(6, site.location().z());
                statement.setInt(7, site.location().rotation());
                statement.setLong(8, now);
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    keys.next();
                    id = keys.getLong(1);
                }
            }
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM build_sites WHERE id = ?")) {
                statement.setLong(1, siteId);
                statement.executeUpdate();
            }
            return new Building(id, site.villageId(), site.type(), 1, site.location(), now);
        });
    }

    public List<Building> findBuildings(long villageId) throws Exception {
        return database.read(connection -> {
            List<Building> buildings = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM buildings WHERE village_id = ? ORDER BY id")) {
                statement.setLong(1, villageId);
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) buildings.add(mapBuilding(result));
                }
            }
            return buildings;
        });
    }

    public Optional<Building> findBuilding(long id) throws Exception {
        return database.read(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM buildings WHERE id = ?")) {
                statement.setLong(1, id);
                try (ResultSet result = statement.executeQuery()) {
                    return result.next() ? Optional.of(mapBuilding(result)) : Optional.empty();
                }
            }
        });
    }

    public void deleteSite(long siteId) throws Exception {
        database.inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM build_sites WHERE id = ?")) {
                statement.setLong(1, siteId);
                statement.executeUpdate();
            }
            return null;
        });
    }

    private int contributeInternal(Connection connection, long siteId, UUID player, ResourceType resource, int amount) throws Exception {
        BuildSite site = findSite(connection, siteId);
        if (site.status() == BuildSite.Status.BUILDING) throw new IllegalStateException("Diese Baustelle wird bereits gebaut.");
        int actual = Math.min(amount, site.remaining(resource));
        if (actual <= 0) return 0;
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE build_site_resources SET contributed = contributed + ? WHERE site_id = ? AND resource = ?")) {
            statement.setInt(1, actual);
            statement.setLong(2, siteId);
            statement.setString(3, resource.name());
            statement.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO build_site_contributors(site_id, player_uuid, resource, amount, contributed_at) VALUES (?, ?, ?, ?, ?)")) {
            statement.setLong(1, siteId);
            statement.setString(2, player.toString());
            statement.setString(3, resource.name());
            statement.setInt(4, actual);
            statement.setLong(5, System.currentTimeMillis());
            statement.executeUpdate();
        }
        boolean complete = site.required().keySet().stream()
                .allMatch(requiredResource -> site.remaining(requiredResource) - (requiredResource == resource ? actual : 0) == 0);
        if (complete) {
            try (PreparedStatement statement = connection.prepareStatement("UPDATE build_sites SET status = 'READY' WHERE id = ?")) {
                statement.setLong(1, siteId);
                statement.executeUpdate();
            }
        }
        return actual;
    }

    private void insertStorageTransaction(Connection connection, long villageId, UUID player,
                                          ResourceType resource, int delta, String reason) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO storage_transactions(village_id, player_uuid, resource, delta, reason, created_at) VALUES (?, ?, ?, ?, ?, ?)")) {
            statement.setLong(1, villageId);
            statement.setString(2, player.toString());
            statement.setString(3, resource.name());
            statement.setInt(4, delta);
            statement.setString(5, reason);
            statement.setLong(6, System.currentTimeMillis());
            statement.executeUpdate();
        }
    }

    private BuildSite findSite(Connection connection, long id) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM build_sites WHERE id = ?")) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new IllegalArgumentException("Baustelle " + id + " existiert nicht.");
                return mapSite(connection, result);
            }
        }
    }

    private BuildSite mapSite(Connection connection, ResultSet result) throws Exception {
        long id = result.getLong("id");
        BlueprintType type = BlueprintType.valueOf(result.getString("type"));
        Map<ResourceType, Integer> contributed = new EnumMap<>(ResourceType.class);
        try (PreparedStatement statement = connection.prepareStatement("SELECT resource, contributed FROM build_site_resources WHERE site_id = ?")) {
            statement.setLong(1, id);
            try (ResultSet resources = statement.executeQuery()) {
                while (resources.next()) {
                    contributed.put(ResourceType.valueOf(resources.getString(1)), resources.getInt(2));
                }
            }
        }
        return new BuildSite(id, result.getLong("village_id"), type,
                new BuildLocation(UUID.fromString(result.getString("world_uuid")), result.getInt("x"), result.getInt("y"),
                        result.getInt("z"), result.getInt("rotation")),
                BuildSite.Status.valueOf(result.getString("status")), UUID.fromString(result.getString("created_by")),
                result.getLong("created_at"), type.cost(), Map.copyOf(contributed));
    }

    private static Building mapBuilding(ResultSet result) throws Exception {
        return new Building(result.getLong("id"), result.getLong("village_id"),
                BlueprintType.valueOf(result.getString("type")), result.getInt("level"),
                new BuildLocation(UUID.fromString(result.getString("world_uuid")), result.getInt("x"), result.getInt("y"),
                        result.getInt("z"), result.getInt("rotation")), result.getLong("created_at"));
    }
}
