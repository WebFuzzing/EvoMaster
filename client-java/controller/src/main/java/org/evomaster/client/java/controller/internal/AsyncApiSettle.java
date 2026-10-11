package org.evomaster.client.java.controller.internal;

import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiActionDto;
import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiReplyDto;
import org.evomaster.client.java.instrumentation.MessageHandlingState;

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
     * How often the SUT's coverage, or what it received, is read while waiting for it to be done,
     * in milliseconds. Short next to any useful stable window, and long enough that reading it, a
     * socket round trip for an external SUT, costs little.
     */
    static final long POLL_INTERVAL_MS = 25;

    /**
     * Where the time comes from, in milliseconds. Only the difference between two readings is
     * ever used, so it does not have to be wall-clock time.
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
     * @param objectiveUpdates  reads how many times the SUT has reached or improved a coverage
     *                          objective in the current test, or null when that cannot be known.
     *                          Only read for {@link AsyncApiActionDto.Completion#COVERAGE_STABLE}
     */
    public void settle(AsyncApiActionDto dto, AsyncApiReplyDto reply, Supplier<Long> objectiveUpdates) {
        settle(dto, reply, objectiveUpdates, address -> null);
    }

    /**
     * As {@link #settle(AsyncApiActionDto, AsyncApiReplyDto, Supplier)}, for every completion.
     *
     * @param messageHandling   reads what the SUT received from an address in the current action,
     *                          and how much of it it handled, or null when that cannot be known.
     *                          Only read, with {@link AsyncApiActionDto#address}, for
     *                          {@link AsyncApiActionDto.Completion#HANDLER_RETURN}
     */
    public void settle(
            AsyncApiActionDto dto,
            AsyncApiReplyDto reply,
            Supplier<Long> objectiveUpdates,
            Function<String, MessageHandlingState> messageHandling
    ) {
        Objects.requireNonNull(dto);
        Objects.requireNonNull(reply);
        Objects.requireNonNull(objectiveUpdates);
        Objects.requireNonNull(messageHandling);

        Long settleMs = dto.settleMs;
        if (settleMs == null || settleMs <= 0) {
            return;
        }

        boolean coverageStable = dto.completion == AsyncApiActionDto.Completion.COVERAGE_STABLE
                && dto.stableMs != null && dto.stableMs > 0;
        boolean handlerReturn = dto.completion == AsyncApiActionDto.Completion.HANDLER_RETURN;

        long start = clock.nowMs();
        AsyncApiReplyDto.SettleEnd end;
        try {
            if (coverageStable) {
                end = waitForStableCoverage(start, settleMs, dto.stableMs, objectiveUpdates);
            } else if (handlerReturn) {
                end = waitForHandlerReturn(start + settleMs, dto.address, messageHandling);
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
     * What {@code reader} reads, where a failure to read counts as not knowing. Reading is a
     * socket round trip for an external SUT, and the action was published all the same: a
     * failure here would otherwise turn a dropped connection into a broken setup.
     *
     * @param warning what to warn about, once, as reads come every few milliseconds; the reason
     *                goes to the debug log
     */
    private static <T> T readOrNull(Supplier<T> reader, String warning) {
        try {
            return reader.get();
        } catch (RuntimeException e) {
            org.evomaster.client.java.utils.SimpleLogger.uniqueWarn(warning);
            org.evomaster.client.java.utils.SimpleLogger.debug(warning + ": " + e.getMessage());
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
     * Stop once the SUT's coverage has gone {@code stableMs} without moving, counted from the
     * start of the wait, or at the deadline.
     */
    private AsyncApiReplyDto.SettleEnd waitForStableCoverage(
            long start,
            long settleMs,
            long stableMs,
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
            if (now - lastChange >= stableMs) {
                return AsyncApiReplyDto.SettleEnd.COVERAGE_STABLE;
            }

            long nap = Math.min(POLL_INTERVAL_MS, Math.min(deadline - now, lastChange + stableMs - now));
            sleeper.sleep(nap);

            Long current = readOrNull(objectiveUpdates, OBJECTIVE_UPDATES_FAILURE_WARNING);
            if (current != null && !current.equals(last)) {
                lastChange = clock.nowMs();
            }
            last = current;
        }
    }
}
