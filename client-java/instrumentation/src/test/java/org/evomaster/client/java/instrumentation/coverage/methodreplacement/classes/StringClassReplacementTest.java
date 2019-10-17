package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class StringClassReplacementTest {

    @Test
    public void testStartsWith() {
        boolean startsWith = StringClassReplacement.startsWith("Hello World", "Hello", ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate");
        assertTrue(startsWith);
    }

    @Test
    public void testEndsWith() {
        boolean endsWith = StringClassReplacement.endsWith("Hello World", "World", ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate");
        assertTrue(endsWith);
    }
}
