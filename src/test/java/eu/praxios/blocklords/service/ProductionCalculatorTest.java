package eu.praxios.blocklords.service;

import eu.praxios.blocklords.model.ResourceType;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class ProductionCalculatorTest {
    @Test
    void activeCycleProducesAndConsumes() {
        ProductionCalculator.Plan plan = ProductionCalculator.calculate(1, 1.0, 1, 1, 2, new Random(1));
        assertEquals(1.0, plan.effectiveCycles());
        assertEquals(2, plan.foodConsumed());
        assertEquals(6, plan.produced().get(ResourceType.WEIZEN));
        assertEquals(4, plan.produced().get(ResourceType.NAHRUNG));
        assertEquals(6, plan.produced().get(ResourceType.STEIN));
    }

    @Test
    void offlineRateHalvesBaseProduction() {
        ProductionCalculator.Plan plan = ProductionCalculator.calculate(1, 0.5, 1, 1, 2, new Random(1));
        assertEquals(0.5, plan.effectiveCycles());
        assertEquals(1, plan.foodConsumed());
        assertEquals(3, plan.produced().get(ResourceType.WEIZEN));
        assertEquals(2, plan.produced().get(ResourceType.NAHRUNG));
        assertEquals(3, plan.produced().get(ResourceType.STEIN));
    }

    @Test
    void missingFoodStopsEverything() {
        ProductionCalculator.Plan plan = ProductionCalculator.calculate(48, 1.0, 1, 1, 0, new Random(1));
        assertEquals(0, plan.effectiveCycles());
        assertTrue(plan.produced().isEmpty());
        assertEquals(0, plan.foodConsumed());
    }

    @Test
    void foodLimitsEffectiveCycles() {
        ProductionCalculator.Plan plan = ProductionCalculator.calculate(10, 1.0, 1, 1, 4, new Random(1));
        assertEquals(2.0, plan.effectiveCycles());
        assertEquals(4, plan.foodConsumed());
    }
}
