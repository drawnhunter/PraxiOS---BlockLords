package eu.praxios.blocklords.ui;

import org.bukkit.Material;

import java.util.List;

public record UiAction(Material icon, String name, List<String> lore, Runnable action, Runnable rightAction) {
    public static UiAction of(Material icon, String name, Runnable action, String... lore) {
        return new UiAction(icon, name, List.of(lore), action, action);
    }

    public static UiAction of(Material icon, String name, Runnable action, Runnable rightAction, String... lore) {
        return new UiAction(icon, name, List.of(lore), action, rightAction);
    }

    public Runnable forClick(boolean rightClick) {
        return rightClick ? rightAction : action;
    }
}
