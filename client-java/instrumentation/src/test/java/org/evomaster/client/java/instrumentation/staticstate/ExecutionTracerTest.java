package org.evomaster.client.java.instrumentation.staticstate;

import org.evomaster.client.java.instrumentation.Action;
import org.evomaster.client.java.instrumentation.EmittedMessage;
import org.evomaster.client.java.instrumentation.shared.TaintInputName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class ExecutionTracerTest {

    /**
     * A numeric comparison reports three objectives at once: less than, equal and greater than.
     */
    private static final int OBJECTIVES_PER_COMPARISON = 3;

    private static final String TOPIC = "replies";

    private static final long JOIN_TIMEOUT_MS = 10_000;

    private static final String COMPARISON = "ExecutionTracerTest_comparison";

    private static final String OTHER_COMPARISON = "ExecutionTracerTest_other";

    private static final String ACTION_NAME = "ExecutionTracerTest_action";

    /**
     * An input variable of 3 chars: 9 bytes at most once encoded in UTF-8
     */
    private static final String VARIABLE = "abc";

    private static final String NINE_BYTES = "123456789";

    private static final String TEN_BYTES = "1234567890";

    @BeforeEach
    @AfterEach
    public void reset() {
        ExecutionTracer.reset();
    }

    @Test
    public void testNothingReachedMeansNoUpdates() {
        assertEquals(0, ExecutionTracer.getObjectiveUpdateCount());
    }

    @Test
    public void testAnObjectiveReachedForTheFirstTimeCounts() {

        ExecutionTracer.executedNumericComparison(COMPARISON, 0.2, 0.2, 0.2);

        assertEquals(OBJECTIVES_PER_COMPARISON, ExecutionTracer.getObjectiveUpdateCount());

        ExecutionTracer.executedNumericComparison(OTHER_COMPARISON, 0.2, 0.2, 0.2);

        assertEquals(2 * OBJECTIVES_PER_COMPARISON, ExecutionTracer.getObjectiveUpdateCount());
    }

    @Test
    public void testRunningTheSameCodeAgainDoesNotCount() {

        /*
            The point of counting improvements rather than executions: a consumer polling a
            broker runs the same lines forever, and must still be seen to go quiet.
         */
        ExecutionTracer.executedNumericComparison(COMPARISON, 0.2, 0.2, 0.2);
        long first = ExecutionTracer.getObjectiveUpdateCount();

        for (int i = 0; i < 10; i++) {
            ExecutionTracer.executedNumericComparison(COMPARISON, 0.2, 0.2, 0.2);
        }

        assertEquals(first, ExecutionTracer.getObjectiveUpdateCount());
    }

    @Test
    public void testOnlyABetterValueCounts() {

        ExecutionTracer.executedNumericComparison(COMPARISON, 0.2, 0.2, 0.2);
        long first = ExecutionTracer.getObjectiveUpdateCount();

        //worse on all three
        ExecutionTracer.executedNumericComparison(COMPARISON, 0.1, 0.1, 0.1);
        assertEquals(first, ExecutionTracer.getObjectiveUpdateCount());

        //better on one of the three
        ExecutionTracer.executedNumericComparison(COMPARISON, 0.1, 0.9, 0.1);
        assertEquals(first + 1, ExecutionTracer.getObjectiveUpdateCount());
    }

    @Test
    public void testATaintPrefixCouldBeTaintWhateverTheLength() {
        String taint = TaintInputName.getTaintName(0);

        assertCouldBeTaint(true, taint);
        assertCouldBeTaint(true, taint + TEN_BYTES + TEN_BYTES);
    }

    @Test
    public void testWithoutInputVariablesOtherBytesCannotBeTaint() {
        assertCouldBeTaint(false, VARIABLE);
        assertCouldBeTaint(false, TEN_BYTES);
    }

    @Test
    public void testTheLongestInputVariableBoundsTheLength() {
        setInputVariable(VARIABLE);

        // any 9 bytes, not only the variable itself: the check does not decode
        assertCouldBeTaint(true, VARIABLE);
        assertCouldBeTaint(true, NINE_BYTES);
        assertCouldBeTaint(false, TEN_BYTES);
    }

    @Test
    public void testResetForgetsTheInputVariables() {
        setInputVariable(VARIABLE);
        assertCouldBeTaint(true, VARIABLE);

        ExecutionTracer.reset();

        assertCouldBeTaint(false, VARIABLE);
    }

    private static void setInputVariable(String variable) {
        ExecutionTracer.setAction(new Action(0, ACTION_NAME, Collections.singleton(variable),
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyList()));
    }

    private static void assertCouldBeTaint(boolean expected, String value) {
        assertEquals(expected, ExecutionTracer.couldBeTaintInput(value.getBytes(StandardCharsets.UTF_8)),
                "Wrong answer for \"" + value + "\"");
    }

    @Test
    public void testEachTestStartsFromZero() {

        ExecutionTracer.executedNumericComparison(COMPARISON, 0.2, 0.2, 0.2);

        ExecutionTracer.reset();
        assertEquals(0, ExecutionTracer.getObjectiveUpdateCount());

        //and what the previous test reached counts again, since coverage is per test
        ExecutionTracer.executedNumericComparison(COMPARISON, 0.2, 0.2, 0.2);
        assertEquals(OBJECTIVES_PER_COMPARISON, ExecutionTracer.getObjectiveUpdateCount());
    }

    @Test
    public void testTheDriverFlagIsPerThread() throws InterruptedException {

        assertFalse(ExecutionTracer.isExecutingDriverAction());

        ExecutionTracer.setExecutingDriverAction(true);
        try {
            assertTrue(ExecutionTracer.isExecutingDriverAction());

            AtomicBoolean seenElsewhere = new AtomicBoolean(true);
            Thread sut = new Thread(() -> seenElsewhere.set(ExecutionTracer.isExecutingDriverAction()));
            sut.start();
            sut.join(JOIN_TIMEOUT_MS);

            assertFalse(seenElsewhere.get(), "the SUT's threads were taken for the driver");
        } finally {
            ExecutionTracer.setExecutingDriverAction(false);
        }

        assertFalse(ExecutionTracer.isExecutingDriverAction());
    }

    @Test
    public void testWhatTheSutEmitsIsRecordedForTheCurrentAction() {

        EmittedMessage message = new EmittedMessage(TOPIC, null, null);

        ExecutionTracer.addEmittedMessage(message);

        assertEquals(Collections.singletonList(message),
                ExecutionTracer.exposeAdditionalInfoList().get(0).getEmittedMessagesView());
    }
}
