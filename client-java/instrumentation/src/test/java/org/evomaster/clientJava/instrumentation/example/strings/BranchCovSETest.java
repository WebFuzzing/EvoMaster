package org.evomaster.clientJava.instrumentation.example.strings;

import com.foo.somedifferentpackage.examples.strings.StringsExampleImp;
import org.evomaster.clientJava.instrumentation.InstrumentingClassLoader;
import org.evomaster.clientJava.instrumentation.staticState.ExecutionTracer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BranchCovSETest {

    @BeforeAll
    @AfterAll
    public static void reset(){
        ExecutionTracer.reset();
    }

    @Test
    public void testisFooWithIf() throws Exception{


        InstrumentingClassLoader cl = new InstrumentingClassLoader("com.foo");

        StringsExample tc =  (StringsExample)
                cl.loadClass(StringsExampleImp.class.getName())
                        .newInstance();

        Consumer<String> lambda = s -> tc.isFooWithIf(s);

        checkIncreasingTillCovered(Arrays.asList("foo123", "foo12", "foo1"), "foo", lambda);
        checkIncreasingTillCovered(Arrays.asList("", "f", "fo"), "foo", lambda);
        checkIncreasingTillCovered(Arrays.asList("foa", "fob", "foc"), "foo", lambda);
        checkIncreasingTillCovered(Arrays.asList("fo}", "fo{"), "foo", lambda);
        checkIncreasingTillCovered(Arrays.asList("f", "xx","fxxx","xxx","xox","fno"), "foo", lambda);
    }

    private void checkIncreasingTillCovered(List<String> inputs, String solution, Consumer<String> lambda) throws Exception{

        ExecutionTracer.reset();
        assertEquals(0, ExecutionTracer.getNumberOfObjectives());

        double heuristics = -1;
        String target = null;

        for(String val : inputs){
            lambda.accept(val);

            Set<String> missing = ExecutionTracer.getNonCoveredObjectives(ExecutionTracer.BRANCH);
            target = missing.iterator().next();
            assertEquals(1, missing.size());

            double h = ExecutionTracer.getValue(target);
            assertTrue(h > heuristics);
            assertTrue(h < 1);
            heuristics = h;
        }

        lambda.accept(solution);

        Set<String> missing = ExecutionTracer.getNonCoveredObjectives(ExecutionTracer.BRANCH);
        assertEquals(0, missing.size());
        double covered = ExecutionTracer.getValue(target);
        assertEquals(1d, covered);
    }
}
