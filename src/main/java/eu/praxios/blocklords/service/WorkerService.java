package eu.praxios.blocklords.service;

import eu.praxios.blocklords.model.*;
import eu.praxios.blocklords.repository.StorageRepository;
import eu.praxios.blocklords.repository.WorkerRepository;
import eu.praxios.blocklords.util.Msg;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.memory.MemoryKey;
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
    private final ResidentService residents;
    private final TaskRunner tasks;
    private final AuditService audit;
    private final java.util.function.Supplier<eu.praxios.blocklords.config.BlockLordsConfig> config;
    private final Map<Long, List<Worker>> workers = new ConcurrentHashMap<>();

    public WorkerService(WorkerRepository repository, StorageRepository storage, ConstructionService construction,
                         VillageService villages, ResidentService residents, TaskRunner tasks, AuditService audit,
                         java.util.function.Supplier<eu.praxios.blocklords.config.BlockLordsConfig> config) {
        this.repository = repository;
        this.storage = storage;
        this.construction = construction;
        this.villages = villages;
        this.residents = residents;
        this.tasks = tasks;
        this.audit = audit;
        this.config = config;
    }

    public void loadInitial(List<Village> villageList) throws Exception {
        for (Village village : villageList) workers.put(village.id(), repository.find(village.id()));
    }

    public List<Worker> workers(long villageId) { return workers.getOrDefault(villageId, List.of()); }

    public int capacity() {
        return config.get().workersPerBuilding();
    }

    public int freeCapacity(long villageId) {
        return construction.buildings(villageId).stream().filter(building -> building.type().workplace())
                .mapToInt(building -> (int) Math.max(0, capacity() - workers(villageId).stream()
                        .filter(worker -> worker.buildingId() == building.id()).count())).sum();
    }

    private boolean hasCapacity(Player player, Village village, long buildingId) {
        long assigned = workers(village.id()).stream().filter(worker -> worker.buildingId() == buildingId).count();
        if (assigned < capacity()) return true;
        Msg.error(player, "Dieses Gebäude ist voll: " + assigned + "/" + capacity() + " Arbeitsplätze belegt.");
        return false;
    }

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
        if (!hasCapacity(player, village, buildingId)) return;
        org.bukkit.World world = org.bukkit.Bukkit.getWorld(building.location().world());
        if (world == null) {
            Msg.error(player, "Die Gebäudewelt ist nicht geladen.");
            return;
        }
        Location center = new Location(world, building.location().x() + 2, building.location().y() + 1, building.location().z() + 2);
        Resident candidate = residents.free(village.id(), workers(village.id())).stream()
                .min((left, right) -> {
                    double leftDistance = Bukkit.getEntity(left.villager()) instanceof Villager villager
                            ? villager.getLocation().distanceSquared(center) : Double.MAX_VALUE;
                    double rightDistance = Bukkit.getEntity(right.villager()) instanceof Villager villager
                            ? villager.getLocation().distanceSquared(center) : Double.MAX_VALUE;
                    return Double.compare(leftDistance, rightDistance);
                }).orElse(null);
        if (candidate == null) {
            Msg.error(player, "Noch kein freier Bewohner registriert. Bleib kurz im Dorf; neue geladene Villager werden automatisch erfasst.");
            return;
        }
        Villager villager = Bukkit.getEntity(candidate.villager()) instanceof Villager found ? found : null;
        tasks.database(() -> {
            storage.initializeProduction(village.id(), System.currentTimeMillis());
            return repository.assign(village.id(), candidate.villager(), buildingId, job);
        }, worker -> {
            refresh(village.id());
            if (villager != null) {
                villager.setProfession(job == Worker.Job.FARMER ? Villager.Profession.FARMER : Villager.Profession.MASON);
                villager.setMemory(MemoryKey.JOB_SITE, jobSiteLocation(building));
                villager.customName(Component.text((job == Worker.Job.FARMER ? "Bauer · " : "Minenarbeiter · ")
                        + residentName(village, villager.getUniqueId())));
                villager.setCustomNameVisible(true);
            }
            villages.markActivity(village, player.getUniqueId());
            audit.log(player, "WORKER_ASSIGNED", job + " " + candidate.villager() + " Gebäude #" + buildingId);
            Msg.success(player, (job == Worker.Job.FARMER ? "Bauer" : "Minenarbeiter") + " zugewiesen. Nächstes: Nahrung einlagern.");
        }, error -> Msg.error(player, "Arbeiter konnte nicht zugewiesen werden: " + error.getMessage()));
    }

    public void assignResident(Player player, Village village, long buildingId, UUID residentId) {
        Role role = villages.role(village.id(), player.getUniqueId());
        if (role != Role.LANDHERR && role != Role.GEFOLGSMANN) {
            Msg.error(player, "Nur Landherr oder Gefolgsmann dürfen Arbeiter zuweisen.");
            return;
        }
        Resident candidate = residents.free(village.id(), workers(village.id())).stream()
                .filter(resident -> resident.villager().equals(residentId)).findFirst().orElse(null);
        if (candidate == null) {
            Msg.error(player, "Dieser Bewohner ist bereits zugewiesen oder nicht im Register.");
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
            Msg.error(player, "Dieses Gebäude bietet keinen Produktionsberuf.");
            return;
        }
        if (!hasCapacity(player, village, buildingId)) return;
        org.bukkit.World world = org.bukkit.Bukkit.getWorld(building.location().world());
        if (world == null) {
            Msg.error(player, "Die Gebäudewelt ist nicht geladen.");
            return;
        }
        Villager villager = Bukkit.getEntity(candidate.villager()) instanceof Villager found ? found : null;
        tasks.database(() -> {
            storage.initializeProduction(village.id(), System.currentTimeMillis());
            return repository.assign(village.id(), residentId, buildingId, job);
        }, worker -> {
            refresh(village.id());
            if (villager != null) {
                villager.setProfession(job == Worker.Job.FARMER ? Villager.Profession.FARMER : Villager.Profession.MASON);
                villager.setMemory(MemoryKey.JOB_SITE, jobSiteLocation(building));
                villager.customName(Component.text((job == Worker.Job.FARMER ? "Bauer · " : "Minenarbeiter · ")
                        + residentName(village, villager.getUniqueId())));
                villager.setCustomNameVisible(true);
            }
            villages.markActivity(village, player.getUniqueId());
            audit.log(player, "WORKER_ASSIGNED", job + " " + residentId + " Gebäude #" + buildingId);
            Msg.success(player, (job == Worker.Job.FARMER ? "Bauer" : "Minenarbeiter") + " zugewiesen. Nächstes: Nahrung einlagern.");
        }, error -> Msg.error(player, "Arbeiter konnte nicht zugewiesen werden: " + error.getMessage()));
    }

    private String residentName(Village village, UUID villagerId) {
        return residents.residents(village.id()).stream().filter(resident -> resident.villager().equals(villagerId))
                .findFirst().map(Resident::displayLabel).orElse("Bewohner");
    }

    private Location jobSiteLocation(Building building) {
        return construction.jobSiteLocation(building);
    }

    public void unassign(Player player, Village village, long workerId) {
        Role role = villages.role(village.id(), player.getUniqueId());
        if (role != Role.LANDHERR && role != Role.GEFOLGSMANN) {
            Msg.error(player, "Nur Landherr oder Gefolgsmann dürfen Arbeitszuweisungen ändern.");
            return;
        }
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

    public void removeVillager(UUID villager) {
        boolean assigned = workers.values().stream().flatMap(List::stream)
                .anyMatch(worker -> worker.villager().equals(villager));
        if (!assigned) return;
        workers.replaceAll((villageId, list) -> list.stream()
                .filter(worker -> !worker.villager().equals(villager)).toList());
        tasks.databaseVoid(() -> {
            repository.removeVillager(villager);
            return null;
        }, error -> {
            throw new IllegalStateException("Arbeitszuweisung eines verlorenen Bewohners konnte nicht entfernt werden", error);
        });
    }
}
