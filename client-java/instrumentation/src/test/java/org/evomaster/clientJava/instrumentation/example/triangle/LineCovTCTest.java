package org.evomaster.clientJava.instrumentation.example.triangle;

import com.foo.somedifferentpackage.examples.triangle.TriangleClassificationImpl;
import org.evomaster.clientJava.instrumentation.InstrumentingClassLoader;
import org.evomaster.clientJava.instrumentation.staticState.ExecutionTracer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LineCovTCTest {

    @BeforeAll @AfterAll
    public static void reset(){
        ExecutionTracer.reset();
    }

    @Test
    public void testLineCov() throws Exception{

        InstrumentingClassLoader cl = new InstrumentingClassLoader("com.foo");

        TriangleClassification tc =  (TriangleClassification)
                cl.loadClass(TriangleClassificationImpl.class.getName())
                        .newInstance();

        ExecutionTracer.reset();
        assertEquals(0, ExecutionTracer.getNumberOfObjectives());

        tc.classify(-1, 0 , 0);
        int a = ExecutionTracer.getNumberOfObjectives();
        //at least one line should had been covered
        assertTrue(a > 0);

        tc.classify(-1, 0 , 0);
        int b = ExecutionTracer.getNumberOfObjectives();
        //nothing new should had been covered
        assertEquals(a, b);

        tc.classify(1, 1 , 1);
        int c = ExecutionTracer.getNumberOfObjectives();
        //new lines have been covered
        assertTrue(c > b);

        tc.classify(1, 2 , 2);
        int d = ExecutionTracer.getNumberOfObjectives();
        //new lines have been covered
        assertTrue(d > c);
    }


    @Test
    public void testLineCovNotInstrumented() throws Exception {

        InstrumentingClassLoader cl = new InstrumentingClassLoader("org.invalid");

        TriangleClassification tc = (TriangleClassification)
                cl.loadClass(TriangleClassificationImpl.class.getName())
                        .newInstance();

        ExecutionTracer.reset();
        assertEquals(0, ExecutionTracer.getNumberOfObjectives());

        tc.classify(-1, 0, 0);
        int a = ExecutionTracer.getNumberOfObjectives();

        //as not instrumented, nothing should had been reported covered
        assertEquals(0, a);
    }
}
