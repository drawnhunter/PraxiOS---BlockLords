package eu.praxios.blocklords.service;

import eu.praxios.blocklords.config.BlockLordsConfig;
import eu.praxios.blocklords.model.Mission;
import eu.praxios.blocklords.model.MissionDefinition;
import eu.praxios.blocklords.model.MissionView;
import eu.praxios.blocklords.model.Role;
import eu.praxios.blocklords.model.Village;
import eu.praxios.blocklords.repository.MissionRepository;
import eu.praxios.blocklords.util.Msg;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class MissionService {
    private final Supplier<BlockLordsConfig> config;
    private final MissionRepository repository;
    private final VillageService villages;
    private final TaskRunner tasks;
    private final AuditService audit;

    public MissionService(Supplier<BlockLordsConfig> config, MissionRepository repository, VillageService villages,
                          TaskRunner tasks, AuditService audit) {
        this.config = config;
        this.repository = repository;
        this.villages = villages;
        this.tasks = tasks;
        this.audit = audit;
    }

    public void list(Player player, Village village, Consumer<List<MissionView>> callback) {
        if (!canParticipate(player, village)) return;
        List<MissionDefinition> definitions = new ArrayList<>(config.get().missionDefinitions());
        Collections.shuffle(definitions);
        tasks.database(() -> repository.prepareOffers(village.id(), player.getUniqueId(),
                        config.get().missionPersonalOffers(), config.get().missionVillageOffers(), definitions,
                        System.currentTimeMillis(), config.get().missionOfferRefreshMillis()),
                callback, error -> fail(player, "Missionen konnten nicht geladen werden", error));
    }

    public void details(Player player, Village village, long missionId, Consumer<MissionView> callback) {
        if (!canParticipate(player, village)) return;
        tasks.database(() -> repository.accessible(village.id(), player.getUniqueId(), missionId), view -> {
            if (view.isEmpty()) {
                Msg.error(player, "Diese Mission gehört nicht zu deinem Dorf oder nicht mehr zu deinen Angeboten.");
                return;
            }
            callback.accept(view.get());
        }, error -> fail(player, "Mission konnte nicht gelesen werden", error));
    }

    public void accept(Player player, Village village, long missionId, Runnable reopened) {
        if (!canParticipate(player, village)) return;
        tasks.database(() -> repository.accept(village.id(), player.getUniqueId(), missionId, System.currentTimeMillis()), mission -> {
            audit.log(player, "MISSION_ACCEPTED", "Mission #" + mission.id() + " " + mission.type());
            Msg.success(player, "Mission angenommen: " + mission.title() + " · 0/" + mission.target() + ".");
            reopened.run();
        }, error -> fail(player, "Mission konnte nicht angenommen werden", error));
    }

    public void replace(Player player, Village village, long missionId, boolean confirmed, Runnable reopened) {
        if (!canParticipate(player, village)) return;
        tasks.database(() -> repository.accessible(village.id(), player.getUniqueId(), missionId), found -> {
            if (found.isEmpty()) {
                Msg.error(player, "Diese Mission gehört nicht zu deinem Dorf.");
                return;
            }
            MissionView view = found.get();
            Mission mission = view.mission();
            if (!mission.personal() && !canManage(player, village)) return;
            if (mission.progress() > 0 && !confirmed) {
                Msg.error(player, "Der Fortschritt " + mission.progress() + "/" + mission.target()
                        + " wird verworfen. Bestätige mit /bl missions replace " + missionId + " confirm.");
                return;
            }
            tasks.database(() -> repository.replace(village.id(), player.getUniqueId(), missionId), replaced -> {
                audit.log(player, "MISSION_REPLACED", "Mission #" + replaced.id() + " " + replaced.type()
                        + " · bisheriger Fortschritt " + mission.progress());
                Msg.success(player, "Mission kostenlos ersetzt. Deine Ressourcen bleiben unverändert.");
                reopened.run();
            }, error -> fail(player, "Mission konnte nicht ersetzt werden", error));
        }, error -> fail(player, "Mission konnte nicht geprüft werden", error));
    }

    public void pin(Player player, Village village, long missionId, Runnable reopened) {
        if (!canParticipate(player, village)) return;
        tasks.database(() -> repository.pin(village.id(), player.getUniqueId(), missionId), mission -> {
            Msg.success(player, "Angepinnt: " + mission.title() + " · " + mission.progress() + "/" + mission.target() + ".");
            reopened.run();
        }, error -> fail(player, "Mission konnte nicht angepinnt werden", error));
    }

    public void sendPinnedHint(Player player) {
        Village village = villages.at(player.getLocation()).orElseGet(() -> villages.owned(player.getUniqueId())
                .stream().findFirst().orElse(null));
        if (village == null || !canParticipate(player.getUniqueId(), village.id())) return;
        tasks.database(() -> repository.pinned(village.id(), player.getUniqueId()), pinned -> pinned.ifPresent(mission ->
                Msg.info(player, "Weiter mit '" + mission.title() + "': " + mission.progress() + "/" + mission.target()
                        + " · /bl missions")), error -> fail(player, "Angepinnte Mission konnte nicht gelesen werden", error));
    }

    public void announceCompletions(long villageId) {
        tasks.database(() -> repository.takeUnnotified(villageId, System.currentTimeMillis()), completions -> {
            for (MissionRepository.Completion completion : completions) {
                Mission mission = completion.mission();
                Village updated = completion.village();
                Village previous = villages.byId(villageId).orElse(updated);
                villages.updateVillage(updated);
                List<Player> recipients = recipients(mission);
                for (Player recipient : recipients) {
                    audit.log(recipient, "MISSION_COMPLETED", "Mission #" + mission.id() + " " + mission.type());
                    Msg.success(recipient, "Mission abgeschlossen: " + mission.title() + ".");
                    if (completion.awardedXp() > 0) {
                        audit.log(recipient, "MISSION_XP_AWARDED", "+" + completion.awardedXp() + " Dorf-XP · Mission #" + mission.id());
                        Msg.success(recipient, "+" + completion.awardedXp() + " Dorf-XP · Gesamt " + updated.xp()
                                + " · Level " + updated.level() + ".");
                    } else {
                        Msg.info(recipient, "Dieser Missionstyp wurde bereits belohnt; diesmal gibt es keine weiteren Dorf-XP.");
                    }
                    showProgressFeedback(recipient, previous, updated, completion.awardedXp());
                }
            }
        }, error -> {
            throw new IllegalStateException("Missionsabschluss konnte nicht gemeldet werden: " + error.getMessage(), error);
        });
    }

    public boolean canManage(Player player, Village village) {
        Role role = villages.role(village.id(), player.getUniqueId());
        return role == Role.LANDHERR || role == Role.GEFOLGSMANN;
    }

    private boolean canParticipate(Player player, Village village) {
        if (canParticipate(player.getUniqueId(), village.id())) return true;
        Msg.error(player, "Missionen sind für Landherr, Gefolgsmann und Bürger dieses Dorfes.");
        return false;
    }

    private boolean canParticipate(java.util.UUID playerId, long villageId) {
        Role role = villages.role(villageId, playerId);
        return role == Role.LANDHERR || role == Role.GEFOLGSMANN || role == Role.BUERGER;
    }

    private List<Player> recipients(Mission mission) {
        if (mission.personal()) {
            Player player = Bukkit.getPlayer(mission.playerId());
            return player == null ? List.of() : List.of(player);
        }
        return villages.membersOf(mission.villageId()).keySet().stream().map(Bukkit::getPlayer)
                .filter(Objects::nonNull).toList();
    }

    private void showProgressFeedback(Player player, Village previous, Village updated, int gainedXp) {
        if (updated.level() > previous.level()) {
            player.showTitle(Title.title(Component.text("Dorf-Level " + updated.level(), NamedTextColor.GOLD),
                    Component.text("Mission abgeschlossen · +" + gainedXp + " Dorf-XP", NamedTextColor.YELLOW),
                    Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(1000))));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        } else if (gainedXp > 0) {
            player.sendActionBar(Component.text("+" + gainedXp + " Dorf-XP · Mission abgeschlossen", NamedTextColor.GOLD));
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.2f);
        }
    }

    private void fail(Player player, String action, Throwable error) {
        Msg.error(player, action + ": " + error.getMessage());
    }
}
