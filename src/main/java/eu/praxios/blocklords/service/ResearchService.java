package eu.praxios.blocklords.service;

import eu.praxios.blocklords.config.BlockLordsConfig;
import eu.praxios.blocklords.model.*;
import eu.praxios.blocklords.repository.ResearchRepository;
import eu.praxios.blocklords.repository.VillageRepository;
import eu.praxios.blocklords.util.Msg;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class ResearchService {
    public static final String WOHNHAUS_II = "WOHNHAUS_II";
    public static final Map<ResourceType, Integer> WOHNHAUS_II_COST = Map.of(
            ResourceType.HOLZ, 32, ResourceType.STEIN, 16, ResourceType.SMARAGD, 8);

    private final JavaPlugin plugin;
    private final Supplier<BlockLordsConfig> config;
    private final ResearchRepository repository;
    private final VillageRepository villagesRepository;
    private final StorageService storage;
    private final VillageService villages;
    private final ConstructionService construction;
    private final TaskRunner tasks;
    private final AuditService audit;
    private final Map<Long, List<ResearchProject>> projects = new ConcurrentHashMap<>();
    private BukkitTask task;

    public ResearchService(JavaPlugin plugin, Supplier<BlockLordsConfig> config, ResearchRepository repository,
                           VillageRepository villagesRepository, StorageService storage, VillageService villages,
                           ConstructionService construction, TaskRunner tasks, AuditService audit) {
        this.plugin = plugin;
        this.config = config;
        this.repository = repository;
        this.villagesRepository = villagesRepository;
        this.storage = storage;
        this.villages = villages;
        this.construction = construction;
        this.tasks = tasks;
        this.audit = audit;
    }

    public void loadInitial(List<Village> villageList) throws Exception {
        for (Village village : villageList) projects.put(village.id(), repository.find(village.id()));
    }

    public void start() {
        stop();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::checkDue, 100L, 100L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public List<ResearchProject> projects(long villageId) { return projects.getOrDefault(villageId, List.of()); }

    public void refresh(long villageId) {
        tasks.database(() -> repository.find(villageId), list -> projects.put(villageId, list), error -> {
            throw new IllegalStateException("Forschung konnte nicht geladen werden", error);
        });
    }

    public void startResearch(Player player, Village village) {
        Role role = villages.role(village.id(), player.getUniqueId());
        if (role != Role.LANDHERR && role != Role.GEFOLGSMANN) {
            Msg.error(player, "Nur Landherr oder Gefolgsmann dürfen Forschung starten.");
            return;
        }
        boolean library = construction.buildings(village.id()).stream().anyMatch(building -> building.type() == BlueprintType.BIBLIOTHEK);
        if (!library) {
            Msg.error(player, "Baue zuerst eine Bibliothek.");
            return;
        }
        tasks.database(() -> {
            if (!storage.consumeCost(village.id(), player.getUniqueId(), WOHNHAUS_II_COST, "RESEARCH")) {
                throw new IllegalStateException("Der Vorrat reicht nicht: 32 Holz, 16 Stein, 8 Smaragde erforderlich.");
            }
            return repository.start(village.id(), WOHNHAUS_II, config.get().researchDurationMillis());
        }, project -> {
            refresh(village.id());
            villages.markActivity(village, player.getUniqueId());
            audit.log(player, "RESEARCH_STARTED", WOHNHAUS_II + " Dorf #" + village.id());
            Msg.success(player, "Forschung Wohnhaus II gestartet. Abschluss in "
                    + (config.get().researchDurationMillis() / 1000) + " Sekunden.");
        }, error -> Msg.error(player, "Forschung konnte nicht gestartet werden: " + error.getMessage()));
    }

    private void checkDue() {
        tasks.database(() -> repository.due(System.currentTimeMillis()), due -> due.forEach(project ->
                tasks.databaseVoid(() -> {
                    repository.complete(project.id());
                    villagesRepository.setFeature(project.villageId(), "RESEARCH_WOHNHAUS_II", true);
                    return null;
                }, error -> {
                    throw new IllegalStateException("Forschungsabschluss fehlgeschlagen", error);
                })), error -> {
            throw new IllegalStateException("Fällige Forschung konnte nicht gelesen werden", error);
        });
        for (Village village : villages.all()) {
            boolean completedNow = projects(village.id()).stream()
                    .anyMatch(project -> project.status() == ResearchProject.Status.RUNNING
                            && project.finishesAt() <= System.currentTimeMillis());
            if (completedNow) {
                refresh(village.id());
                construction.refresh(village.id());
                Player owner = plugin.getServer().getPlayer(village.owner());
                if (owner != null) Msg.success(owner, "Forschung Wohnhaus II abgeschlossen; neuer Bauplan verfügbar.");
            }
        }
    }
}
