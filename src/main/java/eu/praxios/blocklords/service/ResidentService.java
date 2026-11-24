package eu.praxios.blocklords.service;

import eu.praxios.blocklords.config.BlockLordsConfig;
import eu.praxios.blocklords.model.*;
import eu.praxios.blocklords.repository.ResidentRepository;
import eu.praxios.blocklords.util.Msg;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
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
    private final Map<Long, List<Resident>> cache = new ConcurrentHashMap<>();
    private BukkitTask task;

    public ResidentService(JavaPlugin plugin, Supplier<BlockLordsConfig> config, ResidentRepository repository,
                           VillageService villages, TaskRunner tasks) {
        this.plugin = plugin;
        this.config = config;
        this.repository = repository;
        this.villages = villages;
        this.tasks = tasks;
    }

    public void loadInitial(List<Village> villageList) throws Exception {
        for (Village village : villageList) cache.put(village.id(), repository.find(village.id()));
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

    public List<Resident> free(long villageId, List<Worker> workers) {
        List<UUID> assigned = workers.stream().map(Worker::villager).toList();
        return residents(villageId).stream().filter(resident -> !assigned.contains(resident.villager())).toList();
    }

    public long confirmed(long villageId) {
        return residents(villageId).stream().filter(resident -> resident.status() == Resident.Status.CONFIRMED).count();
    }

    public long unconfirmed(long villageId) {
        return residents(villageId).stream().filter(resident -> resident.status() == Resident.Status.UNCONFIRMED).count();
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
            Resident evaluated = evaluate(village, villager, previous.get(villager.getUniqueId()), false);
            if (evaluated == null) continue;
            Resident old = previous.get(villager.getUniqueId());
            if (old == null || !old.sameLinks(evaluated)) changed.add(evaluated);
        }
        persist(village, changed);
    }

    /** Sneak + Rechtsklick auf die Dorfglocke: Vanilla-Versammlung und explizite Bestätigung. */
    public void assemble(Player player, Village village) {
        List<Villager> loaded = loadedVillagers(village);
        Map<UUID, Resident> previous = previous(village.id());
        Location bell = bellLocation(village);
        List<Resident> records = new ArrayList<>();
        int newlyConfirmed = 0;
        int withoutBed = 0;
        long now = System.currentTimeMillis();
        for (Villager villager : loaded) {
            BlockLink bed = bedInside(village, villager);
            Resident old = previous.get(villager.getUniqueId());
            if (old == null || old.status() == Resident.Status.UNCONFIRMED) newlyConfirmed++;
            if (bed == null) withoutBed++;
            villager.setMemory(MemoryKey.MEETING_POINT, bell);
            records.add(new Resident(village.id(), villager.getUniqueId(), Resident.Status.CONFIRMED,
                    bed != null, bed, link(bell), old == null ? now : old.firstSeen(), now));
        }
        persist(village, records);
        world(village).ifPresent(world -> {
            world.playSound(bell, Sound.BLOCK_BELL_USE, 1.0f, 1.0f);
            world.spawnParticle(Particle.HAPPY_VILLAGER, bell.clone().add(0, 1, 0), 12, 0.5, 0.5, 0.5);
        });
        Msg.success(player, "Versammlung in '" + village.name() + "': " + records.size() + " versammelt, "
                + newlyConfirmed + " neu bestätigt, " + withoutBed + " ohne Bett markiert.");
        player.sendActionBar(Component.text("Dorfbewohner werden zur Glocke gerufen.", NamedTextColor.GOLD));
    }

    private Resident evaluate(Village village, Villager villager, Resident old, boolean forceConfirm) {
        Location meeting = villager.getMemory(MemoryKey.MEETING_POINT);
        boolean confirmedByBell = meeting != null && meetingMatches(village, meeting);
        BlockLink bed = bedInside(village, villager);
        if (!confirmedByBell && bed == null && !forceConfirm) return null;
        Resident.Status status = confirmedByBell || forceConfirm ? Resident.Status.CONFIRMED : Resident.Status.UNCONFIRMED;
        long now = System.currentTimeMillis();
        return new Resident(village.id(), villager.getUniqueId(), status, bed != null, bed,
                meeting == null ? null : link(meeting), old == null ? now : old.firstSeen(), now);
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

    private static BlockLink link(Location location) {
        return new BlockLink(location.getWorld().getUID(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }
}
