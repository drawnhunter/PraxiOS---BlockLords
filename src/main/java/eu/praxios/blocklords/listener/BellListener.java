package eu.praxios.blocklords.listener;

import eu.praxios.blocklords.service.FoundingService;
import eu.praxios.blocklords.ui.UiService;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public final class BellListener implements Listener {
    private final UiService ui;
    private final FoundingService founding;

    public BellListener(UiService ui, FoundingService founding) {
        this.ui = ui;
        this.founding = founding;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        if (founding.handleSurveyInteraction(player, block)) {
            event.setCancelled(true);
            return;
        }
        if (block.getType() == Material.BELL) {
            event.setCancelled(true);
            ui.openBell(player, block);
        }
    }
}
