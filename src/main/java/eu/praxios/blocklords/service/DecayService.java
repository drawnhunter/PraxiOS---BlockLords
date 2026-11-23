package eu.praxios.blocklords.service;

import eu.praxios.blocklords.config.BlockLordsConfig;
import eu.praxios.blocklords.model.Village;
import eu.praxios.blocklords.model.VillageStatus;
import eu.praxios.blocklords.util.Msg;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.function.Supplier;

/** Inaktivität verändert nur Status/Produktion; der Schutz bleibt unangetastet. */
public final class DecayService {
    private final JavaPlugin plugin;
    private final Supplier<BlockLordsConfig> config;
    private final VillageService villages;
    private BukkitTask task;

    public DecayService(JavaPlugin plugin, Supplier<BlockLordsConfig> config, VillageService villages) {
        this.plugin = plugin;
        this.config = config;
        this.villages = villages;
    }

    public void start() {
        stop();
        long period = config.get().decayCheckTicks();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::check, period, period);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void check() {
        if (!config.get().decayEnabled()) {
            villages.all().forEach(village -> {
                if (village.status() != VillageStatus.ACTIVE) villages.updateStatus(village, VillageStatus.ACTIVE);
            });
            return;
        }
        long now = System.currentTimeMillis();
        for (Village village : villages.all()) {
            long days = (now - village.lastActivity()) / 86_400_000L;
            VillageStatus desired = days >= 30 ? VillageStatus.REVIEW
                    : days >= config.get().restingDays() ? VillageStatus.RESTING : VillageStatus.ACTIVE;
            if (desired != village.status()) {
                villages.updateStatus(village, desired);
            }
        }
    }

    public void sendJoinWarnings(Player player) {
        if (!config.get().decayEnabled()) return;
        long now = System.currentTimeMillis();
        for (Village village : villages.owned(player.getUniqueId())) {
            long days = (now - village.lastActivity()) / 86_400_000L;
            if (days >= 30) {
                Msg.error(player, "Dorf '" + village.name() + "' ist zur Admin-Prüfung vorgemerkt. Reaktiviere es durch einen Dorfbeitrag.");
            } else if (days >= config.get().warningDays()) {
                Msg.info(player, "Dorf '" + village.name() + "' ist seit " + days + " Tagen ohne Dorfaktivität. Sein Schutz bleibt aktiv.");
            }
        }
    }
}
