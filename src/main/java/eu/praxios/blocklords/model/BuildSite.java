package eu.praxios.blocklords.model;

import java.util.Map;
import java.util.UUID;

public record BuildSite(long id, long villageId, BlueprintType type, BuildLocation location,
                        Status status, UUID createdBy, long createdAt,
                        Map<ResourceType, Integer> required, Map<ResourceType, Integer> contributed,
                        BlockLink chest) {
    public enum Status { COLLECTING, READY, BUILDING }

    public int remaining(ResourceType resource) {
        return Math.max(0, required.getOrDefault(resource, 0) - contributed.getOrDefault(resource, 0));
    }

    public boolean complete() {
        return required.keySet().stream().allMatch(resource -> remaining(resource) == 0);
    }
}
