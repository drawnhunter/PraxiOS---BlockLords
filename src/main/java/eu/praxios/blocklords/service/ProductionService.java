package eu.praxios.blocklords.service;

import eu.praxios.blocklords.config.BlockLordsConfig;
import eu.praxios.blocklords.model.*;
import eu.praxios.blocklords.repository.ProductionReportRepository;
import eu.praxios.blocklords.repository.StorageRepository;
import eu.praxios.blocklords.repository.WorkerRepository;
import eu.praxios.blocklords.util.Msg;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public final class ProductionService {
    private final JavaPlugin plugin;
    private final Supplier<BlockLordsConfig> config;
    private final WorkerRepository workers;
    private final StorageRepository storage;
    private final VillageService villages;
    private final StorageService storageService;
    private final ProductionReportRepository reports;
    private final TaskRunner tasks;
    private final LongSupplier clock;
    private final Random random = new Random();
    private BukkitTask task;

    public ProductionService(JavaPlugin plugin, Supplier<BlockLordsConfig> config, WorkerRepository workers,
                             StorageRepository storage, VillageService villages, StorageService storageService,
                             TaskRunner tasks, ProductionReportRepository reports) {
        this(plugin, config, workers, storage, villages, storageService, tasks, reports, System::currentTimeMillis);
    }

    public ProductionService(JavaPlugin plugin, Supplier<BlockLordsConfig> config, WorkerRepository workers,
                             StorageRepository storage, VillageService villages, StorageService storageService,
                             TaskRunner tasks, ProductionReportRepository reports, LongSupplier clock) {
        this.plugin = plugin;
        this.config = config;
        this.workers = workers;
        this.storage = storage;
        this.villages = villages;
        this.storageService = storageService;
        this.tasks = tasks;
        this.reports = reports;
        this.clock = clock;
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
        long now = clock.getAsLong();
        tasks.database(() -> {
            List<Worker> assigned = workers.find(village.id());
            StorageRepository.ProductionState state = storage.productionState(village.id());
            if (state.lastCycleAt() <= 0) {
                storage.initializeProduction(village.id(), now);
                return null;
            }
            int farmers = (int) assigned.stream().filter(worker -> worker.job() == Worker.Job.FARMER).count();
            int miners = (int) assigned.stream().filter(worker -> worker.job() == Worker.Job.MINER).count();
            if (village.status() != VillageStatus.ACTIVE) {
                pause(village, now, false, farmers, miners, "Dorf ruht");
                return null;
            }
            boolean active = now - village.lastActivity() <= config.get().activeWindowMillis();
            long creditUntil = active ? now : Math.min(now, village.lastActivity() + config.get().offlineMaxMillis());
            if (creditUntil <= state.lastCycleAt()) {
                if (!active) pause(village, now, false, farmers, miners, "Offline-Limit erreicht");
                return null;
            }
            int cycleSeconds = config.get().productionCycleSeconds();
            long elapsedSeconds = Math.max(0, (creditUntil - state.lastCycleAt()) / 1000L);
            int cycles = (int) Math.min(elapsedSeconds / cycleSeconds,
                    config.get().offlineMaxMillis() / (cycleSeconds * 1000L));
            if (cycles <= 0) return null;
            double rate = active ? 1.0 : config.get().offlineRate();
            Map<ResourceType, Integer> balances = storage.balances(village.id());
            ProductionCalculator.Plan plan = ProductionCalculator.calculate(cycles, rate, farmers, miners,
                    balances.getOrDefault(ResourceType.NAHRUNG, 0), random);
            StorageRepository.ProductionResult result = storage.applyProduction(village.id(), plan.produced(),
                    plan.foodConsumed(), storageService.capacity(village.id()), now, describe(plan, active));
            List<String> pauses = new ArrayList<>();
            if (farmers == 0) pauses.add("Bauer: kein Arbeiter");
            if (miners == 0) pauses.add("Minenarbeiter: kein Arbeiter");
            if (farmers + miners > 0 && plan.effectiveCycles() <= 0) pauses.add("Nahrung fehlt");
            List<ProductionReport.Entry> entries = new ArrayList<>();
            plan.produced().forEach((resource, planned) -> {
                int accepted = result.accepted().getOrDefault(resource, 0);
                ProductionReport.Entry entry = new ProductionReport.Entry(resource, planned, accepted);
                entries.add(entry);
                if (accepted < planned) pauses.add(capacityPause(entry));
            });
            reports.save(new ProductionReport(village.id(), now, now, active, plan.effectiveCycles(), farmers, miners,
                    result.foodConsumed(), pauses, entries));
            return result;
        }, result -> {
            if (result != null) storageService.refresh(village.id());
        }, error -> plugin.getLogger().warning("Produktion für Dorf " + village.id() + " fehlgeschlagen: " + error.getMessage()));
    }

    public void report(long villageId, java.util.function.Consumer<ReportView> callback) {
        tasks.database(() -> {
            var current = reports.find(villageId);
            String legacy = current.isPresent() ? null : storage.productionState(villageId).lastReport();
            return new ReportView(current.orElse(null), legacy);
        }, callback, error -> plugin.getLogger().warning("Produktionsbilanz konnte nicht gelesen werden: " + error.getMessage()));
    }

    public void sendJoinReport(Player player, Village village) {
        report(village.id(), view -> {
            if (!player.isOnline()) return;
            String details;
            if (view.current() != null) details = describe(view.current());
            else if (view.legacyText() != null) details = view.legacyText();
            else return;
            String message = "Dorfbericht '" + village.name() + "': " + details;
            Msg.info(player, message);
            plugin.getLogger().info("DORFBERICHT | " + player.getName() + " | " + message);
        });
    }

    public List<String> pauseLines(ProductionReport report) {
        List<String> lines = new ArrayList<>();
        report.pauses().stream().filter(reason -> !isCapacityPause(reason)).forEach(lines::add);
        report.resources().forEach(entry -> {
            if (entry.accepted() < entry.planned()) lines.add(capacityPause(entry));
        });
        return lines;
    }

    private boolean isCapacityPause(String reason) {
        for (ResourceType resource : ResourceType.values()) {
            if (reason.startsWith(resource.displayName() + ":") && reason.contains("Lager voll")) return true;
        }
        return false;
    }

    private String capacityPause(ProductionReport.Entry entry) {
        return entry.resource().displayName() + ": " + (entry.planned() - entry.accepted()) + " von " + entry.planned()
                + " nicht eingelagert – Lager voll";
    }

    private void pause(Village village, long now, boolean active, int farmers, int miners, String reason) throws Exception {
        storage.pauseProduction(village.id(), now, reason);
        reports.save(new ProductionReport(village.id(), now, now, active, 0, farmers, miners, 0,
                List.of(reason), List.of()));
    }

    public String cycleInfo() {
        return "1 Arbeitszyklus = " + config.get().productionCycleSeconds() + " Sekunden · Offline-Ertrag "
                + Math.round(config.get().offlineRate() * 100) + " %";
    }

    private String signed(int amount) {
        return amount > 0 ? "+" + amount : Integer.toString(amount);
    }

    private String describe(ProductionReport report) {
        StringBuilder text = new StringBuilder(String.format("%.1f Zyklen (%s): ",
                report.effectiveCycles(), report.active() ? "aktiv" : "offline"));
        report.resources().forEach(entry -> {
            int consumed = entry.resource() == ResourceType.NAHRUNG ? report.foodConsumed() : 0;
            int net = entry.accepted() - consumed;
            text.append(entry.resource().displayName()).append(' ').append(signed(net)).append(" netto (")
                    .append(signed(entry.accepted())).append("/-").append(consumed).append("), ");
        });
        List<String> pauses = pauseLines(report);
        if (!pauses.isEmpty()) text.append(String.join(", ", pauses)).append(", ");
        if (report.resources().isEmpty() && pauses.isEmpty()) text.append("keine Produktion, ");
        return text.substring(0, text.length() - 2);
    }

    private String describe(ProductionCalculator.Plan plan, boolean active) {
        StringBuilder text = new StringBuilder(String.format("%.1f Zyklen (%s): ", plan.effectiveCycles(), active ? "aktiv" : "offline"));
        plan.produced().forEach((resource, amount) -> text.append('+').append(amount).append(' ')
                .append(resource.displayName()).append(", "));
        if (plan.foodConsumed() > 0) text.append('-').append(plan.foodConsumed()).append(" Nahrung, ");
        if (plan.produced().isEmpty()) text.append("keine Produktion (Arbeiter/Nahrung/Lager prüfen), ");
        return text.substring(0, text.length() - 2);
    }

    public record ReportView(ProductionReport current, String legacyText) {
    }
}
