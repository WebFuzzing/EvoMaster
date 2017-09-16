package org.evomaster.clientJava.instrumentation.example.triangle;

import com.foo.somedifferentpackage.examples.triangle.TriangleClassificationImpl;
import org.evomaster.clientJava.instrumentation.InstrumentingClassLoader;
import org.evomaster.clientJava.instrumentation.ObjectiveNaming;
import org.evomaster.clientJava.instrumentation.staticState.ExecutionTracer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by arcuri82 on 20-Feb-17.
 */
public class BranchCovTCTest {

    @BeforeAll
    @AfterAll
    public static void reset(){
        ExecutionTracer.reset();
    }

    @Test
    public void testBaseBranchCov() throws Exception{

        InstrumentingClassLoader cl = new InstrumentingClassLoader("com.foo");

        TriangleClassification tc =  (TriangleClassification)
                cl.loadClass(TriangleClassificationImpl.class.getName())
                        .newInstance();

        ExecutionTracer.reset();
        assertEquals(0, ExecutionTracer.getNumberOfObjectives());

        tc.classify(-10, 0 , 0);

        Set<String> missing = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.BRANCH);
        String target = missing.iterator().next();
        assertEquals(1, missing.size());

        double heuristic = ExecutionTracer.getValue(target);

        tc.classify(-2, 0 , 0);

        missing = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.BRANCH);
        assertEquals(1, missing.size());
        assertEquals(target, missing.iterator().next());

        double improved = ExecutionTracer.getValue(target);

        assertTrue(improved > heuristic);
    }


    @Test
    public void testEquilateral() throws Exception{

        InstrumentingClassLoader cl = new InstrumentingClassLoader("com.foo");

        TriangleClassification tc =  (TriangleClassification)
                cl.loadClass(TriangleClassificationImpl.class.getName())
                        .newInstance();

        ExecutionTracer.reset();
        assertEquals(0, ExecutionTracer.getNumberOfObjectives());

        tc.classify(-1,  1,  1);
        tc.classify( 1, -1,  1);
        tc.classify( 2,  1, -1);
        tc.classify( 9,  1,  1);
        tc.classify( 1,  9,  1);
        tc.classify( 1,  2,  9);
        tc.classify( 4,  3,  2);
        tc.classify( 4,  3 , 3);
        tc.classify( 4,  3 , 4);
        tc.classify(20, 20,  1); //only case with a==b
        //by now, all branches but last "b==c" check on equilateral should had been taken

        Set<String> missing = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.BRANCH);
        String target = missing.iterator().next();
        assertEquals(1, missing.size());

        double h = ExecutionTracer.getValue(target);

        tc.classify(20, 20,  100); // no better
        double same = ExecutionTracer.getValue(target);

        assertEquals(h, same, 0.001);

        tc.classify(20, 20,  10); //better
        double better =  ExecutionTracer.getValue(target);
        assertTrue(better > h);

        tc.classify(20, 20,  20); //covered
        double covered =  ExecutionTracer.getValue(target);
        assertTrue(covered > better);
        assertEquals(1.0, covered, 0.001);

        missing = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.BRANCH);
        assertEquals(0, missing.size());
    }
}
