package eu.praxios.blocklords.service;

import eu.praxios.blocklords.config.BlockLordsConfig;
import eu.praxios.blocklords.model.BlueprintType;
import eu.praxios.blocklords.model.ResourceType;
import eu.praxios.blocklords.model.Village;
import eu.praxios.blocklords.repository.ConstructionRepository;
import eu.praxios.blocklords.repository.StorageRepository;
import eu.praxios.blocklords.util.Msg;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class StorageService {
    private final Supplier<BlockLordsConfig> config;
    private final StorageRepository repository;
    private final ConstructionRepository construction;
    private final TaskRunner tasks;
    private final AuditService audit;
    private final VillageService villageService;
    private final Map<Long, Map<ResourceType, Integer>> balances = new ConcurrentHashMap<>();
    private final Map<Long, Integer> capacities = new ConcurrentHashMap<>();

    public StorageService(Supplier<BlockLordsConfig> config, StorageRepository repository,
                          ConstructionRepository construction, TaskRunner tasks, AuditService audit,
                          VillageService villageService) {
        this.config = config;
        this.repository = repository;
        this.construction = construction;
        this.tasks = tasks;
        this.audit = audit;
        this.villageService = villageService;
    }

    public void loadInitial(List<Village> villages) throws Exception {
        for (Village village : villages) {
            balances.put(village.id(), repository.balances(village.id()));
            capacities.put(village.id(), calculateCapacity(village.id()));
        }
    }

    public void refresh(long villageId) {
        tasks.database(() -> new CacheEntry(repository.balances(villageId), calculateCapacity(villageId)), entry -> {
            balances.put(villageId, entry.balances());
            capacities.put(villageId, entry.capacity());
        }, error -> {
            throw new IllegalStateException("Lager konnte nicht aktualisiert werden", error);
        });
    }

    public Map<ResourceType, Integer> balances(long villageId) {
        return balances.getOrDefault(villageId, Map.of());
    }

    public int capacity(long villageId) {
        return capacities.getOrDefault(villageId, config.get().storageBaseCapacity());
    }

    public int total(long villageId) {
        return balances(villageId).values().stream().mapToInt(Integer::intValue).sum();
    }

    public String summary(long villageId) {
        StringBuilder text = new StringBuilder("Vorrat ").append(total(villageId)).append('/').append(capacity(villageId)).append(": ");
        for (ResourceType resource : ResourceType.values()) {
            text.append(resource.displayName()).append(' ').append(balances(villageId).getOrDefault(resource, 0)).append(" · ");
        }
        return text.substring(0, text.length() - 3);
    }

    public void depositAll(Player player, Village village) {
        Map<ResourceType, Integer> removed = removeAllAccepted(player);
        if (removed.isEmpty()) {
            Msg.error(player, "Dein Inventar enthält keine einlagerbaren Ressourcen.");
            return;
        }
        tasks.database(() -> {
            Map<ResourceType, Integer> accepted = new EnumMap<>(ResourceType.class);
            for (var entry : removed.entrySet()) {
                int actual = repository.deposit(village.id(), player.getUniqueId(), entry.getKey(), entry.getValue(), capacity(village.id()));
                if (actual > 0) accepted.put(entry.getKey(), actual);
            }
            return accepted;
        }, accepted -> {
            refundMissing(player, removed, accepted);
            if (accepted.isEmpty()) {
                Msg.error(player, "Der Dorfvorrat ist voll oder diese Items werden nicht akzeptiert. Entnimm zuerst Ressourcen.");
                return;
            }
            refresh(village.id());
            villagesActivity(village, player);
            int count = accepted.values().stream().mapToInt(Integer::intValue).sum();
            Msg.success(player, count + " Ressourcen in '" + village.name() + "' eingelagert.");
            accepted.forEach((resource, amount) -> Msg.plain(player, "+ " + amount + " " + resource.displayName()));
        }, error -> {
            refundMissing(player, removed, Map.of());
            Msg.error(player, "Einlagerung fehlgeschlagen; deine Items wurden zurückerstattet.");
        });
    }

    public void deposit(Player player, Village village, ResourceType resource, int amount) {
        int removed = removeItems(player, resource, amount);
        if (removed <= 0) {
            Msg.error(player, "Du hast keine passenden Items für " + resource.displayName() + " im Inventar.");
            return;
        }
        tasks.database(() -> repository.deposit(village.id(), player.getUniqueId(), resource, removed, capacity(village.id())), actual -> {
            if (actual <= 0) {
                give(player, resource, removed);
                Msg.error(player, "Der Dorfvorrat ist voll. Entnimm zuerst Ressourcen.");
                return;
            }
            if (actual < removed) give(player, resource, removed - actual);
            refresh(village.id());
            villagesActivity(village, player);
            Msg.success(player, actual + " " + resource.displayName() + " eingelagert."
                    + (actual < removed ? " Rest wegen Lagerkapazität zurückgegeben." : ""));
        }, error -> {
            give(player, resource, removed);
            Msg.error(player, "Einlagerung fehlgeschlagen; Items zurückerstattet.");
        });
    }

    public void withdraw(Player player, Village village, ResourceType resource, int amount) {
        tasks.database(() -> repository.withdraw(village.id(), player.getUniqueId(), resource, amount), actual -> {
            if (actual <= 0) {
                Msg.error(player, "Diese Ressource ist nicht im Dorfvorrat.");
                return;
            }
            if (actual > 0) give(player, resource, actual);
            refresh(village.id());
            villagesActivity(village, player);
            Msg.success(player, actual + " " + resource.displayName() + " aus dem Dorf entnommen.");
        }, error -> Msg.error(player, "Entnahme fehlgeschlagen: " + error.getMessage()));
    }

    public boolean consumeCost(long villageId, UUID player, Map<ResourceType, Integer> cost, String reason) throws Exception {
        boolean consumed = repository.consumeCost(villageId, player, cost, reason);
        if (consumed) refresh(villageId);
        return consumed;
    }

    public StorageRepository.ProductionState productionState(long villageId) throws Exception {
        return repository.productionState(villageId);
    }

    private int calculateCapacity(long villageId) throws Exception {
        long warehouses = construction.findBuildings(villageId).stream()
                .filter(building -> building.type() == BlueprintType.LAGERHAUS).count();
        return config.get().storageBaseCapacity() + Math.toIntExact(warehouses) * config.get().storagePerWarehouse();
    }

    private Map<ResourceType, Integer> removeAllAccepted(Player player) {
        Map<ResourceType, Integer> removed = new EnumMap<>(ResourceType.class);
        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (item == null) continue;
            ResourceType resource = ResourceType.from(item.getType()).orElse(null);
            if (resource != null) {
                removed.merge(resource, item.getAmount(), Integer::sum);
                player.getInventory().setItem(slot, null);
            }
        }
        return removed;
    }

    private int removeItems(Player player, ResourceType resource, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
            ItemStack item = contents[slot];
            if (item == null || !resource.accepts(item.getType())) continue;
            int take = Math.min(remaining, item.getAmount());
            remaining -= take;
            if (take >= item.getAmount()) player.getInventory().setItem(slot, null);
            else item.setAmount(item.getAmount() - take);
        }
        return amount - remaining;
    }

    private void refundMissing(Player player, Map<ResourceType, Integer> removed, Map<ResourceType, Integer> accepted) {
        removed.forEach((resource, original) -> {
            int missing = original - accepted.getOrDefault(resource, 0);
            if (missing > 0) give(player, resource, missing);
        });
    }

    private void give(Player player, ResourceType resource, int amount) {
        while (amount > 0) {
            int stack = Math.min(64, amount);
            Map<Integer, ItemStack> left = player.getInventory().addItem(new ItemStack(resource.canonicalItem(), stack));
            left.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
            amount -= stack;
        }
    }

    private void villagesActivity(Village village, Player player) {
        villageService.markActivity(village, player.getUniqueId());
        audit.log(player, "STORAGE_ACTIVITY", "Dorf #" + village.id());
    }

    private record CacheEntry(Map<ResourceType, Integer> balances, int capacity) {
    }
}
