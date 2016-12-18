package org.evomaster.clientJava.instrumentation.example.positiveinteger;

import org.evomaster.clientJava.instrumentation.InstrumentingClassLoader;
import org.evomaster.clientJava.instrumentation.staticState.ExecutionTracer;
import org.foo.somedifferentpackage.examples.positiveinteger.PositiveIntegerImp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PIinstrumentedTest extends PositiveIntegerTestBase {


    @Override
    protected PositiveInteger getInstance() throws Exception {

        InstrumentingClassLoader cl = new InstrumentingClassLoader("org.foo");

        return (PositiveInteger)
                cl.loadClass(PositiveIntegerImp.class.getName())
                        .newInstance();
    }

    @BeforeEach
    public void init(){
        ExecutionTracer.resetState();
        assertEquals(0 , ExecutionTracer.getNumberOfObjectives());
    }

    @AfterEach
    public void checkInstrumentation(){
        assertTrue(ExecutionTracer.getNumberOfObjectives() > 0);
    }

}
