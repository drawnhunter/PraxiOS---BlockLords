package eu.praxios.blocklords.database;

import eu.praxios.blocklords.model.BlueprintType;
import eu.praxios.blocklords.model.ClaimRegion;
import eu.praxios.blocklords.model.PlacementSnapshot;
import eu.praxios.blocklords.model.Village;
import eu.praxios.blocklords.repository.PlacementRepository;
import eu.praxios.blocklords.repository.VillageRepository;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class PlacementRepositoryTest {
    @TempDir Path temp;
    private Database database;
    private PlacementRepository placements;
    private UUID player;
    private Village village;

    @BeforeEach
    void setup() throws Exception {
        database = new Database(temp.resolve("placement.db"));
        new MigrationRunner(database, Logger.getLogger("test")).migrate();
        placements = new PlacementRepository(database);
        player = UUID.randomUUID();
        village = new VillageRepository(database).createVillage("Sessiondorf", player,
                new ClaimRegion(UUID.randomUUID(), 0, 64, 0, 48), 4, 20, false, 21);
    }

    @AfterEach
    void close() throws Exception { database.close(); }

    @Test
    void snapshotRoundTripPreservesItemsAndClosesOnce() throws Exception {
        byte[] contents = new byte[]{1, 2, 3, 4};
        byte[] armor = new byte[]{5, 6};
        byte[] extras = new byte[]{7, 8};
        PlacementSnapshot snapshot = new PlacementSnapshot(player, village.id(), BlueprintType.RATHAUS,
                contents, armor, extras, 3, 1L);
        placements.saveActive(snapshot);
        PlacementSnapshot loaded = placements.findActive(player).orElseThrow();
        assertArrayEquals(contents, loaded.contents());
        assertEquals(3, loaded.selectedSlot());
        assertArrayEquals(armor, loaded.armor());
        assertArrayEquals(extras, loaded.extras());
        placements.close(player);
        assertTrue(placements.findActive(player).isEmpty());
    }
}
