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

    @Test
    public void testEqualsIgnoreCase() throws Exception {

        Consumer<Pair> lambda = p -> sc.callEqualsIgnoreCase(p.first, p.second);

        String target = "target";

        checkIncreasingTillCovered(Arrays.asList(
                new Pair("", target),
                new Pair("a", target),
                new Pair("ta", target),
                new Pair("taZ", target),
                new Pair("tap", target),
                new Pair("taS", target)
                ),
                new Pair(target, target), lambda);
    }


    @Test
    public void testStartsWith() throws Exception {

        Consumer<Pair> lambda = p -> sc.callStartsWith(p.first, p.second);

        String target = "abc";

        checkIncreasingTillCovered(Arrays.asList(
                new Pair("", target),
                new Pair("1", target),
                new Pair("12", target),
                new Pair("12345", target),
                new Pair("1b345", target),
                new Pair("ab345", target)
                ),
                new Pair(target + "12345", target), lambda);
    }


    private class Triple {
        public final String first;
        public final String second;
        public final int number;

        public Triple(String first, String second, int number) {
            this.first = first;
            this.second = second;
            this.number = number;
        }

        @Override
        public String toString() {
            return "['" + first + '\'' + ", '" + second + ", " + number + "]";
        }
    }


    @Test
    public void testStartsWithOffset() throws Exception {

        Consumer<Triple> lambda = t -> sc.callStartsWith(t.first, t.second, t.number);

        String target = "abc";

        checkIncreasingTillCovered(Arrays.asList(
                new Triple("", target, -100),
                new Triple("1", target, -100),
                new Triple("123abc456", target, -100),
                new Triple("123abc456", target, -90),
                new Triple("123abc456", target, 50),
                new Triple("123abc456", target, 8),
                new Triple("123abc456", target, 7),
                new Triple("123abc456", target, 0),
                new Triple("123abc456", target, 2)
                ),
                new Triple("123" + target + "456", target, 3), lambda);
    }

    @Test
    public void testEndsWith() throws Exception {

        Consumer<Pair> lambda = p -> sc.callEndsWith(p.first, p.second);

        String target = "123";

        checkIncreasingTillCovered(Arrays.asList(
                new Pair("", target),
                new Pair("a", target),
                new Pair("abced", target),
                new Pair("abce1", target),
                new Pair("abce2", target),
                new Pair("abc12", target),
                new Pair("abc121", target)
                ),
                new Pair("foobar" + target, target), lambda);
    }

    @Test
    public void testIsEmpty() throws Exception {


        Consumer<String> lambda = s -> sc.callIsEmpty(s);

        checkIncreasingTillCovered(Arrays.asList("a  b", "a  ", "a ", "a"), "", lambda);
    }


    @Test
    public void testContentEquals() throws Exception {

        Consumer<Pair> lambda = p -> sc.callContentEquals(p.first, p.second);

        String target = "foo";

        checkIncreasingTillCovered(Arrays.asList(
                new Pair("", target),
                new Pair("a", target),
                new Pair("f", target),
                new Pair("fo", target),
                new Pair("f12", target)
                ),
                new Pair(target, target), lambda);
    }

    @Test
    public void testContains() throws Exception {

        Consumer<Pair> lambda = p -> sc.callContains(p.first, p.second);

        String target = "abc";

        checkIncreasingTillCovered(Arrays.asList(
                new Pair("", target),
                new Pair("a", target),
                new Pair("z  ", target),
                new Pair("z  zbcd", target),
                new Pair("z  zbcd bbd", target),
                new Pair("z  zbcd abbd", target)
                ),
                new Pair(target, target), lambda);
    }
}
