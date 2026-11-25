package eu.praxios.blocklords.listener;

import eu.praxios.blocklords.model.BuildSite;
import eu.praxios.blocklords.service.ConstructionService;
import eu.praxios.blocklords.util.Msg;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

public final class BuilderChestProtectionListener implements Listener {
    private final ConstructionService construction;

    public BuilderChestProtectionListener(ConstructionService construction) {
        this.construction = construction;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        BuildSite site = construction.siteAtChest(event.getBlock()).orElse(null);
        if (site != null) {
            event.setCancelled(true);
            Msg.error(event.getPlayer(), "Baustellenkisten können nicht abgebaut werden. Abbruch: /bl build cancel "
                    + site.id() + " oder das Baustellenmenü.");
            return;
        }
        if (construction.buildingAtChest(event.getBlock()).isPresent()) {
            event.setCancelled(true);
            Msg.error(event.getPlayer(), "Diese Gebäudekiste ist geschützt. Informationen: /bl build sites.");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(this::isBuilderChest);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(this::isBuilderChest);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(this::isBuilderChest)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(this::isBuilderChest)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        if (isBuilderChest(event.getBlock())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent event) {
        if (isBuilderChest(event.getBlock())) event.setCancelled(true);
    }

    private boolean isBuilderChest(Block block) {
        return construction.siteAtChest(block).isPresent() || construction.buildingAtChest(block).isPresent();
    }
}
