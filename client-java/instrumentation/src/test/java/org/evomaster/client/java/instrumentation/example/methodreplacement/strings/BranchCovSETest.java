package org.evomaster.client.java.instrumentation.example.methodreplacement.strings;

import com.foo.somedifferentpackage.examples.methodreplacement.strings.StringsExampleImp;
import org.evomaster.client.java.instrumentation.InstrumentingClassLoader;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.function.Consumer;

import static org.evomaster.client.java.instrumentation.example.ExampleUtils.checkIncreasingTillCoveredForSingleMethodReplacement;

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

        checkIncreasingTillCoveredForSingleMethodReplacement(Arrays.asList("foo123", "foo12", "foo1"), "foo", lambda);
        checkIncreasingTillCoveredForSingleMethodReplacement(Arrays.asList("", "f", "fo"), "foo", lambda);
        checkIncreasingTillCoveredForSingleMethodReplacement(Arrays.asList("foa", "fob", "foc"), "foo", lambda);
        checkIncreasingTillCoveredForSingleMethodReplacement(Arrays.asList("fo}", "fo{"), "foo", lambda);
        checkIncreasingTillCoveredForSingleMethodReplacement(Arrays.asList("f", "xx","fxxx","xxx","xox","fno"), "foo", lambda);
    }


}
