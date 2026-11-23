package eu.praxios.blocklords.service;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import eu.praxios.blocklords.model.BlueprintType;
import eu.praxios.blocklords.model.BuildLocation;
import org.bukkit.Bukkit;
import org.bukkit.World;

/** FAWE-Bau mit kontrolliertem Kontext und Undo-Versuch bei Fehlern. */
public final class FaweBuilder {
    private final TaskRunner tasks;

    public FaweBuilder(TaskRunner tasks) {
        this.tasks = tasks;
    }

    public void paste(BlueprintType type, BuildLocation location,
                      java.util.function.Consumer<Integer> success,
                      java.util.function.Consumer<Throwable> failure) {
        tasks.scan(() -> {
            World world = Bukkit.getWorld(location.world());
            if (world == null) throw new IllegalStateException("Bauwelt ist nicht geladen.");
            int count = 0;
            try (EditSession session = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(world))) {
                try {
                    for (BlueprintBlocks.OffsetBlock block : BlueprintBlocks.blocks(type)) {
                        int[] rotated = rotate(block.x(), block.z(), location.rotation());
                        BlockVector3 destination = BlockVector3.at(location.x() + rotated[0],
                                location.y() + block.y(), location.z() + rotated[1]);
                        session.setBlock(destination,
                                (com.sk89q.worldedit.function.pattern.Pattern) BukkitAdapter.adapt(block.data()));
                        count++;
                    }
                    session.flushQueue();
                    return count;
                } catch (Exception exception) {
                    session.undo(session);
                    throw exception;
                }
            }
        }, success, failure);
    }

    private static int[] rotate(int x, int z, int rotation) {
        return switch (((rotation % 360) + 360) % 360) {
            case 90 -> new int[]{-z, x};
            case 180 -> new int[]{-x, -z};
            case 270 -> new int[]{z, -x};
            default -> new int[]{x, z};
        };
    }
}
