package org.evomaster.client.java.instrumentation.example.methodreplacement.subclass;

import com.foo.somedifferentpackage.examples.methodreplacement.subclass.SubclassExmImp;
import org.evomaster.client.java.instrumentation.InstrumentingClassLoader;
import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.instrumentation.staticstate.ObjectiveRecorder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by arcuri82 on 19-Sep-19.
 */
public class SubclassExmInstrumentedTest {


    protected SubclassExm getInstance() throws Exception {

        InstrumentingClassLoader cl = new InstrumentingClassLoader("com.foo");

        return (SubclassExm) cl.loadClass(SubclassExmImp.class.getName()).newInstance();
    }

    @BeforeAll
    public static void initClass() {
        ObjectiveRecorder.reset(true);
    }


    @BeforeEach
    public void init() {
        ObjectiveRecorder.reset(false);
        ExecutionTracer.reset();
        assertEquals(0, ExecutionTracer.getNumberOfObjectives());
    }


    @Test
    public void testNoSideEffects() throws Exception{

        assertEquals(0, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));

        SubclassExm instance = getInstance();

        String result = instance.exe();

        assertEquals("foobar", result);

        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));
    }
}
