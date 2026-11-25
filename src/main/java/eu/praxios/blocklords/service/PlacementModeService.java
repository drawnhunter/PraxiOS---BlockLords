package eu.praxios.blocklords.service;

import eu.praxios.blocklords.model.BlueprintType;
import eu.praxios.blocklords.model.PlacementSnapshot;
import eu.praxios.blocklords.model.Village;
import eu.praxios.blocklords.repository.PlacementRepository;
import eu.praxios.blocklords.util.Msg;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Hotbar-Menüs und Edit-Werkzeuge teilen eine persistierte, sicher wiederherstellbare Inventar-Session. */
public final class PlacementModeService {
    private static final int PAGE_SIZE = 8;
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

    /** Root: Bauen, Baustellen, Bewohner, Vorschau; offhand = zurück/verlassen. */
    public void openRoot(Player player, Village village) {
        SessionState current = active.get(player.getUniqueId());
        if (current != null) {
            active.put(player.getUniqueId(), current.withMenu(List.of("ROOT"), 0));
            installRoot(player);
            return;
        }
        startSession(player, village, BlueprintType.RATHAUS, this::installRoot);
    }

    /** Nach Auswahl eines Bauplans wird direkt die Edit-Hotbar aktiviert. */
    public void enterExisting(Player player) {
        if (!construction.hasPreview(player)) {
            Msg.error(player, "Wähle zuerst eine Bauplan-Vorschau.");
            return;
        }
        SessionState current = active.get(player.getUniqueId());
        if (current != null) {
            active.put(player.getUniqueId(), current.withEdit());
            installTools(player);
            return;
        }
        Village village = villagesFor(player);
        if (village == null) {
            Msg.error(player, "Die Vorschau gehört zu keinem geladenen Dorf.");
            return;
        }
        startSession(player, village, construction.previewType(player), this::installTools);
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
        active.put(player.getUniqueId(), current.withAnchor(!current.anchored()));
        Msg.info(player, !current.anchored() ? "Preview-Anker fixiert." : "Preview-Anker gelöst; Sneak + X+ wählt die Position.");
    }

    public boolean handleInteraction(Player player, ItemStack item) {
        if (!isActive(player) || !isTool(item)) return false;
        String action = item.getItemMeta().getPersistentDataContainer().get(toolKey, PersistentDataType.STRING);
        if (action == null) return true;
        if (action.startsWith("MENU_") || action.startsWith("BLUEPRINT:")) {
            handleMenu(player, action);
        } else {
            handleEdit(player, action);
        }
        return true;
    }

    private void handleMenu(Player player, String action) {
        SessionState session = active.get(player.getUniqueId());
        if (session == null) return;
        switch (action) {
            case "MENU_BUILD" -> installBuild(player, 0);
            case "MENU_SITES" -> Bukkit.dispatchCommand(player, "bl build sites");
            case "MENU_RESIDENTS" -> Bukkit.dispatchCommand(player, "bl workers");
            case "MENU_PREVIEW" -> enterExisting(player);
            case "MENU_NEXT" -> installBuild(player, session.page() + 1);
            case "MENU_PREVIOUS" -> installBuild(player, session.page() - 1);
            case "MENU_BACK_OR_EXIT" -> backOrExit(player);
            case "MENU_NOOP" -> { }
            default -> {
                if (action.startsWith("BLUEPRINT:")) {
                    selectBlueprint(player, BlueprintType.valueOf(action.substring("BLUEPRINT:".length())));
                }
            }
        }
    }

    private void handleEdit(Player player, String action) {
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

    private void selectBlueprint(Player player, BlueprintType type) {
        SessionState current = active.get(player.getUniqueId());
        if (current == null) return;
        Village village = construction.villageById(current.snapshot().villageId());
        if (village == null) return;
        if (construction.previewInFront(player, village, type)) {
            active.put(player.getUniqueId(), current.withEdit());
            installTools(player);
        }
    }

    private void backOrExit(Player player) {
        SessionState current = active.get(player.getUniqueId());
        if (current == null) return;
        if (current.trail().size() <= 1) {
            exit(player, "Verlassen", true);
            return;
        }
        List<String> trail = new ArrayList<>(current.trail());
        String leaving = trail.removeLast();
        if (leaving.equals("EDIT")) construction.cancelPreview(player);
        active.put(player.getUniqueId(), current.withMenu(trail, 0));
        if (trail.getLast().equals("BUILD")) installBuild(player, 0);
        else installRoot(player);
    }

    private void startSession(Player player, Village village, BlueprintType type,
                              java.util.function.Consumer<Player> installer) {
        PlacementSnapshot snapshot = snapshot(player, village, type == null ? BlueprintType.RATHAUS : type);
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
            active.put(player.getUniqueId(), SessionState.menu(saved));
            installer.accept(player);
            audit.log(player, "PLACEMENT_MODE_START", "Dorf #" + village.id() + " " + saved.blueprint());
            Msg.success(player, "Baumodus aktiv: Hotbar-Werkzeuge ersetzt. Bestätigen/Abbrechen stellt dein Inventar wieder her.");
            Msg.info(player, "X/Z ±, links/rechts, hoch/runter. Rechtsklick = bestätigen, Sneak = abbrechen. Alternativ: /bl build confirm|cancel|move|left|right.");
        }, error -> {
            logs.error("PLACEMENT-START", "Snapshot konnte nicht gespeichert werden; Hotbar bleibt unverändert", error);
            Msg.error(player, "Baumodus nicht aktiviert: Snapshot konnte nicht gespeichert werden.");
        });
    }

    private void installRoot(Player player) {
        clearToolbar(player);
        set(player, 0, Material.OAK_PLANKS, "MENU_BUILD", "Bauen", "Baupläne in der Hotbar öffnen");
        set(player, 1, Material.CRAFTING_TABLE, "MENU_SITES", "Baustellen", "Vorhandene Baustellen öffnen");
        set(player, 2, Material.VILLAGER_SPAWN_EGG, "MENU_RESIDENTS", "Bewohner", "Bewohnerverwaltung öffnen");
        set(player, 3, Material.COMPASS, "MENU_PREVIEW", "Vorschau bearbeiten", "Vorhandene Vorschau in den Edit-Modus bringen");
        set(player, 7, Material.ARROW, "MENU_BACK_OR_EXIT", "Zurück", "Bis zum Root zurück; dort Verlassen");
        set(player, 8, Material.BARRIER, "MENU_BACK_OR_EXIT", "Verlassen", "Beendet den Modus und stellt das Inventar wieder her");
        setOffhand(player, Material.BARRIER, "MENU_BACK_OR_EXIT", "Zurück / Verlassen", "Eine Stufe zurück; am Root beenden");
        player.updateInventory();
    }

    private void installBuild(Player player, int requestedPage) {
        SessionState current = active.get(player.getUniqueId());
        if (current == null) return;
        List<BlueprintType> types = List.of(BlueprintType.values());
        int pageCount = Math.max(1, (types.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int page = Math.max(0, Math.min(requestedPage, pageCount - 1));
        clearToolbar(player);
        int start = page * PAGE_SIZE;
        int end = Math.min(types.size(), start + PAGE_SIZE);
        for (int index = start; index < end; index++) {
            BlueprintType type = types.get(index);
            String lore = type.requiredLevel() > currentVillageLevel(current)
                    ? "Gesperrt: Dorf-Level " + type.requiredLevel() + " benötigt"
                    : "Kosten: " + type.cost();
            set(player, index - start, Material.OAK_PLANKS, "BLUEPRINT:" + type.name(), type.displayName(), lore);
        }
        if (end < types.size()) {
            set(player, 8, Material.ARROW, "MENU_NEXT", "Weiter", "Seite " + (page + 2) + "/" + pageCount);
        } else {
            set(player, 8, Material.LIGHT_GRAY_STAINED_GLASS_PANE, "MENU_NOOP", " ", " ");
        }
        setOffhand(player, Material.ARROW, "MENU_BACK_OR_EXIT", "Zurück / Verlassen", "Zurück zum Root, dort Verlassen");
        active.put(player.getUniqueId(), current.withMenu(List.of("ROOT", "BUILD"), page));
        player.updateInventory();
    }

    private void installTools(Player player) {
        clearToolbar(player);
        set(player, 0, Material.RED_CONCRETE, "X_MINUS", "X −1", "Sneak = Anker fixieren/lösen");
        set(player, 1, Material.LIME_CONCRETE, "X_PLUS", "X +1", "Sneak = Position frei wählen");
        set(player, 2, Material.BLUE_CONCRETE, "Y_MINUS", "Z −1", "Z um 1 verringern");
        set(player, 3, Material.LIGHT_BLUE_CONCRETE, "Y_PLUS", "Z +1", "Z um 1 erhöhen");
        set(player, 4, Material.REDSTONE_TORCH, "ROTATE_LEFT", "Links drehen", "90° gegen den Uhrzeigersinn");
        set(player, 5, Material.REPEATER, "ROTATE_RIGHT", "Rechts drehen", "90° im Uhrzeigersinn");
        set(player, 6, Material.FEATHER, "UP", "Hoch +1", "Eine Ebene nach oben");
        set(player, 7, Material.ANVIL, "DOWN", "Runter −1", "Eine Ebene nach unten");
        set(player, 8, Material.EMERALD, "CONFIRM_OR_CANCEL", "Bestätigen / Abbrechen", "Rechtsklick = bestätigen · Sneak = abbrechen");
        setOffhand(player, Material.ARROW, "MENU_BACK_OR_EXIT", "Zurück / Verlassen", "Zurück zur Bauplanauswahl");
        player.getInventory().setHeldItemSlot(0);
        player.updateInventory();
        player.sendActionBar(Component.text("Baumodus aktiv · Anker fixiert", NamedTextColor.GOLD));
    }

    private void clearToolbar(Player player) {
        for (int slot = 0; slot < 9; slot++) player.getInventory().setItem(slot, null);
        player.getInventory().setItemInOffHand(null);
    }

    private void set(Player player, int slot, Material material, String action, String name, String lore) {
        player.getInventory().setItem(slot, tool(material, action, name, lore));
    }

    private void setOffhand(Player player, Material material, String action, String name, String lore) {
        player.getInventory().setItemInOffHand(tool(material, action, name, lore));
    }

    private int currentVillageLevel(SessionState current) {
        Village village = construction.villageById(current.snapshot().villageId());
        return village == null ? 1 : village.level();
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

    private Village villagesFor(Player player) {
        return construction.villageById(construction.previewVillageId(player));
    }

    private record SessionState(PlacementSnapshot snapshot, boolean anchored, String mode, List<String> trail, int page) {
        static SessionState menu(PlacementSnapshot snapshot) {
            return new SessionState(snapshot, true, "MENU", List.of("ROOT"), 0);
        }

        SessionState withAnchor(boolean value) {
            return new SessionState(snapshot, value, mode, trail, page);
        }

        SessionState withEdit() {
            List<String> updated = new ArrayList<>(trail);
            if (!updated.contains("BUILD")) updated.add("BUILD");
            if (!updated.contains("EDIT")) updated.add("EDIT");
            return new SessionState(snapshot, true, "EDIT", List.copyOf(updated), 0);
        }

        SessionState withMenu(List<String> newTrail, int newPage) {
            return new SessionState(snapshot, true, "MENU", List.copyOf(newTrail), newPage);
        }
    }
}
