package hospital.management.backend.utils.listeners;

import hospital.management.backend.config.AppLogger;
import hospital.management.backend.config.security.SessionManager;
import hospital.management.backend.service.log.DualLogBridge;
import javafx.application.Platform;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Application-wide publish/subscribe event bus.
 *
 * Components subscribe to event types they care about. When any other component
 * publishes an event, all subscribers for that type are notified automatically.
 * This decouples publishers from subscribers — the patient service does not need
 * to know that the dashboard widget or the stats card wants to refresh.
 *
 * Thread safety: listeners are always invoked on the JavaFX Application Thread
 * (via Platform.runLater) so UI components can update controls directly.
 *
 * Usage:
 *
 *   // In DashboardController.initialize():
 *   EventBus.subscribe(AppEventType.PATIENT_CREATED, e -> refreshPatientCount());
 *
 *   // In PatientService after saving:
 *   EventBus.publish(AppEventType.PATIENT_CREATED, savedPatient);
 *
 *   // In DashboardController.cleanup() or on page navigate away:
 *   EventBus.unsubscribe(AppEventType.PATIENT_CREATED, listener);
 */
public final class EventBus {

    private static final AppLogger logger =
        AppLogger.getLogger(EventBus.class);

    private static final Map<AppEventType, List<Consumer<AppEvent>>> LISTENERS =
        new ConcurrentHashMap<>();

    private EventBus() {}

    // ── Subscribe ─────────────────────────────────────────────────────────────

    /**
     * Registers a listener for the given event type.
     * The same listener instance can be removed later with {@link #unsubscribe}.
     *
     * @param type     the event to listen for
     * @param listener called every time an event of this type is published
     */
    public static void subscribe(AppEventType type, Consumer<AppEvent> listener) {
        LISTENERS.computeIfAbsent(type, k -> new ArrayList<>()).add(listener);
    }

    /**
     * Removes a previously registered listener.
     * Always call this when the subscribing controller is navigated away from,
     * otherwise the listener will keep running and may hold a reference to a
     * controller that is no longer on screen.
     */
    public static void unsubscribe(AppEventType type, Consumer<AppEvent> listener) {
        List<Consumer<AppEvent>> list = LISTENERS.get(type);
        if (list != null) list.remove(listener);
    }

    /** Removes all listeners for a given event type. */
    public static void clearAll(AppEventType type) {
        LISTENERS.remove(type);
    }

    // ── Publish ───────────────────────────────────────────────────────────────

    /**
     * Publishes an event to all registered listeners.
     * If called from a background thread, delivery is deferred to the
     * JavaFX Application Thread so listeners can safely update UI controls.
     *
     * @param type    the event type
     * @param payload the data attached to the event (may be null)
     */
    public static void publish(AppEventType type, Object payload) {
        AppEvent event = new AppEvent(type, payload);

        // Central dual logging for benchmarking: every service event is mirrored
        // to PostgreSQL system_logs and MongoDB system_log_benchmark.
        if (type != AppEventType.AUDIT_LOG_RECORDED && type != AppEventType.SYSTEM_LOG_RECORDED) {
            try {
                String userId = null;
                try { userId = SessionManager.getCurrentUserId(); } catch (Exception ignored) {}
                DualLogBridge.recordServiceEvent(type, payload, userId);
            } catch (Exception e) {
                logger.warn("Failed to mirror event log: " + e.getMessage());
            }
        }

        List<Consumer<AppEvent>> list = LISTENERS.get(type);
        if (list == null || list.isEmpty()) return;

        Runnable dispatch = () -> {
            for (Consumer<AppEvent> listener : new ArrayList<>(list)) {
                try {
                    listener.accept(event);
                } catch (Exception e) {
                    logger.error("Listener threw during " + type + " event", e);
                }
            }
        };

        if (Platform.isFxApplicationThread()) {
            dispatch.run();
        } else {
            Platform.runLater(dispatch);
        }
    }

    /** Convenience overload with no payload. */
    public static void publish(AppEventType type) {
        publish(type, null);
    }
}