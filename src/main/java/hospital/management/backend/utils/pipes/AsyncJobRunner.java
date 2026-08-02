package hospital.management.backend.utils.pipes;

import hospital.management.backend.config.AppLogger;
import hospital.management.backend.utils.listeners.AppEventType;
import hospital.management.backend.utils.listeners.EventBus;
import javafx.application.Platform;
import javafx.concurrent.Task;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
 * Submits long-running jobs to a background thread pool so the JavaFX UI
 * stays responsive. Results and progress are delivered back on the FX thread.
 *
 * The pool is a daemon thread pool — threads are killed automatically when
 * the JVM exits, so no explicit shutdown is needed in a desktop app.
 *
 * Usage — simple async call:
 *   AsyncJobRunner.submit(() -> patientService.getAll(req), result -> table.refresh());
 *
 * Usage — data cleaning with progress:
 *   AsyncJobRunner.clean(
 *       dirtyRows,
 *       row  -> pipeline.run(row),
 *       done -> EventBus.publish(AppEventType.DATA_CLEANING_COMPLETED, done)
 *   );
 */
public final class AsyncJobRunner {

    private static final AppLogger logger = AppLogger.getLogger(AsyncJobRunner.class);

    private static final ExecutorService POOL =
        Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() - 1),
            r -> {
                Thread t = new Thread(r, "hms-job-pool");
                t.setDaemon(true);
                return t;
            });

    private AsyncJobRunner() {}

    // ── Simple async submit ───────────────────────────────────────────────────

    /**
     * Runs {@code job} on a background thread. On success, {@code onSuccess} is
     * called on the JavaFX Application Thread with the result.
     * Exceptions are logged; the UI is not notified — use the 3-arg overload
     * below if the caller needs to react to a failure (e.g. show an error message).
     *
     * @param job       work to do off the FX thread
     * @param onSuccess callback invoked on the FX thread with the result
     */
    public static <T> Future<?> submit(java.util.concurrent.Callable<T> job,
                                       Consumer<T> onSuccess) {
        return submit(job, onSuccess, e -> logger.error("Async job failed: " + e.getMessage(), e));
    }

    /**
     * Same as {@link #submit(java.util.concurrent.Callable, Consumer)}, but also
     * notifies the caller of a failure — the callback runs on the FX thread so it
     * can safely update UI (e.g. show a validation/error label).
     *
     * @param job       work to do off the FX thread
     * @param onSuccess callback invoked on the FX thread with the result
     * @param onError   callback invoked on the FX thread with the thrown exception
     */
    public static <T> Future<?> submit(java.util.concurrent.Callable<T> job,
                                       Consumer<T> onSuccess,
                                       Consumer<Throwable> onError) {
        return POOL.submit(() -> {
            try {
                T result = job.call();
                // onSuccess itself touches JavaFX nodes (setOptions, selectById, etc.) — if it
                // throws, it must still route to onError instead of silently vanishing inside
                // Platform.runLater's own uncaught-exception handling, which would otherwise
                // leave a caller's loading spinner/disabled fields stuck forever with no feedback.
                Platform.runLater(() -> {
                    try {
                        onSuccess.accept(result);
                    } catch (Throwable t) {
                        logger.error("Async job's onSuccess callback failed: " + t.getMessage(), t);
                        onError.accept(t);
                    }
                });
            } catch (Throwable e) {
                // Catches Throwable, not just Exception — an Error (e.g. thrown from a
                // misbehaving mapper) must still reach onError rather than disappearing into
                // the executor, which never surfaces exceptions unless something calls Future.get().
                logger.error("Async job failed: " + e.getMessage(), e);
                Platform.runLater(() -> {
                    try {
                        onError.accept(e);
                    } catch (Throwable t) {
                        logger.error("Async job's onError callback failed: " + t.getMessage(), t);
                    }
                });
            }
        });
    }

    // ── Batch data cleaning ───────────────────────────────────────────────────

    /**
     * Processes a list of items in batches on a background thread, reporting
     * progress through the EventBus (DATA_CLEANING_PROGRESS with Integer 0–100).
     *
     * Items that fail are logged and skipped. When finished, DATA_CLEANING_COMPLETED
     * is published with the list of successfully cleaned items.
     *
     * @param items     the raw data to clean
     * @param pipe      transformation applied to each item (may throw — item is skipped)
     * @param onDone    called on the FX thread with the cleaned list when finished
     */
    public static <T, R> void clean(List<T> items,
                                    DataPipe<T, R> pipe,
                                    Consumer<List<R>> onDone) {
        Task<List<R>> task = new Task<>() {
            @Override
            protected List<R> call() {
                EventBus.publish(AppEventType.DATA_CLEANING_STARTED, items.size());
                List<R> cleaned = new java.util.ArrayList<>(items.size());
                int total = items.size();

                for (int i = 0; i < total; i++) {
                    try {
                        cleaned.add(pipe.process(items.get(i)));
                    } catch (Exception e) {
                        logger.warn("Cleaning skipped item " + i + ": " + e.getMessage());
                    }
                    int pct = (int) (((i + 1) / (double) total) * 100);
                    updateProgress(i + 1, total);
                    EventBus.publish(AppEventType.DATA_CLEANING_PROGRESS, pct);
                }
                return cleaned;
            }
        };

        task.setOnSucceeded(e -> {
            List<R> result = task.getValue();
            EventBus.publish(AppEventType.DATA_CLEANING_COMPLETED, result);
            onDone.accept(result);
        });

        task.setOnFailed(e -> {
            logger.error("Data cleaning task failed", task.getException());
            EventBus.publish(AppEventType.DATA_CLEANING_FAILED, task.getException());
        });

        POOL.submit(task);
    }
}