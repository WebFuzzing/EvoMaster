package org.evomaster.clientJava.instrumentation.example.positiveinteger;

import com.foo.somedifferentpackage.examples.positiveinteger.PositiveIntegerImp;
import org.evomaster.clientJava.instrumentation.InstrumentingClassLoader;
import org.evomaster.clientJava.instrumentation.staticState.ExecutionTracer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LineCovPITest {

    @BeforeAll
    @AfterAll
    public static void reset() {
        ExecutionTracer.reset();
    }

    @Test
    public void testLineCov() throws Exception {

        InstrumentingClassLoader cl = new InstrumentingClassLoader("com.foo");

        PositiveInteger pi = (PositiveInteger)
                cl.loadClass(PositiveIntegerImp.class.getName())
                        .newInstance();

        ExecutionTracer.reset();
        assertEquals(0, ExecutionTracer.getNumberOfObjectives());

        pi.isPositive(2);
        int a = ExecutionTracer.getNumberOfObjectives();
        //at least one line should had been covered
        assertTrue(a > 0);

        pi.isPositive(3);
        int b = ExecutionTracer.getNumberOfObjectives();
        //nothing new should had been covered
        assertEquals(a, b);

        pi.isPositive(-4);
        int c = ExecutionTracer.getNumberOfObjectives();
        //new lines have been covered
        assertTrue(c > b);
    }


    @Test
    public void testLineCovNotInstrumented() throws Exception {

        InstrumentingClassLoader cl = new InstrumentingClassLoader("org.invalid");

        PositiveInteger pi = (PositiveInteger)
                cl.loadClass(PositiveIntegerImp.class.getName())
                        .newInstance();

        ExecutionTracer.reset();
        assertEquals(0, ExecutionTracer.getNumberOfObjectives());

        pi.isPositive(2);

        assertEquals(0, ExecutionTracer.getNumberOfObjectives());
    }
}
