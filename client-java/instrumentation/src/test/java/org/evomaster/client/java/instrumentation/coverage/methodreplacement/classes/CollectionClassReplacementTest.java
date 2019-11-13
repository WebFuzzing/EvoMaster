package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by jgaleotti on 29-Ago-19.
 */
public class CollectionClassReplacementTest {

    @BeforeEach
    public void setUp() {
        ExecutionTracer.reset();
    }

    @Test
    public void testIsEmpty() {
        final String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "idTemplate";
        List<Object> emptyList = Collections.emptyList();
        boolean isEmptyValue = CollectionClassReplacement.isEmpty(emptyList, prefix);
        assertTrue(isEmptyValue);
        Set<String> nonCoveredObjectives = ExecutionTracer.getNonCoveredObjectives(prefix);
        assertEquals(1, nonCoveredObjectives.size());
        String objectiveId = nonCoveredObjectives.iterator().next();
        double value = ExecutionTracer.getValue(objectiveId);
        assertEquals(0, value);
    }

    @Test
    public void testIsNotEmpty() {
        List<Object> emptyList = Collections.singletonList("Hello World");
        boolean isEmptyValue = CollectionClassReplacement.isEmpty(emptyList, ObjectiveNaming.METHOD_REPLACEMENT + "idTemplate");
        assertFalse(isEmptyValue);
    }

    @Test
    public void testNull() {
        assertThrows(NullPointerException.class,
                () -> {
                    CollectionClassReplacement.isEmpty(null, ObjectiveNaming.METHOD_REPLACEMENT + "idTemplate");
                });
    }

}
