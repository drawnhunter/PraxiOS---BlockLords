package eu.praxios.blocklords.service;

import eu.praxios.blocklords.config.BlockLordsConfig;
import eu.praxios.blocklords.model.ClaimRegion;
import eu.praxios.blocklords.model.QuestState;
import eu.praxios.blocklords.model.Village;
import eu.praxios.blocklords.repository.ClaimException;
import eu.praxios.blocklords.repository.QuestRepository;
import eu.praxios.blocklords.repository.VillageRepository;
import eu.praxios.blocklords.util.Msg;
import eu.praxios.blocklords.util.VillageNames;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class FoundingService {
    private static final Set<Material> WORKSTATIONS = EnumSet.of(Material.LECTERN, Material.SMITHING_TABLE,
            Material.FLETCHING_TABLE, Material.CARTOGRAPHY_TABLE, Material.LOOM, Material.STONECUTTER,
            Material.GRINDSTONE, Material.BLAST_FURNACE, Material.SMOKER, Material.BARREL, Material.BREWING_STAND,
            Material.CAULDRON, Material.WATER_CAULDRON, Material.LAVA_CAULDRON, Material.POWDER_SNOW_CAULDRON);

    private final Supplier<BlockLordsConfig> config;
    private final VillageRepository villageRepository;
    private final QuestRepository questRepository;
    private final VillageService villages;
    private final TaskRunner tasks;
    private final AuditService audit;
    private final Map<UUID, QuestState> quests = new ConcurrentHashMap<>();
    private final Set<UUID> pendingClaims = ConcurrentHashMap.newKeySet();

    public FoundingService(Supplier<BlockLordsConfig> config, VillageRepository villageRepository,
                           QuestRepository questRepository, VillageService villages, TaskRunner tasks, AuditService audit) {
        this.config = config;
        this.villageRepository = villageRepository;
        this.questRepository = questRepository;
        this.villages = villages;
        this.tasks = tasks;
        this.audit = audit;
    }

    public void loadPlayer(Player player) {
        tasks.database(() -> questRepository.find(player.getUniqueId()), state -> quests.put(player.getUniqueId(), state),
                error -> Msg.error(player, "Dein Gründungsstand konnte nicht geladen werden."));
    }

    public void unloadPlayer(Player player) {
        quests.remove(player.getUniqueId());
    }

    public QuestState quest(Player player) {
        return quests.computeIfAbsent(player.getUniqueId(), id -> QuestState.fresh(id, System.currentTimeMillis()));
    }

    public void startQuest(Player player, Block bell) {
        if (bell.getType() != Material.BELL) {
            Msg.error(player, "Zum Starten brauchst du eine Dorfglocke.");
            return;
        }
        if (!villages.owned(player.getUniqueId()).isEmpty()) {
            Msg.error(player, "Du bist bereits Landherr. Die Gründungsquest ist nur für deine erste Gründung nötig.");
            return;
        }
        if (quest(player).founderCompleted()) {
            Msg.info(player, "Deine Gründungsquest ist bereits abgeschlossen. Du kannst direkt einen Dorfnamen wählen.");
            return;
        }
        ClaimRegion region = new ClaimRegion(bell.getWorld().getUID(), bell.getX(), bell.getY(), bell.getZ(), 1);
        save(new QuestState(player.getUniqueId(), QuestState.Stage.SURVEY, region, 0,
                quest(player).founderCompleted(), System.currentTimeMillis()));
        audit.log(player, "QUEST_START", "Glocke " + coordinates(bell));
        Msg.info(player, "Gründungsquest: Finde im Dorf ein Bett, eine Nahrungsquelle und einen Arbeitsplatz.");
        Msg.plain(player, "Klicke dazu ein Bett, Ackerland/Getreide/Komposter und einen Villager-Arbeitsblock an.");
    }

    /** @return true, wenn die Interaktion zur Gründungsquest gehörte. */
    public boolean handleSurveyInteraction(Player player, Block block) {
        QuestState state = quest(player);
        if (state.stage() != QuestState.Stage.SURVEY || state.bell() == null) return false;
        if (!state.bell().world().equals(block.getWorld().getUID())) return false;
        int dx = block.getX() - state.bell().bellX();
        int dz = block.getZ() - state.bell().bellZ();
        if (dx * dx + dz * dz > (double) config.get().radius() * config.get().radius()) return false;

        int flag = flagFor(block.getType());
        if (flag == 0) return false;
        if (state.has(flag)) {
            Msg.info(player, "Diesen Questschritt hast du bereits erledigt.");
            return true;
        }
        int progress = state.progress() | flag;
        boolean complete = progress == QuestState.ALL;
        QuestState updated = new QuestState(player.getUniqueId(), complete ? QuestState.Stage.READY : QuestState.Stage.SURVEY,
                state.bell(), progress, complete || state.founderCompleted(), System.currentTimeMillis());
        save(updated);
        audit.log(player, "QUEST_PROGRESS", flagName(flag) + " bei " + coordinates(block));
        if (complete) {
            if (!state.founderCompleted()) {
                giveStarterItems(player);
            }
            Msg.success(player, "Gründungsquest abgeschlossen! Das Gründungspaket liegt in deinem Inventar oder zu deinen Füßen.");
            Msg.info(player, "Klicke die Glocke erneut und wähle einen Dorfnamen.");
        } else {
            Msg.success(player, flagName(flag) + " bestätigt. Questfortschritt: " + Integer.bitCount(progress) + "/3.");
        }
        return true;
    }

    public String questInfo(Player player) {
        QuestState state = quest(player);
        return switch (state.stage()) {
            case NOT_STARTED -> "Noch keine Gründungsquest aktiv. Rechtsklick auf eine unbeanspruchte Dorfglocke.";
            case SURVEY -> "Dorferkundung: Bett " + mark(state.has(QuestState.BED)) + ", Nahrung "
                    + mark(state.has(QuestState.FOOD)) + ", Arbeitsplatz " + mark(state.has(QuestState.WORK)) + ".";
            case READY -> "Bereit zur Gründung. Wähle an der Glocke einen Namen oder nutze /bl found <Name>.";
        };
    }

    public void found(Player player, Block bell, String rawName, Consumer<Village> success) {
        if (pendingClaims.contains(player.getUniqueId())) {
            Msg.error(player, "Eine Gründung läuft bereits. Bitte warte einen Moment.");
            return;
        }
        if (bell.getType() != Material.BELL) {
            Msg.error(player, "Du musst die Dorfglocke direkt anvisieren.");
            return;
        }
        if (villages.atBell(bell).isPresent()) {
            Msg.error(player, "Diese Glocke gehört bereits zu einem BlockLords-Dorf.");
            return;
        }
        String name = VillageNames.normalize(rawName);
        String invalid = VillageNames.validate(name);
        if (invalid != null) {
            Msg.error(player, invalid);
            return;
        }
        QuestState state = quest(player);
        if (config.get().requireQuest() && !state.founderCompleted()) {
            Msg.error(player, "Schließe zuerst die kurze Gründungsquest ab: " + questInfo(player));
            return;
        }

        boolean first = villages.owned(player.getUniqueId()).isEmpty();
        if (!first && !config.get().multiClaimEnabled()) {
            Msg.error(player, "Weitere Dörfer werden mit den Wirtschafts- und Herrschaftssystemen der späteren Phasen freigeschaltet.");
            return;
        }
        int cost = first ? 0 : config.get().nextVillageCost();
        if (cost > 0 && !player.getInventory().contains(Material.EMERALD, cost)) {
            Msg.error(player, "Für dieses weitere Dorf benötigst du " + cost + " Smaragde im Inventar.");
            return;
        }

        pendingClaims.add(player.getUniqueId());
        if (cost > 0) {
            player.getInventory().removeItem(new ItemStack(Material.EMERALD, cost));
        }
        ClaimRegion region = new ClaimRegion(bell.getWorld().getUID(), bell.getX(), bell.getY(), bell.getZ(), config.get().radius());
        tasks.database(() -> villageRepository.createVillage(name, player.getUniqueId(), region,
                        config.get().maxPerPlayer(), config.get().maxPerWorld(), config.get().multiClaimEnabled(),
                        config.get().decayEnabled() ? config.get().decayBlockDays() : 0), village -> {
                    pendingClaims.remove(player.getUniqueId());
                    villages.addVillage(village);
                    if (first && !state.founderCompleted()) {
                        giveStarterItems(player);
                    }
                    save(new QuestState(player.getUniqueId(), QuestState.Stage.READY, state.bell(), state.progress(), true,
                            System.currentTimeMillis()));
                    audit.log(player, "VILLAGE_FOUNDED", village.name() + " an Glocke " + coordinates(bell));
                    Msg.success(player, "Dorf '" + village.name() + "' gegründet. Du bist jetzt Landherr.");
                    Msg.info(player, "Die Rathaus-Basis öffnest du über die Dorfglocke oder /bl info.");
                    success.accept(village);
                }, error -> {
                    pendingClaims.remove(player.getUniqueId());
                    if (cost > 0) {
                        giveItems(player, new ItemStack(Material.EMERALD, cost));
                    }
                    if (error instanceof ClaimException claimException) {
                        Msg.error(player, claimException.getMessage());
                    } else {
                        Msg.error(player, "Die Gründung ist technisch fehlgeschlagen. Smaragde wurden zurückerstattet.");
                        throw new IllegalStateException("Dorfgründung fehlgeschlagen", error);
                    }
                });
    }

    private int flagFor(Material material) {
        if (Tag.BEDS.isTagged(material)) return QuestState.BED;
        if (material == Material.FARMLAND || material == Material.WHEAT || material == Material.COMPOSTER) return QuestState.FOOD;
        if (WORKSTATIONS.contains(material)) return QuestState.WORK;
        return 0;
    }

    private String flagName(int flag) {
        return switch (flag) {
            case QuestState.BED -> "Wohnraum";
            case QuestState.FOOD -> "Nahrungsquelle";
            case QuestState.WORK -> "Arbeitsplatz";
            default -> "Dorfpunkt";
        };
    }

    private String mark(boolean done) {
        return done ? "✓" : "✗";
    }

    private void giveStarterItems(Player player) {
        for (ItemStack item : config.get().starterItems()) {
            giveItems(player, item);
        }
        audit.log(player, "STARTER_PACKAGE", "Gründungspaket ausgegeben");
    }

    private void giveItems(Player player, ItemStack item) {
        Map<Integer, ItemStack> remaining = player.getInventory().addItem(item);
        remaining.values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }

    private void save(QuestState state) {
        quests.put(state.player(), state);
        tasks.databaseVoid(() -> {
            questRepository.save(state);
            return null;
        }, error -> {
            throw new IllegalStateException("Gründungsquest konnte nicht gespeichert werden", error);
        });
    }

    private static String coordinates(Block block) {
        return block.getWorld().getName() + " " + block.getX() + "/" + block.getY() + "/" + block.getZ();
    }
}
