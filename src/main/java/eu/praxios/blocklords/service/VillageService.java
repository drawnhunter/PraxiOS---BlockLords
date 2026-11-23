package eu.praxios.blocklords.service;

import eu.praxios.blocklords.model.LegacyPlot;
import eu.praxios.blocklords.model.Role;
import eu.praxios.blocklords.model.Village;
import eu.praxios.blocklords.model.VillageStatus;
import eu.praxios.blocklords.repository.LegacyRepository;
import eu.praxios.blocklords.repository.VillageRepository;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Main-Thread-Cache für die kleine, begrenzte Dorfmenge; keine DB-Abfrage in Schutzevents. */
public final class VillageService {
    private final VillageRepository villages;
    private final LegacyRepository legacy;
    private final TaskRunner tasks;
    private volatile Map<Long, Village> villageCache = Map.of();
    private volatile Map<Long, Map<UUID, Role>> memberCache = Map.of();
    private volatile List<LegacyPlot> legacyCache = List.of();
    private final Map<String, Long> activityThrottle = new ConcurrentHashMap<>();

    public VillageService(VillageRepository villages, LegacyRepository legacy, TaskRunner tasks) {
        this.villages = villages;
        this.legacy = legacy;
        this.tasks = tasks;
    }

    public void loadInitial() throws Exception {
        villageCache = index(villages.findAll());
        memberCache = copyMembers(villages.findAllMembers());
        legacyCache = List.copyOf(legacy.findAll());
    }

    public void refresh() {
        tasks.database(() -> new Snapshot(villages.findAll(), villages.findAllMembers(), legacy.findAll()), snapshot -> {
            villageCache = index(snapshot.villages());
            memberCache = copyMembers(snapshot.members());
            legacyCache = List.copyOf(snapshot.legacy());
        }, error -> {
            throw new IllegalStateException("Dorf-Cache konnte nicht aktualisiert werden", error);
        });
    }

    public List<Village> all() {
        return List.copyOf(villageCache.values());
    }

    public Optional<Village> byId(long id) {
        return Optional.ofNullable(villageCache.get(id));
    }

    public Optional<Village> byName(String name) {
        return villageCache.values().stream().filter(village -> village.name().equalsIgnoreCase(name)).findFirst();
    }

    public Optional<Village> at(Location location) {
        if (location.getWorld() == null) return Optional.empty();
        UUID world = location.getWorld().getUID();
        return villageCache.values().stream()
                .filter(village -> village.region().contains(world, location.getX(), location.getZ()))
                .findFirst();
    }

    public Optional<Village> atBell(Block block) {
        UUID world = block.getWorld().getUID();
        return villageCache.values().stream().filter(village -> village.region().world().equals(world)
                && village.region().bellX() == block.getX()
                && village.region().bellY() == block.getY()
                && village.region().bellZ() == block.getZ()).findFirst();
    }

    public List<Village> owned(UUID player) {
        return villageCache.values().stream().filter(village -> village.owner().equals(player)).toList();
    }

    public Role role(long villageId, UUID player) {
        Village village = villageCache.get(villageId);
        if (village != null && village.owner().equals(player)) return Role.LANDHERR;
        return memberCache.getOrDefault(villageId, Map.of()).get(player);
    }

    public int memberCount(long villageId) {
        return memberCache.getOrDefault(villageId, Map.of()).size();
    }

    public Optional<LegacyPlot> legacyAt(Location location) {
        if (location.getWorld() == null) return Optional.empty();
        UUID world = location.getWorld().getUID();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        return legacyCache.stream().filter(plot -> plot.contains(world, x, y, z)).findFirst();
    }

    public List<LegacyPlot> legacyPlots() {
        return legacyCache;
    }

    public void addVillage(Village village) {
        Map<Long, Village> updated = new HashMap<>(villageCache);
        updated.put(village.id(), village);
        villageCache = Map.copyOf(updated);
        Map<Long, Map<UUID, Role>> members = new HashMap<>(memberCache);
        members.put(village.id(), Map.of(village.owner(), Role.LANDHERR));
        memberCache = copyMembers(members);
    }

    public void markActivity(Village village, UUID player) {
        long now = System.currentTimeMillis();
        String throttleKey = village.id() + ":" + player;
        Long previous = activityThrottle.get(throttleKey);
        if (previous != null && now - previous < 300_000L) return;
        activityThrottle.put(throttleKey, now);
        Village updated = new Village(village.id(), village.name(), village.owner(), village.region(), village.level(),
                village.xp(), VillageStatus.ACTIVE, village.createdAt(), now);
        Map<Long, Village> updatedVillages = new HashMap<>(villageCache);
        updatedVillages.put(village.id(), updated);
        villageCache = Map.copyOf(updatedVillages);
        tasks.databaseVoid(() -> {
            villages.updateActivity(village.id(), now);
            return null;
        }, error -> {
            throw new IllegalStateException("Dorfaktivität konnte nicht gespeichert werden", error);
        });
    }

    public void updateStatus(Village village, VillageStatus status) {
        if (village.status() == status) return;
        Village updated = new Village(village.id(), village.name(), village.owner(), village.region(), village.level(),
                village.xp(), status, village.createdAt(), village.lastActivity());
        Map<Long, Village> updatedVillages = new HashMap<>(villageCache);
        updatedVillages.put(village.id(), updated);
        villageCache = Map.copyOf(updatedVillages);
        tasks.databaseVoid(() -> {
            villages.updateStatus(village.id(), status);
            return null;
        }, error -> {
            throw new IllegalStateException("Dorfstatus konnte nicht gespeichert werden", error);
        });
    }

    private static Map<Long, Village> index(List<Village> villages) {
        Map<Long, Village> indexed = new HashMap<>();
        for (Village village : villages) indexed.put(village.id(), village);
        return Map.copyOf(indexed);
    }

    private static Map<Long, Map<UUID, Role>> copyMembers(Map<Long, Map<UUID, Role>> source) {
        Map<Long, Map<UUID, Role>> result = new HashMap<>();
        source.forEach((id, members) -> result.put(id, Map.copyOf(members)));
        return Map.copyOf(result);
    }

    private record Snapshot(List<Village> villages, Map<Long, Map<UUID, Role>> members, List<LegacyPlot> legacy) {
        private Snapshot {
            villages = new ArrayList<>(villages);
            members = new HashMap<>(members);
            legacy = new ArrayList<>(legacy);
        }
    }
}
