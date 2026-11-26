package eu.praxios.blocklords.model;

import java.util.List;

public record VillageStats(int villagers, int beds, int loadedChunks, int totalChunks, long scannedAt,
                           List<BlockLink> bedLocations) {
    public VillageStats(int villagers, int beds, int loadedChunks, int totalChunks, long scannedAt) {
        this(villagers, beds, loadedChunks, totalChunks, scannedAt, List.of());
    }

    public boolean complete() {
        return loadedChunks == totalChunks;
    }
}
