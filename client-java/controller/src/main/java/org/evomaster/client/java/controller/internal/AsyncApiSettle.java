package org.evomaster.client.java.controller.internal;

import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiActionDto;
import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiReplyDto;
import org.evomaster.client.java.instrumentation.MessageHandlingState;
import org.evomaster.client.java.utils.SimpleLogger;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Keeps an AsyncAPI action open for a while after the driver has returned from publishing it,
 * so that what the service does with the message, on a thread of its own, is still attributed
 * to that action. Done in the controller so that drivers need not implement it, and the core
 * pays no extra round trip.
 */
public class AsyncApiSettle {

    private static final long NANOS_PER_MILLI = 1_000_000L;

    private static final String OBJECTIVE_UPDATES_FAILURE_WARNING = "Failed to read how many coverage objectives" +
            " the SUT has reached or improved, so the whole settle window is waited";

    private static final String MESSAGE_HANDLING_FAILURE_WARNING = "Failed to read what the SUT received from the" +
            " address a message was published to, so the whole settle window is waited";

    /**
     * How often, in milliseconds, the completion condition is checked while waiting. Short next
     * to any useful quiet window, long enough that each check (a socket round trip for an
     * external SUT, or a few requests to a broker) costs little.
     */
    static final long POLL_INTERVAL_MS = 25;

    private static final String CAUGHT_UP_FAILURE_WARNING = "The driver failed to tell whether the SUT's" +
            " consumers have caught up, so the whole settle window is waited";

    /**
     * Time in milliseconds. Only differences are used, so it need not be wall-clock time.
     */
    interface Clock {
        long nowMs();
    }

    /**
     * How the waiting thread is put to sleep.
     */
    interface Sleeper {
        void sleep(long ms) throws InterruptedException;
    }

    private final Clock clock;

    private final Sleeper sleeper;

    /**
     * Sleeps for real, and measures with {@link System#nanoTime()}, which does not jump when the
     * system clock changes.
     */
    public AsyncApiSettle() {
        this(() -> System.nanoTime() / NANOS_PER_MILLI, Thread::sleep);
    }

    AsyncApiSettle(Clock clock, Sleeper sleeper) {
        this.clock = Objects.requireNonNull(clock);
        this.sleeper = Objects.requireNonNull(sleeper);
    }

    /**
     * Wait as {@link AsyncApiActionDto#settleMs} and {@link AsyncApiActionDto#completion} ask,
     * and record in {@link AsyncApiReplyDto#settledMs} how long that actually was and in
     * {@link AsyncApiReplyDto#settleEndedBy} why it ended. Nothing is waited or recorded when no
     * wait is asked for.
     *
     * If the thread is interrupted, the wait ends there and the interrupt flag is left set.
     *
     * @param dto               the action that was just published
     * @param reply             what the driver reported about it, to which the wait is added
     * @param objectiveUpdates  how many times the SUT has reached or improved a coverage
     *                          objective in the current test, null if unknown. Only read for
     *                          {@link AsyncApiActionDto.Completion#COVERAGE_QUIESCENCE}
     * @param messageHandling   what the SUT received from an address in the current action, and
     *                          how much of it it handled, null if unknown. Only read for
     *                          {@link AsyncApiActionDto.Completion#HANDLER_RETURN}
     * @param consumersCaughtUp whether the SUT's consumers have caught up, per the driver, null
     *                          if it cannot tell. Only read for
     *                          {@link AsyncApiActionDto.Completion#CONSUMER_LAG}
     */
    public void settle(
            AsyncApiActionDto dto,
            AsyncApiReplyDto reply,
            Supplier<Long> objectiveUpdates,
            Function<String, MessageHandlingState> messageHandling,
            Supplier<Boolean> consumersCaughtUp
    ) {
        Objects.requireNonNull(dto);
        Objects.requireNonNull(reply);
        Objects.requireNonNull(objectiveUpdates);
        Objects.requireNonNull(messageHandling);
        Objects.requireNonNull(consumersCaughtUp);

        Long settleMs = dto.settleMs;
        if (settleMs == null || settleMs <= 0) {
            return;
        }

        boolean quiescence = dto.completion == AsyncApiActionDto.Completion.COVERAGE_QUIESCENCE
                && dto.quietMs != null && dto.quietMs > 0;
        boolean handlerReturn = dto.completion == AsyncApiActionDto.Completion.HANDLER_RETURN;
        boolean consumerLag = dto.completion == AsyncApiActionDto.Completion.CONSUMER_LAG;

        long start = clock.nowMs();
        AsyncApiReplyDto.SettleEnd end;
        try {
            if (quiescence) {
                end = waitForQuiescence(start, settleMs, dto.quietMs, objectiveUpdates);
            } else if (handlerReturn) {
                end = waitForHandlerReturn(start + settleMs, dto.address, messageHandling);
            } else if (consumerLag) {
                end = waitForConsumers(start + settleMs, consumersCaughtUp);
            } else {
                end = waitUntil(start + settleMs);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            end = AsyncApiReplyDto.SettleEnd.INTERRUPTED;
        }
        reply.settledMs = clock.nowMs() - start;
        reply.settleEndedBy = end;
    }

    /**
     * What {@code reader} reads, with a failure counting as unknown: the action was published
     * all the same, so a dropped connection or a slow broker must not fail it.
     *
     * @param warning warned once, as reads come every few milliseconds; the reason goes to the
     *                debug log
     */
    private static <T> T readOrNull(Supplier<T> reader, String warning) {
        try {
            return reader.get();
        } catch (RuntimeException e) {
            SimpleLogger.uniqueWarn(warning);
            SimpleLogger.debug(warning + ": " + e.getMessage());
            return null;
        }
    }

    private AsyncApiReplyDto.SettleEnd waitUntil(long deadline) throws InterruptedException {
        long left = deadline - clock.nowMs();
        if (left > 0) {
            sleeper.sleep(left);
        }
        return AsyncApiReplyDto.SettleEnd.DEADLINE;
    }

    /**
     * Stop once the SUT received something from the action's address in this action and is done
     * with all of it, or at the deadline. What was received before the driver returned counts
     * too, as does a leftover message from an earlier action consumed only now. A missing
     * address or unreadable state means the whole window.
     */
    private AsyncApiReplyDto.SettleEnd waitForHandlerReturn(
            long deadline,
            String address,
            Function<String, MessageHandlingState> messageHandling
    ) throws InterruptedException {

        if (address == null) {
            return waitUntil(deadline);
        }

        while (true) {
            MessageHandlingState state = readOrNull(() -> messageHandling.apply(address),
                    MESSAGE_HANDLING_FAILURE_WARNING);
            if (state == null) {
                return waitUntil(deadline);
            }
            if (state.isHandled()) {
                return AsyncApiReplyDto.SettleEnd.HANDLER_RETURN;
            }

            long now = clock.nowMs();
            if (now >= deadline) {
                return AsyncApiReplyDto.SettleEnd.DEADLINE;
            }
            sleeper.sleep(Math.min(POLL_INTERVAL_MS, deadline - now));
        }
    }

    /**
     * Stop once the SUT's coverage has gone {@code quietMs} without moving, counted from the
     * start of the wait, or at the deadline.
     */
    private AsyncApiReplyDto.SettleEnd waitForQuiescence(
            long start,
            long settleMs,
            long quietMs,
            Supplier<Long> objectiveUpdates
    ) throws InterruptedException {

        long deadline = start + settleMs;

        Long last = readOrNull(objectiveUpdates, OBJECTIVE_UPDATES_FAILURE_WARNING);
        long lastChange = start;

        while (true) {
            if (last == null) {
                //coverage unknown: wait out the window
                return waitUntil(deadline);
            }

            long now = clock.nowMs();
            if (now >= deadline) {
                return AsyncApiReplyDto.SettleEnd.DEADLINE;
            }
            if (now - lastChange >= quietMs) {
                return AsyncApiReplyDto.SettleEnd.QUIESCENCE;
            }

            long nap = Math.min(POLL_INTERVAL_MS, Math.min(deadline - now, lastChange + quietMs - now));
            sleeper.sleep(nap);

            Long current = readOrNull(objectiveUpdates, OBJECTIVE_UPDATES_FAILURE_WARNING);
            if (current != null && !current.equals(last)) {
                lastChange = clock.nowMs();
            }
            last = current;
        }
    }

    /**
     * Stop once the driver says the SUT's consumers have caught up, or at the deadline. The
     * first check is immediate: after a reply the SUT has usually taken the message in already.
     */
    private AsyncApiReplyDto.SettleEnd waitForConsumers(
            long deadline,
            Supplier<Boolean> consumersCaughtUp
    ) throws InterruptedException {

        while (true) {
            //user code, eg asking a broker that may not answer in time
            Boolean caughtUp = readOrNull(consumersCaughtUp, CAUGHT_UP_FAILURE_WARNING);
            if (caughtUp == null) {
                //the driver cannot tell: wait out the window
                return waitUntil(deadline);
            }
            if (caughtUp) {
                return AsyncApiReplyDto.SettleEnd.CAUGHT_UP;
            }

            long left = deadline - clock.nowMs();
            if (left <= 0) {
                return AsyncApiReplyDto.SettleEnd.DEADLINE;
            }
            sleeper.sleep(Math.min(POLL_INTERVAL_MS, left));
        }
    }
}
