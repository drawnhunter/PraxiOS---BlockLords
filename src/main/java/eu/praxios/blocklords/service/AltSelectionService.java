package eu.praxios.blocklords.service;

import eu.praxios.blocklords.repository.LegacyRepository;
import eu.praxios.blocklords.util.Msg;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Admin-Auswahl für die einmalige, ehrliche Registrierung bestehender Spielerbauten. */
public final class AltSelectionService {
    private final LegacyRepository repository;
    private final VillageService villages;
    private final TaskRunner tasks;
    private final AuditService audit;
    private final Map<UUID, Location> first = new ConcurrentHashMap<>();
    private final Map<UUID, Location> second = new ConcurrentHashMap<>();

    public AltSelectionService(LegacyRepository repository, VillageService villages, TaskRunner tasks, AuditService audit) {
        this.repository = repository;
        this.villages = villages;
        this.tasks = tasks;
        this.audit = audit;
    }

    public void select(Player player, int position) {
        if (position == 1) first.put(player.getUniqueId(), player.getLocation());
        else second.put(player.getUniqueId(), player.getLocation());
        Msg.success(player, "Altparzellen-Position " + position + " auf deinen aktuellen Standort gesetzt.");
    }

    public void register(Player admin, UUID owner, String name) {
        Location one = first.get(admin.getUniqueId());
        Location two = second.get(admin.getUniqueId());
        if (one == null || two == null || one.getWorld() == null || !one.getWorld().equals(two.getWorld())) {
            Msg.error(admin, "Setze zuerst /bl admin alt pos1 und pos2 in derselben Welt.");
            return;
        }
        if (name == null || name.isBlank() || name.length() > 48) {
            Msg.error(admin, "Der Altparzellen-Name muss 1–48 Zeichen haben.");
            return;
        }
        UUID world = one.getWorld().getUID();
        tasks.database(() -> repository.register(name, owner, world,
                        one.getBlockX(), one.getBlockY(), one.getBlockZ(),
                        two.getBlockX(), two.getBlockY(), two.getBlockZ(), admin.getUniqueId()), plot -> {
                    villages.refresh();
                    audit.log(admin, "LEGACY_REGISTERED", "ID " + plot.id() + " '" + plot.name() + "' Besitzer "
                            + (owner == null ? "unbekannt" : owner));
                    Msg.success(admin, "Altparzelle '" + plot.name() + "' mit ID " + plot.id() + " registriert.");
                    first.remove(admin.getUniqueId());
                    second.remove(admin.getUniqueId());
                }, error -> Msg.error(admin, "Altparzelle konnte nicht registriert werden: " + error.getMessage()));
    }

    public void remove(Player admin, long id) {
        tasks.database(() -> {
            repository.delete(id);
            return id;
        }, removed -> {
            villages.refresh();
            audit.log(admin, "LEGACY_REMOVED", "ID " + id);
            Msg.success(admin, "Altparzelle " + id + " entfernt.");
        }, error -> Msg.error(admin, "Altparzelle konnte nicht entfernt werden: " + error.getMessage()));
    }
}
