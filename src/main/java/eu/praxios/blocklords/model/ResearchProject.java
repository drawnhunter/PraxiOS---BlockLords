package eu.praxios.blocklords.model;

public record ResearchProject(long id, long villageId, String key, Status status,
                              long startedAt, long finishesAt, Long completedAt) {
    public enum Status { RUNNING, COMPLETED, CANCELLED }
}
