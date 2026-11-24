package eu.praxios.blocklords.service;

import eu.praxios.blocklords.model.AuditEntry;
import eu.praxios.blocklords.repository.AuditRepository;
import eu.praxios.blocklords.util.Msg;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public final class AuditService {
    private final AuditRepository repository;
    private final TaskRunner tasks;
    private final LogService logs;

    public AuditService(AuditRepository repository, TaskRunner tasks, LogService logs) {
        this.repository = repository;
        this.tasks = tasks;
        this.logs = logs;
    }

    public void log(CommandSender actor, String action, String details) {
        UUID actorId = actor instanceof Player player ? player.getUniqueId() : null;
        String id = logs.actionId("AUDIT");
        logs.action(id, action, actor.getName() + " | " + details);
        tasks.databaseVoid(() -> {
            repository.log(actorId, action, actor.getName() + ": " + details);
            return null;
        }, error -> logs.error(id, "Audit-Schreiben fehlgeschlagen: " + action, error));
    }

    public void recent(int limit, Consumer<List<AuditEntry>> callback) {
        tasks.database(() -> repository.recent(limit), entries -> {
            logs.debug("AUDIT READ " + entries.size() + " Einträge");
            callback.accept(entries);
        }, error -> logs.error("AUDIT-READ", "Audit konnte nicht gelesen werden", error));
    }

    /** Schreibt eine bekannte Probe und verlangt ihren unmittelbaren Nachweis beim Lesen. */
    public void selfTest(CommandSender sender) {
        String id = logs.actionId("AUDITTEST");
        UUID actor = sender instanceof Player player ? player.getUniqueId() : null;
        String marker = "PROBE-" + UUID.randomUUID();
        logs.action(id, "START", "Audit-Schreib-/Leseprobe");
        tasks.database(() -> {
            repository.log(actor, "AUDIT_SELFTEST", marker);
            boolean found = repository.recent(100).stream().anyMatch(entry -> entry.details().contains(marker));
            if (!found) {
                throw new IllegalStateException("Audit-Probe wurde geschrieben, aber nicht wiedergefunden: " + marker);
            }
            return marker;
        }, found -> {
            logs.action(id, "SUCCESS", found);
            Msg.success(sender, "Audit-Schreib-/Leseprobe erfolgreich: " + found + " ist sichtbar.");
        }, error -> {
            logs.error(id, "Audit-Schreib-/Leseprobe fehlgeschlagen", error);
            Msg.error(sender, "Audit-Test fehlgeschlagen: " + error.getMessage());
        });
    }
}
