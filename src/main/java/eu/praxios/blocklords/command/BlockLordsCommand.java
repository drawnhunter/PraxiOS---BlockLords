package eu.praxios.blocklords.command;

import eu.praxios.blocklords.BlockLords;
import eu.praxios.blocklords.model.AuditEntry;
import eu.praxios.blocklords.model.BlueprintType;
import eu.praxios.blocklords.model.Village;
import eu.praxios.blocklords.repository.VillageRepository;
import eu.praxios.blocklords.service.*;
import eu.praxios.blocklords.ui.UiService;
import eu.praxios.blocklords.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class BlockLordsCommand implements CommandExecutor, TabCompleter {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
            .withZone(ZoneId.systemDefault());
    private final BlockLords plugin;
    private final VillageService villages;
    private final FoundingService founding;
    private final UiService ui;
    private final ProtectionService protection;
    private final AltSelectionService alt;
    private final AuditService audit;
    private final TaskRunner tasks;
    private final VillageRepository villageRepository;
    private final DecayService decay;
    private final EconomyCommand economy;
    private final GuideItemService guide;

    public BlockLordsCommand(BlockLords plugin, VillageService villages, FoundingService founding, UiService ui,
                             ProtectionService protection, AltSelectionService alt, AuditService audit,
                             TaskRunner tasks, VillageRepository villageRepository, DecayService decay,
                             EconomyCommand economy, GuideItemService guide) {
        this.plugin = plugin;
        this.villages = villages;
        this.founding = founding;
        this.ui = ui;
        this.protection = protection;
        this.alt = alt;
        this.audit = audit;
        this.tasks = tasks;
        this.villageRepository = villageRepository;
        this.decay = decay;
        this.economy = economy;
        this.guide = guide;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) ui.openTargetInfo(player);
            else help(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "info" -> {
                if (sender instanceof Player player) ui.openTargetInfo(player);
                else Msg.error(sender, "Diese Aktion benötigt einen Spieler.");
            }
            case "quest" -> {
                if (sender instanceof Player player) Msg.info(player, founding.questInfo(player));
                else Msg.error(sender, "Diese Aktion benötigt einen Spieler.");
            }
            case "menu" -> {
                if (sender instanceof Player player) guide.give(player);
                else Msg.error(sender, "Diese Aktion benötigt einen Spieler.");
            }
            case "found" -> found(sender, args);
            case "build", "storage", "workers", "research", "missions", "level" -> economy.handle(sender, args);
            case "admin" -> admin(sender, args);
            default -> help(sender);
        }
        return true;
    }

    private void found(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Msg.error(sender, "Dorfgründung benötigt einen Spieler.");
            return;
        }
        if (args.length < 2) {
            Msg.error(player, "Verwendung: /bl found <Dorfname>");
            return;
        }
        Block bell = player.getTargetBlockExact(6);
        if (bell == null || bell.getType() != Material.BELL) {
            Msg.error(player, "Visiere eine Dorfglocke in höchstens 6 Blöcken Entfernung an.");
            return;
        }
        founding.found(player, bell, String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)),
                village -> Msg.info(player, "Dorf-ID: " + village.id()));
    }

    private void admin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("blocklords.admin")) {
            Msg.error(sender, "Dafür fehlt dir die Berechtigung blocklords.admin.");
            return;
        }
        if (args.length < 2) {
            adminHelp(sender);
            return;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                if (plugin.reloadBlockLordsConfig()) Msg.success(sender, "Konfiguration validiert und neu geladen.");
                else Msg.error(sender, "Reload abgebrochen; die alte Konfiguration bleibt aktiv. Details stehen im Log.");
            }
            case "bypass" -> {
                if (sender instanceof Player player) Msg.success(player, "Admin-Bypass " + (protection.toggleBypass(player) ? "aktiviert" : "deaktiviert") + ".");
                else Msg.error(sender, "Bypass benötigt einen Spieler.");
            }
            case "list" -> {
                if (villages.all().isEmpty()) Msg.info(sender, "Noch keine Dörfer registriert.");
                else villages.all().forEach(village -> Msg.plain(sender, "#" + village.id() + " " + village.name()
                        + " · Level " + village.level() + " · " + village.status()));
            }
            case "audit" -> audit(sender, args);
            case "auditselftest" -> audit.selfTest(sender);
            case "itemtest" -> plugin.itemTest(sender);
            case "backup" -> plugin.backup(sender);
            case "decay" -> {
                decay.check();
                Msg.success(sender, "Zerfallsprüfung ausgeführt; der Schutz wurde nicht verändert.");
            }
            case "disband" -> disband(sender, args);
            case "transfer" -> transfer(sender, args);
            case "tp" -> teleport(sender, args);
            case "selftest" -> selfTest(sender, args);
            case "alt" -> alt(sender, args);
            default -> adminHelp(sender);
        }
    }

    private void teleport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Msg.error(sender, "Dorf-Teleport benötigt einen Spieler.");
            return;
        }
        if (args.length < 3) {
            Msg.error(player, "Verwendung: /bl admin tp <Dorf-ID>");
            return;
        }
        Long id = parseId(player, args[2]);
        if (id == null) return;
        Village village = villages.byId(id).orElse(null);
        if (village == null) {
            Msg.error(player, "Dorf-ID " + id + " existiert nicht.");
            return;
        }
        org.bukkit.World world = Bukkit.getWorld(village.region().world());
        if (world == null) {
            Msg.error(player, "Die Welt dieses Dorfes ist nicht geladen.");
            return;
        }
        Location target = safeVillageLocation(world, village);
        if (target == null) {
            Msg.error(player, "Kein sicherer Teleportpunkt nahe der Glocke gefunden. Bitte Umgebung prüfen.");
            return;
        }
        player.teleportAsync(target).thenAccept(success -> {
            if (success) {
                Msg.success(player, "Teleport zu Dorf #" + village.id() + " '" + village.name() + "'.");
                audit.log(player, "ADMIN_TELEPORT", "Dorf #" + village.id() + " " + village.name());
            } else {
                Msg.error(player, "Teleport wurde vom Server abgelehnt.");
            }
        });
    }

    private Location safeVillageLocation(org.bukkit.World world, Village village) {
        int[][] offsets = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {2, 0}, {0, 2}, {-2, 0}, {0, -2}};
        for (int dy = 2; dy >= -5; dy--) {
            for (int[] offset : offsets) {
                int x = village.region().bellX() + offset[0];
                int y = village.region().bellY() + dy;
                int z = village.region().bellZ() + offset[1];
                Block feet = world.getBlockAt(x, y, z);
                Block head = feet.getRelative(0, 1, 0);
                Block ground = feet.getRelative(0, -1, 0);
                if (feet.isPassable() && head.isPassable() && ground.getType().isSolid()) {
                    return new Location(world, x + 0.5, y, z + 0.5, 0, 0);
                }
            }
        }
        return null;
    }

    private void selfTest(CommandSender sender, String[] args) {
        if (args.length == 3) {
            try {
                plugin.selfTest(sender, BlueprintType.valueOf(args[2].toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException exception) {
                Msg.error(sender, "Unbekannter Bauplantyp: " + args[2]);
            }
            return;
        }
        if (args.length < 6) {
            Msg.error(sender, "Verwendung: /bl admin selftest <Bauplantyp> [<x> <y> <z>] — ohne Koordinaten gilt dein anvisierter Block.");
            return;
        }
        try {
            int rotation = args.length >= 7 ? Integer.parseInt(args[6]) : 0;
            plugin.selfTest(sender, BlueprintType.valueOf(args[2].toUpperCase(Locale.ROOT)),
                    Integer.parseInt(args[3]), Integer.parseInt(args[4]), Integer.parseInt(args[5]), rotation);
        } catch (IllegalArgumentException exception) {
            Msg.error(sender, "Unbekannter Bauplantyp oder ungültige Koordinaten.");
        }
    }

    private void audit(CommandSender sender, String[] args) {
        int limit = 10;
        if (args.length >= 3) {
            try {
                limit = Integer.parseInt(args[2]);
            } catch (NumberFormatException exception) {
                Msg.error(sender, "Audit-Limit muss eine Zahl sein.");
                return;
            }
        }
        audit.recent(limit, entries -> {
            if (entries.isEmpty()) {
                Msg.info(sender, "Keine Audit-Einträge vorhanden.");
                return;
            }
            for (AuditEntry entry : entries) {
                Msg.plain(sender, "#" + entry.id() + " " + TIME.format(Instant.ofEpochMilli(entry.createdAt()))
                        + " · " + entry.action() + " · " + entry.details());
            }
        });
    }

    private void disband(CommandSender sender, String[] args) {
        if (args.length < 3) {
            Msg.error(sender, "Verwendung: /bl admin disband <Dorf-ID> confirm");
            return;
        }
        Long id = parseId(sender, args[2]);
        if (id == null) return;
        Village village = villages.byId(id).orElse(null);
        if (village == null) {
            Msg.error(sender, "Dorf-ID " + id + " existiert nicht.");
            return;
        }
        if (args.length < 4 || !args[3].equalsIgnoreCase("confirm")) {
            Msg.error(sender, "Bestätige mit /bl admin disband " + id + " confirm — Gebäude bleiben stehen, der Claim fällt.");
            return;
        }
        tasks.database(() -> {
            villageRepository.disband(id);
            return village;
        }, deleted -> {
            villages.refresh();
            audit.log(sender, "VILLAGE_DISBANDED", "#" + id + " " + village.name());
            Msg.success(sender, "Dorf '" + village.name() + "' aufgelöst; Gebäude bleiben erhalten.");
        }, error -> Msg.error(sender, "Dorfauflösung fehlgeschlagen: " + error.getMessage()));
    }

    private void transfer(CommandSender sender, String[] args) {
        if (args.length < 4) {
            Msg.error(sender, "Verwendung: /bl admin transfer <Dorf-ID> <OnlineSpieler>");
            return;
        }
        Long id = parseId(sender, args[2]);
        if (id == null) return;
        Player target = Bukkit.getPlayerExact(args[3]);
        Village village = villages.byId(id).orElse(null);
        if (village == null || target == null) {
            Msg.error(sender, "Dorf oder Online-Spieler wurde nicht gefunden.");
            return;
        }
        tasks.database(() -> {
            villageRepository.transfer(id, village.owner(), target.getUniqueId());
            return village;
        }, transferred -> {
            villages.refresh();
            audit.log(sender, "VILLAGE_TRANSFER", "#" + id + " an " + target.getName());
            Msg.success(sender, "Dorf '" + village.name() + "' an " + target.getName() + " übertragen.");
        }, error -> Msg.error(sender, "Dorfübertragung fehlgeschlagen: " + error.getMessage()));
    }

    private void alt(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Msg.error(sender, "Altparzellen-Auswahl benötigt einen Spieler.");
            return;
        }
        if (args.length < 3) {
            Msg.error(sender, "Alt: pos1, pos2, register, remove, list");
            return;
        }
        switch (args[2].toLowerCase(Locale.ROOT)) {
            case "pos1" -> alt.select(player, 1);
            case "pos2" -> alt.select(player, 2);
            case "list" -> {
                if (villages.legacyPlots().isEmpty()) Msg.info(player, "Keine Altparzellen registriert.");
                else villages.legacyPlots().forEach(plot -> Msg.plain(player, "#" + plot.id() + " " + plot.name()
                        + " · Besitzer " + (plot.owner() == null ? "unbekannt" : plot.owner())));
            }
            case "remove" -> {
                if (args.length < 4) {
                    Msg.error(player, "Verwendung: /bl admin alt remove <ID>");
                    return;
                }
                Long id = parseId(player, args[3]);
                if (id != null) alt.remove(player, id);
            }
            case "register" -> {
                if (args.length < 5) {
                    Msg.error(player, "Verwendung: /bl admin alt register <OnlineSpieler|unbekannt> <Name>");
                    return;
                }
                UUID owner = null;
                if (!args[3].equalsIgnoreCase("unbekannt")) {
                    Player target = Bukkit.getPlayerExact(args[3]);
                    if (target == null) {
                        Msg.error(player, "Der Besitzer muss online sein oder als 'unbekannt' registriert werden.");
                        return;
                    }
                    owner = target.getUniqueId();
                }
                alt.register(player, owner, String.join(" ", java.util.Arrays.copyOfRange(args, 4, args.length)));
            }
            default -> Msg.error(player, "Alt: pos1, pos2, register, remove, list");
        }
    }

    private Long parseId(CommandSender sender, String raw) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException exception) {
            Msg.error(sender, "Dorf-/Altparzellen-ID muss eine Zahl sein.");
            return null;
        }
    }

    private void help(CommandSender sender) {
        Msg.info(sender, "/bl menu · /bl info · /bl quest · /bl found <Name>");
        Msg.plain(sender, "/bl storage · /bl build · /bl workers · /bl research · /bl missions · /bl level");
        if (sender.hasPermission("blocklords.admin")) adminHelp(sender);
    }

    private void adminHelp(CommandSender sender) {
        Msg.plain(sender, "/bl admin list|reload|backup|decay|audit [n]|auditselftest|itemtest|bypass|selftest <Typ> <x> <y> <z>");
        Msg.plain(sender, "/bl admin tp <Dorf-ID> · disband <ID> confirm · transfer <ID> <Spieler>");
        Msg.plain(sender, "/bl admin alt pos1|pos2|register <Spieler|unbekannt> <Name>|remove <ID>|list");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return filter(List.of("menu", "info", "quest", "found", "build", "storage", "workers", "research", "missions", "level", "admin"), args[0]);
        if (args.length == 2 && args[0].equalsIgnoreCase("missions")) {
            return filter(List.of("list", "accept", "replace", "pin"), args[1]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("missions") && args[1].equalsIgnoreCase("replace")) {
            return filter(List.of("confirm"), args[3]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("admin") && sender.hasPermission("blocklords.admin")) {
            return filter(List.of("list", "reload", "backup", "decay", "audit", "auditselftest", "itemtest", "bypass", "selftest", "tp", "disband", "transfer", "alt"), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("alt")) {
            return filter(List.of("pos1", "pos2", "register", "remove", "list"), args[2]);
        }
        return List.of();
    }

    private List<String> filter(List<String> values, String prefix) {
        return values.stream().filter(value -> value.startsWith(prefix.toLowerCase(Locale.ROOT))).toList();
    }
}
