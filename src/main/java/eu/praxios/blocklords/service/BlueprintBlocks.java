package eu.praxios.blocklords.service;

import eu.praxios.blocklords.model.BlueprintType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Kleine kontrollierte Startbaupläne; später können diese durch Schematic-Dateien ersetzt werden. */
public final class BlueprintBlocks {
    private BlueprintBlocks() {
    }

    public static List<OffsetBlock> blocks(BlueprintType type) {
        List<OffsetBlock> blocks = new ArrayList<>();
        Material wall = type == BlueprintType.MINE ? Material.STONE_BRICKS : Material.OAK_PLANKS;
        Material roof = type == BlueprintType.MINE ? Material.DEEPSLATE_TILES : Material.COBBLESTONE;
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                blocks.add(new OffsetBlock(x, 0, z, data(Material.COBBLESTONE)));
                if (x == 0 || x == 4 || z == 0 || z == 4) {
                    for (int y = 1; y <= 3; y++) {
                        if (z == 0 && x == 2 && y <= 2) continue;
                        blocks.add(new OffsetBlock(x, y, z, data(wall)));
                    }
                }
                blocks.add(new OffsetBlock(x, 4, z, data(roof)));
            }
        }
        blocks.add(new OffsetBlock(2, 1, 0, data(Material.OAK_DOOR)));
        blocks.add(new OffsetBlock(2, 2, 0, data(Material.OAK_DOOR)));
        switch (type) {
            case RATHAUS -> blocks.add(new OffsetBlock(2, 1, 2, data(Material.LECTERN)));
            case WOHNHAUS -> addBeds(blocks, 2);
            case WOHNHAUS_II -> addBeds(blocks, 3);
            case FARM -> {
                for (int x = 1; x <= 3; x++) {
                    blocks.add(new OffsetBlock(x, 1, 2, data(Material.FARMLAND)));
                    blocks.add(new OffsetBlock(x, 2, 2, data(Material.WHEAT)));
                }
                blocks.add(new OffsetBlock(2, 1, 1, data(Material.COMPOSTER)));
            }
            case MINE -> {
                blocks.add(new OffsetBlock(2, 1, 2, data(Material.LADDER)));
                blocks.add(new OffsetBlock(2, 2, 2, data(Material.LADDER)));
                blocks.add(new OffsetBlock(2, 3, 2, data(Material.LADDER)));
            }
            case LAGERHAUS -> {
                blocks.add(new OffsetBlock(1, 1, 1, data(Material.CHEST)));
                blocks.add(new OffsetBlock(3, 1, 1, data(Material.CHEST)));
                blocks.add(new OffsetBlock(1, 1, 3, data(Material.CHEST)));
                blocks.add(new OffsetBlock(3, 1, 3, data(Material.CHEST)));
            }
            case BIBLIOTHEK -> {
                blocks.add(new OffsetBlock(1, 1, 1, data(Material.BOOKSHELF)));
                blocks.add(new OffsetBlock(3, 1, 1, data(Material.BOOKSHELF)));
                blocks.add(new OffsetBlock(2, 1, 2, data(Material.LECTERN)));
            }
            case SCHMIEDE -> {
                blocks.add(new OffsetBlock(1, 1, 2, data(Material.BLAST_FURNACE)));
                blocks.add(new OffsetBlock(3, 1, 2, data(Material.ANVIL)));
            }
        }
        blocks.sort(Comparator.comparingInt(OffsetBlock::y));
        return blocks;
    }

    private static void addBeds(List<OffsetBlock> blocks, int count) {
        for (int index = 0; index < count; index++) {
            blocks.add(new OffsetBlock(1 + index, 1, 2, data(Material.RED_BED)));
        }
    }

    private static BlockData data(Material material) {
        return Bukkit.createBlockData(material);
    }

    public record OffsetBlock(int x, int y, int z, BlockData data) {
    }
}
