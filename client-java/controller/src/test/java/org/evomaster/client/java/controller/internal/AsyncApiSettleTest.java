package org.evomaster.client.java.controller.internal;

import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiActionDto;
import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiReplyDto;
import org.evomaster.client.java.instrumentation.MessageHandlingState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

public class AsyncApiSettleTest {

    /**
     * A SUT whose coverage cannot be read.
     */
    private static final Supplier<Long> NO_COVERAGE = () -> null;

    /**
     * A driver that cannot tell whether the SUT's consumers have caught up.
     */
    private static final Supplier<Boolean> CANNOT_TELL = () -> null;

    /**
     * A SUT whose message handling cannot be read.
     */
    private static final Function<String, MessageHandlingState> NO_HANDLING = address -> null;

    private static final long SETTLE_MS = 1_000;

    /**
     * Where the action under test publishes to.
     */
    private static final String ADDRESS = "requests";

    private static final MessageHandlingState NOTHING_RECEIVED = new MessageHandlingState(0, 0);

    private static final MessageHandlingState BEING_HANDLED = new MessageHandlingState(1, 1);

    private static final MessageHandlingState HANDLED = new MessageHandlingState(1, 0);

    private static final long QUIET_MS = 100;

    /**
     * Where the fake clock starts. Anything but zero, so that a wait measured from zero rather
     * than from its start would show.
     */
    private static final long START = 1_000;

    /**
     * A clock that only moves when the fake sleeper sleeps, so that nothing here depends on how
     * long anything really takes.
     */
    private long now = START;

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

        settleThatSleeps().settle(asking(250L), reply, NO_COVERAGE, NO_HANDLING, CANNOT_TELL);

        assertEquals(1, sleeps.size());
        assertEquals(250L, sleeps.get(0));
        assertEquals(250L, reply.settledMs);
        assertEquals(AsyncApiReplyDto.SettleEnd.DEADLINE, reply.settleEndedBy);
    }

    @Test
    public void testNothingIsWaitedNorRecordedWhenNoWaitIsAskedFor() {

        AsyncApiSettle settle = settleThatSleeps();

        for (Long none : new Long[]{null, 0L, -5L}) {
            AsyncApiReplyDto reply = new AsyncApiReplyDto();
            settle.settle(asking(none), reply, NO_COVERAGE, NO_HANDLING, CANNOT_TELL);
            assertNull(reply.settledMs, "a wait was recorded for settleMs=" + none);
            assertNull(reply.settleEndedBy, "a wait was recorded for settleMs=" + none);
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
        settle.settle(asking(250L), reply, NO_COVERAGE, NO_HANDLING, CANNOT_TELL);

        assertTrue(Thread.currentThread().isInterrupted(), "the interrupt was swallowed");
        assertEquals(40L, reply.settledMs, "what was recorded is not the time actually waited");
        assertEquals(AsyncApiReplyDto.SettleEnd.INTERRUPTED, reply.settleEndedBy);
    }

    @Test
    public void testWhatTheDriverReportedIsLeftAlone() {

        AsyncApiReplyDto reply = new AsyncApiReplyDto();
        reply.published = true;
        reply.replyExpected = true;
        reply.replyReceived = true;
        reply.waitedMs = 12L;

        settleThatSleeps().settle(asking(100L), reply, NO_COVERAGE, NO_HANDLING, CANNOT_TELL);

        assertTrue(reply.published);
        assertTrue(reply.replyReceived);
        assertEquals(12L, reply.waitedMs, "the driver's wait for the reply is not the controller's settle");
        assertEquals(100L, reply.settledMs);
    }

    @Test
    public void testNoneOfTheArgumentsCanBeNull() {

        AsyncApiSettle settle = settleThatSleeps();

        assertThrows(NullPointerException.class, () -> settle.settle(null, new AsyncApiReplyDto(), NO_COVERAGE, NO_HANDLING, CANNOT_TELL));
        assertThrows(NullPointerException.class, () -> settle.settle(asking(1L), null, NO_COVERAGE, NO_HANDLING, CANNOT_TELL));
        assertThrows(NullPointerException.class, () -> settle.settle(asking(1L), new AsyncApiReplyDto(), null, NO_HANDLING, CANNOT_TELL));
        assertThrows(NullPointerException.class, () -> settle.settle(asking(1L), new AsyncApiReplyDto(), NO_COVERAGE, null, CANNOT_TELL));
        assertThrows(NullPointerException.class, () -> settle.settle(asking(1L), new AsyncApiReplyDto(), NO_COVERAGE, NO_HANDLING, null));
    }

    @Test
    public void testTheRealClockAndSleeperWait() {

        /*
            Only that a wait is recorded, never how long it took: a loaded machine can always
            take longer, and an assertion on that would be flaky.
         */
        AsyncApiReplyDto reply = new AsyncApiReplyDto();

        new AsyncApiSettle().settle(asking(1L), reply, NO_COVERAGE, NO_HANDLING, CANNOT_TELL);

        assertNotNull(reply.settledMs);
        assertTrue(reply.settledMs >= 0, "negative wait: " + reply.settledMs);
    }

    private static AsyncApiActionDto quiescence(Long quietMs) {
        AsyncApiActionDto dto = asking(SETTLE_MS);
        dto.completion = AsyncApiActionDto.Completion.COVERAGE_QUIESCENCE;
        dto.quietMs = quietMs;
        return dto;
    }

    @Test
    public void testASutThatReachesNothingNewIsLetGoOnceQuiet() {

        AsyncApiReplyDto reply = new AsyncApiReplyDto();

        settleThatSleeps().settle(quiescence(QUIET_MS), reply, () -> 7L, NO_HANDLING, CANNOT_TELL);

        assertEquals(AsyncApiReplyDto.SettleEnd.QUIESCENCE, reply.settleEndedBy);
        assertEquals(QUIET_MS, reply.settledMs);
        assertTrue(sleeps.stream().allMatch(ms -> ms <= AsyncApiSettle.POLL_INTERVAL_MS),
                "the coverage was not read often enough: " + sleeps);
    }

    @Test
    public void testCoverageStillImprovingKeepsTheActionOpen() {

        /*
            The count moves on each of the first reads, until 300 ms in, then stops: the wait
            ends a quiet window after the last move, not after the first.
         */
        long[] count = {0};
        Supplier<Long> busyUntil300 = () -> {
            if (now - START <= 300) {
                count[0]++;
            }
            return count[0];
        };

        AsyncApiReplyDto reply = new AsyncApiReplyDto();
        settleThatSleeps().settle(quiescence(QUIET_MS), reply, busyUntil300, NO_HANDLING, CANNOT_TELL);

        assertEquals(AsyncApiReplyDto.SettleEnd.QUIESCENCE, reply.settleEndedBy);
        assertEquals(300L + QUIET_MS, reply.settledMs);
    }

    @Test
    public void testTheDeadlineCapsASutThatNeverGoesQuiet() {

        long[] count = {0};

        AsyncApiReplyDto reply = new AsyncApiReplyDto();
        settleThatSleeps().settle(quiescence(QUIET_MS), reply, () -> ++count[0], NO_HANDLING, CANNOT_TELL);

        assertEquals(AsyncApiReplyDto.SettleEnd.DEADLINE, reply.settleEndedBy);
        assertEquals(SETTLE_MS, reply.settledMs);
    }

    @Test
    public void testCoverageThatCannotBeReadMeansTheWholeWindow() {

        AsyncApiReplyDto reply = new AsyncApiReplyDto();
        settleThatSleeps().settle(quiescence(QUIET_MS), reply, NO_COVERAGE, NO_HANDLING, CANNOT_TELL);

        assertEquals(AsyncApiReplyDto.SettleEnd.DEADLINE, reply.settleEndedBy);
        assertEquals(SETTLE_MS, reply.settledMs);
    }

    @Test
    public void testCoverageLostHalfWayMeansTheRestOfTheWindow() {

        //eg the connection to an external SUT's agent dropping
        Supplier<Long> lostAfter50 = () -> now - START < 50 ? Long.valueOf(now) : null;

        AsyncApiReplyDto reply = new AsyncApiReplyDto();
        settleThatSleeps().settle(quiescence(QUIET_MS), reply, lostAfter50, NO_HANDLING, CANNOT_TELL);

        assertEquals(AsyncApiReplyDto.SettleEnd.DEADLINE, reply.settleEndedBy);
        assertEquals(SETTLE_MS, reply.settledMs);
    }

    @Test
    public void testCoverageThatFailsToBeReadMeansTheWholeWindow() {

        /*
            Reading it for an external SUT is a round trip to its agent, which can fail. The
            action was published all the same, so it still succeeds, only with the whole window
            waited.
         */
        AsyncApiReplyDto reply = new AsyncApiReplyDto();
        settleThatSleeps().settle(quiescence(QUIET_MS), reply, () -> {
            throw new IllegalStateException("agent gone");
        }, NO_HANDLING, CANNOT_TELL);

        assertEquals(AsyncApiReplyDto.SettleEnd.DEADLINE, reply.settleEndedBy);
        assertEquals(SETTLE_MS, reply.settledMs);
    }

    @Test
    public void testCoverageThatFailsToBeReadHalfWayMeansTheRestOfTheWindow() {

        Supplier<Long> failingAfter50 = () -> {
            if (now - START >= 50) {
                throw new IllegalStateException("agent gone");
            }
            return now;
        };

        AsyncApiReplyDto reply = new AsyncApiReplyDto();
        settleThatSleeps().settle(quiescence(QUIET_MS), reply, failingAfter50, NO_HANDLING, CANNOT_TELL);

        assertEquals(AsyncApiReplyDto.SettleEnd.DEADLINE, reply.settleEndedBy);
        assertEquals(SETTLE_MS, reply.settledMs);
    }

    @Test
    public void testWithoutAQuietWindowQuiescenceIsAFixedWait() {

        for (Long none : new Long[]{null, 0L, -1L}) {
            AsyncApiReplyDto reply = new AsyncApiReplyDto();
            settleThatSleeps().settle(quiescence(none), reply, () -> 7L, NO_HANDLING, CANNOT_TELL);
            assertEquals(AsyncApiReplyDto.SettleEnd.DEADLINE, reply.settleEndedBy, "quietMs=" + none);
        }
    }

    @Test
    public void testAFixedWaitNeverReadsTheCoverage() {

        AsyncApiActionDto dto = asking(SETTLE_MS);
        dto.completion = AsyncApiActionDto.Completion.FIXED;
        dto.quietMs = QUIET_MS;

        AsyncApiReplyDto reply = new AsyncApiReplyDto();
        settleThatSleeps().settle(dto, reply,
                () -> fail("the coverage was read for a fixed wait"),
                neverRead(),
                () -> fail("the driver was asked about its consumers for a fixed wait"));

        assertEquals(AsyncApiReplyDto.SettleEnd.DEADLINE, reply.settleEndedBy);
        assertEquals(SETTLE_MS, reply.settledMs);
    }

    @Test
    public void testAnInterruptWhileWaitingForQuiescenceEndsTheWait() {

        AsyncApiSettle settle = new AsyncApiSettle(() -> now, ms -> {
            now += ms;
            if (now - START >= 50) {
                throw new InterruptedException();
            }
        });

        long[] count = {0};
        AsyncApiReplyDto reply = new AsyncApiReplyDto();
        settle.settle(quiescence(QUIET_MS), reply, () -> ++count[0], NO_HANDLING, CANNOT_TELL);

        assertTrue(Thread.currentThread().isInterrupted(), "the interrupt was swallowed");
        assertEquals(AsyncApiReplyDto.SettleEnd.INTERRUPTED, reply.settleEndedBy);
        assertEquals(50L, reply.settledMs);
    }

    private static AsyncApiActionDto untilCaughtUp() {
        AsyncApiActionDto dto = asking(SETTLE_MS);
        dto.completion = AsyncApiActionDto.Completion.CONSUMER_LAG;
        return dto;
    }

    @Test
    public void testConsumersAlreadyCaughtUpEndTheWaitStraightAway() {

        /*
            The usual case after a reply: the SUT took the message in before answering it, so
            there is nothing left to wait for.
         */
        AsyncApiReplyDto reply = new AsyncApiReplyDto();
        settleThatSleeps().settle(untilCaughtUp(), reply, NO_COVERAGE, NO_HANDLING, () -> true);

        assertEquals(AsyncApiReplyDto.SettleEnd.CAUGHT_UP, reply.settleEndedBy);
        assertEquals(0L, reply.settledMs);
        assertTrue(sleeps.isEmpty(), "slept although the consumers had caught up: " + sleeps);
    }

    @Test
    public void testTheWaitEndsOnceTheConsumersCatchUp() {

        Supplier<Boolean> caughtUpAfter200 = () -> now - START >= 200;

        AsyncApiReplyDto reply = new AsyncApiReplyDto();
        settleThatSleeps().settle(untilCaughtUp(), reply, NO_COVERAGE, NO_HANDLING, caughtUpAfter200);

        assertEquals(AsyncApiReplyDto.SettleEnd.CAUGHT_UP, reply.settleEndedBy);
        assertEquals(200L, reply.settledMs);
        assertTrue(sleeps.stream().allMatch(ms -> ms <= AsyncApiSettle.POLL_INTERVAL_MS),
                "the driver was not asked often enough: " + sleeps);
    }

    @Test
    public void testTheDeadlineCapsConsumersThatNeverCatchUp() {

        AsyncApiReplyDto reply = new AsyncApiReplyDto();
        settleThatSleeps().settle(untilCaughtUp(), reply, NO_COVERAGE, NO_HANDLING, () -> false);

        assertEquals(AsyncApiReplyDto.SettleEnd.DEADLINE, reply.settleEndedBy);
        assertEquals(SETTLE_MS, reply.settledMs);
    }

    @Test
    public void testADriverThatCannotTellGetsTheWholeWindow() {

        AsyncApiReplyDto reply = new AsyncApiReplyDto();
        settleThatSleeps().settle(untilCaughtUp(), reply, NO_COVERAGE, NO_HANDLING, CANNOT_TELL);

        assertEquals(AsyncApiReplyDto.SettleEnd.DEADLINE, reply.settleEndedBy);
        assertEquals(SETTLE_MS, reply.settledMs);
    }

    @Test
    public void testADriverThatStopsBeingAbleToTellGetsTheRestOfTheWindow() {

        //eg the broker going away half-way through the wait
        Supplier<Boolean> lostAfter50 = () -> now - START < 50 ? Boolean.FALSE : null;

        AsyncApiReplyDto reply = new AsyncApiReplyDto();
        settleThatSleeps().settle(untilCaughtUp(), reply, NO_COVERAGE, NO_HANDLING, lostAfter50);

        assertEquals(AsyncApiReplyDto.SettleEnd.DEADLINE, reply.settleEndedBy);
        assertEquals(SETTLE_MS, reply.settledMs);
    }

    @Test
    public void testADriverThatFailsToTellIsTreatedAsOneThatCannot() {

        /*
            The question is user code. Failing to answer it does not undo the publish, so the
            action still succeeds, only with the whole window waited.
         */
        AsyncApiReplyDto reply = new AsyncApiReplyDto();
        settleThatSleeps().settle(untilCaughtUp(), reply, NO_COVERAGE, NO_HANDLING, () -> {
            throw new IllegalStateException("broker timed out");
        });

        assertEquals(AsyncApiReplyDto.SettleEnd.DEADLINE, reply.settleEndedBy);
        assertEquals(SETTLE_MS, reply.settledMs);
    }

    @Test
    public void testWaitingForConsumersNeverReadsTheCoverage() {

        AsyncApiReplyDto reply = new AsyncApiReplyDto();
        settleThatSleeps().settle(untilCaughtUp(), reply,
                () -> fail("the coverage was read while waiting for the consumers"),
                neverRead(), () -> true);

        assertEquals(AsyncApiReplyDto.SettleEnd.CAUGHT_UP, reply.settleEndedBy);
    }

    @Test
    public void testAnInterruptWhileWaitingForConsumersEndsTheWait() {

        AsyncApiSettle settle = new AsyncApiSettle(() -> now, ms -> {
            now += ms;
            if (now - START >= 50) {
                throw new InterruptedException();
            }
        });

        AsyncApiReplyDto reply = new AsyncApiReplyDto();
        settle.settle(untilCaughtUp(), reply, NO_COVERAGE, NO_HANDLING, () -> false);

        assertTrue(Thread.currentThread().isInterrupted(), "the interrupt was swallowed");
        assertEquals(AsyncApiReplyDto.SettleEnd.INTERRUPTED, reply.settleEndedBy);
        assertEquals(50L, reply.settledMs);
    }

    private static AsyncApiActionDto untilHandled() {
        AsyncApiActionDto dto = asking(SETTLE_MS);
        dto.completion = AsyncApiActionDto.Completion.HANDLER_RETURN;
        dto.address = ADDRESS;
        return dto;
    }

    private static Function<String, MessageHandlingState> always(MessageHandlingState state) {
        return address -> state;
    }

    private static Function<String, MessageHandlingState> neverRead() {
        return address -> fail("what the SUT received was read for address " + address);
    }

    @Test
    public void testAMessageAlreadyHandledWhenTheDriverReturnsEndsTheWaitAtOnce() {

        AsyncApiReplyDto reply = new AsyncApiReplyDto();

        settleThatSleeps().settle(untilHandled(), reply, NO_COVERAGE, always(HANDLED), CANNOT_TELL);

        assertEquals(AsyncApiReplyDto.SettleEnd.HANDLER_RETURN, reply.settleEndedBy);
        assertEquals(0L, reply.settledMs);
        assertTrue(sleeps.isEmpty(), "slept although the SUT was done: " + sleeps);
    }

    @Test
    public void testTheActionIsKeptOpenUntilTheHandlerReturns() {

        Function<String, MessageHandlingState> doneAfter100 =
                address -> now - START < 100 ? BEING_HANDLED : HANDLED;

        AsyncApiReplyDto reply = new AsyncApiReplyDto();
        settleThatSleeps().settle(untilHandled(), reply, NO_COVERAGE, doneAfter100, CANNOT_TELL);

        assertEquals(AsyncApiReplyDto.SettleEnd.HANDLER_RETURN, reply.settleEndedBy);
        assertEquals(100L, reply.settledMs);
        assertTrue(sleeps.stream().allMatch(ms -> ms <= AsyncApiSettle.POLL_INTERVAL_MS),
                "what the SUT received was not read often enough: " + sleeps);
    }

    @Test
    public void testNothingReceivedIsWaitedOnUntilTheDeadline() {

        AsyncApiReplyDto reply = new AsyncApiReplyDto();
        settleThatSleeps().settle(untilHandled(), reply, NO_COVERAGE, always(NOTHING_RECEIVED), CANNOT_TELL);

        assertEquals(AsyncApiReplyDto.SettleEnd.DEADLINE, reply.settleEndedBy);
        assertEquals(SETTLE_MS, reply.settledMs);
    }

    @Test
    public void testTheDeadlineCapsAHandlerThatNeverReturns() {

        AsyncApiReplyDto reply = new AsyncApiReplyDto();
        settleThatSleeps().settle(untilHandled(), reply, NO_COVERAGE, always(BEING_HANDLED), CANNOT_TELL);

        assertEquals(AsyncApiReplyDto.SettleEnd.DEADLINE, reply.settleEndedBy);
        assertEquals(SETTLE_MS, reply.settledMs);
    }

    @Test
    public void testWhatTheSutReceivedThatCannotBeReadMeansTheWholeWindow() {

        AsyncApiReplyDto reply = new AsyncApiReplyDto();
        settleThatSleeps().settle(untilHandled(), reply, NO_COVERAGE, address -> null, CANNOT_TELL);

        assertEquals(AsyncApiReplyDto.SettleEnd.DEADLINE, reply.settleEndedBy);
        assertEquals(SETTLE_MS, reply.settledMs);
    }

    @Test
    public void testWhatTheSutReceivedThatFailsToBeReadMeansTheRestOfTheWindow() {

        //eg the external SUT's agent going away half-way through the wait
        Function<String, MessageHandlingState> failingAfter50 = address -> {
            if (now - START >= 50) {
                throw new IllegalStateException("agent gone");
            }
            return BEING_HANDLED;
        };

        AsyncApiReplyDto reply = new AsyncApiReplyDto();
        settleThatSleeps().settle(untilHandled(), reply, NO_COVERAGE, failingAfter50, CANNOT_TELL);

        assertEquals(AsyncApiReplyDto.SettleEnd.DEADLINE, reply.settleEndedBy);
        assertEquals(SETTLE_MS, reply.settledMs);
    }

    @Test
    public void testWithoutAnAddressTheWholeWindow() {

        AsyncApiActionDto dto = untilHandled();
        dto.address = null;

        AsyncApiReplyDto reply = new AsyncApiReplyDto();
        settleThatSleeps().settle(dto, reply, NO_COVERAGE, neverRead(), CANNOT_TELL);

        assertEquals(AsyncApiReplyDto.SettleEnd.DEADLINE, reply.settleEndedBy);
        assertEquals(SETTLE_MS, reply.settledMs);
    }

    @Test
    public void testWhatIsReadIsWhatCameFromThePublishedAddress() {

        List<String> asked = new ArrayList<>();
        Function<String, MessageHandlingState> recording = address -> {
            asked.add(address);
            return now - START < 50 ? NOTHING_RECEIVED : HANDLED;
        };

        settleThatSleeps().settle(untilHandled(), new AsyncApiReplyDto(), NO_COVERAGE, recording, CANNOT_TELL);

        assertFalse(asked.isEmpty());
        assertTrue(asked.stream().allMatch(ADDRESS::equals), "Asked about: " + asked);
    }

    @Test
    public void testOtherCompletionsNeverReadWhatTheSutReceived() {

        AsyncApiActionDto fixed = asking(SETTLE_MS);
        fixed.address = ADDRESS;
        settleThatSleeps().settle(fixed, new AsyncApiReplyDto(), NO_COVERAGE, neverRead(), CANNOT_TELL);

        AsyncApiActionDto quiet = quiescence(QUIET_MS);
        quiet.address = ADDRESS;
        AsyncApiReplyDto reply = new AsyncApiReplyDto();
        settleThatSleeps().settle(quiet, reply, () -> 7L, neverRead(), CANNOT_TELL);

        assertEquals(AsyncApiReplyDto.SettleEnd.QUIESCENCE, reply.settleEndedBy);
    }

    @Test
    public void testAnInterruptWhileWaitingForTheHandlerEndsTheWait() {

        AsyncApiSettle settle = new AsyncApiSettle(() -> now, ms -> {
            now += ms;
            if (now - START >= 50) {
                throw new InterruptedException();
            }
        });

        AsyncApiReplyDto reply = new AsyncApiReplyDto();
        settle.settle(untilHandled(), reply, NO_COVERAGE, always(BEING_HANDLED), CANNOT_TELL);

        assertTrue(Thread.currentThread().isInterrupted(), "the interrupt was swallowed");
        assertEquals(AsyncApiReplyDto.SettleEnd.INTERRUPTED, reply.settleEndedBy);
        assertEquals(50L, reply.settledMs);
    }
}
