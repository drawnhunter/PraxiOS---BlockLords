package eu.praxios.blocklords.listener;

import eu.praxios.blocklords.service.GuideItemService;
import eu.praxios.blocklords.service.VillageService;
import eu.praxios.blocklords.ui.UiService;
import eu.praxios.blocklords.util.Msg;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public final class GuideItemListener implements Listener {
    private final GuideItemService guide;
    private final VillageService villages;
    private final UiService ui;

    public GuideItemListener(GuideItemService guide, VillageService villages, UiService ui) {
        this.guide = guide;
        this.villages = villages;
        this.ui = ui;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!guide.isGuide(event.getItem())) return;
        if (event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.BELL) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        villages.at(player.getLocation()).ifPresentOrElse(village -> ui.openVillage(player, village), () -> {
            Block target = player.getTargetBlockExact(6);
            if (target != null && target.getType() == Material.BELL) ui.openBell(player, target);
            else Msg.info(player, "Betritt ein Dorf oder visiere seine Glocke an. Baumenü: /bl build.");
        });
    }
}
