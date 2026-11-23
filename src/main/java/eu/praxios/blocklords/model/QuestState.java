package eu.praxios.blocklords.model;

import java.util.UUID;

public record QuestState(
        UUID player,
        Stage stage,
        ClaimRegion bell,
        int progress,
        boolean founderCompleted,
        long updatedAt) {

    public static final int BED = 1;
    public static final int FOOD = 2;
    public static final int WORK = 4;
    public static final int ALL = BED | FOOD | WORK;

    public enum Stage {
        NOT_STARTED,
        SURVEY,
        READY
    }

    public static QuestState fresh(UUID player, long now) {
        return new QuestState(player, Stage.NOT_STARTED, null, 0, false, now);
    }

    public boolean has(int flag) {
        return (progress & flag) != 0;
    }
}
