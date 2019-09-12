package org.evomaster.client.java.instrumentation.example.methodreplacement;

import com.foo.somedifferentpackage.examples.methodreplacement.TestabilityExcImp;
import org.evomaster.client.java.instrumentation.InstrumentingClassLoader;
import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.instrumentation.staticstate.ObjectiveRecorder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;

import static org.evomaster.client.java.instrumentation.coverage.methodreplacement.DistanceHelper.H_REACHED_BUT_NULL;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by arcuri82 on 27-Jun-19.
 */
public class TestabilityExcInstrumentedTest {

    protected TestabilityExc getInstance() throws Exception {

        InstrumentingClassLoader cl = new InstrumentingClassLoader("com.foo");

        return (TestabilityExc)
                cl.loadClass(TestabilityExcImp.class.getName()).newInstance();
    }

    @BeforeAll
    public static void initClass() {
        ObjectiveRecorder.reset(true);
    }


    @BeforeEach
    public void init() {
        ObjectiveRecorder.reset(false);
        ExecutionTracer.reset();
        assertEquals(0, ExecutionTracer.getNumberOfObjectives());
    }


    @Test
    public void testParseIntValid() throws Exception {

        TestabilityExc te = getInstance();

        assertThrows(Exception.class, () -> te.parseInt(null));

        assertEquals(2, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.METHOD_REPLACEMENT));
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));

        int value = te.parseInt("1");
        assertEquals(1, value);
        assertEquals(0, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));
    }


    @Test
    public void testParseIntHeuristic() throws Exception {

        TestabilityExc te = getInstance();

        assertThrows(Exception.class, () -> te.parseInt(null));

        assertEquals(2, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.METHOD_REPLACEMENT));
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));

        String targetId = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT)
                .iterator().next();

        double h0 = ExecutionTracer.getValue(targetId);
        assertTrue(h0 > 0); //reached
        assertTrue(h0 < 1);//but no covered
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));

        assertThrows(Exception.class, () -> te.parseInt("z"));
        double h1 = ExecutionTracer.getValue(targetId);
        assertTrue(h1 > h0); //better
        assertTrue(h1 < 1);//but still no covered
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));


        assertThrows(Exception.class, () -> te.parseInt("a"));
        double h2 = ExecutionTracer.getValue(targetId);
        assertTrue(h2 > h1); //better
        assertTrue(h2 < 1);//but still no covered
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));

        te.parseInt("1");
        double h3 = ExecutionTracer.getValue(targetId);
        assertTrue(h3 > h2); //better
        assertEquals(1, h3);//covered
        assertEquals(0, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));
    }


    @Test
    public void testParseLocalDateHeuristic() throws Exception {

        TestabilityExc te = getInstance();

        assertThrows(Exception.class, () -> te.parseLocalDate(null));

        assertEquals(2, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.METHOD_REPLACEMENT));
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));

        String targetId = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT)
                .iterator().next();

        double h0 = ExecutionTracer.getValue(targetId);
        assertTrue(h0 > 0); //reached
        assertTrue(h0 < 1);//but no covered
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));

        assertThrows(Exception.class, () -> te.parseLocalDate("z"));
        double h1 = ExecutionTracer.getValue(targetId);
        assertTrue(h1 > h0); //better
        assertTrue(h1 < 1);//but still no covered
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));


        assertThrows(Exception.class, () -> te.parseLocalDate("1234-11-aa"));
        double h2 = ExecutionTracer.getValue(targetId);
        assertTrue(h2 > h1); //better
        assertTrue(h2 < 1);//but still no covered
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));

        LocalDate date = te.parseLocalDate("1234-11-11");
        assertEquals(1234, date.getYear());
        double h3 = ExecutionTracer.getValue(targetId);
        assertTrue(h3 > h2); //better
        assertEquals(1, h3);//covered
        assertEquals(0, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));
    }


    @Test
    public void testDateAfter() throws Exception {

        TestabilityExc te = getInstance();

        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm a");
        Date dateInstance1 = sdf.parse("07/15/2016 11:00 AM");
        Date dateInstance2 = sdf.parse("07/15/2016 11:15 AM");

        te.after(dateInstance1, dateInstance2);

        assertEquals(2, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.METHOD_REPLACEMENT));
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));

        String targetId = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT)
                .iterator().next();

        double h0 = ExecutionTracer.getValue(targetId);
        assertTrue(h0 > 0); //reached
        assertTrue(h0 < 1);//but no covered
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));

        te.after(dateInstance1, dateInstance1);
        double h1 = ExecutionTracer.getValue(targetId);

        assertTrue(h1 > h0); //better
        assertTrue(h1 < 1);//but not covered
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));

        te.after(dateInstance2, dateInstance1);
        double h2 = ExecutionTracer.getValue(targetId);

        assertTrue(h2 > h1); //better
        assertEquals(1, h2);//covered
        assertEquals(0, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));

    }

    @Test
    public void testDateBefore() throws Exception {

        TestabilityExc te = getInstance();

        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm a");
        Date dateInstance1 = sdf.parse("07/15/2016 11:00 AM");
        Date dateInstance2 = sdf.parse("07/15/2016 11:15 AM");

        te.before(dateInstance2, dateInstance1);

        assertEquals(2, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.METHOD_REPLACEMENT));
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));

        String targetId = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT)
                .iterator().next();

        double h0 = ExecutionTracer.getValue(targetId);
        assertTrue(h0 > 0); //reached
        assertTrue(h0 < 1);//but no covered
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));

        te.before(dateInstance1, dateInstance1);
        double h1 = ExecutionTracer.getValue(targetId);

        assertTrue(h1 > h0); //better
        assertTrue(h1 < 1);//but not covered
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));

        te.before(dateInstance1, dateInstance2);
        double h2 = ExecutionTracer.getValue(targetId);

        assertTrue(h2 > h1); //better
        assertEquals(1, h2);//covered
        assertEquals(0, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));

    }

    @Test
    public void testDateEquals() throws Exception {

        TestabilityExc te = getInstance();

        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm a");
        Date dateInstance1 = sdf.parse("07/15/2016 11:00 AM");
        Date dateInstance2 = sdf.parse("07/15/2016 11:15 AM");
        Date dateInstance3 = sdf.parse("07/15/2016 11:30 AM");

        te.equals(dateInstance1, dateInstance3);

        assertEquals(2, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.METHOD_REPLACEMENT));
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));

        String targetId = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT)
                .iterator().next();

        double h0 = ExecutionTracer.getValue(targetId);
        assertTrue(h0 > 0); //reached
        assertTrue(h0 < 1);//but no covered
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));

        te.equals(dateInstance1, dateInstance2);
        double h1 = ExecutionTracer.getValue(targetId);

        assertTrue(h1 > h0); //better
        assertTrue(h1 < 1);//but not covered
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));

        te.equals(dateInstance1, dateInstance1);
        double h2 = ExecutionTracer.getValue(targetId);

        assertTrue(h2 > h1); //better
        assertEquals(1, h2);//covered
        assertEquals(0, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));

    }


    @Test
    public void testCollectionsIsEmpty() throws Exception {

        TestabilityExc te = getInstance();

        List<String> nonSingletonNonEmptyList = Arrays.asList("One", "Two");
        te.isEmpty(nonSingletonNonEmptyList);

        assertEquals(2, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.METHOD_REPLACEMENT));
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));

        String targetId = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT)
                .iterator().next();

        double h0 = ExecutionTracer.getValue(targetId);
        assertTrue(h0 > 0); //reached
        assertTrue(h0 < 1);//but no covered
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));

        List<String> singletonList = Arrays.asList("One");
        te.isEmpty(singletonList);
        double h1 = ExecutionTracer.getValue(targetId);

        assertTrue(h1 > h0); //better
        assertTrue(h1 < 1);//but not covered
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));

        List<String> emptyList = Collections.emptyList();
        te.isEmpty(emptyList);
        double h2 = ExecutionTracer.getValue(targetId);

        assertTrue(h2 > h1); //better
        assertEquals(1, h2);//covered
        assertEquals(0, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));

    }

    @Test
    public void testCollectionsContainsString() throws Exception {

        TestabilityExc te = getInstance();

        List<String> list0 = Arrays.asList("One", "Two", "Three");
        te.contains(list0, "O__");

        assertEquals(2, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.METHOD_REPLACEMENT));
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));

        String targetId = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT)
                .iterator().next();

        double h0 = ExecutionTracer.getValue(targetId);
        assertTrue(h0 > 0); //reached
        assertTrue(h0 < 1);//but no covered
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));

        te.contains(list0, "On_");
        double h1 = ExecutionTracer.getValue(targetId);

        assertTrue(h1 > h0); //better
        assertTrue(h1 < 1);//but not covered
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));

        te.contains(list0, "One");
        double h2 = ExecutionTracer.getValue(targetId);

        assertTrue(h2 > h1); //better
        assertEquals(1, h2);//covered
        assertEquals(0, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));

    }

    @Test
    public void testCollectionsContainsInteger() throws Exception {

        TestabilityExc te = getInstance();

        List<Integer> list0 = Arrays.asList(10, 1000, 10000);
        te.contains(list0, 99);

        assertEquals(2, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.METHOD_REPLACEMENT));
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));

        String targetId = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT)
                .iterator().next();

        double h0 = ExecutionTracer.getValue(targetId);
        assertTrue(h0 > 0); //reached
        assertTrue(h0 < 1);//but no covered
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));

        te.contains(list0, 19);
        double h1 = ExecutionTracer.getValue(targetId);

        assertTrue(h1 > h0); //better
        assertTrue(h1 < 1);//but not covered
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));

        te.contains(list0, 10);
        double h2 = ExecutionTracer.getValue(targetId);

        assertTrue(h2 > h1); //better
        assertEquals(1, h2);//covered
        assertEquals(0, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));

    }

    @Test
    public void testCollectionsContainsEmpty() throws Exception {

        TestabilityExc te = getInstance();

        List<Integer> list0 = Collections.emptyList();
        te.contains(list0, 99);

        assertEquals(2, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.METHOD_REPLACEMENT));
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));

        String targetId = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT)
                .iterator().next();

        double h0 = ExecutionTracer.getValue(targetId);
        assertEquals(0, h0); //not reached
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));
    }

    @Test
    public void testCollectionsContainsNonHomogeneousList() throws Exception {

        TestabilityExc te = getInstance();

        List<Object> list0 = Arrays.asList(new Object(), "Hello", 100);
        te.contains(list0, 99);

        assertEquals(2, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.METHOD_REPLACEMENT));
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));

        String targetId = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT)
                .iterator().next();

        double h0 = ExecutionTracer.getValue(targetId);
        assertTrue(h0 > 0); //reached
        assertTrue(h0 < 1);//but no covered
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));
    }

    @Test
    public void testMapContainsStringKey() throws Exception {

        TestabilityExc te = getInstance();

        Map<String, Object> map0 = new HashMap<>();
        map0.put("One", null);
        map0.put("Two", null);
        map0.put("Three", null);
        te.containsKey(map0, "O__");

        assertEquals(2, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.METHOD_REPLACEMENT));
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));

        String targetId = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT)
                .iterator().next();

        double h0 = ExecutionTracer.getValue(targetId);
        assertTrue(h0 > 0); //reached
        assertTrue(h0 < 1);//but no covered
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));

        te.containsKey(map0, "On_");
        double h1 = ExecutionTracer.getValue(targetId);

        assertTrue(h1 > h0); //better
        assertTrue(h1 < 1);//but not covered
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));

        te.containsKey(map0, "One");
        double h2 = ExecutionTracer.getValue(targetId);

        assertTrue(h2 > h1); //better
        assertEquals(1, h2);//covered
        assertEquals(0, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));

    }

    @Test
    public void testObjectEquals() throws Exception {
        TestabilityExc te = getInstance();
        te.objectEquals(null, null);

        assertEquals(2, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.METHOD_REPLACEMENT));
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));

        String targetId = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT)
                .iterator().next();

        double h0 = ExecutionTracer.getValue(targetId);
        assertEquals(0, h0); // null value provides no gradient to reach false branch

        te.objectEquals("Hello!", null);

        double h1 = ExecutionTracer.getValue(targetId);
        assertEquals(1, h1); // false branch was covered

    }

    @Test
    public void testObjectEqualsNonNull() throws Exception {
        TestabilityExc te = getInstance();

        te.objectEquals("Hello!", null);

        assertEquals(2, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.METHOD_REPLACEMENT));
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));

        String targetId = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT)
                .iterator().next();

        double h0 = ExecutionTracer.getValue(targetId);
        assertEquals(0, h0); // null value provides no gradient to reach false branch

        te.objectEquals("Hello!", "Hell");

        double h1 = ExecutionTracer.getValue(targetId);
        assertTrue(h1 > 0); // true branch was reached
        assertTrue(h1 < 1); // true branch still not covered
        assertTrue(h1 > h0); // but we are doing better than before

        te.objectEquals("Hello!", "Hello!");
        double h2 = ExecutionTracer.getValue(targetId);
        assertEquals(1, h2); // true branch was covered
    }


    @Test
    public void testUnknownPatternDateFormatParse() throws Exception {
        TestabilityExc te = getInstance();

        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:SS");
        assertThrows(Exception.class, () -> te.dateFormatParse(sdf, "07/"));

        assertEquals(2, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.METHOD_REPLACEMENT));
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));

        String targetId = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT)
                .iterator().next();

        double h0 = ExecutionTracer.getValue(targetId);
        assertEquals(0, h0); // no guidance is provided since the pattern is unknown
    }

    @Test
    public void testDateFormatParse() throws Exception {
        TestabilityExc te = getInstance();

        SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-DD HH:SS");
        assertThrows(Exception.class, () -> te.dateFormatParse(sdf, "1234-aa-aa"));

        assertEquals(2, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.METHOD_REPLACEMENT));
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));

        String targetId = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT)
                .iterator().next();

        double h0 = ExecutionTracer.getValue(targetId);
        assertTrue(h0 > 0); // true branch was reached
        assertTrue(h0 < 1); // true branch still not covered

        assertThrows(Exception.class, () -> te.dateFormatParse(sdf, "1234-11-aa"));

        double h1 = ExecutionTracer.getValue(targetId);
        assertTrue(h1 > h0); // distance has improved
        assertTrue(h1 < 1); // but still the true branch is not covered

        assertThrows(Exception.class, () -> te.dateFormatParse(sdf, "1234-11-11"));
        double h2 = ExecutionTracer.getValue(targetId);
        assertTrue(h2 > h1); // true branch was reached
        assertTrue(h1 < 1); // true branch  not covered

        te.dateFormatParse(sdf, "1234-11-11 11:11");
        double h3 = ExecutionTracer.getValue(targetId);
        assertEquals(1, h3); // true branch was covered

    }

    @Test
    public void testBooleanParse() throws Exception {
        TestabilityExc te = getInstance();
        te.parseBoolean(null);

        assertEquals(2, ExecutionTracer.getNumberOfObjectives(ObjectiveNaming.METHOD_REPLACEMENT));
        assertEquals(1, ExecutionTracer.getNumberOfNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT));

        String targetId = ExecutionTracer.getNonCoveredObjectives(ObjectiveNaming.METHOD_REPLACEMENT)
                .iterator().next();

        double h0 = ExecutionTracer.getValue(targetId);
        assertEquals(0, h0);

        te.parseBoolean("____");
        double h1 = ExecutionTracer.getValue(targetId);
        assertTrue(h1 > h0);
        assertTrue(h1 < 1);

        te.parseBoolean("T___");
        double h2 = ExecutionTracer.getValue(targetId);
        assertTrue(h2 > h1);
        assertTrue(h2 < 1);

        te.parseBoolean("T__E");
        double h3 = ExecutionTracer.getValue(targetId);
        assertTrue(h3> h2);
        assertTrue(h3 < 1);

        te.parseBoolean("TruE");
        double h4 = ExecutionTracer.getValue(targetId);
        assertTrue(h4> h3);
        assertEquals(1,h4);

    }
}