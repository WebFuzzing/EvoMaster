package org.evomaster.clientJava.instrumentation.example.strings;

import com.foo.somedifferentpackage.examples.strings.StringsExampleImp;
import org.evomaster.clientJava.instrumentation.InstrumentingClassLoader;
import org.evomaster.clientJava.instrumentation.staticState.ExecutionTracer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.function.Consumer;

import static org.evomaster.clientJava.instrumentation.example.ExampleUtils.checkIncreasingTillCovered;

public class BranchCovSETest {

    @BeforeAll
    @AfterAll
    public static void reset(){
        ExecutionTracer.reset();
    }

    @Test
    public void testIsFooWithIf() throws Exception{


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


}
