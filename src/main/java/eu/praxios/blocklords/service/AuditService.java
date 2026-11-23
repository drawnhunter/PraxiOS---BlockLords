package eu.praxios.blocklords.service;

import eu.praxios.blocklords.model.AuditEntry;
import eu.praxios.blocklords.repository.AuditRepository;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public final class AuditService {
    private final AuditRepository repository;
    private final TaskRunner tasks;

    public AuditService(AuditRepository repository, TaskRunner tasks) {
        this.repository = repository;
        this.tasks = tasks;
    }

    public void log(CommandSender actor, String action, String details) {
        UUID actorId = actor instanceof Player player ? player.getUniqueId() : null;
        tasks.databaseVoid(() -> {
            repository.log(actorId, action, actor.getName() + ": " + details);
            return null;
        }, error -> {
            throw new IllegalStateException("Audit konnte nicht geschrieben werden", error);
        });
    }

    public void recent(int limit, Consumer<List<AuditEntry>> callback) {
        tasks.database(() -> repository.recent(limit), callback, error -> {
            throw new IllegalStateException("Audit konnte nicht gelesen werden", error);
        });
    }
}
