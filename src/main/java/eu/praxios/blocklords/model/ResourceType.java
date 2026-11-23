package eu.praxios.blocklords.model;

import org.bukkit.Material;
import org.bukkit.Tag;

import java.util.Optional;

public enum ResourceType {
    NAHRUNG("Nahrung", Material.BREAD),
    WEIZEN("Weizen", Material.WHEAT),
    HOLZ("Holz", Material.OAK_LOG),
    STEIN("Stein", Material.COBBLESTONE),
    KOHLE("Kohle", Material.COAL),
    EISEN("Eisen", Material.IRON_INGOT),
    SMARAGD("Smaragd", Material.EMERALD),
    PFEIL("Pfeil", Material.ARROW);

    private final String displayName;
    private final Material canonicalItem;

    ResourceType(String displayName, Material canonicalItem) {
        this.displayName = displayName;
        this.canonicalItem = canonicalItem;
    }

    public String displayName() { return displayName; }
    public Material canonicalItem() { return canonicalItem; }

    public boolean accepts(Material material) {
        return switch (this) {
            case NAHRUNG -> material == Material.BREAD || material == Material.COOKED_BEEF
                    || material == Material.COOKED_PORKCHOP || material == Material.COOKED_CHICKEN
                    || material == Material.COOKED_MUTTON || material == Material.BAKED_POTATO
                    || material == Material.APPLE || material == Material.CARROT;
            case WEIZEN -> material == Material.WHEAT;
            case HOLZ -> Tag.LOGS.isTagged(material) || Tag.PLANKS.isTagged(material);
            case STEIN -> material == Material.STONE || material == Material.COBBLESTONE
                    || material == Material.COBBLED_DEEPSLATE || material == Material.STONE_BRICKS;
            case KOHLE -> material == Material.COAL || material == Material.CHARCOAL;
            case EISEN -> material == Material.IRON_INGOT;
            case SMARAGD -> material == Material.EMERALD;
            case PFEIL -> material == Material.ARROW;
        };
    }

    public static Optional<ResourceType> from(Material material) {
        for (ResourceType resource : values()) {
            if (resource.accepts(material)) return Optional.of(resource);
        }
        return Optional.empty();
    }
}
