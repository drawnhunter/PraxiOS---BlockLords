package eu.praxios.blocklords;

import org.bukkit.plugin.java.JavaPlugin;

public final class BlockLords extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getLogger().info("BlockLords " + getDescription().getVersion() + " aktiviert. Konzept: siehe KONZEPT.md im Repo.");
    }

    @Override
    public void onDisable() {
        getLogger().info("BlockLords deaktiviert.");
    }
}
