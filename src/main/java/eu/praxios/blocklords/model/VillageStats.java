package eu.praxios.blocklords.model;

public record VillageStats(int villagers, int beds, int loadedChunks, int totalChunks, long scannedAt) {
    public boolean complete() {
        return loadedChunks == totalChunks;
    }
}
