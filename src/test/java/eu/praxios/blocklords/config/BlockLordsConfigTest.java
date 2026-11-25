package eu.praxios.blocklords.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
