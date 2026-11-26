package eu.praxios.blocklords.listener;

import eu.praxios.blocklords.service.DecayService;
import eu.praxios.blocklords.service.FoundingService;
import eu.praxios.blocklords.service.MissionService;
import eu.praxios.blocklords.service.ProductionService;
import eu.praxios.blocklords.service.VillageService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlayerLifecycleListener implements Listener {
    private final FoundingService founding;
    private final DecayService decay;
    private final ProductionService production;
    private final VillageService villages;
    private final JavaPlugin plugin;
    private final MissionService missions;

    public PlayerLifecycleListener(FoundingService founding, DecayService decay, ProductionService production,
                                   VillageService villages, JavaPlugin plugin, MissionService missions) {
        this.founding = founding;
        this.decay = decay;
        this.production = production;
        this.villages = villages;
        this.plugin = plugin;
        this.missions = missions;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        founding.loadPlayer(event.getPlayer());
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            decay.sendJoinWarnings(event.getPlayer());
            villages.owned(event.getPlayer().getUniqueId())
                    .forEach(village -> production.sendJoinReport(event.getPlayer(), village));
            missions.sendPinnedHint(event.getPlayer());
        }, 20L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        founding.unloadPlayer(event.getPlayer());
    }
}
