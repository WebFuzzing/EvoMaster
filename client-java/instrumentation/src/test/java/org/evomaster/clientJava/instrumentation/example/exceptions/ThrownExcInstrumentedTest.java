package org.evomaster.clientJava.instrumentation.example.exceptions;

import com.foo.somedifferentpackage.examples.exceptions.ThrownExcImp;
import org.evomaster.clientJava.instrumentation.InstrumentingClassLoader;
import org.evomaster.clientJava.instrumentation.ObjectiveNaming;
import org.evomaster.clientJava.instrumentation.staticState.ExecutionTracer;
import org.evomaster.clientJava.instrumentation.staticState.ObjectiveRecorder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ThrownExcInstrumentedTest {


    protected ThrownExc getInstance() throws Exception {

        InstrumentingClassLoader cl = new InstrumentingClassLoader("com.foo");

        return (ThrownExc)
                cl.loadClass(ThrownExcImp.class.getName()).newInstance();
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
    public void testDirectReturn() throws Exception{

        ThrownExc te = getInstance();

        //constructor has a default call to Object()
        assertEquals(1, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.SUCCESS_CALL));

        try {
            te.directReturn(null);
            fail("");
        } catch (Exception e){
        }

        assertEquals(2, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.SUCCESS_CALL));
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.SUCCESS_CALL));

        te.directReturn("");
        assertEquals(0, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.SUCCESS_CALL));
    }


    @Test
    public void testDirectInTry() throws Exception{

        ThrownExc te = getInstance();

        //constructor has a default call to Object()
        assertEquals(1, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.SUCCESS_CALL));

        try {
            te.directInTry(null);
            fail("");
        } catch (Exception e){
        }

        assertEquals(2, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.SUCCESS_CALL));
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.SUCCESS_CALL));

        te.directInTry("");
        assertEquals(0, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.SUCCESS_CALL));
    }

    @Test
    public void testDoubleCall() throws Exception{

        ThrownExc te = getInstance();

        int n0 = ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.SUCCESS_CALL);

        try {
            te.doubleCall(null, null);
            fail("");
        } catch (Exception e){
        }

        int n1 = ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.SUCCESS_CALL);
        assertTrue(n1 > n0);
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.SUCCESS_CALL));

        try {
            te.doubleCall("", null);
            fail("");
        } catch (Exception e){
        }

        int n2 = ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.SUCCESS_CALL);
        assertTrue(n2 > n1);
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.SUCCESS_CALL));


        te.doubleCall("", "");
        assertEquals(0, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.SUCCESS_CALL));
    }

    @Test
    public void testCallOnArray() throws Exception{

        ThrownExc te = getInstance();

        try {
            te.callOnArray(null, 0);
            fail("");
        } catch (Exception e){
        }

        //TODO this will change once we handle arrays

        // no call reached yet
        assertEquals(1, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.SUCCESS_CALL));
        assertEquals(0, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.SUCCESS_CALL));

        try {
            te.callOnArray(new String[]{"foo"}, -1);
            fail("");
        } catch (Exception e){
        }

        // still not reached yet
        assertEquals(1, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.SUCCESS_CALL));
        assertEquals(0, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.SUCCESS_CALL));

        try {
            te.callOnArray(new String[]{null}, 0);
            fail("");
        } catch (Exception e){
        }

        // now it is reached yet
        assertEquals(2, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.SUCCESS_CALL));
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.SUCCESS_CALL));

        te.callOnArray(new String[]{"foo"}, 0);

        assertEquals(0, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.SUCCESS_CALL));
    }
}
