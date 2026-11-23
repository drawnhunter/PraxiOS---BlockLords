package eu.praxios.blocklords.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

public final class Msg {
    private Msg() {
    }

    public static void info(CommandSender receiver, String text) {
        receiver.sendMessage(Component.text("BlockLords · ", NamedTextColor.GOLD)
                .append(Component.text(text, NamedTextColor.YELLOW)));
    }

    public static void success(CommandSender receiver, String text) {
        receiver.sendMessage(Component.text("BlockLords · ", NamedTextColor.GOLD)
                .append(Component.text(text, NamedTextColor.GREEN)));
    }

    public static void error(CommandSender receiver, String text) {
        receiver.sendMessage(Component.text("BlockLords · ", NamedTextColor.GOLD)
                .append(Component.text(text, NamedTextColor.RED)));
    }

    public static void plain(CommandSender receiver, String text) {
        receiver.sendMessage(Component.text(text, NamedTextColor.GRAY));
    }
}
