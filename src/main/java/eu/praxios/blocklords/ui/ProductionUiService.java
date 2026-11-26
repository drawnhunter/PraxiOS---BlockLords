package eu.praxios.blocklords.ui;

import eu.praxios.blocklords.model.ProductionReport;
import eu.praxios.blocklords.model.Village;
import eu.praxios.blocklords.service.ProductionService;
import eu.praxios.blocklords.service.StorageService;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class ProductionUiService {
    private final ProductionService production;
    private final StorageService storage;
    private final UiService ui;

    public ProductionUiService(ProductionService production, StorageService storage, UiService ui) {
        this.production = production;
        this.storage = storage;
        this.ui = ui;
    }

    public void open(Player player, Village village) {
        production.report(village.id(), view -> {
            if (!player.isOnline()) return;
            List<UiAction> actions = new ArrayList<>();
            ProductionReport report = view.current();
            String content;
            if (report != null) {
                content = (report.active() ? "Aktive Produktion" : "Offline-Produktion") + " · "
                        + String.format(java.util.Locale.GERMAN, "%.1f", report.effectiveCycles()) + " Zyklen"
                        + "\nBauern " + report.farmers() + " · Minenarbeiter " + report.miners()
                        + "\nBerichtszeit " + Instant.ofEpochMilli(report.recordedAt());
                for (ProductionReport.Entry entry : report.resources()) {
                    List<String> lore = new ArrayList<>();
                    lore.add("Bestand " + storage.balances(village.id()).getOrDefault(entry.resource(), 0)
                            + "/" + storage.capacity(village.id()) + " pro Ressource");
                    if (entry.accepted() < entry.planned()) lore.add("Nur teilweise eingelagert; Lagergrund prüfen.");
                    actions.add(UiAction.of(entry.resource().canonicalItem(), entry.resource().displayName()
                            + " · +" + entry.accepted() + "/" + entry.planned(), () -> ui.openStorage(player, village),
                            lore.toArray(new String[0])));
                }
                if (report.foodConsumed() > 0) {
                    actions.add(UiAction.of(Material.BREAD, "Nahrungsverbrauch · -" + report.foodConsumed(),
                            () -> ui.openStorage(player, village), "Versorgung der zugewiesenen Arbeiter."));
                }
                if (!report.pauses().isEmpty()) {
                    actions.add(UiAction.of(Material.BARRIER, "Pausen und Gründe", () -> { },
                            report.pauses().toArray(new String[0])));
                }
            } else {
                content = view.legacyText() == null ? "Noch keine strukturierte Produktionsperiode gespeichert."
                        : "Bisheriger Dorfbericht: " + view.legacyText();
                actions.add(UiAction.of(Material.BOOK, "Produktionsbilanz", () -> { },
                        "Der nächste abgeschlossene Arbeitszyklus wird hier aufgeschlüsselt."));
            }
            actions.add(UiAction.of(Material.CHEST, "Zum Dorfvorrat", () -> ui.openStorage(player, village)));
            actions.add(UiAction.of(Material.ARROW, "Zurück", () -> ui.openVillage(player, village)));
            ui.openActions(player, "Produktion · " + village.name(), content, actions);
        });
    }
}
