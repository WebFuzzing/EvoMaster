package org.evomaster.client.java.instrumentation.staticstate;

import org.evomaster.client.java.instrumentation.Action;
import org.evomaster.client.java.instrumentation.MessageHandlingState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

public class AsyncMessageTrackerTest {

    private static final String REQUESTS = "requests";

    private static final String OTHER = "other";

    private static final long JOIN_TIMEOUT_MS = 10_000;

    @BeforeEach
    public void init() {
        ExecutionTracer.reset();
    }

    @AfterEach
    public void tearDown() {
        ExecutionTracer.reset();
    }

    /**
     * As if this thread had received one message from each of these addresses, in one call.
     */
    private static void receive(String... addresses) {
        AsyncMessageTracker.received(Arrays.asList(addresses));
    }

    private static MessageHandlingState stateOf(String address) {
        return AsyncMessageTracker.getHandlingState(address);
    }

    private static void startAction(int index) {
        ExecutionTracer.setAction(new Action(index, "action" + index, Collections.emptySet(),
                new HashMap<>(), new HashMap<>(), new ArrayList<>()));
    }

    /**
     * Run on a thread of its own, as a second consumer loop would, and wait for it.
     */
    private static void onAnotherThread(Runnable runnable) throws InterruptedException {
        Thread thread = new Thread(runnable);
        thread.start();
        thread.join(JOIN_TIMEOUT_MS);
        assertFalse(thread.isAlive(), "the other thread did not finish");
    }

    @Test
    public void testNothingReceivedIsNotHandled() {

        MessageHandlingState state = stateOf(REQUESTS);

        assertEquals(0, state.getReceived());
        assertEquals(0, state.getUnhandled());
        assertFalse(state.isHandled(), "nothing was received, so nothing can have been handled");
    }

    @Test
    public void testABatchIsHandledWhenTheSameThreadWaitsForMore() {

        receive(REQUESTS, REQUESTS);

        MessageHandlingState received = stateOf(REQUESTS);
        assertEquals(2, received.getReceived());
        assertEquals(2, received.getUnhandled());
        assertFalse(received.isHandled());

        AsyncMessageTracker.askingForMore(true);

        MessageHandlingState handled = stateOf(REQUESTS);
        assertEquals(2, handled.getReceived());
        assertEquals(0, handled.getUnhandled());
        assertTrue(handled.isHandled());
    }

    @Test
    public void testAskingForMoreWithoutWaitingDoesNotHandleTheBatch() {

        receive(REQUESTS);

        //as Spring Kafka does to keep the consumer alive while it retries a failed batch
        AsyncMessageTracker.askingForMore(false);

        assertEquals(1, stateOf(REQUESTS).getUnhandled());
    }

    @Test
    public void testAnotherThreadAskingForMoreDoesNotHandleTheBatch() throws InterruptedException {

        receive(REQUESTS);

        onAnotherThread(() -> AsyncMessageTracker.askingForMore(true));

        assertEquals(1, stateOf(REQUESTS).getUnhandled(), "a thread finished a batch it never received");
    }

    @Test
    public void testEachThreadHandlesItsOwnBatch() throws InterruptedException {

        receive(REQUESTS);
        onAnotherThread(() -> receive(REQUESTS));

        AsyncMessageTracker.askingForMore(true);

        MessageHandlingState state = stateOf(REQUESTS);
        assertEquals(2, state.getReceived());
        assertEquals(1, state.getUnhandled(), "the other thread's batch was taken as handled");
    }

    @Test
    public void testOnlyTheAskedAddressCounts() {

        receive(OTHER);

        assertEquals(0, stateOf(REQUESTS).getReceived());
        assertEquals(1, stateOf(OTHER).getReceived());
    }

    @Test
    public void testABatchFromSeveralAddressesIsHandledForAllOfThem() {

        receive(REQUESTS, OTHER);

        assertFalse(stateOf(REQUESTS).isHandled());
        assertFalse(stateOf(OTHER).isHandled());

        AsyncMessageTracker.askingForMore(true);

        assertTrue(stateOf(REQUESTS).isHandled());
        assertTrue(stateOf(OTHER).isHandled());
    }

    @Test
    public void testSeveralBatchesWithoutWaitingInBetweenAreAllHandledAtOnce() {

        receive(REQUESTS);
        AsyncMessageTracker.askingForMore(false);
        receive(REQUESTS);

        assertEquals(2, stateOf(REQUESTS).getUnhandled());

        AsyncMessageTracker.askingForMore(true);

        assertEquals(0, stateOf(REQUESTS).getUnhandled());
    }

    @Test
    public void testWhatIsReceivedCountsForTheActionItWasReceivedIn() {

        startAction(0);
        receive(REQUESTS);

        startAction(1);
        assertEquals(0, stateOf(REQUESTS).getReceived(), "what the previous action received was counted for this one");

        receive(REQUESTS);
        AsyncMessageTracker.askingForMore(true);

        MessageHandlingState state = stateOf(REQUESTS);
        assertEquals(1, state.getReceived());
        assertTrue(state.isHandled());
    }

    @Test
    public void testAMessageReceivedAgainIsUnhandledAgain() {

        receive(REQUESTS);
        AsyncMessageTracker.askingForMore(true);
        //as after a failed handler, when the consumer is sent back to the message
        receive(REQUESTS);

        MessageHandlingState state = stateOf(REQUESTS);
        assertEquals(2, state.getReceived());
        assertEquals(1, state.getUnhandled(), "the message delivered again is being handled again");
    }

    @Test
    public void testAResetForgetsEverything() {

        receive(REQUESTS);

        ExecutionTracer.reset();

        assertEquals(0, stateOf(REQUESTS).getReceived());

        //and the batch left open in the previous test is not handled in this one
        AsyncMessageTracker.askingForMore(true);
        receive(REQUESTS);
        assertEquals(1, stateOf(REQUESTS).getUnhandled());
    }

    @Test
    public void testABatchReadIntoAnActionOfATestThatIsOverIsDropped() {

        /*
            A consumer thread reads the action index, then a reset starts the next test before
            the batch is recorded: the batch belongs to the test that is over, and must not show
            up in the next one under that action's index.
         */
        startAction(3);
        long generation = AsyncMessageTracker.getGeneration();
        int actionIndex = ExecutionTracer.getActionIndex();

        ExecutionTracer.reset();
        startAction(3);

        AsyncMessageTracker.received(Collections.singletonList(REQUESTS), generation, actionIndex);

        assertEquals(0, stateOf(REQUESTS).getReceived(), "a batch of the previous test was counted in this one");

        //while one read in this test is counted
        AsyncMessageTracker.received(Collections.singletonList(REQUESTS), AsyncMessageTracker.getGeneration(),
                ExecutionTracer.getActionIndex());
        assertEquals(1, stateOf(REQUESTS).getReceived());
    }

    @Test
    public void testNothingReceivedInACallIsNoBatch() {

        AsyncMessageTracker.received(Collections.emptyList());

        assertEquals(0, stateOf(REQUESTS).getReceived());
    }
}
