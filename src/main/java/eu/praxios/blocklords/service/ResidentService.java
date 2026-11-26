package eu.praxios.blocklords.service;

import eu.praxios.blocklords.config.BlockLordsConfig;
import eu.praxios.blocklords.model.*;
import eu.praxios.blocklords.repository.ResidentRepository;
import eu.praxios.blocklords.util.Msg;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.memory.MemoryKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/** Vanilla-POI-Daten plus explizites Glockenritual; keine erfundene Vanilla-Dorfmitgliedschaft. */
public final class ResidentService {
    private final JavaPlugin plugin;
    private final Supplier<BlockLordsConfig> config;
    private final ResidentRepository repository;
    private final VillageService villages;
    private final TaskRunner tasks;
    private final AuditService audit;
    private final VillageScanner scanner;
    private final Map<Long, List<Resident>> cache = new ConcurrentHashMap<>();
    private final Map<Long, ResidentIdentityAllocator> identities = new ConcurrentHashMap<>();
    private BukkitTask task;

    public ResidentService(JavaPlugin plugin, Supplier<BlockLordsConfig> config, ResidentRepository repository,
                           VillageService villages, TaskRunner tasks, AuditService audit) {
        this(plugin, config, repository, villages, tasks, audit, null);
    }

    public ResidentService(JavaPlugin plugin, Supplier<BlockLordsConfig> config, ResidentRepository repository,
                           VillageService villages, TaskRunner tasks, AuditService audit, VillageScanner scanner) {
        this.plugin = plugin;
        this.config = config;
        this.repository = repository;
        this.villages = villages;
        this.tasks = tasks;
        this.audit = audit;
        this.scanner = scanner;
    }

    public void loadInitial(List<Village> villageList) throws Exception {
        for (Village village : villageList) {
            List<Resident> stored = repository.find(village.id());
            List<Resident> repaired = repairIdentities(stored);
            if (!repaired.equals(stored)) {
                repository.touchAll(village.id(), repaired);
                plugin.getLogger().info("BlockLords: Bewohneridentitäten in Dorf #" + village.id() + " repariert.");
            }
            cache.put(village.id(), repaired);
            identities.put(village.id(), new ResidentIdentityAllocator(repaired));
        }
    }

    public void start() {
        stop();
        long interval = config.get().residentScanTicks();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::scanAll, interval, interval);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public List<Resident> residents(long villageId) { return cache.getOrDefault(villageId, List.of()); }

    public List<Resident> living(long villageId) {
        return residents(villageId).stream().filter(resident -> !resident.dead()).toList();
    }

    public List<Resident> dead(long villageId) {
        return residents(villageId).stream().filter(Resident::dead).toList();
    }

    public List<Resident> free(long villageId, List<Worker> workers) {
        List<UUID> assigned = workers.stream().map(Worker::villager).toList();
        return living(villageId).stream().filter(resident -> !assigned.contains(resident.villager())).toList();
    }

    public long confirmed(long villageId) {
        return living(villageId).stream().filter(resident -> resident.status() == Resident.Status.CONFIRMED).count();
    }

    public long unconfirmed(long villageId) {
        return living(villageId).stream().filter(resident -> resident.status() == Resident.Status.UNCONFIRMED).count();
    }

    public void scanAll() {
        villages.all().forEach(this::scan);
    }

    public void scan(Village village) {
        List<Villager> loaded = loadedVillagers(village);
        if (loaded.isEmpty()) return;
        Map<UUID, Resident> previous = previous(village.id());
        List<Resident> changed = new ArrayList<>();
        for (Villager villager : loaded) {
            Resident known = previous.get(villager.getUniqueId());
            if (known != null && known.dead()) continue;
            if (known != null && known.bed() != null && villager.getMemory(MemoryKey.HOME) == null) {
                villager.setMemory(MemoryKey.HOME, locationFromLink(known.bed()));
            }
            Resident evaluated = evaluate(village, villager, known, false);
            if (evaluated == null) continue;
            Resident old = previous.get(villager.getUniqueId());
            if (old == null || !old.sameLinks(evaluated)) changed.add(evaluated);
        }
        persist(village, changed);
    }

    /** Sneak + Rechtsklick auf die Dorfglocke: Vanilla-Versammlung und explizite Bestätigung. */
    public void assemble(Player player, Village village) {
        World world = world(village).orElse(null);
        if (world == null) {
            Msg.error(player, "Die Dorfwelt ist nicht geladen.");
            return;
        }
        List<Villager> loaded = loadedVillagers(village);
        Map<UUID, Resident> previous = previous(village.id());
        Location bell = bellLocation(village);
        Set<String> occupied = new HashSet<>();
        List<Resident> records = new ArrayList<>();
        int newlyConfirmed = 0;
        int withoutBed = 0;
        int teleported = 0;
        int notPlaced = 0;
        long now = System.currentTimeMillis();
        for (Villager villager : loaded) {
            BlockLink bed = bedInside(village, villager);
            Resident old = previous.get(villager.getUniqueId());
            if (old != null && old.dead()) continue;
            if (old == null || old.status() == Resident.Status.UNCONFIRMED) newlyConfirmed++;
            if (bed == null) withoutBed++;
            villager.setMemory(MemoryKey.MEETING_POINT, bell);
            Location destination = safeBellLocation(world, bell, occupied);
            if (destination != null && villager.teleport(destination)) teleported++;
            else notPlaced++;
            ResidentIdentityAllocator.Identity identity = identity(village.id(), old);
            records.add(new Resident(village.id(), villager.getUniqueId(), Resident.Status.CONFIRMED,
                    bed != null, bed, link(bell), old == null ? now : old.firstSeen(), now,
                    identity.name(), identity.number(), false));
        }
        persist(village, records);
        world.playSound(bell, Sound.BLOCK_BELL_USE, 1.0f, 1.0f);
        world.spawnParticle(Particle.HAPPY_VILLAGER, bell.clone().add(0, 1, 0), 12, 0.5, 0.5, 0.5);
        audit.log(player, "RESIDENT_ASSEMBLED", "Dorf #" + village.id() + " " + teleported + " teleportiert, "
                + notPlaced + " ohne Platz");
        Msg.success(player, "Versammlung in '" + village.name() + "': " + records.size() + " versammelt, "
                + teleported + " zur Glocke teleportiert, " + newlyConfirmed + " neu bestätigt, "
                + withoutBed + " ohne Bett" + (notPlaced > 0 ? ", " + notPlaced + " ohne freien Platz." : "."));
        player.sendActionBar(Component.text("Dorfbewohner werden zur Glocke gebracht.", NamedTextColor.GOLD));
    }

    public java.util.Optional<BlockLink> freeBed(Village village) {
        if (scanner != null && scanner.fresh(village).isPresent()) {
            for (BlockLink candidate : scanner.fresh(village).get().bedLocations()) {
                if (availableBed(village, candidate)) return java.util.Optional.of(candidate);
            }
        }
        return freeBedNear(village, bellLocation(village), 12);
    }

    private java.util.Optional<BlockLink> freeBedNear(Village village, Location center, int radius) {
        World world = center.getWorld();
        if (world == null) return java.util.Optional.empty();
        Block nearest = null;
        double best = Double.MAX_VALUE;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -4; y <= 4; y++) {
                for (int z = -radius; z <= radius; z++) {
                    int blockX = center.getBlockX() + x;
                    int blockY = center.getBlockY() + y;
                    int blockZ = center.getBlockZ() + z;
                    if (!world.isChunkLoaded(blockX >> 4, blockZ >> 4)) continue;
                    BlockLink candidate = new BlockLink(world.getUID(), blockX, blockY, blockZ);
                    if (!availableBed(village, candidate)) continue;
                    Block block = world.getBlockAt(candidate.x(), candidate.y(), candidate.z());
                    double distance = block.getLocation().distanceSquared(center);
                    if (distance < best) {
                        best = distance;
                        nearest = block;
                    }
                }
            }
        }
        return nearest == null ? java.util.Optional.empty()
                : java.util.Optional.of(new BlockLink(world.getUID(), nearest.getX(), nearest.getY(), nearest.getZ()));
    }

    private boolean availableBed(Village village, BlockLink candidate) {
        World world = Bukkit.getWorld(candidate.world());
        if (world == null || !world.isChunkLoaded(candidate.x() >> 4, candidate.z() >> 4)) return false;
        if (!(world.getBlockAt(candidate.x(), candidate.y(), candidate.z()).getBlockData()
                instanceof org.bukkit.block.data.type.Bed bed) || bed.getPart() != org.bukkit.block.data.type.Bed.Part.FOOT) {
            return false;
        }
        return living(village.id()).stream().noneMatch(resident -> candidate.equals(resident.bed()));
    }

    public void refresh(long villageId) {
        tasks.database(() -> repository.find(villageId), list -> {
            cache.put(villageId, list);
            identities.put(villageId, new ResidentIdentityAllocator(list));
        }, error -> {
            throw new IllegalStateException("Bewohnerregister konnte nicht aktualisiert werden", error);
        });
    }

    public void assignBed(Player player, Village village, Resident resident) {
        if (resident.dead()) {
            Msg.error(player, "Verlorenen Bewohnern kann kein Bett zugewiesen werden.");
            return;
        }
        Location center = Bukkit.getEntity(resident.villager()) instanceof Villager villager
                ? villager.getLocation() : bellLocation(village);
        java.util.Optional<BlockLink> freeBed = freeBedNear(village, center, 12);
        if (freeBed.isEmpty()) {
            Msg.error(player, "Kein freies Bett im Umkreis von 12 Blöcken gefunden. Bitte Betten freimachen oder bauen.");
            return;
        }
        BlockLink link = freeBed.get();
        Resident updated = new Resident(village.id(), resident.villager(), resident.status(), true, link,
                resident.meetingPoint(), resident.firstSeen(), System.currentTimeMillis(),
                resident.displayName(), resident.number(), resident.dead());
        if (Bukkit.getEntity(resident.villager()) instanceof Villager villager) {
            villager.setMemory(MemoryKey.HOME, new Location(Bukkit.getWorld(link.world()), link.x() + 0.5, link.y(), link.z() + 0.5));
        }
        persist(village, List.of(updated));
        Msg.success(player, "Bett zugewiesen: " + link.x() + "/" + link.y() + "/" + link.z() + ".");
    }

    public void markDead(UUID villager) {
        Resident current = cache.values().stream().flatMap(List::stream)
                .filter(resident -> resident.villager().equals(villager)).findFirst().orElse(null);
        if (current == null || current.dead()) return;
        Resident updated = new Resident(current.villageId(), current.villager(), current.status(), current.hasBed(),
                current.bed(), current.meetingPoint(), current.firstSeen(), System.currentTimeMillis(),
                current.displayName(), current.number(), true);
        cache.put(current.villageId(), residents(current.villageId()).stream()
                .map(resident -> resident.villager().equals(villager) ? updated : resident).toList());
        tasks.database(() -> {
            repository.markDead(villager);
            return repository.find(current.villageId());
        }, list -> cache.put(current.villageId(), list), error -> {
            throw new IllegalStateException("Verlorener Bewohner konnte nicht gespeichert werden", error);
        });
    }

    public void purgeDead(Player player, Village village) {
        Role role = villages.role(village.id(), player.getUniqueId());
        if (role != Role.LANDHERR && role != Role.GEFOLGSMANN) {
            Msg.error(player, "Nur Landherr oder Gefolgsmann dürfen verlorene Bewohner entfernen.");
            return;
        }
        if (dead(village.id()).isEmpty()) {
            Msg.info(player, "Es sind keine verlorenen Bewohner im Register.");
            return;
        }
        tasks.database(() -> repository.deleteDead(village.id()), deleted -> {
            cache.put(village.id(), living(village.id()));
            Msg.success(player, deleted + " verlorene Bewohner aus dem Register entfernt.");
        }, error -> Msg.error(player, "Verlorene Bewohner konnten nicht entfernt werden: " + error.getMessage()));
    }

    public void teleportToBell(Player player, Village village, Resident resident) {
        if (!canManage(player, village)) return;
        if (resident.dead()) {
            Msg.error(player, "Verlorene Bewohner können nicht teleportiert werden.");
            return;
        }
        World world = world(village).orElse(null);
        if (world == null) {
            Msg.error(player, "Die Dorfwelt ist nicht geladen.");
            return;
        }
        if (!(Bukkit.getEntity(resident.villager()) instanceof Villager villager)) {
            Msg.error(player, "Dieser Bewohner ist nicht geladen. Bitte betritt zuerst seinen Chunk.");
            return;
        }
        Location bell = bellLocation(village);
        Location destination = safeBellLocation(world, bell, new HashSet<>());
        if (destination == null) {
            Msg.error(player, "Kein sicherer freier Platz neben der Dorfglocke gefunden.");
            return;
        }
        villager.setMemory(MemoryKey.MEETING_POINT, bell);
        if (!villager.teleport(destination)) {
            Msg.error(player, "Der Teleport wurde vom Server abgelehnt.");
            return;
        }
        Resident updated = new Resident(village.id(), resident.villager(), Resident.Status.CONFIRMED,
                resident.hasBed(), resident.bed(), link(bell), resident.firstSeen(), System.currentTimeMillis(),
                resident.displayName(), resident.number(), false);
        persist(village, List.of(updated));
        audit.log(player, "RESIDENT_TELEPORTED", resident.displayLabel() + " zur Glocke, Dorf #" + village.id());
        Msg.success(player, resident.displayLabel() + " wurde zur Dorfglocke teleportiert.");
    }

    public void removeResident(Player player, Village village, Resident resident) {
        if (!canManage(player, village)) return;
        if (resident.dead()) {
            Msg.error(player, "Verlorene Bewohner bitte über den Verlorene-Bereich entfernen.");
            return;
        }
        if (Bukkit.getEntity(resident.villager()) instanceof Villager villager) {
            villager.setMemory(MemoryKey.HOME, null);
            villager.setMemory(MemoryKey.MEETING_POINT, null);
        }
        tasks.database(() -> {
            repository.deleteResident(village.id(), resident.villager());
            return repository.find(village.id());
        }, list -> {
            cache.put(village.id(), list);
            audit.log(player, "RESIDENT_REMOVED", resident.displayLabel() + " aus Register, Dorf #" + village.id());
            Msg.success(player, resident.displayLabel() + " wurde aus dem Register entfernt. Der Villager bleibt am Leben.");
        }, error -> Msg.error(player, "Bewohner konnte nicht aus dem Register entfernt werden: " + error.getMessage()));
    }

    static List<Resident> repairIdentities(List<Resident> stored) {
        List<Resident> sorted = new ArrayList<>(stored);
        sorted.sort(Comparator.comparingLong(Resident::firstSeen).thenComparing(resident -> resident.villager().toString()));
        ResidentIdentityAllocator allocator = new ResidentIdentityAllocator(List.of());
        Set<Integer> usedNumbers = new HashSet<>();
        List<Resident> repaired = new ArrayList<>();
        for (Resident resident : sorted) {
            boolean preserve = resident.number() > 0 && resident.displayName() != null
                    && !resident.displayName().isBlank() && usedNumbers.add(resident.number());
            ResidentIdentityAllocator.Identity identity = preserve ? allocator.next(resident) : allocator.next(null);
            usedNumbers.add(identity.number());
            repaired.add(withIdentity(resident, identity));
        }
        return List.copyOf(repaired);
    }

    private static Resident withIdentity(Resident resident, ResidentIdentityAllocator.Identity identity) {
        if (Objects.equals(resident.displayName(), identity.name()) && resident.number() == identity.number()) return resident;
        return new Resident(resident.villageId(), resident.villager(), resident.status(), resident.hasBed(),
                resident.bed(), resident.meetingPoint(), resident.firstSeen(), resident.lastSeen(),
                identity.name(), identity.number(), resident.dead());
    }

    static Location safeBellLocation(World world, Location bell, Set<String> occupied) {
        int[][] offsets = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {2, 0}, {-2, 0}, {0, 2}, {0, -2},
                {1, 1}, {-1, 1}, {1, -1}, {-1, -1}, {2, 1}, {-2, 1}, {2, -1}, {-2, -1}};
        for (int dy = 0; dy >= -3; dy--) {
            for (int[] offset : offsets) {
                int x = bell.getBlockX() + offset[0];
                int y = bell.getBlockY() + dy;
                int z = bell.getBlockZ() + offset[1];
                String key = world.getUID() + ":" + x + ":" + y + ":" + z;
                if (occupied.contains(key)) continue;
                Block feet = world.getBlockAt(x, y, z);
                Block head = feet.getRelative(0, 1, 0);
                Block ground = feet.getRelative(0, -1, 0);
                if (!feet.getType().isAir() || !head.getType().isAir() || !ground.getType().isSolid()) continue;
                occupied.add(key);
                return new Location(world, x + 0.5, y, z + 0.5);
            }
        }
        return null;
    }

    public boolean canManage(Player player, Village village) {
        Role role = villages.role(village.id(), player.getUniqueId());
        if (role == Role.LANDHERR || role == Role.GEFOLGSMANN) return true;
        Msg.error(player, "Nur Landherr oder Gefolgsmann dürfen Bewohner verwalten.");
        return false;
    }

    private Resident evaluate(Village village, Villager villager, Resident old, boolean forceConfirm) {
        Location meeting = villager.getMemory(MemoryKey.MEETING_POINT);
        boolean confirmedByBell = meeting != null && meetingMatches(village, meeting);
        BlockLink bed = bedInside(village, villager);
        if (!confirmedByBell && bed == null && !forceConfirm) return null;
        Resident.Status status = confirmedByBell || forceConfirm ? Resident.Status.CONFIRMED : Resident.Status.UNCONFIRMED;
        long now = System.currentTimeMillis();
        ResidentIdentityAllocator.Identity identity = identity(village.id(), old);
        return new Resident(village.id(), villager.getUniqueId(), status, bed != null, bed,
                meeting == null ? null : link(meeting), old == null ? now : old.firstSeen(), now,
                identity.name(), identity.number(), false);
    }

    private ResidentIdentityAllocator.Identity identity(long villageId, Resident old) {
        return identities.computeIfAbsent(villageId, id -> new ResidentIdentityAllocator(residents(id))).next(old);
    }

    private void persist(Village village, List<Resident> records) {
        if (records.isEmpty()) return;
        tasks.database(() -> {
            repository.touchAll(village.id(), records);
            return repository.find(village.id());
        }, list -> cache.put(village.id(), list), error -> {
            throw new IllegalStateException("Bewohnerregister konnte nicht aktualisiert werden", error);
        });
    }

    private Map<UUID, Resident> previous(long villageId) {
        Map<UUID, Resident> previous = new HashMap<>();
        residents(villageId).forEach(resident -> previous.put(resident.villager(), resident));
        return previous;
    }

    private List<Villager> loadedVillagers(Village village) {
        World world = Bukkit.getWorld(village.region().world());
        if (world == null) return List.of();
        int radius = village.region().radius();
        return world.getNearbyEntities(new BoundingBox(
                        village.region().bellX() - radius, world.getMinHeight(), village.region().bellZ() - radius,
                        village.region().bellX() + radius + 1, world.getMaxHeight(), village.region().bellZ() + radius + 1))
                .stream().filter(entity -> entity instanceof Villager).map(entity -> (Villager) entity)
                .filter(villager -> village.region().contains(world.getUID(), villager.getLocation().getX(), villager.getLocation().getZ()))
                .toList();
    }

    private boolean meetingMatches(Village village, Location meeting) {
        return meeting.getWorld() != null && meeting.getWorld().getUID().equals(village.region().world())
                && meeting.getBlockX() == village.region().bellX()
                && meeting.getBlockY() == village.region().bellY()
                && meeting.getBlockZ() == village.region().bellZ();
    }

    private BlockLink bedInside(Village village, Villager villager) {
        Location bed = villager.getMemory(MemoryKey.HOME);
        if (bed == null || bed.getWorld() == null) return null;
        return village.region().contains(bed.getWorld().getUID(), bed.getX(), bed.getZ()) ? link(bed) : null;
    }

    private Optional<World> world(Village village) {
        return Optional.ofNullable(Bukkit.getWorld(village.region().world()));
    }

    private Location bellLocation(Village village) {
        return new Location(Bukkit.getWorld(village.region().world()), village.region().bellX() + 0.5,
                village.region().bellY(), village.region().bellZ() + 0.5);
    }

    private Location locationFromLink(BlockLink link) {
        return new Location(Bukkit.getWorld(link.world()), link.x() + 0.5, link.y(), link.z() + 0.5);
    }

    private static BlockLink link(Location location) {
        return new BlockLink(location.getWorld().getUID(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }
}
