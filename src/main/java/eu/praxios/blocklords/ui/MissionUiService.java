package eu.praxios.blocklords.ui;

import eu.praxios.blocklords.model.Mission;
import eu.praxios.blocklords.model.MissionView;
import eu.praxios.blocklords.model.Village;
import eu.praxios.blocklords.service.MissionService;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class MissionUiService {
    private final MissionService missions;
    private final UiService ui;

    public MissionUiService(MissionService missions, UiService ui) {
        this.missions = missions;
        this.ui = ui;
    }

    public void open(Player player, Village village) {
        missions.list(player, village, views -> {
            if (!player.isOnline()) return;
            List<UiAction> actions = new ArrayList<>();
            for (MissionView view : views) {
                Mission mission = view.mission();
                boolean active = mission.status() == Mission.Status.ACTIVE;
                String progress = mission.status() == Mission.Status.OFFERED ? "Angebot"
                        : mission.progress() + "/" + mission.target();
                Material icon = mission.personal() ? Material.PAPER : Material.BELL;
                String name = (active ? "Aktiv · " : "") + mission.title() + " · " + progress;
                if (active) {
                    actions.add(UiAction.glowing(icon, name,
                            () -> openDetail(player, village, mission.id()), lore(view).toArray(new String[0])));
                } else {
                    actions.add(UiAction.of(icon, name,
                            () -> openDetail(player, village, mission.id()), lore(view).toArray(new String[0])));
                }
            }
            actions.add(UiAction.of(Material.BOOK, "So funktionieren Missionen", () -> { },
                    "Annehmen aktiviert den Fortschritt ab diesem Moment.",
                    "Erstabschluss gibt Dorf-XP · Wiederholung keine XP.",
                    "Kein Verfall · Kostenlos ersetzen · Persönlich anpinnen."));
            actions.add(UiAction.of(Material.ARROW, "Zurück", () -> ui.openVillage(player, village)));
            ui.openActions(player, "Missionen · " + village.name(),
                    "Zwei persönliche Angebote und eine gemeinsame Dorfmission.\nKein Zeitdruck; nur echte Beiträge zählen.", actions);
        });
    }

    private void openDetail(Player player, Village village, long missionId) {
        missions.details(player, village, missionId, view -> {
            if (!player.isOnline()) return;
            Mission mission = view.mission();
            List<UiAction> actions = new ArrayList<>();
            if (mission.status() == Mission.Status.OFFERED) {
                actions.add(UiAction.of(Material.EMERALD, "Mission annehmen",
                        () -> missions.accept(player, village, missionId, () -> open(player, village)),
                        "Startet bei 0/" + mission.target() + " · keine Altbestände."));
            }
            if (mission.status() == Mission.Status.ACTIVE && mission.personal()) {
                actions.add(UiAction.of(Material.MAP, mission.pinned() ? "Erneut anpinnen" : "Für später anpinnen",
                        () -> missions.pin(player, village, missionId, () -> open(player, village)),
                        "Zeigt diese Aufgabe beim nächsten Login als kurzen Hinweis."));
            }
            if ((mission.status() == Mission.Status.OFFERED || mission.status() == Mission.Status.ACTIVE)
                    && (mission.personal() || missions.canManage(player, village))) {
                actions.add(UiAction.of(Material.BARRIER, "Kostenlos ersetzen", () -> {
                    if (mission.progress() > 0) openReplacementConfirmation(player, village, missionId);
                    else missions.replace(player, village, missionId, true, () -> open(player, village));
                }, "Verwirft diesen Missionsfortschritt, keine Ressourcen."));
            }
            actions.add(UiAction.of(Material.ARROW, "Zurück zu Missionen", () -> open(player, village)));
            String content = (mission.personal() ? "Persönliche Mission" : "Gemeinsame Dorfmission")
                    + "\nFortschritt: " + mission.progress() + "/" + mission.target()
                    + (mission.personal() ? "" : "\nDein Beitrag: " + view.ownContribution())
                    + "\nErstabschluss: " + mission.xp() + " Dorf-XP · Wiederholung: 0 XP";
            ui.openActions(player, mission.title(), content, actions);
        });
    }

    private void openReplacementConfirmation(Player player, Village village, long missionId) {
        List<UiAction> actions = List.of(
                UiAction.of(Material.BARRIER, "Fortschritt verwerfen und ersetzen",
                        () -> missions.replace(player, village, missionId, true, () -> open(player, village)),
                        "Kostenlos · Ressourcen bleiben unverändert."),
                UiAction.of(Material.ARROW, "Mission behalten", () -> openDetail(player, village, missionId)));
        ui.openActions(player, "Mission ersetzen?",
                "Der bisherige Missionsfortschritt wird verworfen. Bereits eingelagerte oder gelieferte Ressourcen bleiben erhalten.", actions);
    }

    private List<String> lore(MissionView view) {
        Mission mission = view.mission();
        List<String> lore = new ArrayList<>();
        lore.add(mission.personal() ? "Persönlich" : "Gemeinsam für das Dorf");
        lore.add(switch (mission.action()) {
            case STORAGE_DEPOSIT -> "Neu einlagern: " + mission.target() + " " + mission.resource().displayName();
            case BUILD_CONTRIBUTION -> mission.target() + " benötigte Ressourcen an Baustellen liefern";
        });
        if (mission.status() == Mission.Status.OFFERED) lore.add("Fortschritt beginnt erst nach dem Annehmen.");
        else lore.add("Fortschritt: " + mission.progress() + "/" + mission.target());
        if (!mission.personal()) lore.add("Dein Beitrag: " + view.ownContribution());
        lore.add("Erstabschluss: " + mission.xp() + " XP · Wiederholung: 0 XP");
        return lore;
    }
}
