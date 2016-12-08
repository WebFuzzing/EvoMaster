package org.evomaster.clientJava.instrumentation.example.triangle;

import org.evomaster.clientJava.instrumentation.InstrumentingClassLoader;
import org.evomaster.clientJava.instrumentation.staticState.ExecutionTracer;
import org.foo.somedifferentpackage.examples.triangle.TriangleClassificationImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TCinstrumentedTest extends TriangleClassificationTestBase {

    @Override
    protected TriangleClassification getInstance() throws Exception {

        InstrumentingClassLoader cl = new InstrumentingClassLoader("org.foo");

        return (TriangleClassification)
                cl.loadClass(TriangleClassificationImpl.class.getName())
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
