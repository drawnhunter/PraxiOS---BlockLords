package eu.praxios.blocklords.service;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** Zentrale Trennung: Weltzugriffe im Main-Thread, Datenbank und Scans auf begrenzten Executors. */
public final class TaskRunner implements AutoCloseable {
    private final JavaPlugin plugin;
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "BlockLords-DB");
        thread.setDaemon(true);
        return thread;
    });
    private final ExecutorService scanExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "BlockLords-Scan");
        thread.setDaemon(true);
        return thread;
    });

    public TaskRunner(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void sync(Runnable action) {
        if (!plugin.isEnabled()) return;
        plugin.getServer().getScheduler().runTask(plugin, action);
    }

    public void syncLater(Runnable action, long delayTicks) {
        if (!plugin.isEnabled()) return;
        plugin.getServer().getScheduler().runTaskLater(plugin, action, delayTicks);
    }

    public <T> void database(Callable<T> action, Consumer<T> success, Consumer<Throwable> failure) {
        databaseExecutor.execute(() -> {
            try {
                T result = action.call();
                sync(() -> success.accept(result));
            } catch (Throwable throwable) {
                sync(() -> failure.accept(throwable));
            }
        });
    }

    public void databaseVoid(Callable<Void> action, Consumer<Throwable> failure) {
        databaseExecutor.execute(() -> {
            try {
                action.call();
            } catch (Throwable throwable) {
                sync(() -> failure.accept(throwable));
            }
        });
    }

    public <T> void scan(Callable<T> action, Consumer<T> success, Consumer<Throwable> failure) {
        scanExecutor.execute(() -> {
            try {
                T result = action.call();
                sync(() -> success.accept(result));
            } catch (Throwable throwable) {
                sync(() -> failure.accept(throwable));
            }
        });
    }

    @Override
    public void close() {
        databaseExecutor.shutdown();
        scanExecutor.shutdown();
        try {
            if (!databaseExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                databaseExecutor.shutdownNow();
            }
            if (!scanExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                scanExecutor.shutdownNow();
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            databaseExecutor.shutdownNow();
            scanExecutor.shutdownNow();
        }
    }
}
