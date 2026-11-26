package eu.praxios.blocklords.ui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class MenuListener implements Listener {
    private final UiService ui;

    public MenuListener(UiService ui) {
        this.ui = ui;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof ActionMenuHolder actions) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player player)) return;
            if (event.getRawSlot() >= event.getInventory().getSize()) {
                if (!actions.menuId().equals("STORAGE")) return;
                if (isNormalInventoryEdit(event)) {
                    event.setCancelled(false);
                } else if (event.isShiftClick()) {
                    ui.handleStorageItemClick(player, event.getCurrentItem(), event.isRightClick());
                }
            } else if (event.getRawSlot() >= 0 && event.getRawSlot() < actions.actions().size()) {
                actions.actions().get(event.getRawSlot()).forClick(event.isRightClick()).run();
            }
            return;
        }
        if (!(event.getInventory().getHolder() instanceof MenuHolder holder)) return;
        event.setCancelled(true);
        if (event.getWhoClicked() instanceof Player player) {
            ui.handleClick(player, holder.context(), event.getRawSlot());
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof ActionMenuHolder actions) {
            boolean touchesMenu = event.getRawSlots().stream().anyMatch(slot -> slot < event.getInventory().getSize());
            event.setCancelled(touchesMenu || !actions.menuId().equals("STORAGE"));
            return;
        }
        if (event.getInventory().getHolder() instanceof MenuHolder) {
            event.setCancelled(true);
        }
    }

    private boolean isNormalInventoryEdit(InventoryClickEvent event) {
        if (event.isShiftClick()) return false;
        return switch (event.getAction()) {
            case PICKUP_ALL, PICKUP_HALF, PICKUP_ONE, PICKUP_SOME, SWAP_WITH_CURSOR -> true;
            default -> false;
        };
    }
}
