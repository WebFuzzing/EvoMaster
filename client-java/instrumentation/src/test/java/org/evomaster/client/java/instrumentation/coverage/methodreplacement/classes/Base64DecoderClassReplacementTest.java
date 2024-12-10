package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.distance.heuristics.DistanceHelper;
import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

public class Base64DecoderClassReplacementTest {

    @BeforeEach
    public void setUp() {
        ExecutionTracer.reset();
    }

    @Test
    public void testUnsuccessfulDecode() {
        String invalidEncodingString = "$";
        Base64.Decoder decoder = Base64.getDecoder();
        String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        try {
            Base64DecoderClassReplacement.decode(decoder, invalidEncodingString, prefix);
            fail();
        } catch (IllegalArgumentException ex) {
            assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(prefix));
            String objectiveId = ExecutionTracer.getNonCoveredObjectives(prefix)
                    .iterator().next();
            double h0 = ExecutionTracer.getValue(objectiveId);
            assertTrue(h0 > 0);
        }
    }


    @Test
    public void testSuccessfulDecode() {
        String encodedString = Base64.getEncoder().encodeToString(new byte[]{0, 1, 2});
        byte[] expected = Base64.getDecoder().decode(encodedString);
        Base64.Decoder decoder = Base64.getDecoder();
        String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        byte[] actual = Base64DecoderClassReplacement.decode(decoder, encodedString, prefix);
        assertArrayEquals(expected, actual);

        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(prefix));

        String objectiveId = ExecutionTracer.getNonCoveredObjectives(prefix)
                .iterator().next();
        double h0 = ExecutionTracer.getValue(objectiveId);
        assertEquals(DistanceHelper.H_NOT_NULL, h0);
    }

    @Test
    public void testSuccessfulDecodeUsingString() {
        String encodedString = "Hello+World/";
        byte[] expected = Base64.getDecoder().decode(encodedString);
        Base64.Decoder decoder = Base64.getDecoder();
        String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        byte[] actual = Base64DecoderClassReplacement.decode(decoder, encodedString, prefix);
        assertArrayEquals(expected, actual);

        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(prefix));

        String objectiveId = ExecutionTracer.getNonCoveredObjectives(prefix)
                .iterator().next();
        double h0 = ExecutionTracer.getValue(objectiveId);
        assertEquals(DistanceHelper.H_NOT_NULL, h0);
    }

    @Test
    public void testUnsuccessfulDecodeUsingString() {
        String invalidEncodingString = "Hello+World/=";
        Base64.Decoder decoder = Base64.getDecoder();
        String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        try {
            Base64DecoderClassReplacement.decode(decoder, invalidEncodingString, prefix);
            fail();
        } catch (IllegalArgumentException ex) {
            assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(prefix));
            String objectiveId = ExecutionTracer.getNonCoveredObjectives(prefix)
                    .iterator().next();
            double h0 = ExecutionTracer.getValue(objectiveId);
            assertTrue(h0 > 0);
        }
    }


    @Test
    public void testSuccessfulPadding() {
        String encodedString = "Helloo==";
        byte[] expected = Base64.getDecoder().decode(encodedString);
        Base64.Decoder decoder = Base64.getDecoder();
        String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        byte[] actual = Base64DecoderClassReplacement.decode(decoder, encodedString, prefix);
        assertArrayEquals(expected, actual);

        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(prefix));

        String objectiveId = ExecutionTracer.getNonCoveredObjectives(prefix)
                .iterator().next();
        double h0 = ExecutionTracer.getValue(objectiveId);
        assertEquals(DistanceHelper.H_NOT_NULL, h0);
    }

}
