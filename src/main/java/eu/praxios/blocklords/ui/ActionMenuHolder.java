package eu.praxios.blocklords.ui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.List;

public final class ActionMenuHolder implements InventoryHolder {
    private final List<UiAction> actions;
    private Inventory inventory;

    public ActionMenuHolder(List<UiAction> actions) {
        this.actions = List.copyOf(actions);
    }

    public List<UiAction> actions() { return actions; }
    public void inventory(Inventory inventory) { this.inventory = inventory; }

    @Override
    public Inventory getInventory() { return inventory; }
}
