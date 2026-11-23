package eu.praxios.blocklords.ui;

import eu.praxios.blocklords.model.ClaimRegion;

import java.util.List;

public record MenuContext(MenuType type, ClaimRegion bell, Long villageId, List<String> names) {
    public enum MenuType {
        UNCLAIMED,
        FOUND_NAME,
        VILLAGE
    }

    public static MenuContext unclaimed(ClaimRegion bell) {
        return new MenuContext(MenuType.UNCLAIMED, bell, null, List.of());
    }

    public static MenuContext foundNames(ClaimRegion bell, List<String> names) {
        return new MenuContext(MenuType.FOUND_NAME, bell, null, List.copyOf(names));
    }

    public static MenuContext village(ClaimRegion bell, long villageId) {
        return new MenuContext(MenuType.VILLAGE, bell, villageId, List.of());
    }
}
