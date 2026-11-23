package eu.praxios.blocklords.model;

import java.util.UUID;

public record AuditEntry(long id, UUID actor, String action, String details, long createdAt) {
}
