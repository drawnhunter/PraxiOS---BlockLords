package eu.praxios.blocklords.listener;

import eu.praxios.blocklords.service.ConstructionService;
import eu.praxios.blocklords.service.VillageService;
import eu.praxios.blocklords.ui.UiService;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

/** Rechtsklick = Auftragsmenü; Sneak + Rechtsklick = physisches Kisteninventar. */
public final class BuilderChestListener implements Listener {
    private final ConstructionService construction;
    private final VillageService villages;
    private final UiService ui;

    public BuilderChestListener(ConstructionService construction, VillageService villages, UiService ui) {
        this.construction = construction;
        this.villages = villages;
        this.ui = ui;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null || event.getPlayer().isSneaking()) return;
        Block block = event.getClickedBlock();
        if (block.getType() != Material.CHEST) return;
        Player player = event.getPlayer();
        construction.siteAtChest(block).ifPresent(site -> {
            event.setCancelled(true);
            villages.byId(site.villageId()).ifPresent(village -> ui.openSite(player, village, site));
        });
        if (event.useInteractedBlock() == Event.Result.DENY) return;
        construction.buildingAtChest(block).ifPresent(building -> {
            event.setCancelled(true);
            villages.byId(building.villageId()).ifPresent(village -> ui.openBuildingInfo(player, village, building));
        });
    }
}
