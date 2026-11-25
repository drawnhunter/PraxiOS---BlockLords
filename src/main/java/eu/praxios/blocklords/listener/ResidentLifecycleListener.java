package eu.praxios.blocklords.listener;

import eu.praxios.blocklords.service.ResidentService;
import eu.praxios.blocklords.service.WorkerService;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public final class ResidentLifecycleListener implements Listener {
    private final ResidentService residents;
    private final WorkerService workers;

    public ResidentLifecycleListener(ResidentService residents, WorkerService workers) {
        this.residents = residents;
        this.workers = workers;
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Villager villager)) return;
        workers.removeVillager(villager.getUniqueId());
        residents.markDead(villager.getUniqueId());
    }
}
