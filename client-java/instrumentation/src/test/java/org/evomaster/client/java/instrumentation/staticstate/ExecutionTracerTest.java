package org.evomaster.client.java.instrumentation.staticstate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExecutionTracerTest {

    /**
     * A numeric comparison reports three objectives at once: less than, equal and greater than.
     */
    private static final int OBJECTIVES_PER_COMPARISON = 3;

    private static final String COMPARISON = "ExecutionTracerTest_comparison";

    private static final String OTHER_COMPARISON = "ExecutionTracerTest_other";

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
    public void testEachTestStartsFromZero() {

        ExecutionTracer.executedNumericComparison(COMPARISON, 0.2, 0.2, 0.2);

        ExecutionTracer.reset();
        assertEquals(0, ExecutionTracer.getObjectiveUpdateCount());

        //and what the previous test reached counts again, since coverage is per test
        ExecutionTracer.executedNumericComparison(COMPARISON, 0.2, 0.2, 0.2);
        assertEquals(OBJECTIVES_PER_COMPARISON, ExecutionTracer.getObjectiveUpdateCount());
    }
}
