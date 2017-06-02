package org.evomaster.clientJava.instrumentation.example.strings;

import com.foo.somedifferentpackage.examples.strings.StringCallsImp;
import org.evomaster.clientJava.instrumentation.InstrumentingClassLoader;
import org.evomaster.clientJava.instrumentation.staticState.ExecutionTracer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SCinstrumentedTest extends StringCallsTestBase {

    @Override
    protected StringCalls getInstance() throws Exception {
        InstrumentingClassLoader cl = new InstrumentingClassLoader("com.foo");

        return (StringCalls)
                cl.loadClass(StringCallsImp.class.getName())
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
