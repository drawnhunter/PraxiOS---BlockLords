package eu.praxios.blocklords.listener;

import eu.praxios.blocklords.repository.OwnershipRepository;
import eu.praxios.blocklords.service.TaskRunner;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.ArrayList;
import java.util.List;

/** Ereignisbasierte Besitzerzuordnung ab Plugin-Installation; kein behaupteter historischer Scan. */
public final class OwnershipListener implements Listener {
    private final OwnershipRepository repository;
    private final TaskRunner tasks;

    public OwnershipListener(OwnershipRepository repository, TaskRunner tasks) {
        this.repository = repository;
        this.tasks = tasks;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (event instanceof BlockMultiPlaceEvent multi) {
            List<Block> blocks = new ArrayList<>();
            for (BlockState state : multi.getReplacedBlockStates()) {
                blocks.add(state.getBlock());
            }
            record(blocks, event);
        } else {
            record(List.of(event.getBlock()), event);
        }
    }

    private void record(List<Block> blocks, BlockPlaceEvent event) {
        List<OwnershipRepository.BlockPosition> positions = blocks.stream()
                .map(OwnershipRepository.BlockPosition::from).toList();
        tasks.databaseVoid(() -> {
            repository.recordPlacements(positions, event.getPlayer().getUniqueId());
            return null;
        }, error -> {
            throw new IllegalStateException("Blockbesitz konnte nicht gespeichert werden", error);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        tasks.databaseVoid(() -> {
            repository.recordRemoval(event.getBlock().getWorld().getUID(), event.getBlock().getX(),
                    event.getBlock().getY(), event.getBlock().getZ());
            return null;
        }, error -> {
            throw new IllegalStateException("Blockbesitz konnte nicht entfernt werden", error);
        });
    }
}
