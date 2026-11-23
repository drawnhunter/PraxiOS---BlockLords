package eu.praxios.blocklords.ui;

import eu.praxios.blocklords.model.Village;
import eu.praxios.blocklords.service.TaskRunner;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.FloodgateApi;

/** Native Bedrock-Menüs; jede Aktion wird wieder in den Main-Thread zurückgeführt. */
public final class BedrockUi {
    private final TaskRunner tasks;

    public BedrockUi(TaskRunner tasks) {
        this.tasks = tasks;
    }

    public boolean available(Player player, boolean enabled) {
        if (!enabled || !Bukkit.getPluginManager().isPluginEnabled("Floodgate")) return false;
        try {
            FloodgateApi api = FloodgateApi.getInstance();
            return api != null && api.isFloodgatePlayer(player.getUniqueId());
        } catch (NoClassDefFoundError | IllegalStateException unavailable) {
            return false;
        }
    }

    public void openUnclaimed(Player player, Block bell, String questInfo, Runnable startQuest, Runnable found) {
        SimpleForm form = SimpleForm.builder()
                .title("BlockLords · Unbeanspruchtes Dorf")
                .content("Glocke: " + bell.getX() + "/" + bell.getY() + "/" + bell.getZ() + "\n\n" + questInfo)
                .button("Gründungsquest starten")
                .button("Dorf gründen")
                .validResultHandler((ignored, response) -> {
                    if (response.clickedButtonId() == 0) tasks.sync(startQuest);
                    if (response.clickedButtonId() == 1) tasks.sync(found);
                })
                .build();
        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }

    public void openFoundName(Player player, String suggestion, java.util.function.Consumer<String> found) {
        CustomForm form = CustomForm.builder()
                .title("BlockLords · Dorf gründen")
                .label("Vergib einen eindeutigen Dorfnamen (3–24 Zeichen).")
                .input("Dorfname", suggestion, suggestion)
                .validResultHandler((ignored, response) -> {
                    String name = response.asInput(0);
                    tasks.sync(() -> found.accept(name));
                })
                .build();
        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }

    public void openVillage(Player player, Village village, String owner, String members, String stats, Runnable scan) {
        SimpleForm form = SimpleForm.builder()
                .title("BlockLords · " + village.name())
                .content("Landherr: " + owner + "\nMitglieder: " + members + "\nLevel: " + village.level()
                        + "\nStatus: " + village.status() + "\n\n" + stats)
                .button("Dorf vermessen")
                .button("Schließen")
                .validResultHandler((ignored, response) -> {
                    if (response.clickedButtonId() == 0) tasks.sync(scan);
                })
                .build();
        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }
}
