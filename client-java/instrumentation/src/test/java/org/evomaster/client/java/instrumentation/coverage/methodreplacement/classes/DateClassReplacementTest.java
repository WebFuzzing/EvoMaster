package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.heuristic.Truthness;
import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by jgaleotti on 29-Ago-19.
 */
public class DateClassReplacementTest {

    @Test
    public void testEqualsDates() {
        Date thisDate = new Date();
        Truthness truthness = DateClassReplacement.getEqualsTruthness(thisDate, thisDate);
        assertTrue(truthness.isTrue());
        assertFalse(truthness.isFalse());

        boolean equalsValue = DateClassReplacement.equals(thisDate, thisDate, ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate");
        assertTrue(equalsValue);
    }

    @Test
    public void testEqualsNull() {
        Date thisDate = new Date();
        Truthness truthness = DateClassReplacement.getEqualsTruthness(thisDate, null);
        assertFalse(truthness.isTrue());
        assertTrue(truthness.isFalse());

        boolean equalsValue = DateClassReplacement.equals(thisDate, null, ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate");
        assertFalse(equalsValue);
    }

    @Test
    public void testDateDistance() throws ParseException {
        String date1 = "07/15/2016";
        String time1 = "11:00 AM";
        String time2 = "11:15 AM";
        String time3 = "11:30 AM";

        String format = "MM/dd/yyyy hh:mm a";

        SimpleDateFormat sdf = new SimpleDateFormat(format);

        Date dateObject1 = sdf.parse(date1 + " " + time1);
        Date dateObject2 = sdf.parse(date1 + " " + time2);
        Date dateObject3 = sdf.parse(date1 + " " + time3);

        Truthness truthness1 = DateClassReplacement.getEqualsTruthness(dateObject1, dateObject2);
        Truthness truthness2 = DateClassReplacement.getEqualsTruthness(dateObject1, dateObject3);

        assertFalse(truthness1.isTrue());
        assertFalse(truthness2.isTrue());

        assertTrue(truthness1.isFalse());
        assertTrue(truthness2.isFalse());

        // 11.15 is closer to 11.00 than 11.30
        assertTrue(truthness1.getOfTrue() > truthness2.getOfTrue());


    }


    @Test
    public void testBeforeDateDistance() throws ParseException {
        String date1 = "07/15/2016";
        String time1 = "11:00 AM";
        String time2 = "11:15 AM";
        String time3 = "11:30 AM";

        String format = "MM/dd/yyyy hh:mm a";

        SimpleDateFormat sdf = new SimpleDateFormat(format);

        Date dateObject1 = sdf.parse(date1 + " " + time1);
        Date dateObject2 = sdf.parse(date1 + " " + time2);
        Date dateObject3 = sdf.parse(date1 + " " + time3);

        Truthness truthness1 = DateClassReplacement.getBeforeTruthness(dateObject1, dateObject2);
        Truthness truthness2 = DateClassReplacement.getBeforeTruthness(dateObject1, dateObject3);

        assertTrue(truthness1.isTrue());
        assertTrue(truthness2.isTrue());

        assertFalse(truthness1.isFalse());
        assertFalse(truthness2.isFalse());

        Truthness truthness3 = DateClassReplacement.getBeforeTruthness(dateObject2, dateObject1);
        Truthness truthness4 = DateClassReplacement.getBeforeTruthness(dateObject3, dateObject1);

        assertFalse(truthness3.isTrue());
        assertFalse(truthness4.isTrue());

        assertTrue(truthness3.isFalse());
        assertTrue(truthness4.isFalse());

        // 11:15 AM is closer to 10:59 AM than 11:30 AM
        assertTrue(truthness3.getOfTrue() > truthness4.getOfTrue());

    }

    @Test
    public void testBeforeDate() throws ParseException {
        String date1 = "07/15/2016";
        String time1 = "11:00 AM";
        String time2 = "11:15 AM";

        String format = "MM/dd/yyyy hh:mm a";

        SimpleDateFormat sdf = new SimpleDateFormat(format);

        Date dateObject1 = sdf.parse(date1 + " " + time1);
        Date dateObject2 = sdf.parse(date1 + " " + time2);

        boolean beforeValue = DateClassReplacement.before(dateObject1, dateObject2, ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate");
        assertTrue(beforeValue);

        boolean notBeforeValue = DateClassReplacement.before(dateObject2, dateObject1, ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate");
        assertFalse(notBeforeValue);

        boolean sameDateValue = DateClassReplacement.before(dateObject1, dateObject1, ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate");
        assertFalse(sameDateValue);

    }

    @Test
    public void testAfterDate() throws ParseException {
        String date1 = "07/15/2016";
        String time1 = "11:00 AM";
        String time2 = "11:15 AM";

        String format = "MM/dd/yyyy hh:mm a";

        SimpleDateFormat sdf = new SimpleDateFormat(format);

        Date dateObject1 = sdf.parse(date1 + " " + time1);
        Date dateObject2 = sdf.parse(date1 + " " + time2);

        boolean afterValue = DateClassReplacement.after(dateObject1, dateObject2, ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate");
        assertFalse(afterValue);

        boolean notAfterValue = DateClassReplacement.after(dateObject2, dateObject1, ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate");
        assertTrue(notAfterValue);

        boolean sameDateValue = DateClassReplacement.after(dateObject1, dateObject1, ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate");
        assertFalse(sameDateValue);
    }

}
