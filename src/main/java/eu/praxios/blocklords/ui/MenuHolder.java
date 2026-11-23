package eu.praxios.blocklords.ui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class MenuHolder implements InventoryHolder {
    private final MenuContext context;
    private Inventory inventory;

    public MenuHolder(MenuContext context) {
        this.context = context;
    }

    public MenuContext context() {
        return context;
    }

    public void inventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
