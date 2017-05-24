package org.evomaster.clientJava.instrumentation.example.strings;

import com.foo.somedifferentpackage.examples.strings.StringCallsImp;
import org.evomaster.clientJava.instrumentation.InstrumentingClassLoader;
import org.evomaster.clientJava.instrumentation.staticState.ExecutionTracer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.function.Consumer;

import static org.evomaster.clientJava.instrumentation.example.ExampleUtils.checkIncreasingTillCovered;

public class BranchCovSCTest {

    private StringCalls sc;

    @BeforeAll
    @AfterAll
    public static void reset() {
        ExecutionTracer.reset();
    }


    @BeforeEach
    public void initTest() throws Exception {
        InstrumentingClassLoader cl = new InstrumentingClassLoader("com.foo");

        sc = (StringCalls)
                cl.loadClass(StringCallsImp.class.getName())
                        .newInstance();
    }

    @Test
    public void testIsEmpty() throws Exception {


        Consumer<String> lambda = s -> sc.callIsEmpty(s);

        checkIncreasingTillCovered(Arrays.asList("a  b", "a  ", "a ", "a"), "", lambda);
    }

    private class Pair {
        public final String first;
        public final String second;

        public Pair(String first, String second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public String toString() {
            return "['" + first + '\'' + ", '" + second + "']";
        }
    }

    @Test
    public void testEquals() throws Exception {

        Consumer<Pair> lambda = p -> sc.callEquals(p.first, p.second);

        String target = "target";

        checkIncreasingTillCovered(Arrays.asList(
                new Pair("", target),
                new Pair("a", target),
                new Pair("t", target),
                new Pair("targ", target),
                new Pair("t1234", target),
                new Pair("tar3456", target),
                new Pair("ta2345", target),
                new Pair("ta234t", target)
                ),
                new Pair(target, target), lambda);
    }
}
