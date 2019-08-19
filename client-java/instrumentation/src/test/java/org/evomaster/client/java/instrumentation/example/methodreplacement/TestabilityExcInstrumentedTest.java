package org.evomaster.client.java.instrumentation.example.methodreplacement;

import com.foo.somedifferentpackage.examples.methodreplacement.TestabilityExcImp;
import org.evomaster.client.java.instrumentation.InstrumentingClassLoader;
import org.evomaster.client.java.instrumentation.ObjectiveNaming;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.instrumentation.staticstate.ObjectiveRecorder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by arcuri82 on 27-Jun-19.
 */
public class TestabilityExcInstrumentedTest {

    protected TestabilityExc getInstance() throws Exception {

        InstrumentingClassLoader cl = new InstrumentingClassLoader("com.foo");

        return (TestabilityExc)
                cl.loadClass(TestabilityExcImp.class.getName()).newInstance();
    }

    @BeforeAll
    public static void initClass(){
        ObjectiveRecorder.reset(true);
    }


    @BeforeEach
    public void init(){
        ObjectiveRecorder.reset(false);
        ExecutionTracer.reset();
        assertEquals(0 , ExecutionTracer.getNumberOfObjectives());
    }


    @Test
    public void testParseIntValid() throws Exception{

        TestabilityExc te = getInstance();

        assertThrows(Exception.class, () -> te.parseInt(null));

        assertEquals(2, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.METHOD_REPLACEMENT));
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));

        int value = te.parseInt("1");
        assertEquals(1, value);
        assertEquals(0, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));
    }


    @Test
    public void testParseIntHeuristic() throws Exception{

        TestabilityExc te = getInstance();

        assertThrows(Exception.class, () -> te.parseInt(null));

        assertEquals(2, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.METHOD_REPLACEMENT));
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));

        String targetId = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT)
                .iterator().next();

        double h0 = ExecutionTracer.getValue(targetId);
        assertTrue(h0> 0); //reached
        assertTrue(h0 < 1);//but no covered
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));

        assertThrows(Exception.class, () -> te.parseInt("z"));
        double h1 = ExecutionTracer.getValue(targetId);
        assertTrue(h1> h0); //better
        assertTrue(h1 < 1);//but still no covered
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));


        assertThrows(Exception.class, () -> te.parseInt("a"));
        double h2 = ExecutionTracer.getValue(targetId);
        assertTrue(h2> h1); //better
        assertTrue(h2 < 1);//but still no covered
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));

        te.parseInt("1");
        double h3 = ExecutionTracer.getValue(targetId);
        assertTrue(h3> h2); //better
        assertEquals(1, h3);//covered
        assertEquals(0, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));
    }


    @Test
    public void testParseLocalDateHeuristic() throws Exception{

        TestabilityExc te = getInstance();

        assertThrows(Exception.class, () -> te.parseLocalDate(null));

        assertEquals(2, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.METHOD_REPLACEMENT));
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));

        String targetId = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT)
                .iterator().next();

        double h0 = ExecutionTracer.getValue(targetId);
        assertTrue(h0> 0); //reached
        assertTrue(h0 < 1);//but no covered
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));

        assertThrows(Exception.class, () -> te.parseLocalDate("z"));
        double h1 = ExecutionTracer.getValue(targetId);
        assertTrue(h1> h0); //better
        assertTrue(h1 < 1);//but still no covered
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));


        assertThrows(Exception.class, () -> te.parseLocalDate("1234-11-aa"));
        double h2 = ExecutionTracer.getValue(targetId);
        assertTrue(h2> h1); //better
        assertTrue(h2 < 1);//but still no covered
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));

        LocalDate date = te.parseLocalDate("1234-11-11");
        assertEquals(1234, date.getYear());
        double h3 = ExecutionTracer.getValue(targetId);
        assertTrue(h3> h2); //better
        assertEquals(1, h3);//covered
        assertEquals(0, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));
    }
}
