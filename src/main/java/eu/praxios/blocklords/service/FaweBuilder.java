package eu.praxios.blocklords.service;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import eu.praxios.blocklords.config.BlockLordsConfig;
import eu.praxios.blocklords.model.BlueprintType;
import eu.praxios.blocklords.model.BuildLocation;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** FAWE-Bau mit Chunk-Prüfung, Aktions-ID, vollständigem Fortschrittslog und genau einem Ergebnis-Callback. */
public final class FaweBuilder {
    private final JavaPlugin plugin;
    private final TaskRunner tasks;
    private final LogService logs;
    private final Supplier<BlockLordsConfig> config;

    public FaweBuilder(JavaPlugin plugin, TaskRunner tasks, LogService logs, Supplier<BlockLordsConfig> config) {
        this.plugin = plugin;
        this.tasks = tasks;
        this.logs = logs;
        this.config = config;
    }

    public void paste(BlueprintType type, BuildLocation location,
                      Consumer<Integer> success, Consumer<Throwable> failure) {
        paste(type, location, Set.of(), success, failure);
    }

    public void paste(BlueprintType type, BuildLocation location, Set<BlockVector3> excluded,
                      Consumer<Integer> success, Consumer<Throwable> failure) {
        String id = logs.actionId("PASTE");
        long started = System.nanoTime();
        AtomicBoolean finished = new AtomicBoolean(false);
        Consumer<Integer> succeed = count -> {
            if (finished.compareAndSet(false, true)) {
                logs.action(id, "SUCCESS", count + " Blöcke, " + elapsedMillis(started) + " ms");
                success.accept(count);
            } else {
                logs.action(id, "LATE_SUCCESS_IGNORED", count + " Blöcke nach Timeout/Fehler");
            }
        };
        Consumer<Throwable> fail = throwable -> {
            if (finished.compareAndSet(false, true)) {
                logs.error(id, "FAILED nach " + elapsedMillis(started) + " ms: " + throwable.getMessage(), throwable);
                failure.accept(throwable);
            } else {
                logs.action(id, "LATE_FAILURE_IGNORED", throwable.getMessage());
            }
        };

        logs.action(id, "START", type + " @ " + location);
        World world = Bukkit.getWorld(location.world());
        if (world == null) {
            fail.accept(new IllegalStateException("Bauwelt ist nicht geladen."));
            return;
        }
        Set<Long> requiredChunks = requiredChunks(type, location, excluded);
        long loaded = requiredChunks.stream().filter(chunk -> world.isChunkLoaded(chunkX(chunk), chunkZ(chunk))).count();
        logs.action(id, "CHUNK_CHECK", loaded + "/" + requiredChunks.size() + " geladen");
        if (loaded != requiredChunks.size()) {
            fail.accept(new IllegalStateException("Nicht alle Ziel-Chunks sind geladen (" + loaded + "/"
                    + requiredChunks.size() + "). Bitte Gebiet vollständig betreten."));
            return;
        }

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!finished.get()) {
                fail.accept(new TimeoutException("FAWE-Aktion hat das Zeitlimit überschritten."));
            }
        }, config.get().actionTimeoutTicks());
        tasks.scan(() -> {
            logs.action(id, "EXECUTOR_START", Thread.currentThread().getName());
            int count = 0;
            try (EditSession session = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(world))) {
                try {
                    logs.action(id, "EDIT_SESSION_OPEN", "Welt " + world.getName());
                    for (BlueprintBlocks.OffsetBlock block : BlueprintBlocks.blocks(type)) {
                        int[] rotated = rotate(block.x(), block.z(), location.rotation());
                        BlockVector3 destination = BlockVector3.at(location.x() + rotated[0],
                                location.y() + block.y(), location.z() + rotated[1]);
                        if (excluded.contains(destination)) continue;
                        session.setBlock(destination,
                                (com.sk89q.worldedit.function.pattern.Pattern) BukkitAdapter.adapt(rotateData(block, location.rotation())));
                        count++;
                    }
                    logs.action(id, "QUEUED", count + " Blöcke");
                    session.flushQueue();
                    logs.action(id, "FLUSH_DONE", "EditSession wird geschlossen");
                    return count;
                } catch (Exception exception) {
                    logs.action(id, "UNDO_START", exception.getMessage());
                    try {
                        session.undo(session);
                        logs.action(id, "UNDO_DONE", "Rollback abgeschlossen");
                    } catch (Exception undoError) {
                        exception.addSuppressed(undoError);
                    }
                    throw exception;
                }
            }
        }, succeed, fail);
    }

    private BlockData rotateData(BlueprintBlocks.OffsetBlock block, int rotation) {
        BlockData data = block.data().clone();
        if (data instanceof Directional directional) {
            BlockFace face = directional.getFacing();
            for (int step = 0; step < ((rotation % 360) + 360) % 360 / 90; step++) {
                face = switch (face) {
                    case NORTH -> BlockFace.EAST;
                    case EAST -> BlockFace.SOUTH;
                    case SOUTH -> BlockFace.WEST;
                    case WEST -> BlockFace.NORTH;
                    default -> face;
                };
            }
            directional.setFacing(face);
        }
        return data;
    }

    private Set<Long> requiredChunks(BlueprintType type, BuildLocation location, Set<BlockVector3> excluded) {
        Set<Long> chunks = new HashSet<>();
        for (BlueprintBlocks.OffsetBlock block : BlueprintBlocks.blocks(type)) {
            int[] rotated = rotate(block.x(), block.z(), location.rotation());
            int x = location.x() + rotated[0];
            int z = location.z() + rotated[1];
            if (excluded.contains(BlockVector3.at(x, location.y() + block.y(), z))) continue;
            chunks.add(pack(Math.floorDiv(x, 16), Math.floorDiv(z, 16)));
        }
        return chunks;
    }

    private static long pack(int x, int z) {
        return ((long) x << 32) | (z & 0xffffffffL);
    }

    private static int chunkX(long packed) {
        return (int) (packed >> 32);
    }

    private static int chunkZ(long packed) {
        return (int) packed;
    }

    private static long elapsedMillis(long started) {
        return (System.nanoTime() - started) / 1_000_000L;
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
