package hospital.management.backend.utils.listeners;

import java.time.LocalDateTime;

/**
 * Immutable event object published through the EventBus.
 * The payload is typed as Object — cast to the expected type in your listener.
 *
 * Convention:
 *   PATIENT_CREATED  → payload is Patient
 *   SESSION_EXPIRED  → payload is null
 *   DATA_CLEANING_PROGRESS → payload is Integer (0–100 percent)
 */
public final class AppEvent {

    private final AppEventType  type;
    private final Object        payload;
    private final LocalDateTime occurredAt;

    public AppEvent(AppEventType type, Object payload) {
        this.type       = type;
        this.payload    = payload;
        this.occurredAt = LocalDateTime.now();
    }

    public AppEventType  getType()       { return type; }
    public Object        getPayload()    { return payload; }
    public LocalDateTime getOccurredAt() { return occurredAt; }

    /** Cast helper — avoids unchecked cast warnings at every call site. */
    @SuppressWarnings("unchecked")
    public <T> T getPayloadAs(Class<T> clazz) {
        return clazz.cast(payload);
    }

    @Override
    public String toString() {
        return "AppEvent[" + type + "] @ " + occurredAt;
    }
}