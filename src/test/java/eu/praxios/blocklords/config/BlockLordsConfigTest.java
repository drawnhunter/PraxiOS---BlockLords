package eu.praxios.blocklords.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockLordsConfigTest {
    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void missingWorkerLimitUsesTwoForExistingConfigs() throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.load(new File("src/main/resources/config.yml"));
        yaml.set("workers.per-building", null);
        assertEquals(2, new BlockLordsConfig(yaml).workersPerBuilding());
    }

    @Test
    void invalidWorkerLimitIsRejected() throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.load(new File("src/main/resources/config.yml"));
        yaml.set("workers.per-building", 0);
        assertThrows(IllegalArgumentException.class, () -> new BlockLordsConfig(yaml));
    }

    @Test
    void missingMissionSectionUsesDefaultsForExistingConfigs() throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.load(new File("src/main/resources/config.yml"));
        yaml.set("missions", null);
        BlockLordsConfig config = new BlockLordsConfig(yaml);
        assertEquals(2, config.missionPersonalOffers());
        assertEquals(1, config.missionVillageOffers());
        assertEquals(86_400_000L, config.missionOfferRefreshMillis());
        assertEquals(4, config.missionDefinitions().size());
        assertTrue(config.missionDefinitions().stream().allMatch(definition ->
                definition.target() == definition.type().defaultTarget() && definition.xp() == definition.type().defaultXp()));
    }

    @Test
    void invalidMissionValuesAreRejected() throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.load(new File("src/main/resources/config.yml"));
        yaml.set("missions.targets.HOLZ_32", 0);
        assertThrows(IllegalArgumentException.class, () -> new BlockLordsConfig(yaml));
        yaml.set("missions.targets.HOLZ_32", 32);
        yaml.set("missions.xp.HOLZ_32", -1);
        assertThrows(IllegalArgumentException.class, () -> new BlockLordsConfig(yaml));
        yaml.set("missions.xp.HOLZ_32", 20);
        yaml.set("missions.personal-offers", 0);
        assertThrows(IllegalArgumentException.class, () -> new BlockLordsConfig(yaml));
    }
}
