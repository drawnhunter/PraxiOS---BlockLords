package eu.praxios.blocklords.command;

import eu.praxios.blocklords.model.*;
import eu.praxios.blocklords.service.*;
import eu.praxios.blocklords.ui.UiService;
import eu.praxios.blocklords.util.Msg;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

/** Befehls-Fallback und Testzugang für denselben Phase-2-Aktionscode wie die GUIs. */
public final class EconomyCommand {
    private final VillageService villages;
    private final ConstructionService construction;
    private final StorageService storage;
    private final WorkerService workers;
    private final ResearchService research;
    private final UiService ui;

    public EconomyCommand(VillageService villages, ConstructionService construction, StorageService storage,
                          WorkerService workers, ResearchService research, UiService ui) {
        this.villages = villages;
        this.construction = construction;
        this.storage = storage;
        this.workers = workers;
        this.research = research;
        this.ui = ui;
    }

    public boolean handle(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Msg.error(sender, "Diese Aktion benötigt einen Spieler.");
            return true;
        }
        Village village = villages.at(player.getLocation()).orElse(null);
        if (village == null) {
            Msg.error(player, "Betritt zuerst dein BlockLords-Dorf oder nutze die Dorfglocke.");
            return true;
        }
        String section = args[0].toLowerCase(Locale.ROOT);
        switch (section) {
            case "build" -> build(player, village, args);
            case "storage" -> storage(player, village, args);
            case "workers" -> workers(player, village, args);
            case "research" -> research(player, village, args);
            case "level" -> Msg.info(player, "Dorf '" + village.name() + "': Level " + village.level()
                    + ", XP " + village.xp() + ", Status " + village.status() + ".");
            default -> Msg.error(player, "Unbekannter Bereich.");
        }
        return true;
    }

    private void build(Player player, Village village, String[] args) {
        if (args.length == 1 || args[1].equalsIgnoreCase("list")) {
            ui.openBuild(player, village);
            return;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "preview" -> {
                if (args.length < 3) {
                    Msg.error(player, "Verwendung: /bl build preview <Bauplantyp>");
                    return;
                }
                try {
                    construction.preview(player, village, BlueprintType.valueOf(args[2].toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException exception) {
                    Msg.error(player, "Unbekannter Bauplantyp: " + args[2]);
                }
            }
            case "confirm" -> construction.confirmPreview(player);
            case "cancel" -> construction.cancelPreview(player);
            case "contribute" -> construction.contribute(player, village);
            case "fund" -> construction.fund(player, village);
            case "start" -> construction.start(player, village);
            case "sites" -> ui.openSites(player, village);
            case "select" -> {
                if (args.length < 3) {
                    Msg.error(player, "Verwendung: /bl build select <Baustellen-ID>");
                    return;
                }
                try {
                    long id = Long.parseLong(args[2]);
                    construction.sites(village.id()).stream().filter(site -> site.id() == id).findFirst()
                            .ifPresentOrElse(site -> {
                                construction.selectSite(player, site);
                                Msg.success(player, "Baustelle #" + id + " ausgewählt.");
                            }, () -> Msg.error(player, "Baustelle-ID nicht gefunden."));
                } catch (NumberFormatException exception) {
                    Msg.error(player, "Baustellen-ID muss eine Zahl sein.");
                }
            }
            default -> Msg.error(player, "/bl build list|preview|confirm|cancel|contribute|fund|start|sites|select");
        }
    }

    private void storage(Player player, Village village, String[] args) {
        if (args.length == 1) {
            ui.openStorage(player, village);
            return;
        }
        if (args[1].equalsIgnoreCase("deposit") && args.length == 2) {
            storage.depositAll(player, village);
            return;
        }
        if (args.length < 4) {
            Msg.error(player, "/bl storage deposit [Ressource Menge] · withdraw <Ressource> <Menge>");
            return;
        }
        ResourceType resource;
        int amount;
        try {
            resource = ResourceType.valueOf(args[2].toUpperCase(Locale.ROOT));
            amount = Integer.parseInt(args[3]);
            if (amount <= 0) throw new NumberFormatException();
        } catch (IllegalArgumentException exception) {
            Msg.error(player, "Unbekannte Ressource oder ungültige Menge.");
            return;
        }
        if (args[1].equalsIgnoreCase("deposit")) storage.deposit(player, village, resource, amount);
        else if (args[1].equalsIgnoreCase("withdraw")) storage.withdraw(player, village, resource, amount);
        else Msg.error(player, "Verwende deposit oder withdraw.");
    }

    private void workers(Player player, Village village, String[] args) {
        if (args.length == 1) {
            ui.openWorkers(player, village);
            return;
        }
        if (args.length < 3) {
            Msg.error(player, "/bl workers assign <Gebäude-ID> · unassign <Arbeiter-ID>");
            return;
        }
        try {
            long id = Long.parseLong(args[2]);
            if (args[1].equalsIgnoreCase("assign")) workers.assign(player, village, id);
            else if (args[1].equalsIgnoreCase("unassign")) workers.unassign(player, village, id);
            else Msg.error(player, "Verwende assign oder unassign.");
        } catch (NumberFormatException exception) {
            Msg.error(player, "ID muss eine Zahl sein.");
        }
    }

    private void research(Player player, Village village, String[] args) {
        if (args.length == 1) {
            ui.openResearch(player, village);
            return;
        }
        if (args[1].equalsIgnoreCase("start")) research.startResearch(player, village);
        else Msg.error(player, "Verwendung: /bl research [start]");
    }
}
