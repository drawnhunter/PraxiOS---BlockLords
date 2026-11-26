package eu.praxios.blocklords.ui;

import eu.praxios.blocklords.config.BlockLordsConfig;
import eu.praxios.blocklords.model.BlueprintType;
import eu.praxios.blocklords.model.BuildSite;
import eu.praxios.blocklords.model.Building;
import eu.praxios.blocklords.model.ClaimRegion;
import eu.praxios.blocklords.model.QuestState;
import eu.praxios.blocklords.model.Resident;
import eu.praxios.blocklords.model.ResourceType;
import eu.praxios.blocklords.model.Village;
import eu.praxios.blocklords.model.VillageStats;
import eu.praxios.blocklords.model.Worker;
import eu.praxios.blocklords.service.ConstructionService;
import eu.praxios.blocklords.service.FoundingService;
import eu.praxios.blocklords.service.ResearchService;
import eu.praxios.blocklords.service.ResidentService;
import eu.praxios.blocklords.service.PlacementModeService;
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
    private final PlacementModeService placement;
    private final MissionUiService missionUi;
    private final ImmigrationUiService immigrationUi;
    private final ProductionUiService productionUi;

    public UiService(Supplier<BlockLordsConfig> config, VillageService villages, FoundingService founding,
                     VillageScanner scanner, TaskRunner tasks, ConstructionService construction,
                     StorageService storage, WorkerService workers, ResearchService research,
                     ResidentService residents, PlacementModeService placement,
                     eu.praxios.blocklords.service.MissionService missions) {
        this(config, villages, founding, scanner, tasks, construction, storage, workers, research, residents, placement,
                missions, null, null);
    }

    public UiService(Supplier<BlockLordsConfig> config, VillageService villages, FoundingService founding,
                     VillageScanner scanner, TaskRunner tasks, ConstructionService construction,
                     StorageService storage, WorkerService workers, ResearchService research,
                     ResidentService residents, PlacementModeService placement,
                     eu.praxios.blocklords.service.MissionService missions,
                     eu.praxios.blocklords.service.ImmigrationService immigration,
                     eu.praxios.blocklords.service.ProductionService production) {
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
        this.placement = placement;
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
        this.missionUi = new MissionUiService(missions, this);
        this.immigrationUi = new ImmigrationUiService(immigration, this);
        this.productionUi = new ProductionUiService(production, storage, this);
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
        actions.add(UiAction.of(Material.OAK_PLANKS, "Bauen & Baustellen", () -> placement.openRoot(player, village),
                "Hotbar-Menü: Bauen, Baustellen, Bewohner und Vorschau."));
        actions.add(UiAction.of(Material.WRITABLE_BOOK, "Missionen", () -> openMissions(player, village),
                "Zwei persönliche Angebote und eine Dorfmission · kein Verfall."));
        actions.add(UiAction.of(Material.VILLAGER_SPAWN_EGG, "Bewohner", () -> openWorkers(player, village)));
        actions.add(UiAction.of(Material.POPPY, "Einwanderung", () -> openImmigration(player, village),
                "Wanderer, Voraussetzungen und Aufnahme ab Level 2."));
        actions.add(UiAction.of(Material.ANVIL, "Produktion", () -> openProduction(player, village),
                "Ertrag, Verbrauch und Pausen pro Ressource."));
        actions.add(UiAction.of(Material.ENCHANTED_BOOK, "Bibliothek & Forschung", () -> openResearch(player, village)));
        actions.add(UiAction.of(Material.BOOK, "Dorf Infos", () -> openVillageInfo(player, village), levelLore(village)));
        actions.add(UiAction.of(Material.COMPASS, "Dorfmenü-Kompass", () -> Bukkit.dispatchCommand(player, "bl menu"),
                "Gibt dir den Kompass zum Öffnen des Dorfmenüs ins Inventar."));
        actions.add(UiAction.of(Material.MAP, "Dorf vermessen", () -> scanner.requestAndNotify(village, player)));
        actions.add(UiAction.of(Material.BARRIER, "Schließen", player::closeInventory));
        openActions(player, "BlockLords · " + village.name(), content, actions);
    }

    public void openMissions(Player player, Village village) {
        missionUi.open(player, village);
    }

    public void openImmigration(Player player, Village village) {
        immigrationUi.open(player, village);
    }

    public void openProduction(Player player, Village village) {
        productionUi.open(player, village);
    }

    public void openVillageInfo(Player player, Village village) {
        java.util.List<UiAction> actions = new java.util.ArrayList<>();
        actions.add(UiAction.of(Material.PAPER, "Dorfname", () -> { }, village.name()));
        actions.add(UiAction.of(Material.PLAYER_HEAD, "Landherr", () -> { }, ownerName(village)));
        actions.add(UiAction.of(Material.EXPERIENCE_BOTTLE, "Dorf-Level " + village.level(), () -> { }, levelLore(village)));
        long assigned = workers.workers(village.id()).size();
        int freeJobs = construction.buildings(village.id()).stream().filter(building -> building.type().workplace())
                .mapToInt(building -> (int) Math.max(0, workers.capacity() - workers.workers(village.id()).stream()
                        .filter(worker -> worker.buildingId() == building.id()).count())).sum();
        actions.add(UiAction.of(Material.ANVIL, "Arbeiter · " + assigned, () -> { }, freeJobs + " freie Arbeitsplätze."));
        actions.add(UiAction.of(Material.VILLAGER_SPAWN_EGG, "Bewohner · " + residents.living(village.id()).size(),
                () -> openWorkers(player, village), residents.confirmed(village.id()) + " bestätigt · "
                + residents.unconfirmed(village.id()) + " unbestätigt"));
        long homeless = residents.living(village.id()).stream().filter(resident -> !resident.hasBed()).count();
        if (scanner.fresh(village).isEmpty()) {
            scanner.request(village, stats -> {
                if (net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                        .serialize(player.getOpenInventory().title()).startsWith("Dorf Infos · ")) {
                    openVillageInfo(player, village);
                }
            });
        }
        long beds = scanner.fresh(village).map(stats -> (long) stats.beds()).orElse(-1L);
        String bedText = beds < 0 ? "Vermessung läuft …" : beds + " Betten";
        actions.add(UiAction.of(Material.RED_BED, "Betten & Obdachlose", () -> { },
                bedText + " · " + homeless + " Bewohner ohne Bett"));
        actions.add(UiAction.of(Material.CHEST, "Dorfvorrat", () -> openStorage(player, village), storage.summary(village.id())));
        for (BlueprintType type : BlueprintType.values()) {
            actions.add(UiAction.of(Material.OAK_PLANKS, type.displayName() + " · +" + type.xp() + " Dorf-XP",
                    () -> { }, "Erstmaliger Bauabschluss"));
        }
        actions.add(UiAction.of(Material.MAP, "Dorf vermessen", () -> scanner.requestAndNotify(village, player)));
        actions.add(UiAction.of(Material.ARROW, "Zurück", () -> openVillage(player, village)));
        openActions(player, "Dorf Infos · " + village.name(), "Dorfdaten und XP für erstmalige Bauabschlüsse.", actions);
    }

    public void openStorage(Player player, Village village) {
        java.util.List<UiAction> actions = new java.util.ArrayList<>();
        actions.add(UiAction.of(Material.HOPPER, "Alles einlagern", () -> {
            storage.depositAll(player, village);
            refreshStorageView(player, village);
        }, "Nimmt nur eindeutig zugeordnete Ressourcen aus deinem Inventar."));
        for (ResourceType resource : ResourceType.values()) {
            int balance = storage.balances(village.id()).getOrDefault(resource, 0);
            actions.add(UiAction.of(resource.canonicalItem(), resource.displayName() + " · Bestand: " + balance,
                    () -> {
                        storage.deposit(player, village, resource, 64);
                        refreshStorageView(player, village);
                    }, () -> {
                        storage.withdraw(player, village, resource, 64);
                        refreshStorageView(player, village);
                    }, "Links: 64 einlagern · Rechts: 64 entnehmen",
                    "Eigene Items normal bearbeiten",
                    "Shift+Links: Stack · Shift+Rechts: 1 einlagern"));
        }
        actions.add(UiAction.of(Material.ARROW, "Zurück", () -> openVillage(player, village)));
        openActions(player, "Vorrat · " + village.name(), storage.summary(village.id()), actions, "STORAGE");
    }

    public void openBuild(Player player, Village village) {
        openBuildPage(player, village, 0);
    }

    public void openBuildPage(Player player, Village village, int page) {
        List<BlueprintType> types = java.util.Arrays.asList(BlueprintType.values());
        int pageSize = 8;
        int safePage = Math.max(0, Math.min(page, Math.max(0, (types.size() - 1) / pageSize)));
        int start = safePage * pageSize;
        int end = Math.min(types.size(), start + pageSize);
        java.util.List<UiAction> actions = new java.util.ArrayList<>();
        for (BlueprintType type : types.subList(start, end)) {
            actions.add(UiAction.of(Material.OAK_PLANKS, type.displayName() + " · Vorschau",
                    () -> {
                        player.closeInventory();
                        if (construction.previewInFront(player, village, type)) {
                            placement.enterExisting(player);
                        }
                    }, type.requiredLevel() > village.level()
                            ? "Gesperrt: benötigt Level " + type.requiredLevel() + " · aktuell " + village.level()
                            : "Kosten: " + type.cost()));
        }
        if (safePage > 0) {
            actions.add(UiAction.of(Material.ARROW, "Zurück · Seite " + safePage,
                    () -> openBuildPage(player, village, safePage - 1)));
        }
        if (end < types.size()) {
            actions.add(UiAction.of(Material.ARROW, "Weiter · Seite " + (safePage + 2),
                    () -> openBuildPage(player, village, safePage + 1)));
        }
        if (construction.hasPreview(player)) {
            actions.add(UiAction.of(Material.REPEATER, "Hotbar-Werkzeugmodus", () -> placement.enterExisting(player),
                    "Sicheres Inventar-Snapshot und Wiederherstellung."));
            actions.add(UiAction.of(Material.COMPASS, "Platzierungssteuerung", () -> openPlacementControls(player)));
        }
        actions.add(UiAction.of(Material.CRAFTING_TABLE, "Meine Baustellen", () -> openSites(player, village)));
        actions.add(UiAction.of(Material.BARRIER, "Verlassen", player::closeInventory));
        openActions(player, "Bauen · " + village.name() + " · " + (safePage + 1) + "/"
                + Math.max(1, (types.size() + pageSize - 1) / pageSize),
                "Acht Bauoptionen pro Seite. Wähle eine Vorschau oder öffne deine Baustellen.", actions);
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

    public void openPlacementControls(Player player) {
        java.util.List<UiAction> actions = new java.util.ArrayList<>();
        actions.add(UiAction.of(Material.OAK_SIGN, "Position neu setzen", () -> construction.movePreview(player)));
        actions.add(UiAction.of(Material.REDSTONE_TORCH, "Links rotieren", () -> construction.rotatePreview(player, -90)));
        actions.add(UiAction.of(Material.REPEATER, "Rechts rotieren", () -> construction.rotatePreview(player, 90)));
        actions.add(UiAction.of(Material.EMERALD, "Baustelle bestätigen", () -> construction.confirmPreview(player)));
        actions.add(UiAction.of(Material.BARRIER, "Vorschau abbrechen", () -> construction.cancelPreview(player)));
        openActions(player, "Platzierung", "Alle Aktionen sind auch als Hotbar-Werkzeuge über /bl build tools verfügbar.", actions);
    }

    public void openSite(Player player, Village village, BuildSite site) {
        construction.selectSite(player, site);
        java.util.List<UiAction> actions = new java.util.ArrayList<>();
        actions.add(UiAction.of(Material.CHEST, "Inventar beisteuern", () -> construction.contribute(player, village)));
        actions.add(UiAction.of(Material.ENDER_CHEST, "Dorfvorrat verwenden", () -> construction.fund(player, village)));
        actions.add(UiAction.of(Material.ANVIL, "FAWE-Bau starten", () -> construction.start(player, village),
                site.complete() ? "Alle Materialien vorhanden." : "Rest: " + remaining(site)));
        actions.add(UiAction.of(Material.BARRIER, "Baustelle abbrechen", () -> {
            player.closeInventory();
            construction.cancelSite(player, village, site.id());
        }, "Gibt das gesamte Kistenmaterial zurück und entfernt die Baustelle."));
        actions.add(UiAction.of(Material.ARROW, "Schließen", player::closeInventory));
        int required = site.required().values().stream().mapToInt(Integer::intValue).sum();
        int supplied = site.contributed().values().stream().mapToInt(Integer::intValue).sum();
        openActions(player, "Baustelle #" + site.id(), site.type().displayName()
                + "\nMaterial: " + supplied + "/" + required + " · Baufortschritt: " + site.status()
                + "\nRest: " + remaining(site), actions);
    }

    public void openWorkers(Player player, Village village) {
        java.util.List<UiAction> actions = new java.util.ArrayList<>();
        actions.add(UiAction.of(Material.BOOK, "Wie wird ein Bewohner Arbeiter?", () -> { },
                "Registrierten freien Bewohner im Detail selbst zuweisen.",
                "Voraussetzung: fertige Farm oder Mine. Farm = Bauernhaus + Feld.",
                "Nahrung ermöglicht Produktion; ein Bett versorgt den Wohnraum."));
        for (Building building : construction.buildings(village.id())) {
            if (building.type() == BlueprintType.FARM || building.type() == BlueprintType.MINE) {
                long assigned = workers.workers(village.id()).stream().filter(worker -> worker.buildingId() == building.id()).count();
                long free = Math.max(0, workers.capacity() - assigned);
                actions.add(UiAction.of(Material.VILLAGER_SPAWN_EGG,
                        "#" + building.id() + " " + building.type().displayName() + " · " + assigned + "/" + workers.capacity() + " Arbeiter",
                        () -> workers.assign(player, village, building.id()), free + "/" + workers.capacity() + " Arbeitsplätze frei."));
            }
        }
        residents.living(village.id()).forEach(resident -> actions.add(UiAction.of(Material.NAME_TAG,
                resident.displayLabel(), () -> openResident(player, village, resident),
                resident.hasBed() ? "Bett vorhanden · Details" : "Bett fehlt · Details")));
        if (!residents.dead(village.id()).isEmpty()) {
            actions.add(UiAction.of(Material.SKELETON_SKULL, "Verlorene · " + residents.dead(village.id()).size(),
                    () -> openDeadResidents(player, village), "Verlorene Bewohner anzeigen und entfernen."));
        }
        workers.workers(village.id()).forEach(worker -> {
            Resident resident = residents.living(village.id()).stream()
                    .filter(candidate -> candidate.villager().equals(worker.villager())).findFirst().orElse(null);
            if (resident != null) {
                actions.add(UiAction.of(Material.NAME_TAG, resident.displayLabel() + " · Arbeiter",
                        () -> openResident(player, village, resident),
                        "Arbeiter #" + worker.id() + " · " + worker.job() + " · Gebäude #" + worker.buildingId()));
            }
        });
        actions.add(UiAction.of(Material.BELL, "Versammlung läuten", () -> residents.assemble(player, village),
                "Bestätigt Anwesende und meldet neue sowie bettlose Bewohner."));
        actions.add(UiAction.of(Material.ARROW, "Zurück", () -> openVillage(player, village)));
        String content = "Register: " + residents.living(village.id()).size() + " lebende Bewohner · "
                + residents.confirmed(village.id()) + " bestätigt · " + residents.unconfirmed(village.id()) + " unbestätigt · "
                + residents.free(village.id(), workers.workers(village.id())).size() + " frei · "
                + residents.dead(village.id()).size() + " verloren.\n"
                + "Sneak + Rechtsklick auf die Dorfglocke startet ebenfalls die Versammlung.";
        openActions(player, "Bewohner · " + village.name(), content, actions);
    }

    public void openDeadResidents(Player player, Village village) {
        java.util.List<UiAction> actions = new java.util.ArrayList<>();
        residents.dead(village.id()).forEach(resident -> actions.add(UiAction.of(Material.SKELETON_SKULL,
                resident.displayLabel(), () -> { }, "Verloren · Namen und Nummer bleiben bis zum Aufräumen erhalten.")));
        if (!residents.dead(village.id()).isEmpty()) {
            actions.add(UiAction.of(Material.BARRIER, "Verlorene entfernen", () -> {
                player.closeInventory();
                residents.purgeDead(player, village);
            }, "Entfernt alle verlorenen Einträge aus diesem Dorfregister."));
        }
        actions.add(UiAction.of(Material.ARROW, "Zurück", () -> openWorkers(player, village)));
        openActions(player, "Verlorene · " + village.name(), "Nur nachweislich verstorbene Bewohner stehen hier.", actions);
    }

    public void openResident(Player player, Village village, Resident resident) {
        java.util.List<UiAction> actions = new java.util.ArrayList<>();
        actions.add(UiAction.of(Material.RED_BED, "Bett zuweisen", () -> residents.assignBed(player, village, resident),
                resident.hasBed() ? "Neues freies Bett suchen" : "Bett ist erforderlich"));
        actions.add(UiAction.of(Material.ANVIL, "Als Arbeiter zuweisen", () -> openResidentAssign(player, village, resident),
                "Farm oder Mine im Dorf wählen"));
        actions.add(UiAction.of(Material.ENDER_PEARL, "Zur Glocke teleportieren", () -> {
            player.closeInventory();
            residents.teleportToBell(player, village, resident);
        }, "Teleportiert diesen Bewohner auf einen sicheren Platz neben die Dorfglocke."));
        actions.add(UiAction.of(Material.STRUCTURE_VOID, "Aus Register entfernen", () -> {
            player.closeInventory();
            if (residents.canManage(player, village)) {
                workers.removeVillager(resident.villager());
                residents.removeResident(player, village, resident);
            }
        }, "Villager bleibt am Leben; Arbeits-, Bett- und Glockenzuordnung wird gelöscht."));
        Worker assigned = workers.workers(village.id()).stream().filter(worker -> worker.villager().equals(resident.villager()))
                .findFirst().orElse(null);
        if (assigned != null) {
            actions.add(UiAction.of(Material.BARRIER, "Arbeitszuweisung aufheben",
                    () -> workers.unassign(player, village, assigned.id())));
        }
        actions.add(UiAction.of(Material.ARROW, "Zurück", () -> openWorkers(player, village)));
        openActions(player, resident.displayLabel(), residentText(village, resident, assigned), actions);
    }

    public void openResidentAssign(Player player, Village village, Resident resident) {
        java.util.List<UiAction> actions = new java.util.ArrayList<>();
        for (Building building : construction.buildings(village.id())) {
            if (building.type() == BlueprintType.FARM || building.type() == BlueprintType.MINE) {
                actions.add(UiAction.of(Material.ANVIL, "Zuweisen: " + building.type().displayName(),
                        () -> workers.assignResident(player, village, building.id(), resident.villager()),
                        "Gebäude #" + building.id()));
            }
        }
        actions.add(UiAction.of(Material.ARROW, "Zurück", () -> openResident(player, village, resident)));
        openActions(player, "Arbeitsplatz wählen", "Nur vorhandene Farmen und Minen werden angeboten.", actions);
    }

    private String residentText(Village village, Resident resident, Worker assigned) {
        StringBuilder text = new StringBuilder();
        text.append("Status: ").append(resident.status()).append("\n");
        text.append("Bett: ").append(resident.hasBed() ? "zugewiesen" : "fehlt").append("\n");
        text.append("Meeting-Point: ").append(resident.meetingPoint() == null ? "nicht gesetzt" : "gesetzt").append("\n");
        if (assigned != null) {
            text.append("Beruf: ").append(assigned.job()).append(" · Gebäude #").append(assigned.buildingId()).append("\n");
            construction.building(assigned.buildingId()).ifPresent(building -> {
                org.bukkit.Location jobSite = construction.jobSiteLocation(building);
                if (jobSite != null) {
                    text.append("Job-Site: ").append(jobSite.getBlockX()).append("/")
                            .append(jobSite.getBlockY()).append("/").append(jobSite.getBlockZ()).append("\n");
                }
            });
        }
        java.util.List<String> missing = new java.util.ArrayList<>();
        if (!resident.hasBed()) missing.add("Bett");
        if (construction.buildings(village.id()).stream().noneMatch(building -> building.type().workplace())) {
            missing.add("Farm oder Mine");
        }
        if (storage.balances(village.id()).getOrDefault(ResourceType.NAHRUNG, 0) <= 0) {
            missing.add("Nahrung im Rathaus");
        }
        text.append(missing.isEmpty() ? "Bereit zur Zuweisung." : "Fehlt: " + String.join(", ", missing));
        return text.toString();
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
        openActions(player, title, content, actions, "GENERIC");
    }

    public void openActions(Player player, String title, String content, java.util.List<UiAction> actions, String menuId) {
        if (bedrockAvailable(player)) {
            bedrock.openActions(player, title, content, actions);
            return;
        }
        int size = Math.max(9, Math.min(54, ((actions.size() + 8) / 9) * 9));
        ActionMenuHolder holder = new ActionMenuHolder(actions, menuId);
        Inventory inventory = Bukkit.createInventory(holder, size, Component.text(title));
        holder.inventory(inventory);
        for (int index = 0; index < actions.size(); index++) {
            UiAction action = actions.get(index);
            ItemStack display = item(action.icon(), action.name(), action.lore().toArray(new String[0]));
            if (action.glint()) {
                ItemMeta meta = display.getItemMeta();
                meta.setEnchantmentGlintOverride(true);
                display.setItemMeta(meta);
            }
            inventory.setItem(index, display);
        }
        ItemStack filler = item(Material.LIGHT_GRAY_STAINED_GLASS_PANE, " ");
        for (int index = actions.size(); index < size; index++) {
            inventory.setItem(index, filler);
        }
        player.openInventory(inventory);
    }

    public void handleStorageItemClick(Player player, ItemStack clicked, boolean rightClick) {
        if (clicked == null) return;
        Village village = villages.at(player.getLocation()).orElse(null);
        if (village == null) {
            Msg.error(player, "Du befindest dich in keinem BlockLords-Dorf.");
            return;
        }
        ResourceType resource = ResourceType.from(clicked.getType()).orElse(null);
        if (resource == null) {
            Msg.error(player, "Dieses Item kann nicht im Dorfvorrat eingelagert werden.");
            return;
        }
        storage.deposit(player, village, resource, rightClick ? 1 : clicked.getAmount());
        refreshStorageView(player, village);
    }

    private void refreshStorageView(Player player, Village village) {
        tasks.syncLater(() -> {
            if (net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(player.getOpenInventory().title()).startsWith("Vorrat · ")) {
                openStorage(player, village);
            }
        }, 4L);
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

    private String[] levelLore(Village village) {
        if (village.level() >= 10) {
            return new String[]{"[x] Höchstes Level erreicht", village.xp() + " XP"};
        }
        int required = eu.praxios.blocklords.repository.VillageRepository.xpForNextLevel(village.level());
        int missing = Math.max(0, required - village.xp());
        return new String[]{
                "[x] Aktuelles Level: " + village.level() + " · " + village.xp() + " XP",
                "[ ] Level " + (village.level() + 1) + ": " + required + " XP (noch " + missing + ")",
                "Freischaltung: " + nextLevelUnlock(village.level() + 1)
        };
    }

    private String nextLevelUnlock(int level) {
        return Arrays.stream(BlueprintType.values())
                .filter(type -> type.requiredLevel() == level)
                .map(BlueprintType::displayName)
                .reduce((left, right) -> left + ", " + right)
                .orElse("Weitere Entwicklung folgt in späteren Phasen.");
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
