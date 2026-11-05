package org.evomaster.client.java.controller.internal;

import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiActionDto;
import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiReplyDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AsyncApiSettleTest {

    /**
     * A clock that only moves when the fake sleeper sleeps, so that nothing here depends on how
     * long anything really takes.
     */
    private long now = 1_000;

    /**
     * Every sleep asked for, in order.
     */
    private final List<Long> sleeps = new ArrayList<>();

    private AsyncApiSettle settleThatSleeps() {
        return new AsyncApiSettle(() -> now, ms -> {
            sleeps.add(ms);
            now += ms;
        });
    }

    private static AsyncApiActionDto asking(Long settleMs) {
        AsyncApiActionDto dto = new AsyncApiActionDto();
        dto.settleMs = settleMs;
        return dto;
    }

    @AfterEach
    public void clearInterrupt() {
        //a test that leaves the flag set would interrupt whatever runs next on this thread
        Thread.interrupted();
    }

    @Test
    public void testTheActionIsKeptOpenForAsLongAsAsked() {

        AsyncApiReplyDto reply = new AsyncApiReplyDto();

        settleThatSleeps().settle(asking(250L), reply);

        assertEquals(1, sleeps.size());
        assertEquals(250L, sleeps.get(0));
        assertEquals(250L, reply.settledMs);
    }

    @Test
    public void testNothingIsWaitedNorRecordedWhenNoWaitIsAskedFor() {

        AsyncApiSettle settle = settleThatSleeps();

        for (Long none : new Long[]{null, 0L, -5L}) {
            AsyncApiReplyDto reply = new AsyncApiReplyDto();
            settle.settle(asking(none), reply);
            assertNull(reply.settledMs, "a wait was recorded for settleMs=" + none);
        }

        assertTrue(sleeps.isEmpty(), "slept although no wait was asked for: " + sleeps);
    }

    @Test
    public void testAnInterruptedWaitEndsThereAndKeepsTheInterrupt() {

        /*
            The thread belongs to the HTTP server, so the interrupt is not this class's to
            swallow: it ends the wait, and stays set for whoever owns the thread.
         */
        AsyncApiSettle settle = new AsyncApiSettle(() -> now, ms -> {
            now += 40;
            throw new InterruptedException();
        });

        AsyncApiReplyDto reply = new AsyncApiReplyDto();
        settle.settle(asking(250L), reply);

        assertTrue(Thread.currentThread().isInterrupted(), "the interrupt was swallowed");
        assertEquals(40L, reply.settledMs, "what was recorded is not the time actually waited");
    }

    @Test
    public void testWhatTheDriverReportedIsLeftAlone() {

        AsyncApiReplyDto reply = new AsyncApiReplyDto();
        reply.published = true;
        reply.replyExpected = true;
        reply.replyReceived = true;
        reply.waitedMs = 12L;

        settleThatSleeps().settle(asking(100L), reply);

        assertTrue(reply.published);
        assertTrue(reply.replyReceived);
        assertEquals(12L, reply.waitedMs, "the driver's wait for the reply is not the controller's settle");
        assertEquals(100L, reply.settledMs);
    }

    @Test
    public void testTheActionAndTheReplyCannotBeNull() {

        AsyncApiSettle settle = settleThatSleeps();

        assertThrows(NullPointerException.class, () -> settle.settle(null, new AsyncApiReplyDto()));
        assertThrows(NullPointerException.class, () -> settle.settle(asking(1L), null));
    }

    @Test
    public void testTheRealClockAndSleeperWait() {

        /*
            Only that a wait is recorded, never how long it took: a loaded machine can always
            take longer, and an assertion on that would be flaky.
         */
        AsyncApiReplyDto reply = new AsyncApiReplyDto();

        new AsyncApiSettle().settle(asking(1L), reply);

        assertNotNull(reply.settledMs);
        assertTrue(reply.settledMs >= 0, "negative wait: " + reply.settledMs);
    }
}
