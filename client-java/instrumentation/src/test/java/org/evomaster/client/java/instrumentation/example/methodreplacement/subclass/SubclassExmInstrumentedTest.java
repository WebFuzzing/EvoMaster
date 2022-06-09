package org.evomaster.client.java.instrumentation.example.methodreplacement.subclass;

import com.foo.somedifferentpackage.examples.methodreplacement.subclass.SubclassExmImp;
import org.evomaster.client.java.instrumentation.InputProperties;
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

        System.setProperty(InputProperties.REPLACEMENT_CATEGORIES, "BASE,SQL,EXT_0");

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

        assertEquals("foobar456", result);

        assertEquals(2, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));
    }

    @Test
    public void testNoCrash() throws Exception{

        assertEquals(0, ExecutionTracer.exposeAdditionalInfoList().get(0).getStringSpecializationsView().size());

        SubclassExm instance = getInstance();

        String result = instance.guavaMap();
        assertEquals("ok", result);

        assertEquals(1, ExecutionTracer.exposeAdditionalInfoList().get(0).getStringSpecializationsView().size());
    }
}
