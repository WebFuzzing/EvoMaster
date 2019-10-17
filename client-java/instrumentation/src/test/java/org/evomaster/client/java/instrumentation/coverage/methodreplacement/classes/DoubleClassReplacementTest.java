package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DoubleClassReplacementTest {

    @Test
    public void testParseSuccessSingleDot() {
        String inputString = "0.0";
        double parsedDouble = DoubleClassReplacement.parseDouble(inputString, ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate");
        assertEquals(0.0, parsedDouble);
    }

    @Test
    public void testParseInteger() {
        String inputString = "0";
        double parsedDouble = DoubleClassReplacement.parseDouble(inputString, ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate");
        assertEquals(0.0, parsedDouble);
    }

    @Test
    public void testParseSuccessMissingZero() {
        String inputString = "0.";
        double parsedDouble = DoubleClassReplacement.parseDouble(inputString, ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate");
        assertEquals(0.0, parsedDouble);
    }

    @Test
    public void testParseSuccessDotZero() {
        String inputString = ".0";
        double parsedDouble = DoubleClassReplacement.parseDouble(inputString, ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate");
        assertEquals(0.0, parsedDouble);
    }

    @Test
    public void testParseSuccessOnlyDot() {
        String inputString = ".";
        assertThrows(NumberFormatException.class, () -> {
            DoubleClassReplacement.parseDouble(inputString, ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate");
        });
    }

    @Test
    public void testParseExponentialNotation() {
        String inputString = "9.18E+09";
        double parsedDouble =DoubleClassReplacement.parseDouble(inputString, ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate");
    }

    @Test
    public void testParseVeryBigDouble() {
        String inputString = "9.18E+10000";
        double parsedDouble =DoubleClassReplacement.parseDouble(inputString, ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate");
        assertEquals(Double.POSITIVE_INFINITY, parsedDouble);
    }
}
