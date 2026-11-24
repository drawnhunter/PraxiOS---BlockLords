package eu.praxios.blocklords.ui;

import eu.praxios.blocklords.config.BlockLordsConfig;
import eu.praxios.blocklords.model.BlueprintType;
import eu.praxios.blocklords.model.BuildSite;
import eu.praxios.blocklords.model.Building;
import eu.praxios.blocklords.model.ClaimRegion;
import eu.praxios.blocklords.model.QuestState;
import eu.praxios.blocklords.model.ResourceType;
import eu.praxios.blocklords.model.Village;
import eu.praxios.blocklords.model.VillageStats;
import eu.praxios.blocklords.service.ConstructionService;
import eu.praxios.blocklords.service.FoundingService;
import eu.praxios.blocklords.service.ResearchService;
import eu.praxios.blocklords.service.ResidentService;
import eu.praxios.blocklords.service.StorageService;
import eu.praxios.blocklords.service.TaskRunner;
import eu.praxios.blocklords.service.VillageScanner;
import eu.praxios.blocklords.service.VillageService;
import eu.praxios.blocklords.service.WorkerService;
import eu.praxios.blocklords.util.Msg;
import eu.praxios.blocklords.util.VillageNames;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public final class UiService {
    private final Supplier<BlockLordsConfig> config;
    private final VillageService villages;
    private final FoundingService founding;
    private final VillageScanner scanner;
    private final TaskRunner tasks;
    private final BedrockUi bedrock;
    private final ConstructionService construction;
    private final StorageService storage;
    private final WorkerService workers;
    private final ResearchService research;
    private final ResidentService residents;

    public UiService(Supplier<BlockLordsConfig> config, VillageService villages, FoundingService founding,
                     VillageScanner scanner, TaskRunner tasks, ConstructionService construction,
                     StorageService storage, WorkerService workers, ResearchService research,
                     ResidentService residents) {
        this.config = config;
        this.villages = villages;
        this.founding = founding;
        this.scanner = scanner;
        this.tasks = tasks;
        this.construction = construction;
        this.storage = storage;
        this.workers = workers;
        this.research = research;
        this.residents = residents;
        BedrockUi candidate = null;
        if (Bukkit.getPluginManager().isPluginEnabled("Floodgate") && config.get().bedrockForms()) {
            try {
                Class.forName("org.geysermc.floodgate.api.FloodgateApi", false, getClass().getClassLoader());
                Class.forName("org.geysermc.cumulus.form.Form", false, getClass().getClassLoader());
                candidate = new BedrockUi(tasks);
            } catch (ClassNotFoundException | NoClassDefFoundError unavailable) {
                Bukkit.getLogger().warning("BlockLords: Floodgate erkannt, aber Formular-API nicht verfügbar; Java/Befehle bleiben aktiv.");
            }
        }
        this.bedrock = candidate;
    }

    public void openBell(Player player, Block bell) {
        villages.atBell(bell).ifPresentOrElse(village -> {
            if (village.owner().equals(player.getUniqueId())) {
                villages.markActivity(village, player.getUniqueId());
            }
            openVillage(player, village);
        }, () -> openUnclaimed(player, bell));
    }

    public void openTargetInfo(Player player) {
        Block target = player.getTargetBlockExact(6);
        if (target != null && target.getType() == Material.BELL) {
            openBell(player, target);
            return;
        }
        villages.at(player.getLocation()).ifPresentOrElse(village -> openVillage(player, village),
                () -> Msg.error(player, "Visiere eine Dorfglocke an oder betritt ein BlockLords-Dorf."));
    }

    public void openUnclaimed(Player player, Block bell) {
        ClaimRegion region = new ClaimRegion(bell.getWorld().getUID(), bell.getX(), bell.getY(), bell.getZ(), 1);
        if (bedrockAvailable(player)) {
            bedrock.openUnclaimed(player, bell, founding.questInfo(player),
                    () -> founding.startQuest(player, bell), () -> openFoundNames(player, bell));
            return;
        }
        MenuHolder holder = new MenuHolder(MenuContext.unclaimed(region));
        Inventory inventory = Bukkit.createInventory(holder, 27, Component.text("BlockLords · Dorf entdecken"));
        holder.inventory(inventory);
        inventory.setItem(11, item(Material.BOOK, "Unbeanspruchte Dorfglocke",
                "Position: " + bell.getX() + "/" + bell.getY() + "/" + bell.getZ(), founding.questInfo(player)));
        inventory.setItem(13, item(Material.OAK_SAPLING, "Gründungsquest starten",
                "Erkunde Bett, Nahrung und Arbeitsplatz.", "Die erste Gründung ist anschließend kostenlos."));
        inventory.setItem(15, item(Material.EMERALD, "Dorf gründen",
                founding.quest(player).founderCompleted() ? "Dorfnamen wählen" : "Erst Gründungsquest abschließen"));
        inventory.setItem(22, item(Material.BARRIER, "Schließen"));
        player.openInventory(inventory);
    }

    public void openFoundNames(Player player, Block bell) {
        QuestState state = founding.quest(player);
        if (config.get().requireQuest() && !state.founderCompleted()) {
            Msg.error(player, "Schließe zuerst die Gründungsquest ab: " + founding.questInfo(player));
            return;
        }
        List<String> names = VillageNames.suggestions();
        if (bedrockAvailable(player)) {
            bedrock.openFoundName(player, names.getFirst(), name -> founding.found(player, bell, name,
                    village -> openVillage(player, village)));
            return;
        }
        ClaimRegion region = new ClaimRegion(bell.getWorld().getUID(), bell.getX(), bell.getY(), bell.getZ(), 1);
        MenuHolder holder = new MenuHolder(MenuContext.foundNames(region, names));
        Inventory inventory = Bukkit.createInventory(holder, 27, Component.text("BlockLords · Dorfname wählen"));
        holder.inventory(inventory);
        int[] slots = {11, 13, 15};
        for (int index = 0; index < names.size(); index++) {
            inventory.setItem(slots[index], item(Material.EMERALD, "Als '" + names.get(index) + "' gründen",
                    "Kosten: " + (villages.owned(player.getUniqueId()).isEmpty() ? "kostenlos" : config.get().nextVillageCost() + " Smaragde")));
        }
        inventory.setItem(18, item(Material.BARRIER, "Zurück"));
        inventory.setItem(22, item(Material.WRITABLE_BOOK, "Eigener Name per Befehl", "/bl found <Dorfname>"));
        player.openInventory(inventory);
    }

    public void openVillage(Player player, Village village) {
        String content = "Landherr: " + ownerName(village) + " · Mitglieder: " + villages.memberCount(village.id())
                + "\nLevel " + village.level() + " · XP " + village.xp() + " · " + village.status()
                + "\n" + statsText(village);
        java.util.List<UiAction> actions = new java.util.ArrayList<>();
        actions.add(UiAction.of(Material.CHEST, "Dorfvorrat", () -> openStorage(player, village), storage.summary(village.id())));
        actions.add(UiAction.of(Material.OAK_PLANKS, "Bauen & Baustellen", () -> openBuild(player, village)));
        actions.add(UiAction.of(Material.VILLAGER_SPAWN_EGG, "Arbeiter", () -> openWorkers(player, village)));
        actions.add(UiAction.of(Material.ENCHANTED_BOOK, "Bibliothek & Forschung", () -> openResearch(player, village)));
        actions.add(UiAction.of(Material.COMPASS, "Dorf vermessen", () -> scanner.requestAndNotify(village, player)));
        actions.add(UiAction.of(Material.BARRIER, "Schließen", player::closeInventory));
        openActions(player, "BlockLords · " + village.name(), content, actions);
    }

    public void openStorage(Player player, Village village) {
        java.util.List<UiAction> actions = new java.util.ArrayList<>();
        actions.add(UiAction.of(Material.HOPPER, "Alles einlagern", () -> storage.depositAll(player, village),
                "Nimmt nur eindeutig zugeordnete Ressourcen aus deinem Inventar."));
        for (ResourceType resource : ResourceType.values()) {
            int balance = storage.balances(village.id()).getOrDefault(resource, 0);
            actions.add(UiAction.of(resource.canonicalItem(), resource.displayName() + " · " + balance,
                    () -> storage.withdraw(player, village, resource, 64), "64 abheben; Bestand und Kapazität werden geprüft."));
        }
        actions.add(UiAction.of(Material.ARROW, "Zurück", () -> openVillage(player, village)));
        openActions(player, "Vorrat · " + village.name(), storage.summary(village.id()), actions);
    }

    public void openBuild(Player player, Village village) {
        java.util.List<UiAction> actions = new java.util.ArrayList<>();
        for (BlueprintType type : construction.available(village)) {
            actions.add(UiAction.of(Material.OAK_PLANKS, type.displayName() + " · Vorschau",
                    () -> {
                        player.closeInventory();
                        construction.preview(player, village, type);
                    }, "Kosten: " + type.cost(), "Erforderliches Dorf-Level: " + type.requiredLevel()));
        }
        actions.add(UiAction.of(Material.CRAFTING_TABLE, "Meine Baustellen", () -> openSites(player, village)));
        actions.add(UiAction.of(Material.ARROW, "Zurück", () -> openVillage(player, village)));
        openActions(player, "Bauen · " + village.name(), "Wähle eine Vorschau oder bestücke eine vorhandene Baustelle.", actions);
    }

    public void openSites(Player player, Village village) {
        java.util.List<UiAction> actions = new java.util.ArrayList<>();
        for (BuildSite site : construction.sites(village.id())) {
            actions.add(UiAction.of(Material.CRAFTING_TABLE, "#" + site.id() + " " + site.type().displayName(),
                    () -> openSite(player, village, site), "Status: " + site.status(), "Rest: " + remaining(site)));
        }
        actions.add(UiAction.of(Material.ARROW, "Zurück", () -> openBuild(player, village)));
        openActions(player, "Baustellen · " + village.name(), construction.sites(village.id()).isEmpty()
                ? "Noch keine Baustellen." : "Beiträge mehrerer Spieler werden protokolliert.", actions);
    }

    public void openSite(Player player, Village village, BuildSite site) {
        construction.selectSite(player, site);
        java.util.List<UiAction> actions = new java.util.ArrayList<>();
        actions.add(UiAction.of(Material.CHEST, "Inventar beisteuern", () -> construction.contribute(player, village)));
        actions.add(UiAction.of(Material.ENDER_CHEST, "Dorfvorrat verwenden", () -> construction.fund(player, village)));
        actions.add(UiAction.of(Material.ANVIL, "FAWE-Bau starten", () -> construction.start(player, village),
                site.complete() ? "Alle Materialien vorhanden." : "Rest: " + remaining(site)));
        actions.add(UiAction.of(Material.ARROW, "Zurück", () -> openSites(player, village)));
        int required = site.required().values().stream().mapToInt(Integer::intValue).sum();
        int supplied = site.contributed().values().stream().mapToInt(Integer::intValue).sum();
        openActions(player, "Baustelle #" + site.id(), site.type().displayName()
                + "\nMaterial: " + supplied + "/" + required + " · Baufortschritt: " + site.status()
                + "\nRest: " + remaining(site), actions);
    }

    public void openWorkers(Player player, Village village) {
        java.util.List<UiAction> actions = new java.util.ArrayList<>();
        for (Building building : construction.buildings(village.id())) {
            if (building.type() == BlueprintType.FARM || building.type() == BlueprintType.MINE) {
                long assigned = workers.workers(village.id()).stream().filter(worker -> worker.buildingId() == building.id()).count();
                actions.add(UiAction.of(Material.VILLAGER_SPAWN_EGG,
                        "#" + building.id() + " " + building.type().displayName() + " · " + assigned + " Arbeiter",
                        () -> workers.assign(player, village, building.id()), "Freien Bewohner im Umkreis zuweisen."));
            }
        }
        workers.workers(village.id()).forEach(worker -> actions.add(UiAction.of(Material.NAME_TAG,
                "Arbeiter #" + worker.id() + " entfernen", () -> workers.unassign(player, village, worker.id()),
                worker.job() + " · Gebäude #" + worker.buildingId())));
        actions.add(UiAction.of(Material.BELL, "Versammlung läuten", () -> residents.assemble(player, village),
                "Bestätigt Anwesende und meldet neue sowie bettlose Bewohner."));
        actions.add(UiAction.of(Material.ARROW, "Zurück", () -> openVillage(player, village)));
        String content = "Register: " + residents.residents(village.id()).size() + " Bewohner · "
                + residents.confirmed(village.id()) + " bestätigt · " + residents.unconfirmed(village.id()) + " unbestätigt · "
                + residents.free(village.id(), workers.workers(village.id())).size() + " frei.\n"
                + "Sneak + Rechtsklick auf die Dorfglocke startet ebenfalls die Versammlung.";
        openActions(player, "Arbeiter · " + village.name(), content, actions);
    }

    public void openResearch(Player player, Village village) {
        java.util.List<UiAction> actions = new java.util.ArrayList<>();
        actions.add(UiAction.of(Material.ENCHANTED_BOOK, "Wohnhaus II erforschen", () -> research.startResearch(player, village),
                "Kosten: 32 Holz, 16 Stein, 8 Smaragde", "Benötigt eine Bibliothek."));
        research.projects(village.id()).forEach(project -> actions.add(UiAction.of(Material.BOOK,
                project.key() + " · " + project.status(), () -> { }, "Ende: " + java.time.Instant.ofEpochMilli(project.finishesAt()))));
        actions.add(UiAction.of(Material.ARROW, "Zurück", () -> openVillage(player, village)));
        openActions(player, "Forschung · " + village.name(), "Bibliotheks-Upgrades erscheinen nach Abschluss als Bauplan.", actions);
    }

    public void openBuildingInfo(Player player, Village village, Building building) {
        java.util.List<UiAction> actions = new java.util.ArrayList<>();
        actions.add(UiAction.of(Material.BOOK, "Gebäudeinformationen", () -> { },
                "Typ: " + building.type().displayName(), "Gebäude-Level: " + building.level(),
                "Dorf-Level: " + village.level() + " · XP: " + village.xp()));
        actions.add(UiAction.of(Material.ARROW, "Zurück zum Dorf", () -> openVillage(player, village)));
        openActions(player, building.type().displayName() + " · " + village.name(),
                "Baustelle abgeschlossen. Die Kiste bleibt als Informations- und Restmaterialpunkt erhalten.", actions);
    }

    public void openActions(Player player, String title, String content, java.util.List<UiAction> actions) {
        if (bedrockAvailable(player)) {
            bedrock.openActions(player, title, content, actions);
            return;
        }
        int size = Math.max(9, Math.min(54, ((actions.size() + 8) / 9) * 9));
        ActionMenuHolder holder = new ActionMenuHolder(actions);
        Inventory inventory = Bukkit.createInventory(holder, size, Component.text(title));
        holder.inventory(inventory);
        for (int index = 0; index < actions.size(); index++) {
            UiAction action = actions.get(index);
            inventory.setItem(index, item(action.icon(), action.name(), action.lore().toArray(new String[0])));
        }
        player.openInventory(inventory);
    }

    private String remaining(BuildSite site) {
        return site.required().keySet().stream().filter(resource -> site.remaining(resource) > 0)
                .map(resource -> site.remaining(resource) + " " + resource.displayName())
                .reduce((left, right) -> left + ", " + right).orElse("bereit");
    }

    public void handleClick(Player player, MenuContext context, int slot) {
        if (slot == 22) {
            player.closeInventory();
            if (context.type() == MenuContext.MenuType.FOUND_NAME) {
                Msg.info(player, "Eigener Dorfname: /bl found <Dorfname> — visiere dazu die Glocke erneut an.");
            }
            return;
        }
        Block bell = bell(context.bell());
        if (bell == null) {
            player.closeInventory();
            Msg.error(player, "Die Welt dieser Glocke ist derzeit nicht geladen.");
            return;
        }
        switch (context.type()) {
            case UNCLAIMED -> {
                if (slot == 13) founding.startQuest(player, bell);
                if (slot == 15) openFoundNames(player, bell);
            }
            case FOUND_NAME -> {
                if (slot == 18) {
                    openUnclaimed(player, bell);
                    return;
                }
                int index = switch (slot) {
                    case 11 -> 0;
                    case 13 -> 1;
                    case 15 -> 2;
                    default -> -1;
                };
                if (index >= 0 && index < context.names().size()) {
                    player.closeInventory();
                    founding.found(player, bell, context.names().get(index), village -> openVillage(player, village));
                }
            }
            case VILLAGE -> {
                if (slot == 15) {
                    villages.byId(context.villageId()).ifPresent(village -> scanner.requestAndNotify(village, player));
                }
            }
        }
    }

    private Block bell(ClaimRegion region) {
        World world = Bukkit.getWorld(region.world());
        return world == null ? null : world.getBlockAt(region.bellX(), region.bellY(), region.bellZ());
    }

    public void handleQuestUpdate(Player player) {
        if (player.getOpenInventory().getTopInventory().getHolder() instanceof MenuHolder holder
                && holder.context().type() == MenuContext.MenuType.UNCLAIMED) {
            Block bell = bell(holder.context().bell());
            if (bell != null) openUnclaimed(player, bell);
        }
    }

    private boolean bedrockAvailable(Player player) {
        return bedrock != null && bedrock.available(player, config.get().bedrockForms());
    }

    private String statsText(Village village) {
        return scanner.fresh(village).map(stats -> stats.villagers() + " Bewohner, " + stats.beds() + " Betten, "
                        + stats.loadedChunks() + "/" + stats.totalChunks() + " Chunks" + (stats.complete() ? "" : " (unvollständig)"))
                .orElse("Noch keine aktuelle Vermessung.");
    }

    private String ownerName(Village village) {
        String name = Bukkit.getOfflinePlayer(village.owner()).getName();
        return name == null ? village.owner().toString().substring(0, 8) : name;
    }

    private ItemStack item(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.GOLD));
        meta.lore(Arrays.stream(lore).map(line -> Component.text(line, NamedTextColor.GRAY)).toList());
        item.setItemMeta(meta);
        return item;
    }
}
