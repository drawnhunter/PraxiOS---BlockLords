package eu.praxios.blocklords.model;

import java.util.UUID;

public record BuildLocation(UUID world, int x, int y, int z, int rotation) {
}
