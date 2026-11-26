package eu.praxios.blocklords.ui;

import eu.praxios.blocklords.config.BlockLordsConfig;
import eu.praxios.blocklords.database.Database;
import eu.praxios.blocklords.database.MigrationRunner;
import eu.praxios.blocklords.model.ClaimRegion;
import eu.praxios.blocklords.model.ResourceType;
import eu.praxios.blocklords.model.Village;
import eu.praxios.blocklords.repository.*;
import eu.praxios.blocklords.service.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.plugin.PluginMock;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class MenuListenerTest {
    @TempDir Path temp;
    private ServerMock server;
    private TaskRunner tasks;
    private Database database;
    private StorageService storage;
    private Village village;
    private PlayerMock player;
    private MenuListener listener;

    @BeforeEach
    void setup() throws Exception {
        server = MockBukkit.mock();
        PluginMock plugin = MockBukkit.createMockPlugin();
        tasks = new TaskRunner(plugin);
        database = new Database(temp.resolve("menu-listener.db"));
        new MigrationRunner(database, Logger.getLogger("test")).migrate();
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.load(new File("src/main/resources/config.yml"));
        BlockLordsConfig settings = new BlockLordsConfig(yaml);
        AuditService audit = new AuditService(new AuditRepository(database), tasks,
                new LogService(Logger.getLogger("test"), () -> settings));

        player = server.addPlayer();
        player.teleport(new Location(server.addSimpleWorld("world"), 0, 65, 0));
        VillageRepository villageRepository = new VillageRepository(database);
        village = villageRepository.createVillage("Klickdorf", player.getUniqueId(),
                new ClaimRegion(player.getWorld().getUID(), 0, 64, 0, 48), 4, 20, false, 21);
        VillageService villages = new VillageService(villageRepository, new LegacyRepository(database), tasks);
        villages.loadInitial();
        storage = new StorageService(() -> settings, new StorageRepository(database),
                new ConstructionRepository(database), tasks, audit, villages);
        storage.loadInitial(villages.all());
        UiService ui = new UiService(() -> settings, villages, null, null, tasks,
                null, storage, null, null, null, null, null);
        listener = new MenuListener(ui);
        ui.openStorage(player, village);
    }

    @AfterEach
    void close() throws Exception {
        if (tasks != null) tasks.close();
        if (database != null) database.close();
        if (server != null) MockBukkit.unmock();
    }

    @Test
    void bottomRightClickAllowsNormalStackSplitting() {
        player.getInventory().setItem(0, new ItemStack(Material.OAK_LOG, 64));
        InventoryClickEvent event = click(bottomSlot(), ClickType.RIGHT, InventoryAction.PICKUP_HALF);
        assertFalse(event.isCancelled());
        assertEquals(64, player.getInventory().getItem(0).getAmount());
    }

    @Test
    void shiftClicksDepositOnlyTheClickedStackOrSingleItem() throws Exception {
        player.getInventory().setItem(0, new ItemStack(Material.OAK_LOG, 32));
        InventoryClickEvent stack = click(bottomSlot(), ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY);
        assertTrue(stack.isCancelled());
        await(() -> storage.balances(village.id()).getOrDefault(ResourceType.HOLZ, 0) == 32);
        assertFalse(player.getInventory().contains(Material.OAK_LOG));

        player.getInventory().setItem(0, new ItemStack(Material.OAK_LOG, 32));
        InventoryClickEvent single = click(bottomSlot(), ClickType.SHIFT_RIGHT, InventoryAction.MOVE_TO_OTHER_INVENTORY);
        assertTrue(single.isCancelled());
        await(() -> storage.balances(village.id()).getOrDefault(ResourceType.HOLZ, 0) == 33);
        assertEquals(31, player.getInventory().getItem(0).getAmount());
    }

    @Test
    void topResourceClickKeepsItsCustomDepositAction() throws Exception {
        player.getInventory().setItem(0, new ItemStack(Material.OAK_LOG, 32));
        InventoryClickEvent event = click(resourceSlot("Holz"), ClickType.LEFT, InventoryAction.PICKUP_ALL);
        assertTrue(event.isCancelled());
        await(() -> storage.balances(village.id()).getOrDefault(ResourceType.HOLZ, 0) == 32);
        assertFalse(player.getInventory().contains(Material.OAK_LOG));
    }

    @Test
    void doubleClickAndTopDragStayBlockedWhileBottomDragIsNormal() {
        player.getInventory().setItem(0, new ItemStack(Material.OAK_LOG, 32));
        InventoryClickEvent doubleClick = click(bottomSlot(), ClickType.DOUBLE_CLICK, InventoryAction.COLLECT_TO_CURSOR);
        assertTrue(doubleClick.isCancelled());
        InventoryDragEvent topDrag = drag(1);
        listener.onDrag(topDrag);
        assertTrue(topDrag.isCancelled());
        InventoryDragEvent bottomDrag = drag(bottomSlot());
        listener.onDrag(bottomDrag);
        assertFalse(bottomDrag.isCancelled());
        assertEquals(0, storage.balances(village.id()).getOrDefault(ResourceType.HOLZ, 0));
        assertEquals(32, player.getInventory().getItem(0).getAmount());
    }

    private int resourceSlot(String name) {
        var holder = (ActionMenuHolder) player.getOpenInventory().getTopInventory().getHolder();
        for (int index = 0; index < holder.actions().size(); index++) {
            if (holder.actions().get(index).name().startsWith(name + " ·")) return index;
        }
        return fail("Ressource nicht im Menü: " + name);
    }

    private int bottomSlot() {
        return player.getOpenInventory().getTopInventory().getSize();
    }

    private InventoryClickEvent click(int rawSlot, ClickType type, InventoryAction action) {
        InventoryClickEvent event = new InventoryClickEvent(player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER, rawSlot, type, action);
        listener.onClick(event);
        return event;
    }

    private InventoryDragEvent drag(int rawSlot) {
        return new InventoryDragEvent(player.getOpenInventory(), new ItemStack(Material.OAK_LOG, 1),
                new ItemStack(Material.OAK_LOG, 32), true, Map.of(rawSlot, new ItemStack(Material.OAK_LOG, 1)));
    }

    private void await(BooleanSupplier condition) throws Exception {
        for (int attempt = 0; attempt < 200; attempt++) {
            server.getScheduler().performTicks(1);
            if (condition.getAsBoolean()) return;
            Thread.sleep(25);
        }
        fail("Asynchrone Lageraktion wurde nicht abgeschlossen");
    }
}
