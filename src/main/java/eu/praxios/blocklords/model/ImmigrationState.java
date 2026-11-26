package eu.praxios.blocklords.model;

public record ImmigrationState(long villageId, long nextArrivalAt, String reason) {
}
