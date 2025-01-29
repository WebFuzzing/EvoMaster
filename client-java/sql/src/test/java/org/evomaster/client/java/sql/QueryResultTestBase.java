package org.evomaster.client.java.sql;

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public abstract class QueryResultTestBase {

    protected abstract DatabaseType getDbType();
    protected abstract Connection getConnection();

    @Test
    public void testDateColumn() throws Exception {
        // set up the database
        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE example_table (\n" +
                "    event_date DATE NOT NULL\n" +
                ");");
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO example_table (event_date)\n" +
                "VALUES ('2025-01-22');\n");

        // create the queryResult
        QueryResult queryResult = SqlScriptRunner.execCommand(getConnection(), "SELECT * FROM example_table");

        // check the results
        assertEquals(1, queryResult.seeRows().size());
        DataRow row = queryResult.seeRows().get(0);
        Object actual = row.getValueByName("event_date", "example_table");
        assertTrue(actual instanceof Date);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        Date expected = sdf.parse("22/01/2025");
        assertEquals(expected, actual);
    }

    @Test
    public void testDateTimeColumn() throws Exception {
        assumeFalse(getDbType()==DatabaseType.POSTGRES);

        // set up the database
        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE example_table (\n" +
                "    event_date_time DATETIME NOT NULL\n" +
                ");");
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO example_table (event_date_time)\n" +
                "VALUES ('2025-01-22 15:30:45');\n");

        // Create the query result
        QueryResult queryResult = SqlScriptRunner.execCommand(getConnection(), "SELECT * FROM example_table");

        // check the results
        assertEquals(1, queryResult.seeRows().size());
        DataRow row = queryResult.seeRows().get(0);
        Object actual = row.getValueByName("event_date_time", "example_table");

        final Object expected;
        switch (getDbType()) {
            case MYSQL: {
                assertTrue(actual instanceof LocalDateTime);
                expected = LocalDateTime.of(2025, 1, 22, 15, 30, 45, 0);
            } break;
            case H2:
            default: {
                assertTrue(actual instanceof Timestamp);
                String timestampString = "2025-01-22 15:30:45.0";
                expected = Timestamp.valueOf(timestampString);
            } break;
        }
        assertEquals(expected, actual);
    }

    @Test
    public void testTimestampColumn() throws Exception {
        // set up the database
        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE example_table (\n" +
                "    event_timestamp TIMESTAMP NOT NULL\n" +
                ");");

        // Insert row with DATETIME value
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO example_table (event_timestamp)\n" +
                "VALUES ('2025-01-22 15:30:45');\n");

        // Query the table
        QueryResult queryResult = SqlScriptRunner.execCommand(getConnection(), "SELECT * FROM example_table");
        assertEquals(1, queryResult.seeRows().size());

        // check the results
        DataRow row = queryResult.seeRows().get(0);
        Object actual = row.getValueByName("event_timestamp", "example_table");

        assertTrue(actual instanceof Timestamp);

        String timestampString = "2025-01-22 15:30:45.0";
        Timestamp expected = Timestamp.valueOf(timestampString);

        assertEquals(expected, actual);
    }

    @Test
    public void testTimestampWithTimeZoneColumn() throws Exception {
        assumeFalse(getDbType() == DatabaseType.MYSQL);

        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE example_table (\n" +
                "    event_zoned_timestamp TIMESTAMP WITH TIME ZONE NOT NULL\n" +
                ");");

        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO example_table (event_zoned_timestamp)\n" +
                "VALUES ('2025-01-22 15:30:45+05:30');\n");

        // create the queryResult
        QueryResult queryResult = SqlScriptRunner.execCommand(getConnection(), "SELECT * FROM example_table");
        assertEquals(1, queryResult.seeRows().size());

        // check the results
        DataRow row = queryResult.seeRows().get(0);

        Object actual = row.getValueByName("event_zoned_timestamp", "example_table");

        Object expected;
        switch (this.getDbType()) {
            case POSTGRES: {
                LocalDateTime localDateTime = LocalDateTime.of(2025, 1, 22, 15, 30, 45);
                ZoneOffset offset = ZoneOffset.ofHoursMinutes(5, 30);
                ZonedDateTime zonedDateTime = localDateTime.atOffset(offset).toZonedDateTime();
                long epochMilli = zonedDateTime.toInstant().toEpochMilli();
                expected = new Timestamp(epochMilli);
            } break;
            case H2:
            case MYSQL:
            default: {
                assertTrue(actual instanceof OffsetDateTime);

                // Define the string with date, time, and timezone offset
                String dateTimeString = "2025-01-22 15:30:45+05:30";

                // Define the DateTimeFormatter to match the input string pattern
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssXXX");

                // Parse the string into a ZonedDateTime instance
                expected = OffsetDateTime.parse(dateTimeString, formatter);

            }
        }
        assertEquals(expected, actual);
    }

    @Test
    public void testTimeColumn() throws Exception {
        // set up the database
        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE example_table (\n" +
                "    time_column TIME NOT NULL\n" +
                ");");

        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO example_table (time_column)\n" +
                "VALUES ('15:30:45');\n");

        // create the queryResult
        QueryResult queryResult = SqlScriptRunner.execCommand(getConnection(), "SELECT * FROM example_table");
        assertEquals(1, queryResult.seeRows().size());
        DataRow row = queryResult.seeRows().get(0);
        Object actual = row.getValueByName("time_column", "example_table");

        // check the results
        assertTrue(actual instanceof Time);
        Time expected = Time.valueOf("15:30:45");
        assertEquals(expected, actual);
    }

    @Test
    public void testTimeWithTimeZoneColumn() throws Exception {
        assumeFalse(getDbType() == DatabaseType.MYSQL);

        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE example_table (\n" +
                "    time_column TIME WITH TIME ZONE NOT NULL\n" +
                ");");

        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO example_table (time_column)\n" +
                "VALUES ('15:30:45+02:00');\n");

        QueryResult queryResult = SqlScriptRunner.execCommand(getConnection(), "SELECT * FROM example_table");
        assertEquals(1, queryResult.seeRows().size());

        DataRow row = queryResult.seeRows().get(0);
        Object actual = row.getValueByName("time_column", "example_table");

        // check the results
        Object expected;
        switch (this.getDbType()) {
            case POSTGRES: {
                OffsetTime offsetTime = OffsetTime.of(15, 30, 45, 0, ZoneOffset.of("+02:00"));
                ZoneId systemZone = ZoneId.systemDefault();
                LocalDate constantDate = LocalDate.of(1970, 1, 1);
                ZonedDateTime zonedDateTime = offsetTime.atDate(constantDate).atZoneSameInstant(systemZone);
                LocalTime systemLocalTime = zonedDateTime.toLocalTime();
                expected = Time.valueOf(systemLocalTime);
            } break;
            case MYSQL:
            case H2:
            default: {
                assertTrue(actual instanceof OffsetTime);
                expected = OffsetTime.of(15, 30, 45, 0, ZoneOffset.ofHours(+2));
            }
        }
        assertEquals(expected, actual);
    }



}
