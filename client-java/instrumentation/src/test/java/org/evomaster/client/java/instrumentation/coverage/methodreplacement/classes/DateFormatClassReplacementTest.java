package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DateFormatClassReplacementTest {

    @Test
    public void testParseDate() throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String input = "2019-07-31";
        Date date = DateFormatClassReplacement.parse(sdf,input, ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate");
        // Month value is 0-based. e.g., 0 for January.
        Calendar myCalendar = new GregorianCalendar(2019, 6, 31);
        Date expectedDate = myCalendar.getTime();
        assertEquals(expectedDate,date);
    }

    @Test
    public void testParseDateTime() throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String input = "2019-07-31 13:45";
        Date date = DateFormatClassReplacement.parse(sdf,input, ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate");
        // Month value is 0-based. e.g., 0 for January.
        Calendar calendar = new GregorianCalendar(2019, 6, 31);
        calendar.add(Calendar.HOUR_OF_DAY, 13);
        calendar.add(Calendar.MINUTE, 45);
        Date expectedDate = calendar.getTime();
        assertEquals(expectedDate,date);
    }

    @Test
    public void testParseDateTimeWithSeconds() throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String input = "2019-07-31 13:45:31";
        Date date = DateFormatClassReplacement.parse(sdf,input, ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate");
        // Month value is 0-based. e.g., 0 for January.
        Calendar calendar = new GregorianCalendar(2019, 6, 31);
        calendar.add(Calendar.HOUR_OF_DAY, 13);
        calendar.add(Calendar.MINUTE, 45);
        calendar.add(Calendar.SECOND, 31);
        Date expectedDate = calendar.getTime();
        assertEquals(expectedDate,date);
    }
}
