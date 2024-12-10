package org.evomaster.client.java.instrumentation.coverage.methodreplacement;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static org.evomaster.client.java.distance.heuristics.DistanceHelper.H_REACHED_BUT_NULL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DateTimeParsingUtilsTest {

    @Test
    public void testSuccessfulParsingInputOfISOLocalDate() {
        LocalDate localDate = LocalDate.of(1978,7,31);
        double h = DateTimeParsingUtils.getHeuristicToISOLocalDateParsing(localDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
        assertEquals(1d,h);
    }
    @Test
    public void testSuccessfulParsingInput() {
        LocalDate localDate = LocalDate.of(1978,7,31);
        LocalTime localTime = LocalTime.of(23,59,59);
        LocalDateTime localDateTime = LocalDateTime.of(localDate,localTime);
        String input = localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        double h = DateTimeParsingUtils.getHeuristicToISOLocalDateTimeParsing(input);
        assertEquals(1d,h);
    }

    @Test
    public void testUnsuccessfulParsingInput() {
        double h = DateTimeParsingUtils.getHeuristicToISOLocalDateTimeParsing("1978-__-__T13:45");
        assertTrue(h>0);
        assertTrue(h<1);
    }

    @Test
    public void testSuccessfulLocalDateParsing() {
        LocalDate localDate = LocalDate.of(1978,12,31);
        String input = localDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
        double h = DateTimeParsingUtils.getHeuristicToISOLocalDateParsing(input);
        assertEquals(1d,h);
    }

    @Test
    public void testSuccessfulLocalTimeParsing() {
        LocalTime localTime = LocalTime.of(23,59,59);
        String input = localTime.format(DateTimeFormatter.ISO_LOCAL_TIME);
        LocalTime.parse(input,DateTimeFormatter.ISO_LOCAL_TIME);
        LocalTime.parse(input);
        double h = DateTimeParsingUtils.getHeuristicToISOLocalTimeParsing(input);
        assertEquals(1d,h);
    }

    @Test
    public void testDistanceToISOLocalDateTimeToNull() {
        double h = DateTimeParsingUtils.getHeuristicToISOLocalDateTimeParsing(null);
        assertEquals(H_REACHED_BUT_NULL, h);
    }

    @Test
    public void testDistanceToISOLocalDateToNull() {
        double h = DateTimeParsingUtils.getHeuristicToISOLocalDateParsing(null);
        assertEquals(H_REACHED_BUT_NULL, h);
    }

    @Test
    public void testDistanceToISOLocalTimeToNull() {
        double h = DateTimeParsingUtils.getHeuristicToISOLocalTimeParsing(null);
        assertEquals(H_REACHED_BUT_NULL, h);
    }

    @Test
    public void testUnsuccessfulParsingInputTooSmall() {
        double h = DateTimeParsingUtils.getHeuristicToISOLocalDateTimeParsing("1978-__-__");
        assertTrue(h>0);
        assertTrue(h<1);
    }

    @Test
    public void testDistanceToISOLocalDateToInvalid() {
        double h = DateTimeParsingUtils.getHeuristicToISOLocalDateParsing("Hello World");
        assertTrue(h>0);
        assertTrue(h<1);
    }


    @Test
    public void testDistanceToDateTimeToInvalid() {
        double h = DateTimeParsingUtils.getHeuristicToDateTimeParsing("Hello World");
        assertTrue(h>0);
        assertTrue(h<1);
    }

    @Test
    public void testDistanceToDateTimeToNull() {
        double h = DateTimeParsingUtils.getHeuristicToDateTimeParsing(null);
        assertEquals(H_REACHED_BUT_NULL, h);
    }


    @Test
    public void testDistanceToDateTimeToValid() {
        double h = DateTimeParsingUtils.getHeuristicToDateTimeParsing("1978-07-31 11:23");
        assertEquals(1d,h);
    }

    @Test
    public void testDistanceToDateTimeToToValidPrefix() {
        double h = DateTimeParsingUtils.getHeuristicToDateTimeParsing("1978-07-31 11:23TTTTT");
        assertEquals(1d,h);
    }

    @Test
    public void testDistanceToDateTimeToTooLongInvalid() {
        double h = DateTimeParsingUtils.getHeuristicToDateTimeParsing("1978-07-31 11:______________");
        assertTrue(h>0);
        assertTrue(h<1);
    }

    @Test
    public void testDistanceToDateTimeToOtherPatterns() {
        String input = "19780731";
        String pattern = "YYYYMMDD";
        double h = DateTimeParsingUtils.getHeuristicToDateTimePatternParsing(input , pattern);
        assertEquals(1d,h);
    }

    @Test
    public void testDistanceToDateTimeToOtherPatternsWithNullInput() {
        String pattern = "YYYYMMDD";
        double h = DateTimeParsingUtils.getHeuristicToDateTimePatternParsing(null , pattern);
        assertEquals(H_REACHED_BUT_NULL, h);
    }

    @Test
    public void testDistanceToISOLocalDateIsTooShort() {
        double h = DateTimeParsingUtils.getHeuristicToISOLocalDateParsing("1978-07");
        assertTrue(h>0);
        assertTrue(h<1);
    }

    @Test
    public void testDistanceToISOLocalDateTimeIsTooLong() {
        double h = DateTimeParsingUtils.getHeuristicToISOLocalDateTimeParsing("1978-07-31T__:__:_________");
        assertTrue(h>0);
        assertTrue(h<1);
    }

    @Test
    public void testDistanceToISOLocalTimeIsTooShort() {
        double h = DateTimeParsingUtils.getHeuristicToISOLocalTimeParsing("");
        assertTrue(h>0);
        assertTrue(h<1);
    }
}
