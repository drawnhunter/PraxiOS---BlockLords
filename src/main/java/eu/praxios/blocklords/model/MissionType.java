package eu.praxios.blocklords.model;

public enum MissionType {
    HOLZ_32(Mission.Scope.PERSONAL, MissionAction.STORAGE_DEPOSIT, ResourceType.HOLZ, "Holz beisteuern", 32, 20),
    STEIN_32(Mission.Scope.PERSONAL, MissionAction.STORAGE_DEPOSIT, ResourceType.STEIN, "Stein beisteuern", 32, 20),
    BAUSTELLE_32(Mission.Scope.PERSONAL, MissionAction.BUILD_CONTRIBUTION, null, "Baustellenhilfe", 32, 20),
    NAHRUNG_120(Mission.Scope.DORF, MissionAction.STORAGE_DEPOSIT, ResourceType.NAHRUNG, "Versorgtes Dorf", 120, 40);

    private final Mission.Scope scope;
    private final MissionAction action;
    private final ResourceType resource;
    private final String displayName;
    private final int defaultTarget;
    private final int defaultXp;

    MissionType(Mission.Scope scope, MissionAction action, ResourceType resource, String displayName,
                int defaultTarget, int defaultXp) {
        this.scope = scope;
        this.action = action;
        this.resource = resource;
        this.displayName = displayName;
        this.defaultTarget = defaultTarget;
        this.defaultXp = defaultXp;
    }

    public Mission.Scope scope() { return scope; }
    public MissionAction action() { return action; }
    public ResourceType resource() { return resource; }
    public String displayName() { return displayName; }
    public int defaultTarget() { return defaultTarget; }
    public int defaultXp() { return defaultXp; }
}
