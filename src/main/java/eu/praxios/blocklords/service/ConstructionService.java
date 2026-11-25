package eu.praxios.blocklords.service;

import eu.praxios.blocklords.model.*;
import eu.praxios.blocklords.repository.ConstructionRepository;
import eu.praxios.blocklords.repository.StorageRepository;
import eu.praxios.blocklords.repository.VillageRepository;
import eu.praxios.blocklords.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

public final class ConstructionService {
    private final ConstructionRepository repository;
    private final VillageRepository villageRepository;
    private final VillageService villages;
    private final StorageService storage;
    private final StorageRepository storageRepository;
    private final FaweBuilder builder;
    private final TaskRunner tasks;
    private final AuditService audit;
    private final Map<UUID, PreviewSession> previews = new ConcurrentHashMap<>();
    private final Map<UUID, Long> selectedSites = new ConcurrentHashMap<>();
    private final Map<Long, List<BuildSite>> sites = new ConcurrentHashMap<>();
    private final Map<Long, List<Building>> buildings = new ConcurrentHashMap<>();
    private final Map<Long, Map<String, Boolean>> features = new ConcurrentHashMap<>();

    public ConstructionService(ConstructionRepository repository, VillageRepository villageRepository,
                               VillageService villages, StorageService storage, StorageRepository storageRepository,
                               FaweBuilder builder, TaskRunner tasks, AuditService audit) {
        this.repository = repository;
        this.villageRepository = villageRepository;
        this.villages = villages;
        this.storage = storage;
        this.storageRepository = storageRepository;
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

    public boolean previewInFront(Player player, Village village, BlueprintType type) {
        return previewAt(player, village, type, defaultPlacementLocation(player));
    }

    public boolean preview(Player player, Village village, BlueprintType type) {
        Block target = player.getTargetBlockExact(6);
        BuildLocation location = target != null
                ? new BuildLocation(target.getWorld().getUID(), target.getX(), target.getY() + 1, target.getZ(), rotation(player))
                : defaultPlacementLocation(player);
        return previewAt(player, village, type, location);
    }

    public boolean previewAt(Player player, Village village, BlueprintType type, BuildLocation location) {
        if (villages.role(village.id(), player.getUniqueId()) != Role.LANDHERR
                && villages.role(village.id(), player.getUniqueId()) != Role.GEFOLGSMANN) {
            Msg.error(player, "Nur Landherr oder Gefolgsmann dürfen Baupläne platzieren.");
            return false;
        }
        if (type.requiredLevel() > village.level()) {
            Msg.error(player, type.displayName() + " benötigt Dorf-Level " + type.requiredLevel() + ".");
            return false;
        }
        if (type == BlueprintType.WOHNHAUS_II
                && !features.getOrDefault(village.id(), Map.of()).getOrDefault("RESEARCH_WOHNHAUS_II", false)) {
            Msg.error(player, "Wohnhaus II benötigt die abgeschlossene Bibliotheksforschung.");
            return false;
        }
        Map<Block, BlockData> original = validateAndCapture(player, village, type, location);
        if (original == null) return false;
        PreviewSession previous = previews.get(player.getUniqueId());
        if (previous != null) restore(previous);
        original.forEach((block, data) -> player.sendBlockChange(block.getLocation(), BlueprintBlocks.blocks(type).stream()
                .filter(offset -> matches(block, location, offset)).findFirst().map(offset -> rotateData(offset, location.rotation()))
                .orElse(data)));
        previews.put(player.getUniqueId(), new PreviewSession(village.id(), type, location, original));
        selectedSites.remove(player.getUniqueId());
        Msg.success(player, "Vorschau für " + type.displayName() + " aktiv. Kosten: " + costText(type));
        Msg.info(player, "Nächstes: /bl build tools oder /bl build confirm.");
        return true;
    }

    public boolean movePreview(Player player) {
        PreviewSession current = previews.get(player.getUniqueId());
        if (current == null) {
            Msg.error(player, "Keine aktive Vorschau zum Verschieben.");
            return false;
        }
        Village village = villages.byId(current.villageId()).orElse(null);
        Block target = player.getTargetBlockExact(6);
        if (village == null) {
            Msg.error(player, "Die Vorschau gehört zu keinem geladenen Dorf.");
            return false;
        }
        BuildLocation location = target != null
                ? new BuildLocation(target.getWorld().getUID(), target.getX(), target.getY() + 1, target.getZ(), current.location().rotation())
                : new BuildLocation(player.getWorld().getUID(), current.location().x(), current.location().y(), current.location().z(),
                        current.location().rotation());
        return previewAt(player, village, current.type(), location);
    }

    public boolean rotatePreview(Player player, int degrees) {
        PreviewSession current = previews.get(player.getUniqueId());
        if (current == null) {
            Msg.error(player, "Keine aktive Vorschau zum Rotieren.");
            return false;
        }
        Village village = villages.byId(current.villageId()).orElse(null);
        if (village == null) return false;
        BuildLocation rotated = new BuildLocation(current.location().world(), current.location().x(), current.location().y(),
                current.location().z(), current.location().rotation() + degrees);
        return previewAt(player, village, current.type(), rotated);
    }

    public Village villageById(long id) {
        return villages.byId(id).orElse(null);
    }

    public boolean offsetPreview(Player player, int dx, int dy, int dz) {
        PreviewSession current = previews.get(player.getUniqueId());
        if (current == null) {
            Msg.error(player, "Keine aktive Vorschau zum Verschieben.");
            return false;
        }
        Village village = villages.byId(current.villageId()).orElse(null);
        if (village == null) return false;
        BuildLocation moved = new BuildLocation(current.location().world(), current.location().x() + dx,
                current.location().y() + dy, current.location().z() + dz, current.location().rotation());
        return previewAt(player, village, current.type(), moved);
    }

    private BuildLocation defaultPlacementLocation(Player player) {
        org.bukkit.util.Vector direction = player.getLocation().getDirection();
        direction.setY(0);
        if (direction.lengthSquared() < 0.0001) direction = player.getFacing().getDirection();
        direction.normalize().multiply(2);
        org.bukkit.Location target = player.getLocation().add(direction);
        int highest = player.getWorld().getHighestBlockYAt(target.getBlockX(), target.getBlockZ());
        int y = Math.max(player.getLocation().getBlockY(), highest + 1);
        return new BuildLocation(player.getWorld().getUID(), target.getBlockX(), y, target.getBlockZ(), rotation(player));
    }

    public boolean hasPreview(Player player) {
        return previews.containsKey(player.getUniqueId());
    }

    public long previewVillageId(Player player) {
        PreviewSession preview = previews.get(player.getUniqueId());
        return preview == null ? -1 : preview.villageId();
    }

    public BlueprintType previewType(Player player) {
        PreviewSession preview = previews.get(player.getUniqueId());
        return preview == null ? null : preview.type();
    }

    public boolean confirmPreview(Player player) {
        PreviewSession preview = previews.get(player.getUniqueId());
        if (preview == null) {
            Msg.error(player, "Du hast keine aktive Bauvorschau.");
            return false;
        }
        Village village = villages.byId(preview.villageId()).orElse(null);
        if (village == null) {
            cancelPreview(player);
            return false;
        }
        restore(preview);
        previews.remove(player.getUniqueId());
        if (validateAndCapture(player, village, preview.type(), preview.location(), preview) == null) return false;
        tasks.database(() -> repository.createSite(village.id(), preview.type(), preview.location(), player.getUniqueId()), site -> {
            restore(preview);
            if (!placeChest(site)) {
                tasks.databaseVoid(() -> {
                    repository.deleteSite(site.id());
                    return null;
                }, error -> Msg.error(player, "Baustellen-Rollback fehlgeschlagen: " + error.getMessage()));
                Msg.error(player, "Baustellenkiste konnte nicht am vorgesehenen Anker platziert werden.");
                return;
            }
            selectedSites.put(player.getUniqueId(), site.id());
            refresh(village.id());
            audit.log(player, "BUILD_SITE_CREATED", "#" + site.id() + " " + site.type() + " mit Baustellenkiste");
            Msg.success(player, "Baustelle #" + site.id() + " für " + site.type().displayName() + " angelegt.");
            Msg.info(player, "Nächstes: /bl build contribute oder /bl build fund. Danach /bl build start.");
        }, error -> Msg.error(player, "Baustelle konnte nicht angelegt werden: " + error.getMessage()));
        return true;
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
        Chest chest = chestFor(site);
        if (chest == null) {
            Msg.error(player, "Die Baustellenkiste fehlt oder ist nicht geladen. Bitte Admin prüfen lassen.");
            return;
        }
        Map<ResourceType, Integer> removed = removeNeeded(player, site, chestCounts(site));
        if (removed.isEmpty()) {
            Msg.error(player, "Dein Inventar enthält kein benötigtes Restmaterial: " + remainingText(site));
            return;
        }
        Map<ResourceType, Integer> accepted = putIntoChest(chest, site, removed);
        removed.forEach((resource, original) -> {
            int missing = original - accepted.getOrDefault(resource, 0);
            if (missing > 0) give(player, resource, missing);
        });
        tasks.database(() -> {
            for (var entry : accepted.entrySet()) {
                repository.recordContribution(site.id(), player.getUniqueId(), entry.getKey(), entry.getValue());
            }
            repository.reconcileFromChest(site.id(), chestCounts(site));
            return accepted;
        }, confirmed -> {
            refresh(village.id());
            villages.markActivity(village, player.getUniqueId());
            audit.log(player, "BUILD_CONTRIBUTION", "Baustellenkiste #" + site.id() + " " + confirmed);
            Msg.success(player, "Material in Baustellenkiste #" + site.id() + " geliefert: " + confirmed);
            repositoryStatus(player, site.id());
        }, error -> Msg.error(player, "Material liegt in der Kiste, aber der Status konnte nicht gespeichert werden: " + error.getMessage()));
    }

    public void fund(Player player, Village village) {
        BuildSite site = selectedSite(player, village).orElse(null);
        if (site == null) {
            Msg.error(player, "Wähle zuerst eine Baustelle aus.");
            return;
        }
        Chest chest = chestFor(site);
        if (chest == null) {
            Msg.error(player, "Die Baustellenkiste fehlt oder ist nicht geladen.");
            return;
        }
        Map<ResourceType, Integer> counts = chestCounts(site);
        tasks.database(() -> {
            Map<ResourceType, Integer> withdrawn = new EnumMap<>(ResourceType.class);
            for (ResourceType resource : site.required().keySet()) {
                int remaining = site.required().get(resource) - counts.getOrDefault(resource, 0);
                int actual = storageRepository.withdraw(village.id(), player.getUniqueId(), resource, remaining);
                if (actual > 0) withdrawn.put(resource, actual);
            }
            return withdrawn;
        }, withdrawn -> {
            Map<ResourceType, Integer> accepted = putIntoChest(chest, site, withdrawn);
            tasks.database(() -> {
                withdrawn.forEach((resource, original) -> {
                    int missing = original - accepted.getOrDefault(resource, 0);
                    if (missing > 0) {
                        try {
                            storageRepository.deposit(village.id(), player.getUniqueId(), resource, missing, storage.capacity(village.id()));
                        } catch (Exception exception) {
                            throw new RuntimeException(exception);
                        }
                    }
                });
                repository.reconcileFromChest(site.id(), chestCounts(site));
                return accepted;
            }, confirmed -> {
                refresh(village.id());
                storage.refresh(village.id());
                villages.markActivity(village, player.getUniqueId());
                audit.log(player, "BUILD_STORAGE_FUNDING", "Baustellenkiste #" + site.id() + " " + confirmed);
                Msg.success(player, "Dorfvorrat in die Baustellenkiste übertragen: " + confirmed);
                repositoryStatus(player, site.id());
            }, error -> Msg.error(player, "Baustellenübertragung fehlgeschlagen: " + error.getMessage()));
        }, error -> Msg.error(player, "Dorfvorrat konnte nicht verwendet werden: " + error.getMessage()));
    }

    public void start(Player player, Village village) {
        BuildSite site = selectedSite(player, village).orElse(null);
        if (site == null) {
            Msg.error(player, "Wähle zuerst eine Baustelle aus.");
            return;
        }
        Chest chest = chestFor(site);
        if (chest == null) {
            Msg.error(player, "Die Baustellenkiste fehlt oder ist nicht geladen.");
            return;
        }
        Map<ResourceType, Integer> counts = chestCounts(site);
        boolean complete = site.required().keySet().stream()
                .allMatch(resource -> counts.getOrDefault(resource, 0) >= site.required().get(resource));
        if (!complete) {
            Msg.error(player, "Es fehlt noch Material: " + remainingFromCounts(site, counts));
            return;
        }
        tasks.database(() -> {
            repository.markBuilding(site.id());
            return site;
        }, marked -> {
            Map<ResourceType, Integer> committed = commitMaterials(chest, site);
            if (committed == null) {
                repositorySafeReady(site.id());
                Msg.error(player, "Materialcommit fehlgeschlagen; kein Bau gestartet.");
                return;
            }
            Msg.info(player, "Bau läuft: Fundament → Wände → Dach.");
            Set<com.sk89q.worldedit.math.BlockVector3> excluded = Set.of(chestVector(site));
            builder.paste(site.type(), site.location(), excluded, count -> tasks.database(() -> {
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
                if (site.type().workplace()) {
                    Msg.info(player, "Nächster Schritt: /bl workers öffnen, registrierte Bewohner zuweisen und Nahrung einlagern.");
                }
            }, error -> {
                returnMaterials(chest, committed);
                repositorySafeReady(site.id());
                Msg.error(player, "Abschluss nach dem Bau fehlgeschlagen; Material wurde zurückgegeben: " + error.getMessage());
            }), error -> {
                if (error instanceof TimeoutException) {
                    Msg.error(player, "FAWE-Timeout: Material bleibt sicher reserviert. Bitte Aktions-ID im Log prüfen.");
                } else {
                    returnMaterials(chest, committed);
                    repositorySafeReady(site.id());
                    Msg.error(player, "FAWE-Bau fehlgeschlagen; Material zurückgegeben und Baustelle freigegeben: " + error.getMessage());
                }
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
        return validateAndCapture(player, village, type, location, null);
    }

    private Map<Block, BlockData> validateAndCapture(Player player, Village village, BlueprintType type,
                                                     BuildLocation location, PreviewSession ownPreview) {
        Map<Block, BlockData> original = new LinkedHashMap<>();
        for (BlueprintBlocks.OffsetBlock offset : BlueprintBlocks.blocks(type)) {
            int[] rotated = rotate(offset.x(), offset.z(), location.rotation());
            Block block = Objects.requireNonNull(player.getWorld()).getBlockAt(location.x() + rotated[0],
                    location.y() + offset.y(), location.z() + rotated[1]);
            if (!block.getWorld().getUID().equals(location.world()) || !village.region().contains(location.world(), block.getX(), block.getZ())) {
                Msg.error(player, "Der Bauplan würde den Dorf-Claim verlassen.");
                return null;
            }
            if (!block.getType().isAir() && !isReplaceableGrass(block.getType())
                    && !isOwnPreviewGhost(block, offset, location, ownPreview)) {
                Msg.error(player, "Kollision bei " + block.getX() + "/" + block.getY() + "/" + block.getZ()
                        + ": " + block.getType() + ". Bitte Bereich freiräumen.");
                return null;
            }
            original.put(block, block.getBlockData());
        }
        return original;
    }

    private boolean isReplaceableGrass(Material material) {
        return material == Material.SHORT_GRASS || material == Material.TALL_GRASS
                || material == Material.FERN || material == Material.LARGE_FERN;
    }

    private boolean isOwnPreviewGhost(Block block, BlueprintBlocks.OffsetBlock offset,
                                      BuildLocation location, PreviewSession preview) {
        if (preview == null || !preview.original().containsKey(block)) return false;
        return matches(block, location, offset) && block.getBlockData().equals(rotateData(offset, location.rotation()));
    }

    private boolean placeChest(BuildSite site) {
        Block block = chestBlock(site);
        if (block == null || !block.getType().isAir()) return false;
        block.setType(Material.CHEST);
        if (!(block.getState() instanceof Chest chest)) return false;
        chest.customName(net.kyori.adventure.text.Component.text("Baustelle · " + site.type().displayName()));
        chest.update(true);
        return true;
    }

    private Block chestBlock(BuildSite site) {
        org.bukkit.World world = Bukkit.getWorld(site.location().world());
        if (world == null) return null;
        int[] rotated = rotate(4, 4, site.location().rotation());
        return world.getBlockAt(site.location().x() + rotated[0], site.location().y() + 1,
                site.location().z() + rotated[1]);
    }

    private com.sk89q.worldedit.math.BlockVector3 chestVector(BuildSite site) {
        Block block = chestBlock(site);
        return block == null ? null : com.sk89q.worldedit.math.BlockVector3.at(block.getX(), block.getY(), block.getZ());
    }

    private Chest chestFor(BuildSite site) {
        Block block = chestBlock(site);
        if (block == null || block.getType() != Material.CHEST) return null;
        return block.getState() instanceof Chest chest ? chest : null;
    }

    public Optional<BuildSite> siteAtChest(Block block) {
        return sites.values().stream().flatMap(List::stream)
                .filter(site -> sameBlock(chestBlock(site), block)).findFirst();
    }

    public Optional<Building> buildingAtChest(Block block) {
        return buildings.values().stream().flatMap(List::stream).filter(building -> {
            Block chest = chestBlock(new BuildSite(0, building.villageId(), building.type(), building.location(),
                    BuildSite.Status.COLLECTING, villageServiceOwner(building), building.createdAt(), Map.of(), Map.of()));
            return sameBlock(chest, block);
        }).findFirst();
    }

    private UUID villageServiceOwner(Building building) {
        return villages.byId(building.villageId()).map(Village::owner).orElse(new UUID(0, 0));
    }

    private boolean sameBlock(Block first, Block second) {
        return first != null && second != null && first.getWorld().equals(second.getWorld())
                && first.getX() == second.getX() && first.getY() == second.getY() && first.getZ() == second.getZ();
    }

    private Map<ResourceType, Integer> chestCounts(BuildSite site) {
        Chest chest = chestFor(site);
        if (chest == null) return Map.of();
        Map<ResourceType, Integer> counts = new EnumMap<>(ResourceType.class);
        for (ItemStack item : chest.getInventory().getContents()) {
            if (item == null) continue;
            ResourceType.from(item.getType()).ifPresent(resource -> counts.merge(resource, item.getAmount(), Integer::sum));
        }
        return counts;
    }

    private Map<ResourceType, Integer> putIntoChest(Chest chest, BuildSite site, Map<ResourceType, Integer> items) {
        Map<ResourceType, Integer> accepted = new EnumMap<>(ResourceType.class);
        Map<ResourceType, Integer> current = chestCounts(site);
        items.forEach((resource, offered) -> {
            int remaining = Math.max(0, site.required().getOrDefault(resource, 0) - current.getOrDefault(resource, 0));
            int requested = Math.min(offered, remaining);
            if (requested <= 0) return;
            Map<Integer, ItemStack> left = chest.getInventory().addItem(new ItemStack(resource.canonicalItem(), requested));
            int actual = requested - left.values().stream().mapToInt(ItemStack::getAmount).sum();
            if (actual > 0) accepted.put(resource, actual);
        });
        return accepted;
    }

    private Map<ResourceType, Integer> commitMaterials(Chest chest, BuildSite site) {
        Map<ResourceType, Integer> committed = new EnumMap<>(ResourceType.class);
        for (var entry : site.required().entrySet()) {
            int remaining = entry.getValue();
            for (ItemStack item : chest.getInventory().getContents()) {
                if (item == null || !entry.getKey().accepts(item.getType())) continue;
                int take = Math.min(remaining, item.getAmount());
                item.setAmount(item.getAmount() - take);
                remaining -= take;
                committed.merge(entry.getKey(), take, Integer::sum);
                if (item.getAmount() <= 0) item.setAmount(0);
                if (remaining == 0) break;
            }
            if (remaining > 0) return null;
        }
        return committed;
    }

    private void returnMaterials(Chest chest, Map<ResourceType, Integer> committed) {
        committed.forEach((resource, amount) -> {
            Map<Integer, ItemStack> left = chest.getInventory().addItem(new ItemStack(resource.canonicalItem(), amount));
            left.values().forEach(item -> chest.getWorld().dropItemNaturally(chest.getLocation(), item));
        });
    }

    private String remainingFromCounts(BuildSite site, Map<ResourceType, Integer> counts) {
        return site.required().keySet().stream()
                .filter(resource -> counts.getOrDefault(resource, 0) < site.required().get(resource))
                .map(resource -> (site.required().get(resource) - counts.getOrDefault(resource, 0)) + " " + resource.displayName())
                .reduce((left, right) -> left + ", " + right).orElse("bereit");
    }

    private Map<ResourceType, Integer> removeNeeded(Player player, BuildSite site, Map<ResourceType, Integer> chestCounts) {
        Map<ResourceType, Integer> removed = new EnumMap<>(ResourceType.class);
        ItemStack[] contents = player.getInventory().getContents();
        for (ResourceType resource : site.required().keySet()) {
            int remaining = site.required().get(resource) - chestCounts.getOrDefault(resource, 0);
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

    private BlockData rotateData(BlueprintBlocks.OffsetBlock offset, int rotation) {
        BlockData data = offset.data().clone();
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

    private boolean matches(Block block, BuildLocation location, BlueprintBlocks.OffsetBlock offset) {
        int[] rotated = rotate(offset.x(), offset.z(), location.rotation());
        return block.getX() == location.x() + rotated[0] && block.getY() == location.y() + offset.y()
                && block.getZ() == location.z() + rotated[1];
    }

    private static int rotation(Player player) {
        float yaw = player.getLocation().getYaw();
        return switch (Math.floorMod(Math.round(yaw / 90f), 4)) {
            case 0 -> 0;
            case 1 -> 90;
            case 2 -> 180;
            default -> 270;
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
