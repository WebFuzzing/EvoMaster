package org.evomaster.clientJava.instrumentation.example.strings;

import com.foo.somedifferentpackage.examples.strings.StringsExampleImp;
import org.evomaster.clientJava.instrumentation.InstrumentingClassLoader;
import org.evomaster.clientJava.instrumentation.staticState.ExecutionTracer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BranchCovSETest {

    @BeforeAll
    @AfterAll
    public static void reset(){
        ExecutionTracer.reset();
    }

    @Test
    public void testBaseBranchCov() throws Exception{

        InstrumentingClassLoader cl = new InstrumentingClassLoader("com.foo");

        StringsExample tc =  (StringsExample)
                cl.loadClass(StringsExampleImp.class.getName())
                        .newInstance();

        ExecutionTracer.reset();
        assertEquals(0, ExecutionTracer.getNumberOfObjectives());

        tc.isFooWithIf("fo1234");

        Set<String> missing = ExecutionTracer.getNonCoveredObjectives(ExecutionTracer.BRANCH);
        String target = missing.iterator().next();
        assertEquals(1, missing.size());
        double heuristic = ExecutionTracer.getValue(target);


        tc.isFooWithIf("fo12");

        missing = ExecutionTracer.getNonCoveredObjectives(ExecutionTracer.BRANCH);
        assertEquals(1, missing.size());
        assertEquals(target, missing.iterator().next());
        double improved = ExecutionTracer.getValue(target);
        assertTrue(improved > heuristic);


        tc.isFooWithIf("foo");

        missing = ExecutionTracer.getNonCoveredObjectives(ExecutionTracer.BRANCH);
        assertEquals(0, missing.size());
        double covered = ExecutionTracer.getValue(target);
        assertEquals(1d, covered);
    }
}
