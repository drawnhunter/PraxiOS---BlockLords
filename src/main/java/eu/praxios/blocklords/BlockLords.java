package eu.praxios.blocklords;

import eu.praxios.blocklords.command.BlockLordsCommand;
import eu.praxios.blocklords.command.EconomyCommand;
import eu.praxios.blocklords.config.BlockLordsConfig;
import eu.praxios.blocklords.database.Database;
import eu.praxios.blocklords.database.MigrationRunner;
import eu.praxios.blocklords.listener.*;
import eu.praxios.blocklords.model.BlueprintType;
import eu.praxios.blocklords.model.BuildLocation;
import eu.praxios.blocklords.repository.*;
import eu.praxios.blocklords.service.*;
import eu.praxios.blocklords.ui.MenuListener;
import eu.praxios.blocklords.ui.UiService;
import eu.praxios.blocklords.util.Msg;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;

public final class BlockLords extends JavaPlugin {
    private static final DateTimeFormatter BACKUP_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private BlockLordsConfig settings;
    private Database database;
    private TaskRunner tasks;
    private DecayService decay;
    private VillageScanner scanner;
    private ProductionService production;
    private ResearchService research;
    private FaweBuilder faweBuilder;
    private LogService logs;
    private ResidentService residents;
    private PlacementModeService placement;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        try {
            settings = new BlockLordsConfig(getConfig());
        } catch (RuntimeException invalid) {
            getLogger().severe("BlockLords-Config ungültig: " + invalid.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        try {
            logs = new LogService(getLogger(), this::settings);
            tasks = new TaskRunner(this);
            database = new Database(getDataFolder().toPath().resolve(settings.databaseFile()));
            if (Files.size(database.path()) > 0) {
                database.backup(backupPath("pre-migration"));
            }
            new MigrationRunner(database, getLogger()).migrate();

            VillageRepository villageRepository = new VillageRepository(database);
            LegacyRepository legacyRepository = new LegacyRepository(database);
            QuestRepository questRepository = new QuestRepository(database);
            OwnershipRepository ownershipRepository = new OwnershipRepository(database);
            AuditRepository auditRepository = new AuditRepository(database);
            MissionRepository missionRepository = new MissionRepository(database, villageRepository);
            ConstructionRepository constructionRepository = new ConstructionRepository(database, missionRepository);
            StorageRepository storageRepository = new StorageRepository(database, missionRepository);
            WorkerRepository workerRepository = new WorkerRepository(database);
            ResearchRepository researchRepository = new ResearchRepository(database);
            ResidentRepository residentRepository = new ResidentRepository(database);
            PlacementRepository placementRepository = new PlacementRepository(database);

            AuditService audit = new AuditService(auditRepository, tasks, logs);
            VillageService villages = new VillageService(villageRepository, legacyRepository, tasks);
            villages.loadInitial();
            MissionService missions = new MissionService(this::settings, missionRepository, villages, tasks, audit);
            GuideItemService guide = new GuideItemService(this);
            FoundingService founding = new FoundingService(this::settings, villageRepository, questRepository, villages, tasks, audit, guide);
            ProtectionService protection = new ProtectionService(villages, audit);
            AltSelectionService alt = new AltSelectionService(legacyRepository, villages, tasks, audit);
            decay = new DecayService(this, this::settings, villages);
            scanner = new VillageScanner(this, this::settings, tasks);
            StorageService storage = new StorageService(this::settings, storageRepository, constructionRepository, tasks, audit, villages,
                    missions::announceCompletions);
            faweBuilder = new FaweBuilder(this, tasks, logs, this::settings);
            ConstructionService construction = new ConstructionService(constructionRepository, villageRepository,
                    villages, storage, storageRepository, faweBuilder, tasks, audit, missions::announceCompletions);
            storage.loadInitial(villages.all());
            construction.loadInitial(villages.all());
            placement = new PlacementModeService(this, placementRepository, construction, tasks, logs, audit);
            residents = new ResidentService(this, this::settings, residentRepository, villages, tasks, audit);
            residents.loadInitial(villages.all());
            WorkerService workers = new WorkerService(workerRepository, storageRepository, construction, villages, residents, tasks, audit, this::settings);
            workers.loadInitial(villages.all());
            research = new ResearchService(this, this::settings, researchRepository, villageRepository,
                    storage, villages, construction, tasks, audit);
            research.loadInitial(villages.all());
            production = new ProductionService(this, this::settings, workerRepository, storageRepository,
                    villages, storage, tasks);
            UiService ui = new UiService(this::settings, villages, founding, scanner, tasks,
                    construction, storage, workers, research, residents, placement, missions);
            founding.setQuestUpdateHandler(ui::handleQuestUpdate);

            getServer().getPluginManager().registerEvents(new BellListener(ui, founding, villages, residents), this);
            getServer().getPluginManager().registerEvents(new ProtectionListener(protection), this);
            getServer().getPluginManager().registerEvents(new OwnershipListener(ownershipRepository, tasks), this);
            getServer().getPluginManager().registerEvents(new MenuListener(ui), this);
            getServer().getPluginManager().registerEvents(new GuideItemListener(guide, villages, ui), this);
            getServer().getPluginManager().registerEvents(new BuilderChestListener(construction, villages, ui), this);
            getServer().getPluginManager().registerEvents(new BuilderChestProtectionListener(construction), this);
            getServer().getPluginManager().registerEvents(new PlacementModeListener(placement), this);
            getServer().getPluginManager().registerEvents(new ResidentLifecycleListener(residents, workers), this);
            getServer().getPluginManager().registerEvents(new PlayerLifecycleListener(founding, decay, production, villages, this, missions), this);

            EconomyCommand economy = new EconomyCommand(villages, construction, storage, workers, research, ui, placement, missions);
            BlockLordsCommand executor = new BlockLordsCommand(this, villages, founding, ui, protection, alt, audit,
                    tasks, villageRepository, decay, economy, guide);
            PluginCommand command = getCommand("blocklords");
            if (command == null) {
                throw new IllegalStateException("Befehl 'blocklords' fehlt in plugin.yml");
            }
            command.setExecutor(executor);
            command.setTabCompleter(executor);
            decay.start();
            production.start();
            research.start();
            residents.start();

            getLogger().info("BlockLords " + getPluginMeta().getVersion()
                    + " aktiviert: Phase 2, " + villages.all().size() + " Dörfer, Java "
                    + System.getProperty("java.version") + ".");
        } catch (Exception exception) {
            getLogger().severe("BlockLords konnte nicht sicher gestartet werden: " + exception.getMessage());
            exception.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (decay != null) decay.stop();
        if (production != null) production.stop();
        if (research != null) research.stop();
        if (residents != null) residents.stop();
        if (scanner != null) scanner.close();
        if (placement != null) placement.closeAll();
        if (tasks != null) tasks.close();
        if (database != null) {
            try {
                database.close();
            } catch (Exception exception) {
                getLogger().warning("Datenbank konnte nicht sauber geschlossen werden: " + exception.getMessage());
            }
        }
        getLogger().info("BlockLords deaktiviert.");
    }

    public boolean reloadBlockLordsConfig() {
        try {
            BlockLordsConfig previous = settings;
            reloadConfig();
            BlockLordsConfig candidate = new BlockLordsConfig(getConfig());
            if (!previous.databaseFile().equals(candidate.databaseFile())) {
                getLogger().warning("database.file kann nur durch einen Neustart geändert werden.");
                return false;
            }
            settings = candidate;
            decay.start();
            production.start();
            residents.start();
            return true;
        } catch (RuntimeException invalid) {
            getLogger().severe("Reload verworfen: " + invalid.getMessage());
            return false;
        }
    }

    public void backup(CommandSender sender) {
        if (database == null || tasks == null) {
            Msg.error(sender, "Datenbank ist nicht verfügbar.");
            return;
        }
        Path target = backupPath("manual");
        tasks.database(() -> {
            database.backup(target);
            pruneBackups();
            return target;
        }, path -> Msg.success(sender, "SQLite-Backup erstellt: " + path.getFileName()),
                error -> Msg.error(sender, "Backup fehlgeschlagen: " + error.getMessage()));
    }

    public void itemTest(CommandSender sender) {
        org.bukkit.inventory.ItemStack[] original = {
                new org.bukkit.inventory.ItemStack(org.bukkit.Material.OAK_LOG, 32),
                new org.bukkit.inventory.ItemStack(org.bukkit.Material.EMERALD, 4)
        };
        byte[] serialized = org.bukkit.inventory.ItemStack.serializeItemsAsBytes(original);
        org.bukkit.inventory.ItemStack[] restored = org.bukkit.inventory.ItemStack.deserializeItemsFromBytes(serialized);
        boolean valid = restored.length == 2 && restored[0].getType() == org.bukkit.Material.OAK_LOG
                && restored[0].getAmount() == 32 && restored[1].getType() == org.bukkit.Material.EMERALD
                && restored[1].getAmount() == 4;
        if (valid) Msg.success(sender, "ItemStack-NBT-Roundtrip erfolgreich (" + serialized.length + " Bytes).");
        else Msg.error(sender, "ItemStack-NBT-Roundtrip fehlgeschlagen.");
    }

    public void selfTest(CommandSender sender, BlueprintType type) {
        if (!(sender instanceof org.bukkit.entity.Player player)) {
            Msg.error(sender, "Ohne Koordinaten wird ein Spieler benötigt.");
            return;
        }
        org.bukkit.block.Block target = player.getTargetBlockExact(6);
        if (target != null) {
            selfTest(sender, type, target.getX(), target.getY() + 1, target.getZ());
        } else {
            selfTest(sender, type, player.getLocation().getBlockX(), player.getLocation().getBlockY() + 1,
                    player.getLocation().getBlockZ());
        }
    }

    public void selfTest(CommandSender sender, BlueprintType type, int x, int y, int z) {
        org.bukkit.World world = sender instanceof org.bukkit.entity.Player player
                ? player.getWorld() : getServer().getWorlds().getFirst();
        logs.info("Selftest akzeptiert: " + type + " @ " + world.getName() + " " + x + "/" + y + "/" + z);
        Msg.info(sender, "FAWE-Diagnose gestartet. Ergebnis oder Timeout folgt mit Aktions-ID im Log.");
        selfTest(sender, type, x, y, z, 0);
    }

    public void selfTest(CommandSender sender, BlueprintType type, int x, int y, int z, int rotation) {
        org.bukkit.World world = sender instanceof org.bukkit.entity.Player player
                ? player.getWorld() : getServer().getWorlds().getFirst();
        logs.info("Selftest akzeptiert: " + type + " @ " + world.getName() + " " + x + "/" + y + "/" + z + " Rotation " + rotation);
        Msg.info(sender, "FAWE-Diagnose gestartet. Ergebnis oder Timeout folgt mit Aktions-ID im Log.");
        faweBuilder.paste(type, new BuildLocation(world.getUID(), x, y, z, rotation),
                count -> Msg.success(sender, "FAWE-Diagnose erfolgreich: " + count + " Blöcke für " + type + "."),
                error -> Msg.error(sender, "FAWE-Diagnose fehlgeschlagen: " + error.getMessage()));
    }

    private BlockLordsConfig settings() {
        return settings;
    }

    private Path backupPath(String prefix) {
        return getDataFolder().toPath().resolve("backups")
                .resolve(prefix + "-" + LocalDateTime.now().format(BACKUP_TIME) + ".db");
    }

    private void pruneBackups() throws Exception {
        Path directory = getDataFolder().toPath().resolve("backups");
        if (!Files.isDirectory(directory)) return;
        try (var stream = Files.list(directory)) {
            var backups = stream.filter(path -> path.getFileName().toString().endsWith(".db"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString(), Comparator.reverseOrder()))
                    .toList();
            for (int index = settings.backupRetention(); index < backups.size(); index++) {
                Files.deleteIfExists(backups.get(index).toAbsolutePath());
            }
        }
    }
}
