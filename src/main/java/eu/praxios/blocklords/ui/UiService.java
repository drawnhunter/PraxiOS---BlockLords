package eu.praxios.blocklords.ui;

import eu.praxios.blocklords.config.BlockLordsConfig;
import eu.praxios.blocklords.model.ClaimRegion;
import eu.praxios.blocklords.model.QuestState;
import eu.praxios.blocklords.model.Village;
import eu.praxios.blocklords.model.VillageStats;
import eu.praxios.blocklords.service.FoundingService;
import eu.praxios.blocklords.service.TaskRunner;
import eu.praxios.blocklords.service.VillageScanner;
import eu.praxios.blocklords.service.VillageService;
import eu.praxios.blocklords.util.Msg;
import eu.praxios.blocklords.util.VillageNames;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public final class UiService {
    private final Supplier<BlockLordsConfig> config;
    private final VillageService villages;
    private final FoundingService founding;
    private final VillageScanner scanner;
    private final TaskRunner tasks;
    private final BedrockUi bedrock;

    public UiService(Supplier<BlockLordsConfig> config, VillageService villages, FoundingService founding,
                     VillageScanner scanner, TaskRunner tasks) {
        this.config = config;
        this.villages = villages;
        this.founding = founding;
        this.scanner = scanner;
        this.tasks = tasks;
        BedrockUi candidate = null;
        if (Bukkit.getPluginManager().isPluginEnabled("Floodgate") && config.get().bedrockForms()) {
            try {
                Class.forName("org.geysermc.floodgate.api.FloodgateApi", false, getClass().getClassLoader());
                Class.forName("org.geysermc.cumulus.form.Form", false, getClass().getClassLoader());
                candidate = new BedrockUi(tasks);
            } catch (ClassNotFoundException | NoClassDefFoundError unavailable) {
                Bukkit.getLogger().warning("BlockLords: Floodgate erkannt, aber Formular-API nicht verfügbar; Java/Befehle bleiben aktiv.");
            }
        }
        this.bedrock = candidate;
    }

    public void openBell(Player player, Block bell) {
        villages.atBell(bell).ifPresentOrElse(village -> {
            if (village.owner().equals(player.getUniqueId())) {
                villages.markActivity(village, player.getUniqueId());
            }
            openVillage(player, village);
        }, () -> openUnclaimed(player, bell));
    }

    public void openTargetInfo(Player player) {
        Block target = player.getTargetBlockExact(6);
        if (target != null && target.getType() == Material.BELL) {
            openBell(player, target);
            return;
        }
        villages.at(player.getLocation()).ifPresentOrElse(village -> openVillage(player, village),
                () -> Msg.error(player, "Visiere eine Dorfglocke an oder betritt ein BlockLords-Dorf."));
    }

    public void openUnclaimed(Player player, Block bell) {
        ClaimRegion region = new ClaimRegion(bell.getWorld().getUID(), bell.getX(), bell.getY(), bell.getZ(), 1);
        if (bedrockAvailable(player)) {
            bedrock.openUnclaimed(player, bell, founding.questInfo(player),
                    () -> founding.startQuest(player, bell), () -> openFoundNames(player, bell));
            return;
        }
        MenuHolder holder = new MenuHolder(MenuContext.unclaimed(region));
        Inventory inventory = Bukkit.createInventory(holder, 27, Component.text("BlockLords · Dorf entdecken"));
        holder.inventory(inventory);
        inventory.setItem(11, item(Material.BOOK, "Unbeanspruchte Dorfglocke",
                "Position: " + bell.getX() + "/" + bell.getY() + "/" + bell.getZ(), founding.questInfo(player)));
        inventory.setItem(13, item(Material.OAK_SAPLING, "Gründungsquest starten",
                "Erkunde Bett, Nahrung und Arbeitsplatz.", "Die erste Gründung ist anschließend kostenlos."));
        inventory.setItem(15, item(Material.EMERALD, "Dorf gründen",
                founding.quest(player).founderCompleted() ? "Dorfnamen wählen" : "Erst Gründungsquest abschließen"));
        inventory.setItem(22, item(Material.BARRIER, "Schließen"));
        player.openInventory(inventory);
    }

    public void openFoundNames(Player player, Block bell) {
        QuestState state = founding.quest(player);
        if (config.get().requireQuest() && !state.founderCompleted()) {
            Msg.error(player, "Schließe zuerst die Gründungsquest ab: " + founding.questInfo(player));
            return;
        }
        List<String> names = VillageNames.suggestions();
        if (bedrockAvailable(player)) {
            bedrock.openFoundName(player, names.getFirst(), name -> founding.found(player, bell, name,
                    village -> openVillage(player, village)));
            return;
        }
        ClaimRegion region = new ClaimRegion(bell.getWorld().getUID(), bell.getX(), bell.getY(), bell.getZ(), 1);
        MenuHolder holder = new MenuHolder(MenuContext.foundNames(region, names));
        Inventory inventory = Bukkit.createInventory(holder, 27, Component.text("BlockLords · Dorfname wählen"));
        holder.inventory(inventory);
        int[] slots = {11, 13, 15};
        for (int index = 0; index < names.size(); index++) {
            inventory.setItem(slots[index], item(Material.EMERALD, "Als '" + names.get(index) + "' gründen",
                    "Kosten: " + (villages.owned(player.getUniqueId()).isEmpty() ? "kostenlos" : config.get().nextVillageCost() + " Smaragde")));
        }
        inventory.setItem(18, item(Material.BARRIER, "Zurück"));
        inventory.setItem(22, item(Material.WRITABLE_BOOK, "Eigener Name per Befehl", "/bl found <Dorfname>"));
        player.openInventory(inventory);
    }

    public void openVillage(Player player, Village village) {
        String owner = ownerName(village);
        String stats = statsText(village);
        if (bedrockAvailable(player)) {
            bedrock.openVillage(player, village, owner, villages.memberCount(village.id()) + "", stats,
                    () -> scanner.requestAndNotify(village, player));
            return;
        }
        MenuHolder holder = new MenuHolder(MenuContext.village(village.region(), village.id()));
        Inventory inventory = Bukkit.createInventory(holder, 27, Component.text("BlockLords · " + village.name()));
        holder.inventory(inventory);
        inventory.setItem(11, item(Material.BOOK, "Dorfinformation", "Level " + village.level(), "Status " + village.status(), stats));
        inventory.setItem(13, item(Material.PLAYER_HEAD, "Landherr: " + owner,
                "Mitglieder: " + villages.memberCount(village.id()), "Rathaus-Basis (Wirtschaft folgt in Phase 2)"));
        inventory.setItem(15, item(Material.COMPASS, "Dorf vermessen", "Bewohner und Betten in geladenen Chunks zählen"));
        inventory.setItem(22, item(Material.BARRIER, "Schließen"));
        player.openInventory(inventory);
    }

    public void handleClick(Player player, MenuContext context, int slot) {
        if (slot == 22) {
            player.closeInventory();
            if (context.type() == MenuContext.MenuType.FOUND_NAME) {
                Msg.info(player, "Eigener Dorfname: /bl found <Dorfname> — visiere dazu die Glocke erneut an.");
            }
            return;
        }
        Block bell = bell(context.bell());
        if (bell == null) {
            player.closeInventory();
            Msg.error(player, "Die Welt dieser Glocke ist derzeit nicht geladen.");
            return;
        }
        switch (context.type()) {
            case UNCLAIMED -> {
                if (slot == 13) founding.startQuest(player, bell);
                if (slot == 15) openFoundNames(player, bell);
            }
            case FOUND_NAME -> {
                if (slot == 18) {
                    openUnclaimed(player, bell);
                    return;
                }
                int index = switch (slot) {
                    case 11 -> 0;
                    case 13 -> 1;
                    case 15 -> 2;
                    default -> -1;
                };
                if (index >= 0 && index < context.names().size()) {
                    player.closeInventory();
                    founding.found(player, bell, context.names().get(index), village -> openVillage(player, village));
                }
            }
            case VILLAGE -> {
                if (slot == 15) {
                    villages.byId(context.villageId()).ifPresent(village -> scanner.requestAndNotify(village, player));
                }
            }
        }
    }

    private Block bell(ClaimRegion region) {
        World world = Bukkit.getWorld(region.world());
        return world == null ? null : world.getBlockAt(region.bellX(), region.bellY(), region.bellZ());
    }

    private boolean bedrockAvailable(Player player) {
        return bedrock != null && bedrock.available(player, config.get().bedrockForms());
    }

    private String statsText(Village village) {
        return scanner.fresh(village).map(stats -> stats.villagers() + " Bewohner, " + stats.beds() + " Betten, "
                        + stats.loadedChunks() + "/" + stats.totalChunks() + " Chunks" + (stats.complete() ? "" : " (unvollständig)"))
                .orElse("Noch keine aktuelle Vermessung.");
    }

    private String ownerName(Village village) {
        String name = Bukkit.getOfflinePlayer(village.owner()).getName();
        return name == null ? village.owner().toString().substring(0, 8) : name;
    }

    private ItemStack item(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.GOLD));
        meta.lore(Arrays.stream(lore).map(line -> Component.text(line, NamedTextColor.GRAY)).toList());
        item.setItemMeta(meta);
        return item;
    }
}
