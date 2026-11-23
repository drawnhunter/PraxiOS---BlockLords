package eu.praxios.blocklords.service;

import eu.praxios.blocklords.model.*;
import eu.praxios.blocklords.repository.StorageRepository;
import eu.praxios.blocklords.repository.WorkerRepository;
import eu.praxios.blocklords.util.Msg;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.util.BoundingBox;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class WorkerService {
    private final WorkerRepository repository;
    private final StorageRepository storage;
    private final ConstructionService construction;
    private final VillageService villages;
    private final TaskRunner tasks;
    private final AuditService audit;
    private final Map<Long, List<Worker>> workers = new ConcurrentHashMap<>();

    public WorkerService(WorkerRepository repository, StorageRepository storage, ConstructionService construction,
                         VillageService villages, TaskRunner tasks, AuditService audit) {
        this.repository = repository;
        this.storage = storage;
        this.construction = construction;
        this.villages = villages;
        this.tasks = tasks;
        this.audit = audit;
    }

    public void loadInitial(List<Village> villageList) throws Exception {
        for (Village village : villageList) workers.put(village.id(), repository.find(village.id()));
    }

    public List<Worker> workers(long villageId) { return workers.getOrDefault(villageId, List.of()); }

    public void refresh(long villageId) {
        tasks.database(() -> repository.find(villageId), list -> workers.put(villageId, list), error -> {
            throw new IllegalStateException("Arbeiter konnten nicht geladen werden", error);
        });
    }

    public void assign(Player player, Village village, long buildingId) {
        Role role = villages.role(village.id(), player.getUniqueId());
        if (role != Role.LANDHERR && role != Role.GEFOLGSMANN) {
            Msg.error(player, "Nur Landherr oder Gefolgsmann dürfen Arbeiter zuweisen.");
            return;
        }
        Building building = construction.building(buildingId).orElse(null);
        if (building == null || building.villageId() != village.id()) {
            Msg.error(player, "Gebäude-ID " + buildingId + " gehört nicht zu diesem Dorf.");
            return;
        }
        Worker.Job job = switch (building.type()) {
            case FARM -> Worker.Job.FARMER;
            case MINE -> Worker.Job.MINER;
            default -> null;
        };
        if (job == null) {
            Msg.error(player, "Dieses Gebäude bietet in Phase 2 keinen Produktionsberuf.");
            return;
        }
        org.bukkit.World world = org.bukkit.Bukkit.getWorld(building.location().world());
        if (world == null) {
            Msg.error(player, "Die Gebäudewelt ist nicht geladen.");
            return;
        }
        Location center = new Location(world, building.location().x() + 2, building.location().y() + 1, building.location().z() + 2);
        List<UUID> assigned = workers(village.id()).stream().map(Worker::villager).toList();
        Villager villager = world.getNearbyEntities(BoundingBox.of(center.clone().add(-12, -6, -12), center.clone().add(12, 6, 12)))
                .stream().filter(entity -> entity instanceof Villager).map(entity -> (Villager) entity)
                .filter(candidate -> !assigned.contains(candidate.getUniqueId()))
                .min((left, right) -> Double.compare(left.getLocation().distanceSquared(center), right.getLocation().distanceSquared(center)))
                .orElse(null);
        if (villager == null) {
            Msg.error(player, "Kein freier Dorfbewohner im Umkreis von 12 Blöcken gefunden.");
            return;
        }
        tasks.database(() -> {
            storage.initializeProduction(village.id(), System.currentTimeMillis());
            return repository.assign(village.id(), villager.getUniqueId(), buildingId, job);
        }, worker -> {
            refresh(village.id());
            villager.setProfession(job == Worker.Job.FARMER ? Villager.Profession.FARMER : Villager.Profession.MASON);
            villager.customName(Component.text(job == Worker.Job.FARMER ? "Bauer" : "Minenarbeiter"));
            villager.setCustomNameVisible(true);
            villages.markActivity(village, player.getUniqueId());
            audit.log(player, "WORKER_ASSIGNED", job + " " + villager.getUniqueId() + " Gebäude #" + buildingId);
            Msg.success(player, (job == Worker.Job.FARMER ? "Bauer" : "Minenarbeiter") + " zugewiesen: " + villager.getUniqueId());
        }, error -> Msg.error(player, "Arbeiter konnte nicht zugewiesen werden: " + error.getMessage()));
    }

    public void unassign(Player player, Village village, long workerId) {
        Worker worker = workers(village.id()).stream().filter(candidate -> candidate.id() == workerId).findFirst().orElse(null);
        if (worker == null) {
            Msg.error(player, "Arbeiter-ID " + workerId + " gehört nicht zu diesem Dorf.");
            return;
        }
        tasks.databaseVoid(() -> {
            repository.remove(workerId);
            return null;
        }, error -> Msg.error(player, "Arbeiter konnte nicht entfernt werden: " + error.getMessage()));
        refresh(village.id());
        audit.log(player, "WORKER_UNASSIGNED", "Arbeiter #" + workerId);
        Msg.success(player, "Arbeiter-Zuordnung aufgehoben.");
    }
}
