package eu.praxios.blocklords.service;

import eu.praxios.blocklords.config.BlockLordsConfig;
import eu.praxios.blocklords.model.*;
import eu.praxios.blocklords.repository.StorageRepository;
import eu.praxios.blocklords.repository.WorkerRepository;
import eu.praxios.blocklords.util.Msg;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

public final class ProductionService {
    private final JavaPlugin plugin;
    private final Supplier<BlockLordsConfig> config;
    private final WorkerRepository workers;
    private final StorageRepository storage;
    private final VillageService villages;
    private final StorageService storageService;
    private final TaskRunner tasks;
    private final Random random = new Random();
    private BukkitTask task;

    public ProductionService(JavaPlugin plugin, Supplier<BlockLordsConfig> config, WorkerRepository workers,
                             StorageRepository storage, VillageService villages, StorageService storageService,
                             TaskRunner tasks) {
        this.plugin = plugin;
        this.config = config;
        this.workers = workers;
        this.storage = storage;
        this.villages = villages;
        this.storageService = storageService;
        this.tasks = tasks;
    }

    public void start() {
        stop();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::runAll,
                config.get().productionCheckTicks(), config.get().productionCheckTicks());
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void runAll() {
        villages.all().forEach(this::runVillage);
    }

    public void runVillage(Village village) {
        long now = System.currentTimeMillis();
        // Die komplette Berechnung und Transaktion läuft im DB-Executor; Weltzugriffe finden hier nicht statt.
        tasks.database(() -> {
            List<Worker> assigned = workers.find(village.id());
            StorageRepository.ProductionState state = storage.productionState(village.id());
            if (state.lastCycleAt() <= 0) {
                storage.initializeProduction(village.id(), now);
                return null;
            }
            int cycleSeconds = config.get().productionCycleSeconds();
            long elapsedSeconds = Math.max(0, (now - state.lastCycleAt()) / 1000L);
            int cycles = (int) Math.min(elapsedSeconds / cycleSeconds,
                    config.get().offlineMaxMillis() / (cycleSeconds * 1000L));
            if (cycles <= 0) return null;
            int farmers = (int) assigned.stream().filter(worker -> worker.job() == Worker.Job.FARMER).count();
            int miners = (int) assigned.stream().filter(worker -> worker.job() == Worker.Job.MINER).count();
            boolean active = now - village.lastActivity() <= config.get().activeWindowMillis();
            double rate = active ? 1.0 : config.get().offlineRate();
            Map<ResourceType, Integer> balances = storage.balances(village.id());
            ProductionCalculator.Plan plan = ProductionCalculator.calculate(cycles, rate, farmers, miners,
                    balances.getOrDefault(ResourceType.NAHRUNG, 0), random);
            String report = describe(plan, active);
            return storage.applyProduction(village.id(), plan.produced(), plan.foodConsumed(),
                    storageService.capacity(village.id()), now, report);
        }, result -> {
            if (result != null) storageService.refresh(village.id());
        }, error -> plugin.getLogger().warning("Produktion für Dorf " + village.id() + " fehlgeschlagen: " + error.getMessage()));
    }

    public void sendJoinReport(Player player, Village village) {
        tasks.database(() -> storage.productionState(village.id()), state -> {
            if (state.lastReport() != null && state.lastReportAt() > 0) {
                Msg.info(player, "Dorfbericht '" + village.name() + "': " + state.lastReport());
            }
        }, error -> plugin.getLogger().warning("Dorfbericht konnte nicht gelesen werden: " + error.getMessage()));
    }

    private String describe(ProductionCalculator.Plan plan, boolean active) {
        StringBuilder text = new StringBuilder(String.format("%.1f Zyklen (%s): ", plan.effectiveCycles(), active ? "aktiv" : "offline"));
        plan.produced().forEach((resource, amount) -> text.append('+').append(amount).append(' ')
                .append(resource.displayName()).append(", "));
        if (plan.foodConsumed() > 0) text.append('-').append(plan.foodConsumed()).append(" Nahrung, ");
        if (plan.produced().isEmpty()) text.append("keine Produktion (Arbeiter/Nahrung/Lager prüfen), ");
        return text.substring(0, text.length() - 2);
    }
}
