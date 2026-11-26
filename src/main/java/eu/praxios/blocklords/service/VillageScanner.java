package eu.praxios.blocklords.service;

import eu.praxios.blocklords.config.BlockLordsConfig;
import eu.praxios.blocklords.model.ClaimRegion;
import eu.praxios.blocklords.model.Village;
import eu.praxios.blocklords.model.VillageStats;
import eu.praxios.blocklords.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Explizite, gestaffelte Scans nur auf Anfrage; keine periodischen Vollscans im Tick. */
public final class VillageScanner {
    private final JavaPlugin plugin;
    private final Supplier<BlockLordsConfig> config;
    private final TaskRunner tasks;
    private final Map<Long, VillageStats> cache = new ConcurrentHashMap<>();
    private final Map<Long, List<Consumer<VillageStats>>> callbacks = new ConcurrentHashMap<>();
    private final Map<Long, BukkitRunnable> running = new ConcurrentHashMap<>();

    public VillageScanner(JavaPlugin plugin, Supplier<BlockLordsConfig> config, TaskRunner tasks) {
        this.plugin = plugin;
        this.config = config;
        this.tasks = tasks;
    }

    public Optional<VillageStats> fresh(Village village) {
        VillageStats stats = cache.get(village.id());
        if (stats == null || System.currentTimeMillis() - stats.scannedAt() > config.get().scanCacheMillis()) {
            return Optional.empty();
        }
        return Optional.of(stats);
    }

    public void request(Village village, Consumer<VillageStats> callback) {
        if (fresh(village).isPresent()) {
            callback.accept(cache.get(village.id()));
            return;
        }
        List<Consumer<VillageStats>> waiting = callbacks.computeIfAbsent(village.id(), ignored -> new ArrayList<>());
        synchronized (waiting) {
            waiting.add(callback);
        }
        if (running.containsKey(village.id())) return;

        World world = Bukkit.getWorld(village.region().world());
        if (world == null) {
            callbacks.remove(village.id());
            return;
        }
        List<ChunkPosition> positions = chunkPositions(village);
        List<SnapshotWithPosition> snapshots = new ArrayList<>();
        int[] index = {0};
        int[] loaded = {0};

        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                int count = 0;
                while (index[0] < positions.size() && count < config.get().chunksPerTick()) {
                    ChunkPosition position = positions.get(index[0]++);
                    if (world.isChunkLoaded(position.x(), position.z())) {
                        snapshots.add(new SnapshotWithPosition(
                                world.getChunkAt(position.x(), position.z()).getChunkSnapshot(), position));
                        loaded[0]++;
                    }
                    count++;
                }
                if (index[0] < positions.size()) return;

                running.remove(village.id());
                int villagers = (int) world.getNearbyEntities(new BoundingBox(
                                village.region().bellX() - village.region().radius(), world.getMinHeight(),
                                village.region().bellZ() - village.region().radius(),
                                village.region().bellX() + village.region().radius() + 1, world.getMaxHeight(),
                                village.region().bellZ() + village.region().radius() + 1)).stream()
                        .filter(entity -> entity instanceof Villager)
                        .filter(entity -> village.region().contains(world.getUID(), entity.getLocation().getX(), entity.getLocation().getZ()))
                        .count();
                tasks.scan(() -> countBeds(snapshots, world.getMinHeight(), world.getMaxHeight(), village.region()), bedScan -> {
                    VillageStats stats = new VillageStats(villagers, bedScan.count(), loaded[0], positions.size(),
                            System.currentTimeMillis(), bedScan.locations());
                    cache.put(village.id(), stats);
                    List<Consumer<VillageStats>> done = callbacks.remove(village.id());
                    if (done != null) {
                        synchronized (done) {
                            done.forEach(consumer -> consumer.accept(stats));
                        }
                    }
                }, error -> {
                    callbacks.remove(village.id());
                    plugin.getLogger().warning("Dorfscan fehlgeschlagen: " + error.getMessage());
                });
            }
        };
        running.put(village.id(), runnable);
        runnable.runTaskTimer(plugin, 1L, 1L);
    }

    public void requestAndNotify(Village village, Player player) {
        Msg.info(player, "Dorfvermessung für '" + village.name() + "' gestartet; nur geladene Chunks werden sofort erfasst.");
        request(village, stats -> Msg.info(player, "Vermessung '" + village.name() + "': " + stats.villagers()
                + " Bewohner, " + stats.beds() + " Betten, " + stats.loadedChunks() + "/" + stats.totalChunks()
                + " geladene Chunks" + (stats.complete() ? "." : " (Ergebnis unvollständig).")));
    }

    private BedScan countBeds(List<SnapshotWithPosition> snapshots, int minHeight, int maxHeight, ClaimRegion region) {
        int count = 0;
        List<eu.praxios.blocklords.model.BlockLink> locations = new ArrayList<>();
        for (SnapshotWithPosition entry : snapshots) {
            ChunkSnapshot snapshot = entry.snapshot();
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    int worldX = entry.position().x() * 16 + x;
                    int worldZ = entry.position().z() * 16 + z;
                    if (!region.contains(region.world(), worldX, worldZ)) continue;
                    for (int y = minHeight; y < maxHeight; y++) {
                        if (Tag.BEDS.isTagged(snapshot.getBlockType(x, y, z))) {
                            locations.add(new eu.praxios.blocklords.model.BlockLink(region.world(), worldX, y, worldZ));
                            count++;
                        }
                    }
                }
            }
        }
        return new BedScan(count / 2, locations);
    }

    private List<ChunkPosition> chunkPositions(Village village) {
        List<ChunkPosition> positions = new ArrayList<>();
        int minX = Math.floorDiv(village.region().bellX() - village.region().radius(), 16);
        int maxX = Math.floorDiv(village.region().bellX() + village.region().radius(), 16);
        int minZ = Math.floorDiv(village.region().bellZ() - village.region().radius(), 16);
        int maxZ = Math.floorDiv(village.region().bellZ() + village.region().radius(), 16);
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                positions.add(new ChunkPosition(x, z));
            }
        }
        return positions;
    }

    public void close() {
        running.values().forEach(BukkitRunnable::cancel);
        running.clear();
        callbacks.clear();
    }

    private record BedScan(int count, List<eu.praxios.blocklords.model.BlockLink> locations) {
    }

    private record ChunkPosition(int x, int z) {
    }

    private record SnapshotWithPosition(ChunkSnapshot snapshot, ChunkPosition position) {
    }
}
