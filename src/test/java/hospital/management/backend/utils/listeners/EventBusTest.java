package hospital.management.backend.utils.listeners;

import javafx.application.Platform;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EventBus.publish() dispatches via Platform.runLater whenever at least one listener is
 * registered for the published type (see EventBus.publish — an empty/absent listener list
 * returns immediately, before Platform is touched at all). That means:
 *  - the "no listeners" case is already fully synchronous/deterministic and needs no
 *    JavaFX toolkit;
 *  - the "has listeners" case is genuinely asynchronous once the toolkit is running (the
 *    calling thread here is never the FX Application Thread), so those tests wait on a
 *    CountDownLatch instead of asserting immediately after publish() returns.
 *
 * The JavaFX toolkit is started once for this test class via Platform.startup(); if some
 * other class already started it earlier in the same JVM, Platform.startup() throws
 * IllegalStateException, which is caught and ignored — either way the toolkit is running
 * by the time the tests below execute.
 */
class EventBusTest {

    @BeforeAll
    static void initJavaFxToolkit() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException alreadyRunning) {
            // Toolkit already initialised elsewhere in this JVM — nothing to do.
        }
    }

    // Tracks every listener this test class registers so it can be torn down afterwards —
    // EventBus is a static, JVM-wide registry with no reset hook, and a leaked listener here
    // could fire unexpectedly during an unrelated test elsewhere in the same run (e.g. the
    // AuditServiceImplTest/SystemLogServiceImplTest tests rely on EventBus having no
    // listeners for AUDIT_LOG_RECORDED/SYSTEM_LOG_RECORDED so publish() short-circuits
    // before ever reaching Platform.runLater).
    private final List<Consumer<AppEvent>> subscribedListeners = new ArrayList<>();
    private final List<AppEventType> subscribedTypes = new ArrayList<>();

    private void subscribe(AppEventType type, Consumer<AppEvent> listener) {
        EventBus.subscribe(type, listener);
        subscribedListeners.add(listener);
        subscribedTypes.add(type);
    }

    @AfterEach
    void tearDown() {
        for (int i = 0; i < subscribedListeners.size(); i++) {
            EventBus.unsubscribe(subscribedTypes.get(i), subscribedListeners.get(i));
        }
        subscribedListeners.clear();
        subscribedTypes.clear();
    }

    @Test
    @DisplayName("publish invokes a subscribed listener with the correct event type and payload")
    void publish_invokesSubscribedListener_withCorrectPayload() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<AppEvent> received = new AtomicReference<>();
        Consumer<AppEvent> listener = event -> {
            received.set(event);
            latch.countDown();
        };
        subscribe(AppEventType.PATIENT_CREATED, listener);

        EventBus.publish(AppEventType.PATIENT_CREATED, "patient-123");

        assertTrue(latch.await(5, TimeUnit.SECONDS), "listener was not invoked within timeout");
        assertEquals(AppEventType.PATIENT_CREATED, received.get().getType());
        assertEquals("patient-123", received.get().getPayload());
    }

    @Test
    @DisplayName("publish with no listeners registered for that type is a safe no-op")
    void publish_withNoListeners_doesNotThrow() {
        assertDoesNotThrow(() -> EventBus.publish(AppEventType.DOCTOR_CREATED, "irrelevant"));
    }

    @Test
    @DisplayName("publish invokes every listener subscribed to the same event type")
    void publish_invokesAllListenersForSameType() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);
        List<AppEventType> seenBy1 = new CopyOnWriteArrayList<>();
        List<AppEventType> seenBy2 = new CopyOnWriteArrayList<>();
        Consumer<AppEvent> listener1 = event -> { seenBy1.add(event.getType()); latch.countDown(); };
        Consumer<AppEvent> listener2 = event -> { seenBy2.add(event.getType()); latch.countDown(); };
        subscribe(AppEventType.APPOINTMENT_BOOKED, listener1);
        subscribe(AppEventType.APPOINTMENT_BOOKED, listener2);

        EventBus.publish(AppEventType.APPOINTMENT_BOOKED, null);

        assertTrue(latch.await(5, TimeUnit.SECONDS), "not all listeners were invoked within timeout");
        assertEquals(1, seenBy1.size());
        assertEquals(1, seenBy2.size());
    }

    @Test
    @DisplayName("a listener subscribed to a different event type is not invoked")
    void publish_doesNotInvokeListener_forDifferentEventType() throws InterruptedException {
        CountDownLatch wrongTypeLatch = new CountDownLatch(1);
        CountDownLatch rightTypeLatch = new CountDownLatch(1);
        Consumer<AppEvent> wrongTypeListener = event -> wrongTypeLatch.countDown();
        Consumer<AppEvent> rightTypeListener = event -> rightTypeLatch.countDown();
        subscribe(AppEventType.INVOICE_PAID, wrongTypeListener);
        subscribe(AppEventType.INVOICE_CREATED, rightTypeListener);

        EventBus.publish(AppEventType.INVOICE_CREATED, null);

        assertTrue(rightTypeLatch.await(5, TimeUnit.SECONDS), "listener for the published type was not invoked");
        assertFalse(wrongTypeLatch.await(200, TimeUnit.MILLISECONDS),
                "listener for a different event type should not have been invoked");
    }

    @Test
    @DisplayName("unsubscribe removes a listener so it no longer receives events")
    void unsubscribe_removesListener() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Consumer<AppEvent> listener = event -> latch.countDown();
        EventBus.subscribe(AppEventType.USER_LOGGED_IN, listener);
        EventBus.unsubscribe(AppEventType.USER_LOGGED_IN, listener);

        EventBus.publish(AppEventType.USER_LOGGED_IN, null);

        assertFalse(latch.await(200, TimeUnit.MILLISECONDS),
                "unsubscribed listener should not have been invoked");
    }

    @Test
    @DisplayName("clearAll removes every listener registered for the given event type")
    void clearAll_removesEveryListenerForType() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);
        EventBus.subscribe(AppEventType.USER_LOGGED_OUT, e -> latch.countDown());
        EventBus.subscribe(AppEventType.USER_LOGGED_OUT, e -> latch.countDown());

        EventBus.clearAll(AppEventType.USER_LOGGED_OUT);
        EventBus.publish(AppEventType.USER_LOGGED_OUT, null);

        assertFalse(latch.await(200, TimeUnit.MILLISECONDS),
                "listeners should have been cleared and therefore not invoked");
    }
}
