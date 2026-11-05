package org.evomaster.client.java.controller.internal;

import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiActionDto;
import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiReplyDto;

import java.util.Objects;

/**
 * Keeps an AsyncAPI action open for a while after the driver has returned from publishing it,
 * so that what the service does with the message, on a thread of its own, is still attributed
 * to that action. Done in the controller so that drivers need not implement it, and the core
 * pays no extra round trip.
 */
public class AsyncApiSettle {

    private static final long NANOS_PER_MILLI = 1_000_000L;

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
     * Wait as long as {@link AsyncApiActionDto#settleMs} asks, and record in
     * {@link AsyncApiReplyDto#settledMs} how long that actually was. Nothing is waited, and
     * nothing recorded, when no wait is asked for.
     *
     * The thread calling this is a request thread of the controller, whose code is never
     * instrumented, so the kill switch does not interrupt it. If it is interrupted all the same,
     * the wait ends there, the interrupt is left set for whoever owns the thread, and the time
     * actually waited is what gets recorded.
     *
     * @param dto   the action that was just published
     * @param reply what the driver reported about it, to which the wait is added
     */
    public void settle(AsyncApiActionDto dto, AsyncApiReplyDto reply) {
        Objects.requireNonNull(dto);
        Objects.requireNonNull(reply);

        Long settleMs = dto.settleMs;
        if (settleMs == null || settleMs <= 0) {
            return;
        }

        long start = clock.nowMs();
        try {
            sleeper.sleep(settleMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        reply.settledMs = clock.nowMs() - start;
    }
}
