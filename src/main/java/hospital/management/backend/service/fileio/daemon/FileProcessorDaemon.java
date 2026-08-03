package hospital.management.backend.service.fileio.daemon;

import hospital.management.backend.service.fileio.FileImportService;
import hospital.management.backend.service.fileio.FileProcessingException;
import hospital.management.backend.service.log.ServiceMongoLogger;

import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Simple daemon executor to run file processors in background without blocking UI.
 */
public class FileProcessorDaemon {

    private static final java.util.concurrent.ExecutorService EXECUTOR = Executors.newCachedThreadPool(new ThreadFactory() {
        @Override public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "file-processor-worker");
            t.setDaemon(true);
            return t;
        }
    });

    public static void submitImport(FileImportService service, Path path, java.util.function.Consumer<java.util.Map<String,String>> consumer) {
        ServiceMongoLogger.info("fileio.daemon", "Queued import job: " + path + " via " + service.getClass().getSimpleName());
        EXECUTOR.submit(() -> {
            try {
                service.importFile(path, consumer);
                ServiceMongoLogger.info("fileio.daemon", "Import job completed: " + path);
            } catch (Exception e) {
                ServiceMongoLogger.error("fileio.daemon", "Import job failed: " + path, e);
                throw new FileProcessingException("Import failed: " + e.getMessage(), e);
            }
        });
    }

    public static void shutdown() {
        try {
            EXECUTOR.shutdown();
            EXECUTOR.awaitTermination(3, TimeUnit.SECONDS);
            ServiceMongoLogger.info("fileio.daemon", "File processor daemon shutdown completed.");
        } catch (InterruptedException ignored) {}
    }
}
