package eu.praxios.blocklords.database;

import eu.praxios.blocklords.model.*;
import eu.praxios.blocklords.repository.ConstructionRepository;
import eu.praxios.blocklords.repository.MissionRepository;
import eu.praxios.blocklords.repository.StorageRepository;
import eu.praxios.blocklords.repository.VillageRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class MissionRepositoryTest {
    @TempDir Path temp;
    private Database database;
    private VillageRepository villages;
    private MissionRepository missions;
    private StorageRepository storage;
    private Village village;
    private final UUID owner = UUID.randomUUID();
    private final List<MissionDefinition> definitions = Arrays.stream(MissionType.values())
            .map(MissionDefinition::defaults).toList();

    @BeforeEach
    void setup() throws Exception {
        database = new Database(temp.resolve("missions.db"));
        new MigrationRunner(database, Logger.getLogger("test")).migrate();
        villages = new VillageRepository(database);
        missions = new MissionRepository(database, villages);
        storage = new StorageRepository(database, missions);
        village = villages.createVillage("Missionsdorf", owner,
                new ClaimRegion(UUID.randomUUID(), 0, 64, 0, 48), 4, 20, false, 21);
    }

    @AfterEach
    void close() throws Exception { database.close(); }

    @Test
    void offersAcceptPinReplaceAndExpiryRemainPersistent() throws Exception {
        List<MissionView> initial = prepare(owner, 1_000L);
        assertEquals(4, initial.size());
        assertEquals(3, initial.stream().filter(view -> view.mission().personal()).count());
        assertTrue(initial.stream().allMatch(view -> view.mission().status() == Mission.Status.OFFERED));
        assertEquals(0, xp());

        Mission active = accept(owner, MissionType.HOLZ_32, 1_100L);
        Mission pinned = missions.pin(village.id(), owner, active.id());
        assertTrue(pinned.pinned());
        assertEquals(active.id(), new MissionRepository(database, villages).pinned(village.id(), owner).orElseThrow().id());

        MissionView staleOffer = initial.stream().filter(view -> view.mission().type() == MissionType.STEIN_32).findFirst().orElseThrow();
        List<MissionView> refreshed = missions.prepareOffers(village.id(), owner, 3, 1, definitions,
                1_100L + 86_400_000L, 86_400_000L);
        Mission activeAfterRefresh = refreshed.stream().map(MissionView::mission)
                .filter(mission -> mission.id() == active.id()).findFirst().orElseThrow();
        assertTrue(activeAfterRefresh.pinned());
        assertEquals(Mission.Status.ACTIVE, activeAfterRefresh.status());
        assertEquals(Mission.Status.REPLACED,
                missions.accessible(village.id(), owner, staleOffer.mission().id()).orElseThrow().mission().status());

        Mission replaced = missions.replace(village.id(), owner, active.id());
        assertEquals(Mission.Status.REPLACED, replaced.status());
        assertTrue(missions.pinned(village.id(), owner).isEmpty());
        assertEquals(0, xp());
    }

    @Test
    void storageCountsOnlyActualDepositAndAwardsFirstCompletionOnce() throws Exception {
        Mission first = accept(owner, MissionType.HOLZ_32, 1_000L);
        storage.deposit(village.id(), owner, ResourceType.STEIN, 8, 1_000);
        storage.deposit(village.id(), owner, ResourceType.HOLZ, 16, 1_000);
        MissionView partial = view(first.id(), owner);
        assertEquals(16, partial.mission().progress());
        assertEquals(Mission.Status.ACTIVE, partial.mission().status());

        storage.deposit(village.id(), owner, ResourceType.HOLZ, 64, 1_000);
        Mission completed = view(first.id(), owner).mission();
        assertEquals(32, completed.progress());
        assertEquals(Mission.Status.COMPLETED, completed.status());
        assertEquals(20, xp());
        List<MissionRepository.Completion> firstNotice = missions.takeUnnotified(village.id(), 2_000L);
        assertEquals(1, firstNotice.size());
        assertEquals(20, firstNotice.getFirst().awardedXp());
        assertTrue(missions.takeUnnotified(village.id(), 2_001L).isEmpty());

        Mission repeated = accept(owner, MissionType.HOLZ_32, 3_000L);
        assertNotEquals(first.id(), repeated.id());
        storage.withdraw(village.id(), owner, ResourceType.HOLZ, 32);
        storage.deposit(village.id(), owner, ResourceType.HOLZ, 32, 1_000);
        assertEquals(Mission.Status.COMPLETED, view(repeated.id(), owner).mission().status());
        assertEquals(20, xp());
        List<MissionRepository.Completion> repeatNotice = missions.takeUnnotified(village.id(), 4_000L);
        assertEquals(1, repeatNotice.size());
        assertEquals(0, repeatNotice.getFirst().awardedXp());
    }

    @Test
    void personalFirstXpIsIndependentPerMember() throws Exception {
        UUID second = UUID.randomUUID();
        addMember(second, "BUERGER");
        Mission first = accept(owner, MissionType.HOLZ_32, 1_000L);
        Mission secondMission = accept(second, MissionType.HOLZ_32, 1_001L);

        storage.deposit(village.id(), owner, ResourceType.HOLZ, 32, 1_000);
        assertEquals(Mission.Status.COMPLETED, view(first.id(), owner).mission().status());
        assertEquals(0, view(secondMission.id(), second).mission().progress());
        storage.deposit(village.id(), second, ResourceType.HOLZ, 32, 1_000);
        assertEquals(Mission.Status.COMPLETED, view(secondMission.id(), second).mission().status());
        assertEquals(40, xp());
        assertEquals(2, missions.takeUnnotified(village.id(), 2_000L).size());
    }

    @Test
    void communityMissionAggregatesMembersButNotStrangers() throws Exception {
        UUID second = UUID.randomUUID();
        addMember(second, "BUERGER");
        Mission shared = accept(owner, MissionType.NAHRUNG_120, 1_000L);
        Mission sameShared = missions.accept(village.id(), second, shared.id(), 1_001L);
        assertEquals(shared.id(), sameShared.id());
        assertEquals(shared.id(), prepare(second, 1_002L).stream().map(MissionView::mission)
                .filter(mission -> mission.type() == MissionType.NAHRUNG_120).findFirst().orElseThrow().id());

        UUID stranger = UUID.randomUUID();
        storage.deposit(village.id(), stranger, ResourceType.NAHRUNG, 20, 1_000);
        assertEquals(0, view(shared.id(), owner).mission().progress());
        storage.deposit(village.id(), owner, ResourceType.NAHRUNG, 50, 1_000);
        assertEquals(50, view(shared.id(), second).mission().progress());
        assertEquals(0, view(shared.id(), second).ownContribution());
        storage.deposit(village.id(), second, ResourceType.NAHRUNG, 70, 1_000);
        assertEquals(Mission.Status.COMPLETED, view(shared.id(), owner).mission().status());
        assertEquals(70, view(shared.id(), second).ownContribution());
        assertEquals(40, xp());
        assertEquals(40, missions.takeUnnotified(village.id(), 2_000L).getFirst().awardedXp());

        Mission repeated = accept(second, MissionType.NAHRUNG_120, 3_000L);
        storage.deposit(village.id(), second, ResourceType.NAHRUNG, 120, 1_000);
        assertEquals(Mission.Status.COMPLETED, view(repeated.id(), second).mission().status());
        assertEquals(40, xp());
        assertEquals(0, missions.takeUnnotified(village.id(), 4_000L).getFirst().awardedXp());
    }

    @Test
    void duplicateEventsRefundsWithdrawalsAndProductionDoNotAdvanceMissions() throws Exception {
        Mission mission = accept(owner, MissionType.HOLZ_32, 1_000L);
        UUID event = UUID.randomUUID();
        database.inTransaction(connection -> {
            missions.recordStorageDeposit(connection, village.id(), owner, ResourceType.HOLZ, 16, event);
            missions.recordStorageDeposit(connection, village.id(), owner, ResourceType.HOLZ, 16, event);
            return null;
        });
        assertEquals(16, view(mission.id(), owner).mission().progress());
        storage.refund(village.id(), owner, ResourceType.HOLZ, 100, 1_000);
        storage.withdraw(village.id(), owner, ResourceType.HOLZ, 16);
        storage.applyProduction(village.id(), Map.of(ResourceType.HOLZ, 20), 0, 1_000, 2_000L, "Test");
        assertEquals(16, view(mission.id(), owner).mission().progress());
        assertEquals(Mission.Status.ACTIVE, view(mission.id(), owner).mission().status());
        assertEquals(0, xp());
        assertTrue(missions.takeUnnotified(village.id(), 2_000L).isEmpty());
    }

    @Test
    void constructionCountsBothContributionPathsOnce() throws Exception {
        ConstructionRepository construction = new ConstructionRepository(database, missions);
        Mission first = accept(owner, MissionType.BAUSTELLE_32, 1_000L);
        BuildSite site = construction.createSite(village.id(), BlueprintType.RATHAUS,
                new BuildLocation(village.region().world(), 10, 64, 10, 0), owner, null);
        construction.contribute(site.id(), owner, ResourceType.HOLZ, 16);
        assertEquals(16, view(first.id(), owner).mission().progress());
        construction.recordContribution(site.id(), owner, ResourceType.STEIN, 16);
        assertEquals(Mission.Status.COMPLETED, view(first.id(), owner).mission().status());
        assertEquals(20, xp());
        assertEquals(20, missions.takeUnnotified(village.id(), 2_000L).getFirst().awardedXp());

        Mission repeated = accept(owner, MissionType.BAUSTELLE_32, 3_000L);
        BuildSite second = construction.createSite(village.id(), BlueprintType.RATHAUS,
                new BuildLocation(village.region().world(), 30, 64, 30, 0), owner, null);
        storage.deposit(village.id(), owner, ResourceType.HOLZ, 64, 1_000);
        construction.fundFromStorage(second.id(), owner, ResourceType.HOLZ, 16);
        assertEquals(16, view(repeated.id(), owner).mission().progress());
        storage.refund(village.id(), owner, ResourceType.HOLZ, 10, 1_000);
        construction.fundFromStorage(second.id(), owner, ResourceType.HOLZ, 16);
        assertEquals(Mission.Status.COMPLETED, view(repeated.id(), owner).mission().status());
        assertEquals(20, xp());
        assertEquals(0, missions.takeUnnotified(village.id(), 4_000L).getFirst().awardedXp());
    }

    @Test
    void personalMissionsRejectForeignPlayersAndInvalidActions() throws Exception {
        Mission offered = offer(owner, MissionType.HOLZ_32).mission();
        assertThrows(IllegalArgumentException.class, () -> missions.pin(village.id(), owner, offered.id()));
        UUID stranger = UUID.randomUUID();
        assertTrue(missions.accessible(village.id(), stranger, offered.id()).isEmpty());
        assertThrows(IllegalArgumentException.class,
                () -> missions.accept(village.id(), stranger, offered.id(), 1_000L));
        Mission active = missions.accept(village.id(), owner, offered.id(), 1_000L);
        database.inTransaction(connection -> {
            missions.recordStorageDeposit(connection, village.id(), stranger, ResourceType.HOLZ, 32, UUID.randomUUID());
            return null;
        });
        assertEquals(0, view(active.id(), owner).mission().progress());
        assertTrue(missions.accessible(village.id() + 99, owner, active.id()).isEmpty());
    }

    private List<MissionView> prepare(UUID player, long now) throws Exception {
        return missions.prepareOffers(village.id(), player, 3, 1, definitions, now, 86_400_000L);
    }

    private MissionView offer(UUID player, MissionType type) throws Exception {
        return prepare(player, 1_000L).stream().filter(view -> view.mission().type() == type).findFirst().orElseThrow();
    }

    private Mission accept(UUID player, MissionType type, long now) throws Exception {
        return missions.accept(village.id(), player, offer(player, type).mission().id(), now);
    }

    private MissionView view(long missionId, UUID player) throws Exception {
        return missions.accessible(village.id(), player, missionId).orElseThrow();
    }

    private int xp() throws Exception {
        return villages.findById(village.id()).orElseThrow().xp();
    }

    private void addMember(UUID player, String role) throws Exception {
        database.inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO members(village_id, player_uuid, role, joined_at) VALUES (?, ?, ?, ?)")) {
                statement.setLong(1, village.id());
                statement.setString(2, player.toString());
                statement.setString(3, role);
                statement.setLong(4, 1L);
                statement.executeUpdate();
            }
            return null;
        });
    }
}
