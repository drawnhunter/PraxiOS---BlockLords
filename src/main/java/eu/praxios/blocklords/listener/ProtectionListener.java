package eu.praxios.blocklords.listener;

import eu.praxios.blocklords.service.ProtectionService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;

public final class ProtectionListener implements Listener {
    private final ProtectionService protection;

    public ProtectionListener(ProtectionService protection) {
        this.protection = protection;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBreak(BlockBreakEvent event) {
        if (!event.isCancelled() && !protection.canBuild(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            protection.deny(event.getPlayer(), "Dieser Bereich ist geschützt.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlace(BlockPlaceEvent event) {
        if (!event.isCancelled() && !protection.canBuild(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            protection.deny(event.getPlayer(), "Hier darfst du nicht bauen.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        if (event.useInteractedBlock() == Event.Result.DENY || event.getClickedBlock().getType() == Material.BELL) return;
        if (!protection.canInteract(event.getPlayer(), event.getClickedBlock().getLocation())) {
            event.setCancelled(true);
            protection.deny(event.getPlayer(), "Diese Einrichtung ist nicht öffentlich.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryOpen(InventoryOpenEvent event) {
        Location location = event.getInventory().getLocation();
        if (location != null && event.getPlayer() instanceof Player player
                && !protection.canOpenContainer(player, location)) {
            event.setCancelled(true);
            protection.deny(player, "Dieser Container ist geschützt.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Player)
                && !protection.canInteract(event.getPlayer(), event.getRightClicked().getLocation())) {
            event.setCancelled(true);
            protection.deny(event.getPlayer(), "Dieses Dorfobjekt ist geschützt.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onArmorStand(PlayerArmorStandManipulateEvent event) {
        if (!protection.canInteract(event.getPlayer(), event.getRightClicked().getLocation())) {
            event.setCancelled(true);
            protection.deny(event.getPlayer(), "Dieser Armor Stand ist geschützt.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player || !protection.isProtected(event.getEntity().getLocation())) return;
        if (event.getDamager() instanceof Player player) {
            if (!protection.canInteract(player, event.getEntity().getLocation())) {
                event.setCancelled(true);
                protection.deny(player, "Dieses Dorfobjekt ist geschützt.");
            }
        } else {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        if (!protection.isProtected(event.getEntity().getLocation())) return;
        if (event.getRemover() instanceof Player player) {
            if (!protection.canBuild(player, event.getEntity().getLocation())) {
                event.setCancelled(true);
                protection.deny(player, "Dieses Dorfobjekt ist geschützt.");
            }
        } else {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (protection.isProtected(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> protection.isProtected(block.getLocation()));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> protection.isProtected(block.getLocation()));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFlow(BlockFromToEvent event) {
        Location source = event.getBlock().getLocation();
        Location target = event.getToBlock().getLocation();
        if (protection.isProtected(target) && !protection.sameZone(source, target)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (crossesZone(event.getBlock(), event.getBlock().getRelative(event.getDirection()), event)) {
            event.setCancelled(true);
            return;
        }
        for (Block block : event.getBlocks()) {
            Block destination = block.getRelative(event.getDirection());
            if (!protection.sameZone(block.getLocation(), destination.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (!event.isSticky()) return;
        for (Block block : event.getBlocks()) {
            Block destination = block.getRelative(event.getDirection().getOppositeFace());
            if (!protection.sameZone(block.getLocation(), destination.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private boolean crossesZone(Block piston, Block head, BlockPistonExtendEvent event) {
        return !protection.sameZone(piston.getLocation(), head.getLocation());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onIgnite(BlockIgniteEvent event) {
        if (!protection.isProtected(event.getBlock().getLocation())) return;
        if (event.getPlayer() == null || !protection.canInteract(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBurn(BlockBurnEvent event) {
        if (protection.isProtected(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onHopper(InventoryMoveItemEvent event) {
        Location source = event.getSource().getLocation();
        Location destination = event.getDestination().getLocation();
        if (source != null && destination != null && !protection.sameZone(source, destination)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (!protection.canBuild(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            protection.deny(event.getPlayer(), "Hier darfst du nicht bauen.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (!protection.canBuild(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            protection.deny(event.getPlayer(), "Hier darfst du nicht eingreifen.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBedEnter(PlayerBedEnterEvent event) {
        if (!protection.canInteract(event.getPlayer(), event.getBed().getLocation())) {
            event.setCancelled(true);
            protection.deny(event.getPlayer(), "Dieses Bett ist nicht für dich freigegeben.");
        }
    }
}
