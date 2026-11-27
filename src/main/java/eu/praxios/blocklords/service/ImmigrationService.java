package eu.praxios.blocklords.service;

import eu.praxios.blocklords.config.BlockLordsConfig;
import eu.praxios.blocklords.model.*;
import eu.praxios.blocklords.repository.ImmigrationRepository;
import eu.praxios.blocklords.util.Msg;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.memory.MemoryKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public final class ImmigrationService {
    private final JavaPlugin plugin;
    private final Supplier<BlockLordsConfig> config;
    private final ImmigrationRepository repository;
    private final VillageService villages;
    private final ResidentService residents;
    private final WorkerService workers;
    private final StorageService storage;
    private final VillageScanner scanner;
    private final TaskRunner tasks;
    private final AuditService audit;
    private final LongSupplier clock;
    private BukkitTask task;

    public ImmigrationService(JavaPlugin plugin, Supplier<BlockLordsConfig> config, ImmigrationRepository repository,
                              VillageService villages, ResidentService residents, WorkerService workers,
                              StorageService storage, VillageScanner scanner, TaskRunner tasks, AuditService audit) {
        this(plugin, config, repository, villages, residents, workers, storage, scanner, tasks, audit,
                System::currentTimeMillis);
    }

    public ImmigrationService(JavaPlugin plugin, Supplier<BlockLordsConfig> config, ImmigrationRepository repository,
                              VillageService villages, ResidentService residents, WorkerService workers,
                              StorageService storage, VillageScanner scanner, TaskRunner tasks, AuditService audit,
                              LongSupplier clock) {
        this.plugin = plugin;
        this.config = config;
        this.repository = repository;
        this.villages = villages;
        this.residents = residents;
        this.workers = workers;
        this.storage = storage;
        this.scanner = scanner;
        this.tasks = tasks;
        this.audit = audit;
        this.clock = clock;
    }

    public void start() {
        stop();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::runAll,
                config.get().immigrationCheckTicks(), config.get().immigrationCheckTicks());
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void runAll() {
        villages.all().forEach(this::runVillage);
    }

    public void runVillage(Village village) {
        tasks.database(() -> repository.snapshot(village.id()), snapshot -> {
            if (snapshot.waiting().isPresent()) return;
            String missing = missingRequirement(village);
            ImmigrationState current = snapshot.state();
            if (missing != null) {
                if (current.nextArrivalAt() != 0 || !missing.equals(current.reason())) {
                    setState(village.id(), 0L, missing);
                }
                return;
            }
            if (current.nextArrivalAt() <= 0L) {
                setState(village.id(), clock.getAsLong() + config.get().immigrationWaitMillis(), "Wanderer erwartet");
                return;
            }
            if (clock.getAsLong() < current.nextArrivalAt()) return;
            arrive(village);
        }, error -> fail("Einwanderungsstatus konnte nicht gelesen werden", error));
    }

    public void menu(Village village, java.util.function.Consumer<ImmigrationRepository.Snapshot> callback) {
        tasks.database(() -> repository.snapshot(village.id()), callback,
                error -> fail("Einwanderungsmenü konnte nicht geladen werden", error));
    }

    public void accept(Player player, Village village, long candidateId, Runnable reopened) {
        if (!canManage(player, village)) return;
        tasks.database(() -> repository.waitingById(village.id(), candidateId), found -> {
            if (found.isEmpty()) {
                Msg.error(player, "Dieser Wanderer ist nicht mehr verfügbar.");
                return;
            }
            ImmigrationCandidate candidate = found.get();
            String missing = missingRequirement(village);
            if (missing != null) {
                setState(village.id(), 0L, missing);
                Msg.error(player, missing);
                return;
            }
            if (!(Bukkit.getEntity(candidate.villagerId()) instanceof Villager villager)) {
                Msg.error(player, "Der Wanderer ist nicht geladen. Bitte betritt zuerst seinen Chunk.");
                return;
            }
            BlockLink bed = residents.freeBed(village).orElse(null);
            if (bed == null) {
                Msg.error(player, "Das freie Bett wurde inzwischen belegt oder entfernt.");
                return;
            }
            long decidedAt = clock.getAsLong();
            Resident newcomer = new Resident(village.id(), candidate.villagerId(), Resident.Status.CONFIRMED, true,
                    bed, bellLink(village), candidate.createdAt(), decidedAt, null, 0, false);
            tasks.database(() -> repository.accept(village.id(), candidateId, newcomer,
                            config.get().immigrationFoodCost(), config.get().immigrationFirstXp(),
                            config.get().immigrationRepeatXp(), decidedAt,
                            decidedAt + config.get().immigrationWaitMillis(), "Nächster Wartezyklus läuft"),
                    acceptance -> completeAcceptance(player, village, villager, acceptance, reopened),
                    error -> Msg.error(player, "Aufnahme fehlgeschlagen; keine Nahrung wurde abgebucht: " + error.getMessage()));
        }, error -> Msg.error(player, "Wanderer konnte nicht geprüft werden: " + error.getMessage()));
    }

    public void reject(Player player, Village village, long candidateId, Runnable reopened) {
        if (!canManage(player, village)) return;
        long decidedAt = clock.getAsLong();
        tasks.database(() -> repository.reject(village.id(), candidateId, decidedAt,
                decidedAt + config.get().immigrationRetryMillis(), "Wanderer abgelehnt; neuer Versuch nach kurzer Pause"), rejected -> {
            if (rejected.isEmpty()) {
                Msg.error(player, "Dieser Wanderer ist nicht mehr verfügbar.");
                return;
            }
            ImmigrationCandidate candidate = rejected.get();
            if (Bukkit.getEntity(candidate.villagerId()) instanceof Villager villager) villager.remove();
            audit.log(player, "IMMIGRATION_REJECTED", "Wanderer #" + candidate.id() + " ohne Nahrungkosten");
            Msg.success(player, "Wanderer ohne Nahrungkosten abgelehnt.");
            reopened.run();
        }, error -> Msg.error(player, "Wanderer konnte nicht abgelehnt werden: " + error.getMessage()));
    }

    public void onDeath(java.util.UUID villagerId) {
        long decidedAt = clock.getAsLong();
        tasks.database(() -> repository.markLost(villagerId, decidedAt,
                decidedAt + config.get().immigrationRetryMillis(), "Wanderer verstorben; neuer Versuch nach kurzer Pause"),
                lost -> lost.ifPresent(candidate -> plugin.getLogger().info("BlockLords: Wanderer #" + candidate.id()
                        + " verstorben; keine Nahrung berechnet.")),
                error -> fail("Wanderertod konnte nicht gespeichert werden", error));
    }

    public long remainingSeconds(ImmigrationState state) {
        return Math.max(0L, (state.nextArrivalAt() - clock.getAsLong()) / 1000L);
    }

    public int foodCost() { return config.get().immigrationFoodCost(); }
    public int firstXp() { return config.get().immigrationFirstXp(); }
    public int repeatXp() { return config.get().immigrationRepeatXp(); }

    public boolean canManage(Player player, Village village) {
        Role role = villages.role(village.id(), player.getUniqueId());
        if (role == Role.LANDHERR || role == Role.GEFOLGSMANN) return true;
        Msg.error(player, "Nur Landherr oder Gefolgsmann dürfen Wanderer aufnehmen oder ablehnen.");
        return false;
    }

    private String missingRequirement(Village village) {
        if (village.level() < 2) return "Einwanderung wird mit Dorf-Level 2 freigeschaltet.";
        if (village.status() != VillageStatus.ACTIVE) return "Das Dorf ruht; Einwanderung ist pausiert.";
        if (scanner.fresh(village).isEmpty()) {
            scanner.request(village, ignored -> { });
            return "Dorfvermessung für freie Betten läuft.";
        }
        if (residents.freeBed(village).isEmpty()) return "Kein freies Bett im geladenen Dorfbereich.";
        int food = storage.balances(village.id()).getOrDefault(ResourceType.NAHRUNG, 0);
        if (food < config.get().immigrationFoodCost()) {
            return "Zu wenig Nahrung im Dorfvorrat: " + food + "/" + config.get().immigrationFoodCost() + ".";
        }
        if (workers.freeCapacity(village.id()) <= 0) return "Kein freier Arbeitsplatz in Farm oder Mine.";
        return null;
    }

    private void arrive(Village village) {
        String missing = missingRequirement(village);
        if (missing != null) {
            setState(village.id(), 0L, missing);
            return;
        }
        World world = Bukkit.getWorld(village.region().world());
        if (world == null) {
            setState(village.id(), clock.getAsLong() + config.get().immigrationRetryMillis(), "Dorfwelt nicht geladen");
            return;
        }
        Location bell = new Location(world, village.region().bellX() + 0.5, village.region().bellY(), village.region().bellZ() + 0.5);
        Location destination = ResidentService.safeBellLocation(world, bell, new HashSet<>());
        if (destination == null) {
            setState(village.id(), clock.getAsLong() + config.get().immigrationRetryMillis(),
                    "Kein sicherer Platz neben der Dorfglocke");
            return;
        }
        Villager villager = (Villager) world.spawnEntity(destination, EntityType.VILLAGER);
        villager.setAdult();
        villager.customName(Component.text("Wanderer · " + village.name()));
        villager.setCustomNameVisible(true);
        tasks.database(() -> repository.createCandidate(village.id(), villager.getUniqueId(), clock.getAsLong()), candidate -> {
            villages.membersOf(village.id()).keySet().forEach(playerId -> {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null) Msg.info(player, "Ein Wanderer ist an der Glocke von '" + village.name()
                        + "' eingetroffen. Öffne /bl immigration zur Aufnahme.");
            });
        }, error -> {
            villager.remove();
            setState(village.id(), clock.getAsLong() + config.get().immigrationRetryMillis(),
                    "Wanderer konnte nicht registriert werden");
            fail("Wanderer konnte nicht registriert werden", error);
        });
    }

    private void completeAcceptance(Player player, Village village, Villager villager,
                                    ImmigrationRepository.Acceptance acceptance, Runnable reopened) {
        Resident resident = acceptance.resident();
        villager.setMemory(MemoryKey.HOME, new Location(Bukkit.getWorld(resident.bed().world()),
                resident.bed().x() + 0.5, resident.bed().y(), resident.bed().z() + 0.5));
        Location bell = new Location(Bukkit.getWorld(village.region().world()), village.region().bellX() + 0.5,
                village.region().bellY(), village.region().bellZ() + 0.5);
        villager.setMemory(MemoryKey.MEETING_POINT, bell);
        villager.customName(Component.text(resident.displayLabel()));
        villager.setCustomNameVisible(true);
        residents.refresh(village.id());
        villages.updateVillage(acceptance.village());
        villages.markActivity(village, player.getUniqueId());
        audit.log(player, "IMMIGRATION_ACCEPTED", "Wanderer #" + acceptance.candidate().id() + " als "
                + resident.displayLabel() + " · -" + config.get().immigrationFoodCost() + " Nahrung · +"
                + acceptance.candidate().xpAwarded() + " XP");
        Msg.success(player, resident.displayLabel() + " ist eingewandert: -" + config.get().immigrationFoodCost()
                + " Nahrung · +" + acceptance.candidate().xpAwarded() + " Dorf-XP.");
        Msg.info(player, "Nächster Schritt: /bl workers öffnen und einen Arbeitsplatz zuweisen.");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.1f);
        reopened.run();
    }

    private BlockLink bellLink(Village village) {
        return new BlockLink(village.region().world(), village.region().bellX(), village.region().bellY(), village.region().bellZ());
    }

    private void setState(long villageId, long nextArrivalAt, String reason) {
        tasks.database(() -> repository.state(villageId, nextArrivalAt, reason), ignored -> { },
                error -> fail("Einwanderungszustand konnte nicht gespeichert werden", error));
    }

    private void fail(String message, Throwable error) {
        plugin.getLogger().warning(message + ": " + error.getMessage());
    }
}
