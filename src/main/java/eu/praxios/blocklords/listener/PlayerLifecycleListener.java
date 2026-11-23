package eu.praxios.blocklords.listener;

import eu.praxios.blocklords.service.DecayService;
import eu.praxios.blocklords.service.FoundingService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlayerLifecycleListener implements Listener {
    private final FoundingService founding;
    private final DecayService decay;
    private final JavaPlugin plugin;

    public PlayerLifecycleListener(FoundingService founding, DecayService decay, JavaPlugin plugin) {
        this.founding = founding;
        this.decay = decay;
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        founding.loadPlayer(event.getPlayer());
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> decay.sendJoinWarnings(event.getPlayer()), 20L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        founding.unloadPlayer(event.getPlayer());
    }
}
