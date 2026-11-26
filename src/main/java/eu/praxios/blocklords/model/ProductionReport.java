package eu.praxios.blocklords.model;

import java.util.List;

public record ProductionReport(long villageId, long recordedAt, long cycleAt, boolean active,
                               double effectiveCycles, int farmers, int miners, int foodConsumed,
                               List<String> pauses, List<Entry> resources) {
    public ProductionReport {
        pauses = List.copyOf(pauses);
        resources = List.copyOf(resources);
    }

    public record Entry(ResourceType resource, int planned, int accepted) {
    }
}
