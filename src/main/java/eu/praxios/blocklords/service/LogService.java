package eu.praxios.blocklords.service;

import eu.praxios.blocklords.config.BlockLordsConfig;

import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Logger;

/** Technische Diagnose bleibt vom persistenten Spieler-Audit getrennt. */
public final class LogService {
    private final Logger logger;
    private final Supplier<BlockLordsConfig> config;

    public LogService(Logger logger, Supplier<BlockLordsConfig> config) {
        this.logger = logger;
        this.config = config;
    }

    public String actionId(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public void action(String id, String event, String details) {
        logger.info("[BlockLords][" + id + "] " + event + " | " + details);
    }

    public void info(String message) {
        logger.info("[BlockLords] " + message);
    }

    public void debug(String message) {
        if ("DEBUG".equals(config.get().logLevel())) {
            logger.info("[BlockLords][DEBUG] " + message);
        }
    }

    public void error(String id, String message, Throwable throwable) {
        logger.severe("[BlockLords][" + id + "] " + message);
        if (throwable != null) {
            throwable.printStackTrace();
        }
    }
}
