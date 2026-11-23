package eu.praxios.blocklords.repository;

import eu.praxios.blocklords.database.Database;

import java.sql.PreparedStatement;
import java.util.Collection;
import java.util.UUID;

public final class OwnershipRepository {
    private final Database database;

    public OwnershipRepository(Database database) {
        this.database = database;
    }

    public void recordPlacements(Collection<BlockPosition> positions, UUID owner) throws Exception {
        if (positions.isEmpty()) return;
        long now = System.currentTimeMillis();
        database.inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO block_ownership(world_uuid, x, y, z, owner_uuid, placed_at) VALUES (?, ?, ?, ?, ?, ?) "
                            + "ON CONFLICT(world_uuid, x, y, z) DO UPDATE SET owner_uuid = excluded.owner_uuid, placed_at = excluded.placed_at")) {
                for (BlockPosition position : positions) {
                    statement.setString(1, position.world().toString());
                    statement.setInt(2, position.x());
                    statement.setInt(3, position.y());
                    statement.setInt(4, position.z());
                    statement.setString(5, owner.toString());
                    statement.setLong(6, now);
                    statement.addBatch();
                }
                statement.executeBatch();
            }
            return null;
        });
    }

    public void recordRemoval(UUID world, int x, int y, int z) throws Exception {
        database.inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM block_ownership WHERE world_uuid = ? AND x = ? AND y = ? AND z = ?")) {
                statement.setString(1, world.toString());
                statement.setInt(2, x);
                statement.setInt(3, y);
                statement.setInt(4, z);
                statement.executeUpdate();
            }
            return null;
        });
    }

    public record BlockPosition(UUID world, int x, int y, int z) {
        public static BlockPosition from(org.bukkit.block.Block block) {
            return new BlockPosition(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
        }
    }
}
