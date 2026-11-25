package eu.praxios.blocklords.service;

import eu.praxios.blocklords.model.BlueprintType;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Door;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.*;

class BlueprintBlocksTest {
    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void doorsHaveExactlyOneLowerAndUpperHalf() {
        var doors = BlueprintBlocks.blocks(BlueprintType.RATHAUS).stream()
                .filter(block -> block.data() instanceof Door).toList();
        assertEquals(2, doors.size());
        assertEquals(1, doors.stream().filter(block -> ((Door) block.data()).getHalf() == Bisected.Half.BOTTOM).count());
        assertEquals(1, doors.stream().filter(block -> ((Door) block.data()).getHalf() == Bisected.Half.TOP).count());
    }

    @Test
    void bedsHaveHeadAndFootAndFarmFieldIsOutside() {
        var beds = BlueprintBlocks.blocks(BlueprintType.WOHNHAUS).stream()
                .filter(block -> block.data() instanceof Bed).toList();
        assertEquals(4, beds.size());
        assertEquals(2, beds.stream().filter(block -> ((Bed) block.data()).getPart() == Bed.Part.FOOT).count());
        assertEquals(2, beds.stream().filter(block -> ((Bed) block.data()).getPart() == Bed.Part.HEAD).count());
        var farm = BlueprintBlocks.blocks(BlueprintType.FARM);
        assertTrue(farm.stream().anyMatch(block -> block.data() instanceof Bed));
        assertTrue(farm.stream().anyMatch(block -> block.data().getMaterial().name().equals("FARMLAND") && block.x() >= 6));
        assertTrue(farm.stream().noneMatch(block -> block.data().getMaterial().name().equals("FARMLAND") && block.x() < 5));
    }

    @Test
    void mineContainsRealSmithingTable() {
        assertTrue(BlueprintBlocks.blocks(BlueprintType.MINE).stream()
                .anyMatch(block -> block.data().getMaterial().name().equals("SMITHING_TABLE")));
    }
}
