package org.evomaster.client.java.instrumentation.example;

import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExampleUtils {

    public static <T> void checkIncreasingTillCoveredForSingleMethodReplacement(
            List<T> inputs,
            T solution,
            Consumer<T> lambda) throws Exception{

        ExecutionTracer.reset();
        assertEquals(0, ExecutionTracer.getNumberOfObjectives());

        double heuristics = -1;
        String target = null;

        for(T val : inputs){
            lambda.accept(val);

            Set<String> missing = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT);
            target = missing.iterator().next();
            assertEquals(1, missing.size());

            double h = ExecutionTracer.getValue(target);
            assertTrue(h >= 0);
            assertTrue(h > heuristics, "Fitness did not improve for: " + val.toString());
            assertTrue(h < 1);
            heuristics = h;
        }

        lambda.accept(solution);

        Set<String> missing = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT);
        assertEquals(0, missing.size());
        double covered = ExecutionTracer.getValue(target);
        assertEquals(1d, covered);
    }
}
