package eu.praxios.blocklords.service;

import eu.praxios.blocklords.model.*;
import eu.praxios.blocklords.repository.ConstructionRepository;
import eu.praxios.blocklords.repository.VillageRepository;
import eu.praxios.blocklords.util.Msg;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class ConstructionService {
    private final ConstructionRepository repository;
    private final VillageRepository villageRepository;
    private final VillageService villages;
    private final StorageService storage;
    private final FaweBuilder builder;
    private final TaskRunner tasks;
    private final AuditService audit;
    private final Map<UUID, PreviewSession> previews = new ConcurrentHashMap<>();
    private final Map<UUID, Long> selectedSites = new ConcurrentHashMap<>();
    private final Map<Long, List<BuildSite>> sites = new ConcurrentHashMap<>();
    private final Map<Long, List<Building>> buildings = new ConcurrentHashMap<>();
    private final Map<Long, Map<String, Boolean>> features = new ConcurrentHashMap<>();

    public ConstructionService(ConstructionRepository repository, VillageRepository villageRepository,
                               VillageService villages, StorageService storage, FaweBuilder builder,
                               TaskRunner tasks, AuditService audit) {
        this.repository = repository;
        this.villageRepository = villageRepository;
        this.villages = villages;
        this.storage = storage;
        this.builder = builder;
        this.tasks = tasks;
        this.audit = audit;
    }

    public void loadInitial(List<Village> villageList) throws Exception {
        for (Village village : villageList) {
            sites.put(village.id(), repository.findSites(village.id()));
            buildings.put(village.id(), repository.findBuildings(village.id()));
            features.put(village.id(), villageRepository.features(village.id()));
        }
    }

    public void refresh(long villageId) {
        tasks.database(() -> new CacheEntry(repository.findSites(villageId), repository.findBuildings(villageId),
                villageRepository.features(villageId)), entry -> {
            sites.put(villageId, entry.sites());
            buildings.put(villageId, entry.buildings());
            features.put(villageId, entry.features());
            storage.refresh(villageId);
        }, error -> {
            throw new IllegalStateException("Bau-Cache konnte nicht aktualisiert werden", error);
        });
    }

    public List<BuildSite> sites(long villageId) { return sites.getOrDefault(villageId, List.of()); }
    public List<Building> buildings(long villageId) { return buildings.getOrDefault(villageId, List.of()); }

    public void selectSite(Player player, BuildSite site) {
        selectedSites.put(player.getUniqueId(), site.id());
    }

    public Optional<BuildSite> selectedSite(Player player, Village village) {
        Long id = selectedSites.get(player.getUniqueId());
        return sites(village.id()).stream().filter(site -> Objects.equals(site.id(), id)).findFirst();
    }

    public List<BlueprintType> available(Village village) {
        return available(village, features.getOrDefault(village.id(), Map.of()));
    }

    public List<BlueprintType> available(Village village, Map<String, Boolean> features) {
        return Arrays.stream(BlueprintType.values())
                .filter(type -> type.requiredLevel() <= village.level())
                .filter(type -> type != BlueprintType.WOHNHAUS_II || features.getOrDefault("RESEARCH_WOHNHAUS_II", false))
                .toList();
    }

    public void preview(Player player, Village village, BlueprintType type) {
        if (villages.role(village.id(), player.getUniqueId()) != Role.LANDHERR
                && villages.role(village.id(), player.getUniqueId()) != Role.GEFOLGSMANN) {
            Msg.error(player, "Nur Landherr oder Gefolgsmann dürfen Baupläne platzieren.");
            return;
        }
        if (type.requiredLevel() > village.level()) {
            Msg.error(player, type.displayName() + " benötigt Dorf-Level " + type.requiredLevel() + ".");
            return;
        }
        if (type == BlueprintType.WOHNHAUS_II
                && !features.getOrDefault(village.id(), Map.of()).getOrDefault("RESEARCH_WOHNHAUS_II", false)) {
            Msg.error(player, "Wohnhaus II benötigt die abgeschlossene Bibliotheksforschung.");
            return;
        }
        Block target = player.getTargetBlockExact(6);
        if (target == null) {
            Msg.error(player, "Visiere einen Bodenblock in deinem Dorf an.");
            return;
        }
        BuildLocation location = new BuildLocation(target.getWorld().getUID(), target.getX(), target.getY() + 1,
                target.getZ(), rotation(player));
        Map<Block, BlockData> original = validateAndCapture(player, village, type, location);
        if (original == null) return;
        cancelPreview(player);
        original.forEach((block, data) -> player.sendBlockChange(block.getLocation(), BlueprintBlocks.blocks(type).stream()
                .filter(offset -> matches(block, location, offset)).findFirst().map(BlueprintBlocks.OffsetBlock::data)
                .orElse(data)));
        PreviewSession session = new PreviewSession(village.id(), type, location, original);
        previews.put(player.getUniqueId(), session);
        selectedSites.remove(player.getUniqueId());
        Msg.success(player, "Vorschau für " + type.displayName() + " aktiv. Kosten: " + costText(type));
        Msg.info(player, "Bestätigen mit /bl build confirm oder Abbrechen mit /bl build cancel. Die Welt ist noch unverändert.");
    }

    public void confirmPreview(Player player) {
        PreviewSession preview = previews.get(player.getUniqueId());
        if (preview == null) {
            Msg.error(player, "Du hast keine aktive Bauvorschau.");
            return;
        }
        Village village = villages.byId(preview.villageId()).orElse(null);
        if (village == null) {
            cancelPreview(player);
            return;
        }
        if (validateAndCapture(player, village, preview.type(), preview.location()) == null) return;
        tasks.database(() -> repository.createSite(village.id(), preview.type(), preview.location(), player.getUniqueId()), site -> {
            restore(preview);
            previews.remove(player.getUniqueId());
            selectedSites.put(player.getUniqueId(), site.id());
            refresh(village.id());
            audit.log(player, "BUILD_SITE_CREATED", "#" + site.id() + " " + site.type());
            Msg.success(player, "Baustelle #" + site.id() + " für " + site.type().displayName() + " angelegt.");
            Msg.info(player, "Mit /bl build contribute Inventar oder /bl build fund Dorfvorrat einzahlen; danach /bl build start.");
        }, error -> Msg.error(player, "Baustelle konnte nicht angelegt werden: " + error.getMessage()));
    }

    public void cancelPreview(Player player) {
        PreviewSession preview = previews.remove(player.getUniqueId());
        if (preview != null) {
            restore(preview);
            Msg.info(player, "Bauvorschau verworfen; keine Blöcke wurden verändert.");
        }
    }

    public void contribute(Player player, Village village) {
        BuildSite site = selectedSite(player, village).orElse(null);
        if (site == null) {
            Msg.error(player, "Wähle zuerst eine Baustelle aus.");
            return;
        }
        Map<ResourceType, Integer> removed = removeNeeded(player, site);
        if (removed.isEmpty()) {
            Msg.error(player, "Dein Inventar enthält kein benötigtes Restmaterial: " + remainingText(site));
            return;
        }
        tasks.database(() -> {
            Map<ResourceType, Integer> accepted = new EnumMap<>(ResourceType.class);
            for (var entry : removed.entrySet()) {
                int actual = repository.contribute(site.id(), player.getUniqueId(), entry.getKey(), entry.getValue());
                if (actual > 0) accepted.put(entry.getKey(), actual);
            }
            return accepted;
        }, accepted -> {
            removed.forEach((resource, original) -> {
                int missing = original - accepted.getOrDefault(resource, 0);
                if (missing > 0) give(player, resource, missing);
            });
            refresh(village.id());
            villages.markActivity(village, player.getUniqueId());
            audit.log(player, "BUILD_CONTRIBUTION", "Baustelle #" + site.id() + " " + accepted);
            Msg.success(player, "Material an Baustelle #" + site.id() + " geliefert: " + accepted);
            repositoryStatus(player, site.id());
        }, error -> {
            removed.forEach((resource, amount) -> give(player, resource, amount));
            Msg.error(player, "Materialbeitrag fehlgeschlagen; Items zurückerstattet: " + error.getMessage());
        });
    }

    public void fund(Player player, Village village) {
        BuildSite site = selectedSite(player, village).orElse(null);
        if (site == null) {
            Msg.error(player, "Wähle zuerst eine Baustelle aus.");
            return;
        }
        tasks.database(() -> {
            Map<ResourceType, Integer> accepted = new EnumMap<>(ResourceType.class);
            for (ResourceType resource : site.required().keySet()) {
                int actual = repository.fundFromStorage(site.id(), player.getUniqueId(), resource, site.remaining(resource));
                if (actual > 0) accepted.put(resource, actual);
            }
            return accepted;
        }, accepted -> {
            refresh(village.id());
            storage.refresh(village.id());
            villages.markActivity(village, player.getUniqueId());
            audit.log(player, "BUILD_STORAGE_FUNDING", "Baustelle #" + site.id() + " " + accepted);
            Msg.success(player, "Dorfvorrat eingesetzt: " + accepted);
            repositoryStatus(player, site.id());
        }, error -> Msg.error(player, "Dorfvorrat konnte nicht verwendet werden: " + error.getMessage()));
    }

    public void start(Player player, Village village) {
        BuildSite site = selectedSite(player, village).orElse(null);
        if (site == null) {
            Msg.error(player, "Wähle zuerst eine Baustelle aus.");
            return;
        }
        if (!site.complete()) {
            Msg.error(player, "Es fehlt noch Material: " + remainingText(site));
            return;
        }
        tasks.database(() -> {
            repository.markBuilding(site.id());
            return site;
        }, marked -> {
            Msg.info(player, "FAWE-Bau für " + site.type().displayName() + " gestartet: Fundament, Wände, Dach.");
            builder.paste(site.type(), site.location(), count -> tasks.database(() -> {
                Building building = repository.completeSite(site.id());
                Village updated = villageRepository.addXp(village.id(), site.type().xp());
                setCompletionFeatures(building);
                return new Completion(building, updated);
            }, completion -> {
                refresh(village.id());
                villages.updateVillage(completion.village());
                villages.markActivity(village, player.getUniqueId());
                audit.log(player, "BUILDING_COMPLETED", completion.building().type() + " #" + completion.building().id());
                Msg.success(player, completion.building().type().displayName() + " fertiggestellt: " + count + " Blöcke.");
                Msg.success(player, "Dorf-Level " + completion.village().level() + " · XP " + completion.village().xp() + ".");
            }, error -> {
                repositorySafeReady(site.id());
                Msg.error(player, "Abschluss nach dem Bau fehlgeschlagen: " + error.getMessage());
            }), error -> {
                repositorySafeReady(site.id());
                Msg.error(player, "FAWE-Bau fehlgeschlagen; Baustelle wurde wieder freigegeben: " + error.getMessage());
            });
        }, error -> Msg.error(player, "Baustart fehlgeschlagen: " + error.getMessage()));
    }

    public Optional<Building> building(long id) {
        return buildings.values().stream().flatMap(List::stream).filter(building -> building.id() == id).findFirst();
    }

    private void repositoryStatus(Player player, long siteId) {
        tasks.database(() -> repository.findSite(siteId), optional -> optional.ifPresent(site ->
                Msg.info(player, site.complete() ? "Baustelle bereit: /bl build start" : "Restmaterial: " + remainingText(site))),
                error -> Msg.error(player, "Baustellenstatus konnte nicht gelesen werden."));
    }

    private void repositorySafeReady(long siteId) {
        tasks.databaseVoid(() -> {
            repository.markReady(siteId);
            return null;
        }, error -> {
            throw new IllegalStateException("Baustelle konnte nicht zurückgesetzt werden", error);
        });
    }

    private void setCompletionFeatures(Building building) throws Exception {
        switch (building.type()) {
            case RATHAUS -> villageRepository.setFeature(building.villageId(), "TOWN_HALL", true);
            case WOHNHAUS, WOHNHAUS_II -> villageRepository.setFeature(building.villageId(), "HOUSING", true);
            case FARM -> {
                villageRepository.setFeature(building.villageId(), "FOOD_STABLE", true);
                villageRepository.setFeature(building.villageId(), "PRODUCTION", true);
            }
            case MINE -> villageRepository.setFeature(building.villageId(), "PRODUCTION", true);
            case BIBLIOTHEK -> villageRepository.setFeature(building.villageId(), "LIBRARY", true);
            default -> { }
        }
    }

    private Map<Block, BlockData> validateAndCapture(Player player, Village village, BlueprintType type, BuildLocation location) {
        Map<Block, BlockData> original = new LinkedHashMap<>();
        for (BlueprintBlocks.OffsetBlock offset : BlueprintBlocks.blocks(type)) {
            int[] rotated = rotate(offset.x(), offset.z(), location.rotation());
            Block block = Objects.requireNonNull(player.getWorld()).getBlockAt(location.x() + rotated[0],
                    location.y() + offset.y(), location.z() + rotated[1]);
            if (!block.getWorld().getUID().equals(location.world()) || !village.region().contains(location.world(), block.getX(), block.getZ())) {
                Msg.error(player, "Der Bauplan würde den Dorf-Claim verlassen.");
                return null;
            }
            if (!block.getType().isAir()) {
                Msg.error(player, "Kollision bei " + block.getX() + "/" + block.getY() + "/" + block.getZ()
                        + ": " + block.getType() + ". Bitte Bereich freiräumen.");
                return null;
            }
            original.put(block, block.getBlockData());
        }
        return original;
    }

    private Map<ResourceType, Integer> removeNeeded(Player player, BuildSite site) {
        Map<ResourceType, Integer> removed = new EnumMap<>(ResourceType.class);
        ItemStack[] contents = player.getInventory().getContents();
        for (ResourceType resource : site.required().keySet()) {
            int remaining = site.remaining(resource);
            if (remaining <= 0) continue;
            for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
                ItemStack item = contents[slot];
                if (item == null || !resource.accepts(item.getType())) continue;
                int take = Math.min(remaining, item.getAmount());
                remaining -= take;
                removed.merge(resource, take, Integer::sum);
                if (take >= item.getAmount()) player.getInventory().setItem(slot, null);
                else item.setAmount(item.getAmount() - take);
            }
        }
        return removed;
    }

    private void give(Player player, ResourceType resource, int amount) {
        while (amount > 0) {
            int stack = Math.min(64, amount);
            Map<Integer, ItemStack> left = player.getInventory().addItem(new ItemStack(resource.canonicalItem(), stack));
            left.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
            amount -= stack;
        }
    }

    private void restore(PreviewSession preview) {
        preview.original().forEach((block, data) -> {
            if (block.getWorld().isChunkLoaded(block.getX() >> 4, block.getZ() >> 4)) {
                for (Player online : block.getWorld().getPlayers()) {
                    online.sendBlockChange(block.getLocation(), data);
                }
            }
        });
    }

    private String costText(BlueprintType type) {
        return type.cost().entrySet().stream().map(entry -> entry.getValue() + " " + entry.getKey().displayName())
                .reduce((a, b) -> a + ", " + b).orElse("kostenlos");
    }

    private String remainingText(BuildSite site) {
        return site.required().keySet().stream().filter(resource -> site.remaining(resource) > 0)
                .map(resource -> site.remaining(resource) + " " + resource.displayName())
                .reduce((a, b) -> a + ", " + b).orElse("bereit");
    }

    private boolean matches(Block block, BuildLocation location, BlueprintBlocks.OffsetBlock offset) {
        int[] rotated = rotate(offset.x(), offset.z(), location.rotation());
        return block.getX() == location.x() + rotated[0] && block.getY() == location.y() + offset.y()
                && block.getZ() == location.z() + rotated[1];
    }

    private static int rotation(Player player) {
        float yaw = player.getLocation().getYaw();
        return switch (Math.round(yaw / 90f)) {
            case 0 -> 180;
            case 1, -3 -> 270;
            case 2, -2 -> 0;
            default -> 90;
        };
    }

    private static int[] rotate(int x, int z, int rotation) {
        return switch (((rotation % 360) + 360) % 360) {
            case 90 -> new int[]{-z, x};
            case 180 -> new int[]{-x, -z};
            case 270 -> new int[]{z, -x};
            default -> new int[]{x, z};
        };
    }

    private record PreviewSession(long villageId, BlueprintType type, BuildLocation location, Map<Block, BlockData> original) {
    }

    private record CacheEntry(List<BuildSite> sites, List<Building> buildings, Map<String, Boolean> features) {
    }

    private record Completion(Building building, Village village) {
    }
}
