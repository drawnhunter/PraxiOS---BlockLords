package eu.praxios.blocklords.config;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Vollständig validierte Config; ungültige Werte verhindern einen halb aktiven Plugin-Start. */
public final class BlockLordsConfig {
    private final int radius;
    private final int maxPerPlayer;
    private final int maxPerWorld;
    private final boolean multiClaimEnabled;
    private final int nextVillageCost;
    private final int decayBlockDays;
    private final boolean requireQuest;
    private final List<ItemStack> starterItems;
    private final boolean decayEnabled;
    private final int warningDays;
    private final int restingDays;
    private final long decayCheckTicks;
    private final String databaseFile;
    private final int backupRetention;
    private final boolean bedrockForms;
    private final long scanCacheMillis;
    private final int chunksPerTick;
    private final int storageBaseCapacity;
    private final int storagePerWarehouse;
    private final int productionCycleSeconds;
    private final long productionCheckTicks;
    private final long activeWindowMillis;
    private final long offlineMaxMillis;
    private final double offlineRate;
    private final long researchDurationMillis;
    private final long residentScanTicks;
    private final int workersPerBuilding;
    private final String logLevel;
    private final long actionTimeoutTicks;
    private final boolean debug;

    public BlockLordsConfig(FileConfiguration config) {
        radius = positive(config, "claims.radius-blocks", 1, 256);
        maxPerPlayer = positive(config, "claims.max-per-player", 1, 100);
        maxPerWorld = positive(config, "claims.max-per-world", 1, 1000);
        multiClaimEnabled = config.getBoolean("claims.multi-claim-enabled", false);
        nextVillageCost = positive(config, "claims.next-village-cost-emeralds", 0, 4096);
        decayBlockDays = positive(config, "claims.decay-block-days", 1, 3650);
        requireQuest = config.getBoolean("founding.require-quest", true);
        starterItems = readStarterItems(config);
        decayEnabled = config.getBoolean("decay.enabled", true);
        warningDays = positive(config, "decay.warning-days", 0, 3650);
        restingDays = positive(config, "decay.resting-days", 1, 3650);
        if (restingDays <= warningDays) {
            throw new IllegalArgumentException("decay.resting-days muss größer als decay.warning-days sein.");
        }
        if (decayBlockDays <= restingDays) {
            throw new IllegalArgumentException("claims.decay-block-days muss größer als decay.resting-days sein.");
        }
        int checkMinutes = positive(config, "decay.check-interval-minutes", 1, 1440);
        decayCheckTicks = checkMinutes * 60L * 20L;
        databaseFile = config.getString("database.file", "blocklords.db");
        if (databaseFile.isBlank() || databaseFile.contains("..")) {
            throw new IllegalArgumentException("database.file ist ungültig.");
        }
        backupRetention = positive(config, "database.backup-retention", 1, 100);
        bedrockForms = config.getBoolean("ui.use-bedrock-forms", true);
        scanCacheMillis = positive(config, "ui.scan-cache-seconds", 10, 3600) * 1000L;
        chunksPerTick = positive(config, "ui.chunks-per-tick", 1, 16);
        storageBaseCapacity = positive(config, "economy.storage-base-capacity", 1, 100_000_000);
        storagePerWarehouse = positive(config, "economy.storage-per-warehouse", 1, 100_000_000);
        productionCycleSeconds = positive(config, "economy.production-cycle-seconds", 30, 86_400);
        productionCheckTicks = positive(config, "economy.production-check-ticks", 20, 72_000);
        activeWindowMillis = positive(config, "economy.active-window-minutes", 1, 1440) * 60_000L;
        int offlineHours = positive(config, "economy.offline-max-hours", 0, 168);
        offlineMaxMillis = offlineHours * 3_600_000L;
        int offlinePercent = positive(config, "economy.offline-rate-percent", 0, 100);
        offlineRate = offlinePercent / 100.0;
        researchDurationMillis = positive(config, "economy.research-duration-seconds", 1, 86_400) * 1000L;
        residentScanTicks = positive(config, "residents.scan-interval-ticks", 20, 72_000);
        workersPerBuilding = config.isInt("workers.per-building") ? positive(config, "workers.per-building", 1, 1000) : 2;
        logLevel = config.getString("logging.level", "INFO").toUpperCase(java.util.Locale.ROOT);
        if (!logLevel.equals("INFO") && !logLevel.equals("DEBUG")) {
            throw new IllegalArgumentException("logging.level muss INFO oder DEBUG sein.");
        }
        actionTimeoutTicks = positive(config, "logging.action-timeout-seconds", 1, 600) * 20L;
        debug = config.getBoolean("debug", false);
    }

    private int positive(FileConfiguration config, String path, int min, int max) {
        if (!config.isInt(path)) {
            throw new IllegalArgumentException("Fehlende oder ungültige Ganzzahl: " + path);
        }
        int value = config.getInt(path);
        if (value < min || value > max) {
            throw new IllegalArgumentException(path + " muss zwischen " + min + " und " + max + " liegen.");
        }
        return value;
    }

    private List<ItemStack> readStarterItems(FileConfiguration config) {
        List<ItemStack> items = new ArrayList<>();
        for (String line : config.getStringList("founding.starter-items")) {
            String[] parts = line.split(":", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Ungültiger Starter-Item-Eintrag: " + line);
            }
            Material material = Material.matchMaterial(parts[0]);
            if (material == null || !material.isItem()) {
                throw new IllegalArgumentException("Unbekanntes Starter-Item: " + parts[0]);
            }
            try {
                int amount = Integer.parseInt(parts[1]);
                if (amount < 1 || amount > material.getMaxStackSize()) {
                    throw new IllegalArgumentException("Ungültige Starter-Item-Anzahl: " + line);
                }
                items.add(new ItemStack(material, amount));
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Ungültige Starter-Item-Anzahl: " + line, exception);
            }
        }
        return List.copyOf(items);
    }

    public int radius() { return radius; }
    public int maxPerPlayer() { return maxPerPlayer; }
    public int maxPerWorld() { return maxPerWorld; }
    public boolean multiClaimEnabled() { return multiClaimEnabled; }
    public int nextVillageCost() { return nextVillageCost; }
    public int decayBlockDays() { return decayBlockDays; }
    public boolean requireQuest() { return requireQuest; }
    public List<ItemStack> starterItems() { return starterItems.stream().map(ItemStack::clone).toList(); }
    public boolean decayEnabled() { return decayEnabled; }
    public int warningDays() { return warningDays; }
    public int restingDays() { return restingDays; }
    public long decayCheckTicks() { return decayCheckTicks; }
    public String databaseFile() { return databaseFile; }
    public int backupRetention() { return backupRetention; }
    public boolean bedrockForms() { return bedrockForms; }
    public long scanCacheMillis() { return scanCacheMillis; }
    public int chunksPerTick() { return chunksPerTick; }
    public int storageBaseCapacity() { return storageBaseCapacity; }
    public int storagePerWarehouse() { return storagePerWarehouse; }
    public int productionCycleSeconds() { return productionCycleSeconds; }
    public long productionCheckTicks() { return productionCheckTicks; }
    public long activeWindowMillis() { return activeWindowMillis; }
    public long offlineMaxMillis() { return offlineMaxMillis; }
    public double offlineRate() { return offlineRate; }
    public long researchDurationMillis() { return researchDurationMillis; }
    public long residentScanTicks() { return residentScanTicks; }
    public int workersPerBuilding() { return workersPerBuilding; }
    public String logLevel() { return logLevel; }
    public long actionTimeoutTicks() { return actionTimeoutTicks; }
    public boolean debug() { return debug; }
}
