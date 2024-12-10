package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.distance.heuristics.DistanceHelper;
import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FloatClassReplacementTest {

    @BeforeEach
    public void setUp() {
        ExecutionTracer.reset();
    }

    @Test
    public void testParseSuccess() {
        String floatString = "0.0";
        float parsedFloat = FloatClassReplacement.parseFloat(floatString, ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate");
        assertEquals(0.0, parsedFloat);
    }

    @Test
    public void testParseFails() {
        String floatString = "Hello";
        try {
            FloatClassReplacement.parseFloat(floatString, ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate");
            fail();
        } catch (NumberFormatException ex) {

        }
    }

    @Test
    public void testTooSmallIsTruncated() {
        String floatString = "0.0" + new String(new char[3000]).replace("\0", "0") + "1";
        float parsedFloat = FloatClassReplacement.parseFloat(floatString, ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate");
        assertEquals(0.0, parsedFloat);
    }


    @Test
    public void testFloatVsDoublePrecision() {
        String str = "0.0" + new String(new char[321]).replace("\0", "0") + "1";
        float parsedFloat = FloatClassReplacement.parseFloat(str, ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate");
        double parsedDouble = DoubleClassReplacement.parseDouble(str, ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate");

        assertEquals(0.0, parsedFloat);
        assertTrue(parsedDouble > 0);
    }


    @Test
    public void testDoubleTooSmallIsTruncated() {
        String str = "0.0" + new String(new char[322]).replace("\0", "0") + "1";
        double parsedDouble = DoubleClassReplacement.parseDouble(str, ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate");
        assertEquals(0.0, parsedDouble);
    }

    @Test
    public void testTooBigGoesToFloatInfinity() {
        String str = "1" + new String(new char[3000]).replace("\0", "0");
        float parsedFloat = FloatClassReplacement.parseFloat(str, ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate");
        assertEquals(Float.POSITIVE_INFINITY, parsedFloat);
    }

    @Test
    public void testTooBigGoesToDoubleInfinity() {
        String str = "1" + new String(new char[3000]).replace("\0", "0");
        double parsedDouble = DoubleClassReplacement.parseDouble(str, ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate");
        assertEquals(Double.POSITIVE_INFINITY, parsedDouble);
    }

    @Test
    public void testFloatVsDoubleInfinityPrecision() {
        String str = "1" + new String(new char[200]).replace("\0", "0");
        float parsedFloat = FloatClassReplacement.parseFloat(str, ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate");
        double parsedDouble = DoubleClassReplacement.parseDouble(str, ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate");

        assertEquals(Float.POSITIVE_INFINITY, parsedFloat);
        assertNotEquals(Double.POSITIVE_INFINITY, parsedDouble);
    }

    @Test
    public void testEqualsNull() {
        String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        boolean equals = FloatClassReplacement.equals(1f, null, prefix);
        assertFalse(equals);

        String objectiveId = ExecutionTracer.getNonCoveredObjectives(prefix)
                .iterator().next();
        double h0 = ExecutionTracer.getValue(objectiveId);

        assertEquals(DistanceHelper.H_REACHED_BUT_NULL, h0);
    }

    @Test
    public void testEqualsNotNull() {
        String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        boolean equals = FloatClassReplacement.equals(1f, 2f, prefix);
        assertFalse(equals);

        String objectiveId = ExecutionTracer.getNonCoveredObjectives(prefix)
                .iterator().next();
        double h0 = ExecutionTracer.getValue(objectiveId);

        assertTrue(h0 > DistanceHelper.H_NOT_NULL);
    }

    @Test
    public void testValueOf() {
        String floatString = "0.0";
        float parsedFloat = FloatClassReplacement.valueOf(floatString, ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate");
        assertEquals(0.0, parsedFloat);
    }


}
