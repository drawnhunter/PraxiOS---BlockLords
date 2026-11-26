package eu.praxios.blocklords.database;

import eu.praxios.blocklords.model.ClaimRegion;
import eu.praxios.blocklords.model.ProductionReport;
import eu.praxios.blocklords.model.ResourceType;
import eu.praxios.blocklords.model.Village;
import eu.praxios.blocklords.repository.ProductionReportRepository;
import eu.praxios.blocklords.repository.VillageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class ProductionReportRepositoryTest {
    @TempDir Path temp;

    @Test
    void pauseWithoutResourcesCanBeStoredAndRead() throws Exception {
        try (Database database = new Database(temp.resolve("production-report.db"))) {
            new MigrationRunner(database, Logger.getLogger("test")).migrate();
            Village village = new VillageRepository(database).createVillage("Berichtsdorf", UUID.randomUUID(),
                    new ClaimRegion(UUID.randomUUID(), 0, 64, 0, 48), 4, 20, false, 21);
            ProductionReportRepository repository = new ProductionReportRepository(database);
            repository.save(new ProductionReport(village.id(), 100L, 100L, false, 0, 1, 0, 0,
                    List.of("Dorf ruht"), List.of()));
            ProductionReport report = repository.find(village.id()).orElseThrow();
            assertEquals(List.of("Dorf ruht"), report.pauses());
            assertTrue(report.resources().isEmpty());

            repository.save(new ProductionReport(village.id(), 200L, 200L, true, 1.0, 1, 0, 1,
                    List.of(), List.of(new ProductionReport.Entry(ResourceType.WEIZEN, 6, 4))));
            ProductionReport updated = repository.find(village.id()).orElseThrow();
            assertTrue(updated.pauses().isEmpty());
            assertEquals(1, updated.resources().size());
            assertEquals(4, updated.resources().getFirst().accepted());
        }
    }
}
