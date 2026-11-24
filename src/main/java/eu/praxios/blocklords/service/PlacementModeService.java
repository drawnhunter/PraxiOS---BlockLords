package eu.praxios.blocklords.service;

import eu.praxios.blocklords.model.BlueprintType;
import eu.praxios.blocklords.model.PlacementSnapshot;
import eu.praxios.blocklords.model.Village;
import eu.praxios.blocklords.repository.PlacementRepository;
import eu.praxios.blocklords.util.Msg;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Optionaler Werkzeugmodus mit persistiertem Originalinventar und idempotenter Wiederherstellung. */
public final class PlacementModeService {
    private final JavaPlugin plugin;
    private final PlacementRepository repository;
    private final ConstructionService construction;
    private final TaskRunner tasks;
    private final LogService logs;
    private final AuditService audit;
    private final NamespacedKey toolKey;
    private final Map<UUID, SessionState> active = new ConcurrentHashMap<>();

    public PlacementModeService(JavaPlugin plugin, PlacementRepository repository, ConstructionService construction,
                                TaskRunner tasks, LogService logs, AuditService audit) {
        this.plugin = plugin;
        this.repository = repository;
        this.construction = construction;
        this.tasks = tasks;
        this.logs = logs;
        this.audit = audit;
        this.toolKey = new NamespacedKey(plugin, "placement_tool");
    }

    public void enterExisting(Player player) {
        if (active.containsKey(player.getUniqueId())) {
            Msg.info(player, "Der Baumodus ist bereits aktiv.");
            return;
        }
        if (!construction.hasPreview(player)) {
            Msg.error(player, "Wähle zuerst eine Bauplan-Vorschau, dann /bl build tools.");
            return;
        }
        Village village = villagesFor(player);
        if (village == null) {
            Msg.error(player, "Die Vorschau gehört zu keinem geladenen Dorf.");
            return;
        }
        PlacementSnapshot snapshot = snapshot(player, village, construction.previewType(player));
        tasks.database(() -> {
            repository.saveActive(snapshot);
            return snapshot;
        }, saved -> {
            if (!player.isOnline()) {
                tasks.databaseVoid(() -> {
                    repository.close(player.getUniqueId());
                    return null;
                }, error -> logs.error("PLACEMENT-RACE", "Session konnte nach Logout nicht geschlossen werden", error));
                return;
            }
            active.put(player.getUniqueId(), new SessionState(saved, true));
            installTools(player);
            audit.log(player, "PLACEMENT_MODE_START", "Dorf #" + village.id() + " " + saved.blueprint());
            Msg.success(player, "Baumodus aktiv: Hotbar-Werkzeuge ersetzt. Bestätigen/Abbrechen stellt dein Inventar wieder her.");
        }, error -> {
            logs.error("PLACEMENT-START", "Snapshot konnte nicht gespeichert werden; Hotbar bleibt unverändert", error);
            Msg.error(player, "Baumodus nicht aktiviert: Snapshot konnte nicht gespeichert werden.");
        });
    }

    public void exit(Player player, String reason, boolean cancelPreview) {
        SessionState session = active.remove(player.getUniqueId());
        if (session == null) return;
        if (cancelPreview) construction.cancelPreview(player);
        restore(player, session.snapshot());
        tasks.databaseVoid(() -> {
            repository.close(player.getUniqueId());
            return null;
        }, error -> logs.error("PLACEMENT-END", "Session konnte nicht geschlossen werden: " + player.getUniqueId(), error));
        audit.log(player, "PLACEMENT_MODE_END", reason);
        Msg.info(player, "Baumodus beendet (" + reason + "); Originalinventar wiederhergestellt.");
    }

    public void closeAll() {
        for (UUID playerId : active.keySet()) {
            Player player = plugin.getServer().getPlayer(playerId);
            if (player != null) exit(player, "Serverstopp/Reload", true);
        }
    }

    public void restoreOnJoin(Player player) {
        tasks.database(() -> repository.findActive(player.getUniqueId()), optional -> optional.ifPresent(snapshot -> {
            restore(player, snapshot);
            tasks.databaseVoid(() -> {
                repository.close(player.getUniqueId());
                return null;
            }, error -> logs.error("PLACEMENT-JOIN", "Session konnte nicht geschlossen werden", error));
            Msg.info(player, "Unterbrochene Bauwerkzeug-Session sicher wiederhergestellt.");
        }), error -> logs.error("PLACEMENT-JOIN", "Snapshot konnte nicht gelesen werden", error));
    }

    public boolean isActive(Player player) {
        return active.containsKey(player.getUniqueId());
    }

    public boolean isTool(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(toolKey, PersistentDataType.STRING);
    }

    public void removeToolsFromDrops(java.util.List<ItemStack> drops) {
        drops.removeIf(this::isTool);
    }

    public void toggleAnchor(Player player) {
        SessionState current = active.get(player.getUniqueId());
        if (current == null) return;
        boolean anchored = !current.anchored();
        active.put(player.getUniqueId(), new SessionState(current.snapshot(), anchored));
        Msg.info(player, anchored ? "Preview-Anker fixiert." : "Preview-Anker gelöst; Positionswerkzeug verschiebt jetzt.");
    }

    private void handleTool(Player player, String action) {
        SessionState session = active.get(player.getUniqueId());
        if (session == null) return;
        switch (action) {
            case "X_MINUS" -> {
                if (player.isSneaking()) toggleAnchor(player);
                else construction.offsetPreview(player, -1, 0, 0);
            }
            case "X_PLUS" -> {
                if (player.isSneaking()) construction.movePreview(player);
                else construction.offsetPreview(player, 1, 0, 0);
            }
            case "Y_MINUS" -> construction.offsetPreview(player, 0, 0, -1);
            case "Y_PLUS" -> construction.offsetPreview(player, 0, 0, 1);
            case "ROTATE_LEFT" -> construction.rotatePreview(player, -90);
            case "ROTATE_RIGHT" -> construction.rotatePreview(player, 90);
            case "UP" -> construction.offsetPreview(player, 0, 1, 0);
            case "DOWN" -> construction.offsetPreview(player, 0, -1, 0);
            case "MOVE" -> {
                if (session.anchored()) Msg.info(player, "Der Anker ist fixiert. Zum Verschieben zuerst das Ankerwerkzeug verwenden.");
                else construction.movePreview(player);
            }
            case "ANCHOR" -> toggleAnchor(player);
            case "CONFIRM", "CONFIRM_OR_CANCEL" -> {
                if (player.isSneaking()) {
                    construction.cancelPreview(player);
                    exit(player, "Abgebrochen", false);
                } else if (construction.confirmPreview(player)) {
                    exit(player, "Bestätigt", false);
                }
            }
            case "CANCEL" -> {
                construction.cancelPreview(player);
                exit(player, "Abgebrochen", false);
            }
            default -> Msg.error(player, "Unbekanntes Bauwerkzeug.");
        }
    }

    public boolean handleInteraction(Player player, ItemStack item) {
        if (!isActive(player) || !isTool(item)) return false;
        String action = item.getItemMeta().getPersistentDataContainer().get(toolKey, PersistentDataType.STRING);
        handleTool(player, action);
        return true;
    }

    private PlacementSnapshot snapshot(Player player, Village village, BlueprintType type) {
        return new PlacementSnapshot(player.getUniqueId(), village.id(), type,
                ItemStack.serializeItemsAsBytes(player.getInventory().getContents()),
                ItemStack.serializeItemsAsBytes(player.getInventory().getArmorContents()),
                ItemStack.serializeItemsAsBytes(player.getInventory().getExtraContents()),
                player.getInventory().getHeldItemSlot(), System.currentTimeMillis());
    }

    private void restore(Player player, PlacementSnapshot snapshot) {
        player.getInventory().setContents(ItemStack.deserializeItemsFromBytes(snapshot.contents()));
        player.getInventory().setArmorContents(ItemStack.deserializeItemsFromBytes(snapshot.armor()));
        player.getInventory().setExtraContents(ItemStack.deserializeItemsFromBytes(snapshot.extras()));
        player.getInventory().setHeldItemSlot(snapshot.selectedSlot());
        player.updateInventory();
    }

    private void installTools(Player player) {
        player.getInventory().setItem(0, tool(Material.RED_CONCRETE, "X_MINUS", "X −1", "Sneak = Anker fixieren/lösen"));
        player.getInventory().setItem(1, tool(Material.LIME_CONCRETE, "X_PLUS", "X +1", "Sneak = Position frei wählen"));
        player.getInventory().setItem(2, tool(Material.BLUE_CONCRETE, "Y_MINUS", "Z −1", "Z um 1 verringern"));
        player.getInventory().setItem(3, tool(Material.LIGHT_BLUE_CONCRETE, "Y_PLUS", "Z +1", "Z um 1 erhöhen"));
        player.getInventory().setItem(4, tool(Material.REDSTONE_TORCH, "ROTATE_LEFT", "Links drehen", "90° gegen den Uhrzeigersinn"));
        player.getInventory().setItem(5, tool(Material.REPEATER, "ROTATE_RIGHT", "Rechts drehen", "90° im Uhrzeigersinn"));
        player.getInventory().setItem(6, tool(Material.FEATHER, "UP", "Hoch +1", "Eine Ebene nach oben"));
        player.getInventory().setItem(7, tool(Material.ANVIL, "DOWN", "Runter −1", "Eine Ebene nach unten"));
        player.getInventory().setItem(8, tool(Material.EMERALD, "CONFIRM_OR_CANCEL", "Bestätigen / Abbrechen", "Rechtsklick = bestätigen · Sneak = abbrechen"));
        player.getInventory().setHeldItemSlot(0);
        player.updateInventory();
        player.sendActionBar(Component.text("Baumodus aktiv · Anker fixiert", NamedTextColor.GOLD));
    }

    private ItemStack tool(Material material, String action, String name, String lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.GOLD));
        meta.lore(java.util.List.of(Component.text(lore, NamedTextColor.GRAY)));
        meta.getPersistentDataContainer().set(toolKey, PersistentDataType.STRING, action);
        item.setItemMeta(meta);
        return item;
    }

    private Village villagesFor(Player player) {
        long id = construction.previewVillageId(player);
        return construction.villageById(id);
    }

    private record SessionState(PlacementSnapshot snapshot, boolean anchored) {
    }
}
