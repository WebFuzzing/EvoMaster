package org.evomaster.client.java.instrumentation.staticstate;

import org.evomaster.client.java.instrumentation.MessageHandlingState;

import java.util.*;

/**
 * Keeps track, per test, of the messages the SUT received from a broker client (eg a Kafka
 * consumer's poll) and of whether it is done handling them, so that the controller can tell when
 * a message-driven service has finished with the message an action published.
 *
 * Nothing tells when a handler is done, so a batch counts as handled when the thread that
 * received it asks the client for more, as a consumer loop does. Blind spots of this heuristic:
 * <ul>
 *     <li>Work handed off to another thread (an executor, an async listener) reads as handled
 *     while still running.</li>
 *     <li>A thread that never asks for more (it stops, or closes the client) leaves its last
 *     batch unhandled for good.</li>
 *     <li>Asking for more without waiting, eg a Kafka poll with a zero timeout, does not count:
 *     Spring Kafka does that to keep the consumer alive while it retries a failed batch. A loop
 *     that only ever polls with a zero timeout never looks done.</li>
 *     <li>A failed message the client is told to redeliver (eg a seek back, as Spring Kafka's
 *     error handlers do) reads as handled until it is received again.</li>
 * </ul>
 *
 * Calls the driver makes itself are not recorded, see
 * {@link ExecutionTracer#isExecutingDriverAction()}. Reset with {@link ExecutionTracer#reset()}.
 */
public class AsyncMessageTracker {

    private static final Object lock = new Object();

    /**
     * How many times the tracker was reset, ie which test it is in. Only changed under
     * {@link #lock}; volatile so that it can be read without it.
     */
    private static volatile long generation = 0;

    /**
     * Thread id -> the batches that thread received and has not asked for more after.
     */
    private static final Map<Long, List<Batch>> openBatches = new HashMap<>();

    /**
     * Action index -> address -> received and handled counts.
     */
    private static final Map<Integer, Map<String, Progress>> progress = new HashMap<>();

    /**
     * What one call to the client returned to one thread.
     */
    private static final class Batch {

        /**
         * The action current when the batch was received, which it is counted for.
         */
        final int actionIndex;

        /**
         * Address -> how many of the batch's messages came from it.
         */
        final Map<String, Integer> messagesPerAddress = new LinkedHashMap<>();

        Batch(int actionIndex) {
            this.actionIndex = actionIndex;
        }
    }

    /**
     * Counts for one address within one action.
     */
    private static final class Progress {
        int received;
        int handled;
    }

    /**
     * Forget everything. Called by {@link ExecutionTracer#reset()} at the start of every test.
     */
    public static void reset() {
        synchronized (lock) {
            openBatches.clear();
            progress.clear();
            generation++;
        }
    }

    /**
     * The current thread is asking the client for more messages. If it waits for them, the ones
     * it received before count as handled.
     *
     * @param waitsForMessages whether the call blocks until messages arrive or a timeout runs
     *                         out, as opposed to returning only what is already there
     */
    public static void askingForMore(boolean waitsForMessages) {
        if (!waitsForMessages) {
            return;
        }
        long thread = Thread.currentThread().getId();
        synchronized (lock) {
            List<Batch> done = openBatches.remove(thread);
            if (done == null) {
                return;
            }
            for (Batch batch : done) {
                batch.messagesPerAddress.forEach((address, count) ->
                        progressOf(batch.actionIndex, address).handled += count);
            }
        }
    }

    /**
     * The current thread received these messages, in one call to the client. They count for the
     * current action, and stay unhandled until this thread asks for more, see
     * {@link #askingForMore(boolean)}.
     *
     * @param addresses where each message came from, eg its Kafka topic, one entry per message
     */
    public static void received(List<String> addresses) {
        Objects.requireNonNull(addresses);
        if (addresses.isEmpty()) {
            return;
        }
        /*
            The action index is read before taking this lock, so it is never held while waiting for
            ExecutionTracer's. A reset can then come in between, and the batch would land in the next
            test under an index of the previous one. The generation, read before the index, detects
            that: if it changed by the time the lock is held, the batch is dropped.
         */
        long readInGeneration = generation;
        int actionIndex = ExecutionTracer.getActionIndex();
        received(addresses, readInGeneration, actionIndex);
    }

    /**
     * As {@link #received(List)}, with the generation and the action index already read, so that
     * tests can put a reset in between.
     */
    static void received(List<String> addresses, long readInGeneration, int actionIndex) {
        addresses.forEach(Objects::requireNonNull);
        long thread = Thread.currentThread().getId();

        synchronized (lock) {
            if (readInGeneration != generation) {
                return;
            }
            Batch batch = new Batch(actionIndex);
            for (String address : addresses) {
                batch.messagesPerAddress.merge(address, 1, Integer::sum);
                progressOf(actionIndex, address).received++;
            }
            openBatches.computeIfAbsent(thread, k -> new ArrayList<>()).add(batch);
        }
    }

    /**
     * @return how many times the tracker was reset so far
     */
    static long getGeneration() {
        return generation;
    }

    /**
     * @param address where messages come from, eg a Kafka topic
     * @return what the SUT received from {@code address} in the current action, and how much of
     * it is handled. Never null: nothing received reads as all zeros.
     */
    public static MessageHandlingState getHandlingState(String address) {
        Objects.requireNonNull(address);
        int actionIndex = ExecutionTracer.getActionIndex();
        synchronized (lock) {
            Map<String, Progress> ofAction = progress.get(actionIndex);
            Progress p = ofAction == null ? null : ofAction.get(address);
            if (p == null) {
                return new MessageHandlingState(0, 0);
            }
            return new MessageHandlingState(p.received, p.received - p.handled);
        }
    }

    private static Progress progressOf(int actionIndex, String address) {
        assert Thread.holdsLock(lock);
        return progress
                .computeIfAbsent(actionIndex, k -> new HashMap<>())
                .computeIfAbsent(address, k -> new Progress());
    }
}
