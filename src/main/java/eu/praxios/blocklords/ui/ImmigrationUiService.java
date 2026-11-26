package eu.praxios.blocklords.ui;

import eu.praxios.blocklords.model.ImmigrationCandidate;
import eu.praxios.blocklords.model.Village;
import eu.praxios.blocklords.service.ImmigrationService;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class ImmigrationUiService {
    private final ImmigrationService immigration;
    private final UiService ui;

    public ImmigrationUiService(ImmigrationService immigration, UiService ui) {
        this.immigration = immigration;
        this.ui = ui;
    }

    public void open(Player player, Village village) {
        immigration.menu(village, snapshot -> {
            if (!player.isOnline()) return;
            List<UiAction> actions = new ArrayList<>();
            String content;
            if (snapshot.waiting().isPresent()) {
                ImmigrationCandidate candidate = snapshot.waiting().get();
                content = "Ein Wanderer wartet an der Dorfglocke.\nAufnahme kostet " + immigration.foodCost()
                        + " Nahrung und vergibt beim ersten Mal " + immigration.firstXp() + " Dorf-XP.";
                actions.add(UiAction.of(Material.VILLAGER_SPAWN_EGG, "Wanderer #" + candidate.id(),
                        () -> openCandidate(player, village, candidate.id()),
                        "Ansehen, aufnehmen oder ablehnen."));
            } else {
                long remaining = immigration.remainingSeconds(snapshot.state());
                content = remaining > 0 ? "Nächster Wanderer voraussichtlich in " + remaining + " Sekunden."
                        : snapshot.state().reason().isBlank() ? "Voraussetzungen werden geprüft." : snapshot.state().reason();
                actions.add(UiAction.of(Material.CLOCK, "Wartezyklus", () -> { },
                        "Freies Bett, " + immigration.foodCost() + " Nahrung und freier Arbeitsplatz nötig.",
                        "Keine Sofort-Spawns durch die Migration."));
            }
            actions.add(UiAction.of(Material.COMPASS, "Aktualisieren", () -> open(player, village)));
            actions.add(UiAction.of(Material.ARROW, "Zurück", () -> ui.openVillage(player, village)));
            ui.openActions(player, "Einwanderung · " + village.name(), content, actions);
        });
    }

    private void openCandidate(Player player, Village village, long candidateId) {
        List<UiAction> actions = List.of(
                UiAction.of(Material.EMERALD, "Wanderer aufnehmen",
                        () -> immigration.accept(player, village, candidateId, () -> open(player, village)),
                        "Kostet " + immigration.foodCost() + " Nahrung.",
                        "Bett, Nahrung und Arbeitsplatz werden erneut geprüft.",
                        "Erste Aufnahme +" + immigration.firstXp() + " XP · weitere +" + immigration.repeatXp() + " XP"),
                UiAction.of(Material.BARRIER, "Wanderer ablehnen",
                        () -> immigration.reject(player, village, candidateId, () -> open(player, village)),
                        "Keine Nahrungkosten · kein Töten."),
                UiAction.of(Material.ARROW, "Zurück", () -> open(player, village)));
        ui.openActions(player, "Wanderer #" + candidateId,
                "Nach der Aufnahme erhält der Wanderer Name, Bett und Dorf-Meeting-Point.\nDanach kannst du ihn über /bl workers zuweisen.", actions);
    }
}
