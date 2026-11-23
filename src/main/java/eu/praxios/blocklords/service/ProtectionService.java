package eu.praxios.blocklords.service;

import eu.praxios.blocklords.model.LegacyPlot;
import eu.praxios.blocklords.model.Role;
import eu.praxios.blocklords.model.Village;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class ProtectionService {
    private final VillageService villages;
    private final AuditService audit;
    private final Set<UUID> bypass = new HashSet<>();

    public ProtectionService(VillageService villages, AuditService audit) {
        this.villages = villages;
        this.audit = audit;
    }

    public boolean canBuild(Player player, Location location) {
        return allowed(player, location, true);
    }

    public boolean canInteract(Player player, Location location) {
        return allowed(player, location, false);
    }

    public boolean canOpenContainer(Player player, Location location) {
        return allowed(player, location, false);
    }

    private boolean allowed(Player player, Location location, boolean build) {
        if (hasBypass(player)) return true;
        Optional<LegacyPlot> legacy = villages.legacyAt(location);
        if (legacy.isPresent()) {
            return legacy.get().owner() != null && legacy.get().owner().equals(player.getUniqueId());
        }
        Optional<Village> village = villages.at(location);
        if (village.isEmpty()) return true;
        // Weitere delegierte Bereiche und Rollen folgen mit Phase 2, nicht als versteckter Vollzugriff.
        return villages.role(village.get().id(), player.getUniqueId()) == Role.LANDHERR;
    }

    public boolean isProtected(Location location) {
        return villages.at(location).isPresent() || villages.legacyAt(location).isPresent();
    }

    public boolean sameZone(Location first, Location second) {
        return zone(first).equals(zone(second));
    }

    private Zone zone(Location location) {
        Optional<LegacyPlot> legacy = villages.legacyAt(location);
        if (legacy.isPresent()) return new Zone("LEGACY", legacy.get().id());
        Optional<Village> village = villages.at(location);
        if (village.isPresent()) return new Zone("VILLAGE", village.get().id());
        if (location.getWorld() == null) return new Zone("NONE", -1L);
        return new Zone("WILDERNESS", location.getWorld().getUID().getLeastSignificantBits());
    }

    public void deny(Player player, String reason) {
        player.sendActionBar(Component.text(reason, NamedTextColor.RED));
    }

    public boolean toggleBypass(Player player) {
        if (!player.hasPermission("blocklords.admin.bypass")) return false;
        boolean enabled;
        if (bypass.contains(player.getUniqueId())) {
            bypass.remove(player.getUniqueId());
            enabled = false;
        } else {
            bypass.add(player.getUniqueId());
            enabled = true;
        }
        audit.log(player, "ADMIN_BYPASS", enabled ? "aktiviert" : "deaktiviert");
        return enabled;
    }

    public boolean hasBypass(Player player) {
        return player.hasPermission("blocklords.admin.bypass") && bypass.contains(player.getUniqueId());
    }

    private record Zone(String type, long id) {
    }
}
