package eu.praxios.blocklords.model;

import java.util.Map;

public enum BlueprintType {
    RATHAUS("Rathaus", 1, 50, false, Map.of(ResourceType.HOLZ, 32, ResourceType.STEIN, 16)),
    WOHNHAUS("Wohnhaus", 1, 40, false, Map.of(ResourceType.HOLZ, 32, ResourceType.STEIN, 16)),
    FARM("Bauernhaus + Feld", 1, 40, true, Map.of(ResourceType.HOLZ, 16, ResourceType.STEIN, 8)),
    MINE("Mine", 2, 60, true, Map.of(ResourceType.HOLZ, 16, ResourceType.STEIN, 32, ResourceType.SMARAGD, 4)),
    LAGERHAUS("Lagerhaus", 2, 40, false, Map.of(ResourceType.HOLZ, 48, ResourceType.STEIN, 16, ResourceType.SMARAGD, 4)),
    BIBLIOTHEK("Bibliothek", 3, 80, false, Map.of(ResourceType.HOLZ, 32, ResourceType.STEIN, 16, ResourceType.SMARAGD, 8)),
    SCHMIEDE("Schmiede", 4, 80, false, Map.of(ResourceType.HOLZ, 16, ResourceType.STEIN, 32, ResourceType.EISEN, 8, ResourceType.SMARAGD, 8)),
    WOHNHAUS_II("Wohnhaus II", 3, 70, false, Map.of(ResourceType.HOLZ, 48, ResourceType.STEIN, 24, ResourceType.SMARAGD, 8));

    private final String displayName;
    private final int requiredLevel;
    private final int xp;
    private final boolean workplace;
    private final Map<ResourceType, Integer> cost;

    BlueprintType(String displayName, int requiredLevel, int xp, boolean workplace, Map<ResourceType, Integer> cost) {
        this.displayName = displayName;
        this.requiredLevel = requiredLevel;
        this.xp = xp;
        this.workplace = workplace;
        this.cost = cost;
    }

    public String displayName() { return displayName; }
    public int requiredLevel() { return requiredLevel; }
    public int xp() { return xp; }
    public boolean workplace() { return workplace; }
    public Map<ResourceType, Integer> cost() { return cost; }
}
