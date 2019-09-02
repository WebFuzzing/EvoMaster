package org.evomaster.client.java.instrumentation.example.methodreplacement.strings;

import com.foo.somedifferentpackage.examples.methodreplacement.strings.StringsExampleImp;
import org.evomaster.client.java.instrumentation.InstrumentingClassLoader;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SEinstrumentedTest extends StringsExampleTestBase{


    @Override
    protected StringsExample getInstance() throws Exception {

        InstrumentingClassLoader cl = new InstrumentingClassLoader("com.foo");

        return (StringsExample)
                cl.loadClass(StringsExampleImp.class.getName())
                        .newInstance();
    }

    @BeforeEach
    public void init(){
        ExecutionTracer.reset();
        assertEquals(0 , ExecutionTracer.getNumberOfObjectives());
    }

    @AfterEach
    public void checkInstrumentation(){
        assertTrue(ExecutionTracer.getNumberOfObjectives() > 0);
    }
}
