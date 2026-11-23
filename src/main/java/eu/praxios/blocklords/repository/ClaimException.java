package eu.praxios.blocklords.repository;

public final class ClaimException extends Exception {
    public enum Reason {
        NAME_TAKEN,
        BELL_CLAIMED,
        OWNER_LIMIT,
        WORLD_LIMIT,
        MULTI_DISABLED,
        PROGRESS_MISSING,
        ALT_CONFLICT,
        OVERLAP
    }

    private final Reason reason;

    public ClaimException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
