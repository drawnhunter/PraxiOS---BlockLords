package eu.praxios.blocklords.ui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.List;

public final class ActionMenuHolder implements InventoryHolder {
    private final List<UiAction> actions;
    private final String menuId;
    private Inventory inventory;

    public ActionMenuHolder(List<UiAction> actions) {
        this(actions, "GENERIC");
    }

    public ActionMenuHolder(List<UiAction> actions, String menuId) {
        this.actions = List.copyOf(actions);
        this.menuId = menuId;
    }

    public List<UiAction> actions() { return actions; }
    public String menuId() { return menuId; }
    public void inventory(Inventory inventory) { this.inventory = inventory; }

    @Override
    public Inventory getInventory() { return inventory; }
}
