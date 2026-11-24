package eu.praxios.blocklords.listener;

import eu.praxios.blocklords.service.PlacementModeService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;

/** Sperrt nur die aktive Werkzeugsession und behandelt alle Abbruchwege. */
public final class PlacementModeListener implements Listener {
    private final PlacementModeService placement;

    public PlacementModeListener(PlacementModeService placement) {
        this.placement = placement;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!placement.isActive(event.getPlayer())) return;
        if (placement.handleInteraction(event.getPlayer(), event.getItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (placement.isActive(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (placement.isActive(event.getPlayer()) || placement.isTool(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player && placement.isActive(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player && placement.isActive(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player && placement.isActive(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent event) {
        if (placement.isActive(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (placement.isActive(event.getPlayer())) placement.exit(event.getPlayer(), "Weltwechsel", true);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        placement.removeToolsFromDrops(event.getDrops());
        placement.exit(event.getPlayer(), "Tod", true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        placement.exit(event.getPlayer(), "Logout", true);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        placement.restoreOnJoin(event.getPlayer());
    }
}
