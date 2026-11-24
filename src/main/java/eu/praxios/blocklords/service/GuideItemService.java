package eu.praxios.blocklords.service;

import eu.praxios.blocklords.util.Msg;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/** Stabile Item-Erkennung über PDC statt über veränderbare Anzeigenamen. */
public final class GuideItemService {
    private final NamespacedKey key;

    public GuideItemService(JavaPlugin plugin) {
        key = new NamespacedKey(plugin, "dorfmenue");
    }

    public ItemStack create() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("BlockLords · Dorfmenü", NamedTextColor.GOLD));
        meta.lore(java.util.List.of(
                Component.text("Rechtsklick öffnet Dorf- und Baumenüs.", NamedTextColor.GRAY),
                Component.text("Zusätzlich: /bl menu", NamedTextColor.GRAY)));
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isGuide(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    public void give(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isGuide(item)) return;
        }
        player.getInventory().addItem(create());
        Msg.info(player, "Dorfmenü-Kompass erhalten: Rechtsklick öffnet BlockLords. Ersatz: /bl menu.");
    }
}
