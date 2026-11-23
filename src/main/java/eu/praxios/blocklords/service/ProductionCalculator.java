package eu.praxios.blocklords.service;

import eu.praxios.blocklords.model.ResourceType;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

/** Reine, deterministisch testbare Ertragsrechnung ohne Bukkit- oder DB-Zugriffe. */
public final class ProductionCalculator {
    private ProductionCalculator() {
    }

    public static Plan calculate(int cycles, double rate, int farmers, int miners, int foodAvailable, Random random) {
        int workers = farmers + miners;
        if (cycles <= 0 || rate <= 0 || workers <= 0) {
            return new Plan(0, Map.of(), 0);
        }
        double effective = cycles * rate;
        double foodLimited = foodAvailable / (double) workers;
        effective = Math.min(effective, foodLimited);
        if (effective <= 0) {
            return new Plan(0, Map.of(), 0);
        }
        int foodConsumed = Math.min(foodAvailable, (int) Math.ceil(effective * workers));
        Map<ResourceType, Integer> produced = new EnumMap<>(ResourceType.class);
        if (farmers > 0) {
            produced.put(ResourceType.WEIZEN, (int) (6 * farmers * effective));
            produced.put(ResourceType.NAHRUNG, (int) (4 * farmers * effective));
        }
        if (miners > 0) {
            produced.put(ResourceType.STEIN, (int) (6 * miners * effective));
            int coal = 0;
            int emeralds = 0;
            for (int cycle = 0; cycle < cycles; cycle++) {
                for (int miner = 0; miner < miners; miner++) {
                    if (random.nextDouble() < 0.50 * rate) coal++;
                    if (random.nextDouble() < 0.25 * rate) emeralds++;
                }
            }
            produced.put(ResourceType.KOHLE, coal);
            produced.put(ResourceType.SMARAGD, emeralds);
        }
        return new Plan(effective, produced, foodConsumed);
    }

    public record Plan(double effectiveCycles, Map<ResourceType, Integer> produced, int foodConsumed) {
    }
}
