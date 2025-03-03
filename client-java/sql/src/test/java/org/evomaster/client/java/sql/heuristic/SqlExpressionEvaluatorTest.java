package org.evomaster.client.java.sql.heuristic;

import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.distance.heuristics.TruthnessUtils;
import org.evomaster.client.java.sql.DataRow;
import org.evomaster.client.java.sql.QueryResult;
import org.evomaster.client.java.sql.internal.SqlDistanceWithMetrics;
import org.evomaster.client.java.sql.internal.SqlNameContext;
import org.evomaster.client.java.sql.internal.SqlParserUtils;
import org.evomaster.client.java.sql.internal.TaintHandler;
import org.junit.jupiter.api.Test;

import java.sql.Time;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static org.evomaster.client.java.sql.heuristic.SqlHeuristicsCalculator.TRUE_TRUTHNESS;
import static org.junit.jupiter.api.Assertions.*;

class SqlExpressionEvaluatorTest {

    private static void assertSqlExpressionEvaluatesToTrue(String sqlCommand, DataRow row) {
        Statement parsedSqlCommand = SqlParserUtils.parseSqlCommand(sqlCommand);
        Select select = (Select) parsedSqlCommand;

        SqlNameContext sqlNameContext = new SqlNameContext(parsedSqlCommand);
        TaintHandler taintHandler = null;

        SqlExpressionEvaluator evaluator = new SqlExpressionEvaluator(sqlNameContext, taintHandler, row);
        select.getPlainSelect().getWhere().accept(evaluator);

        Truthness truthness = evaluator.getEvaluatedTruthness();
        assertTrue(truthness.isTrue());
    }

    private static void assertSqlExpressionEvaluatesToFalse(String sqlCommand, DataRow row) {
        Statement parsedSqlCommand = SqlParserUtils.parseSqlCommand(sqlCommand);
        Select select = (Select) parsedSqlCommand;

        SqlNameContext sqlNameContext = new SqlNameContext(parsedSqlCommand);
        TaintHandler taintHandler = null;

        SqlExpressionEvaluator evaluator = new SqlExpressionEvaluator(sqlNameContext, taintHandler, row);
        select.getPlainSelect().getWhere().accept(evaluator);

        Truthness truthness = evaluator.getEvaluatedTruthness();
        assertFalse(truthness.isTrue());
    }

    @Test
    public void testOrCondition() {
        String sqlCommand = "SELECT name, age FROM Persons WHERE age<18 OR age>30";
        DataRow row = new DataRow(
                "Persons",
                Arrays.asList("name", "age"),
                Arrays.asList("John", 17));

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testAndCondition() {
        String sqlCommand = "SELECT name, age FROM Persons WHERE age>18 AND age<30";
        DataRow row = new DataRow(
                "Persons",
                Arrays.asList("name", "age"),
                Arrays.asList("John", 25));

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testBetweenNumbers() {
        String sqlCommand = "SELECT name, age FROM Persons WHERE age BETWEEN 18 AND 30";
        DataRow row = new DataRow(
                "Persons",
                Arrays.asList("name", "age"),
                Arrays.asList("John", 23));

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testEqualsTo() {
        String sqlCommand = "SELECT name, age FROM Persons WHERE age=18";
        DataRow row = new DataRow(
                "Persons",
                Arrays.asList("name", "age"),
                Arrays.asList("John", 18));

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testFalseMinorThan() {
        String sqlCommand = "SELECT name, age FROM Persons WHERE age<18";
        DataRow row = new DataRow(
                "Persons",
                Arrays.asList("name", "age"),
                Arrays.asList("John", 33));

        assertSqlExpressionEvaluatesToFalse(sqlCommand, row);
    }

    @Test
    public void testMinorThan() {
        String sqlCommand = "SELECT name, age FROM Persons WHERE age<18";
        DataRow row = new DataRow(
                "Persons",
                Arrays.asList("name", "age"),
                Arrays.asList("John", 17));

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testEqualsToString() {
        String sqlCommand = "SELECT name, age FROM Persons WHERE name='John'";
        DataRow row = new DataRow(
                "Persons",
                Arrays.asList("name", "age"),
                Arrays.asList("John", 17));

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testMinorThanEquals() {
        String sqlCommand = "SELECT name, age FROM Persons WHERE age<=18";
        DataRow row = new DataRow(
                "Persons",
                Arrays.asList("name", "age"),
                Arrays.asList("John", 18));

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testGreaterThan() {
        String sqlCommand = "SELECT name, age FROM Persons WHERE 18>age";
        DataRow row = new DataRow(
                "Persons",
                Arrays.asList("name", "age"),
                Arrays.asList("John", 17));

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testGreaterThanEquals() {
        String sqlCommand = "SELECT name, age FROM Persons WHERE 18>=age";
        DataRow row = new DataRow(
                "Persons",
                Arrays.asList("name", "age"),
                Arrays.asList("John", 18));

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testNotEqualsTo() {
        String sqlCommand = "SELECT name, age FROM Persons WHERE age!=18";
        DataRow row = new DataRow(
                "Persons",
                Arrays.asList("name", "age"),
                Arrays.asList("John", 20));

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testNotEqualsToStrings() {
        String sqlCommand = "SELECT name, age FROM Persons WHERE name!='John'";
        DataRow row = new DataRow(
                "Persons",
                Arrays.asList("name", "age"),
                Arrays.asList("Jack", 17));

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testEqualsToBoolean() {
        String sqlCommand = "SELECT name, age, is_member FROM Persons WHERE is_member=true";
        DataRow row = new DataRow(
                "Persons",
                Arrays.asList("name", "age", "is_member"),
                Arrays.asList("John", 18, true));

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testIsTrue() {
        String sqlCommand = "SELECT name, age, is_member FROM Persons WHERE is_member IS TRUE";
        DataRow row = new DataRow(
                "Persons",
                Arrays.asList("name", "age", "is_member"),
                Arrays.asList("John", 18, true));

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testIsFalse() {
        String sqlCommand = "SELECT name, age, is_member FROM Persons WHERE is_member IS FALSE";
        DataRow row = new DataRow(
                "Persons",
                Arrays.asList("name", "age", "is_member"),
                Arrays.asList("John", 18, false));

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testIsNull() {
        String sqlCommand = "SELECT name, age FROM Persons WHERE name IS NULL";
        DataRow row = new DataRow(
                "Persons",
                Arrays.asList("name", "age"),
                Arrays.asList(null, 18));

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testIsNotNull() {
        String sqlCommand = "SELECT name, age FROM Persons WHERE name IS NOT NULL";
        DataRow row = new DataRow(
                "Persons",
                Arrays.asList("name", "age"),
                Arrays.asList("John", 18));

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testDoubleComparison() {
        String sqlCommand = "SELECT price FROM Products WHERE price<=19.99";
        DataRow row = new DataRow(
                "Products",
                Collections.singletonList("price"),
                Collections.singletonList(9.99));

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testAddition() {
        String sqlCommand = "SELECT salary, bonus FROM Employees WHERE salary + bonus > 50000";
        DataRow row = new DataRow(
                "Employees",
                Arrays.asList("salary", "bonus"),
                Arrays.asList(40000, 20000));

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testDivision() {
        String sqlCommand = "SELECT name, salary FROM Employees WHERE salary / 12 > 3000";
        DataRow row = new DataRow(
                "Employees",
                Arrays.asList("name", "salary"),
                Arrays.asList("John", 48000));

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testMultiplication() {
        String sqlCommand = "SELECT product_name, price, quantity FROM Products WHERE price * quantity > 100";
        DataRow row = new DataRow(
                "Products",
                Arrays.asList("product_name", "price", "quantity"),
                Arrays.asList("Laptop", 120, 2));

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testSubtraction() {
        String sqlCommand = "SELECT product_name, original_price, discount_price FROM Products WHERE original_price - discount_price > 20";
        DataRow row = new DataRow(
                "Products",
                Arrays.asList("product_name", "original_price", "discount_price"),
                Arrays.asList("Laptop", 300, 200));

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testIntegerDivision() {
        String sqlCommand = "SELECT name, salary FROM Employees WHERE salary DIV 12 > 3000";
        DataRow row = new DataRow(
                "Employees",
                Arrays.asList("name", "salary"),
                Arrays.asList("John", 48000));

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testPlusSignedExpression() {
        String sqlCommand = "SELECT price FROM Products WHERE +price > 100";
        DataRow row = new DataRow(
                "Products",
                Collections.singletonList("price"),
                Collections.singletonList(200));

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testMinusSignedExpression() {
        String sqlCommand = "SELECT price FROM Products WHERE -price > 100";
        DataRow row = new DataRow(
                "Products",
                Collections.singletonList("price"),
                Collections.singletonList(-200));

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testBitwiseNotSignedExpression() {
        String sqlCommand = "SELECT permissions FROM Users WHERE ~permissions = 0";
        DataRow row = new DataRow(
                "Users",
                Collections.singletonList("permissions"),
                Collections.singletonList(-1));

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testShouldReturnZeroDistanceForTimestamp() {
        String sqlCommand = "SELECT order_id, customer_id, order_timestamp FROM Orders WHERE order_timestamp = '2025-01-14 12:30:45'";
        DataRow row = new DataRow(
                "Orders",
                Arrays.asList("order_id", "customer_id", "order_timestamp"),
                Arrays.asList(1, 1, Timestamp.valueOf("2025-01-14 12:30:45")));

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testEqualsToDate() throws ParseException {
        String sqlCommand = "SELECT event_id, event_date FROM Events WHERE event_date = '2025-01-14'";
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        java.util.Date date = dateFormat.parse("2025-01-14");

        DataRow row = new DataRow(
                "Events",
                Arrays.asList("event_id", "event_date"),
                Arrays.asList(1, date));

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testEqualsToTime() {
        String sqlCommand = "SELECT schedule_id, start_time FROM Schedules WHERE start_time = '12:30:45'";
        Time time = Time.valueOf("12:30:45");

        DataRow row = new DataRow(
                "Schedules",
                Arrays.asList("schedule_id", "start_time"),
                Arrays.asList(1, time));

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testEqualToDateTime() throws ParseException {
        String sqlCommand = "SELECT appointment_id, appointment_datetime FROM Appointments WHERE appointment_datetime = '2025-01-14 12:30:45'";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date dateTime = sdf.parse("2025-01-14 12:30:45");

        DataRow row = new DataRow(
                "Appointments",
                Arrays.asList("appointment_id", "appointment_datetime"),
                Arrays.asList(1, dateTime));

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testEqualsToTimeWithTimeZone() {
        String sqlCommand = "SELECT event_id, event_time FROM Events WHERE event_time = '12:30:45+02:00'";
        OffsetTime offsetTime = OffsetTime.of(12, 30, 45, 0, ZoneOffset.ofHours(2));

        DataRow row = new DataRow(
                "Events",
                Arrays.asList("event_id", "event_time"),
                Arrays.asList(1, offsetTime));

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testEqualsToTimeStampWithTimeZone() {
        String sqlCommand = "SELECT event_id, event_time FROM Events WHERE event_time = '2025-01-14 12:30:45+02:00'";
        OffsetDateTime offsetDateTime = OffsetDateTime.parse("2025-01-14T12:30:45+02:00");

        DataRow row = new DataRow(
                "Events",
                Arrays.asList("event_id", "event_time"),
                Arrays.asList(1, offsetDateTime));

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testEqualToYear() throws ParseException {
        String sqlCommand = "SELECT employee_id, hire_year FROM employees WHERE hire_year = 2018";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
        java.util.Date hireYear = sdf.parse("2018");

        DataRow row = new DataRow(
                "employees",
                Arrays.asList("employee_id", "hire_year"),
                Arrays.asList(1, hireYear));

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testDateTimeLiteralExpression() throws ParseException {
        String sqlCommand = "SELECT * FROM orders WHERE order_date = TIMESTAMP '2025-01-22 15:30:45'";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date orderDate = sdf.parse("2025-01-22 15:30:45");

        DataRow row = new DataRow(
                "orders",
                Arrays.asList("order_id", "order_date"),
                Arrays.asList(1, orderDate));

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testTimestampValue() throws ParseException {
        String sqlCommand = "SELECT * FROM orders WHERE order_date = {ts '2025-01-22 15:30:45'}";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date orderDate = sdf.parse("2025-01-22 15:30:45");

        DataRow row = new DataRow(
                "orders",
                Arrays.asList("order_id", "order_date"),
                Arrays.asList(1, orderDate));

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testDateLiteralExpression() throws ParseException {
        String sqlCommand = "SELECT * FROM orders WHERE order_date = DATE '2025-01-22'";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date orderDate = sdf.parse("2025-01-22");

        DataRow row = new DataRow(
                "orders",
                Arrays.asList("order_id", "order_date"),
                Arrays.asList(1, orderDate));

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testDateValue() throws ParseException {
        String sqlCommand = "SELECT * FROM orders WHERE order_date = {d '2025-01-22'}";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date orderDate = sdf.parse("2025-01-22");

        DataRow row = new DataRow(
                "orders",
                Arrays.asList("order_id", "order_date"),
                Arrays.asList(1, orderDate));

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testTimeLiteralExpression() {
        String sqlCommand = "SELECT * FROM orders WHERE order_time = TIME '15:30:45'";
        Time orderTime = Time.valueOf("15:30:45");

        DataRow row = new DataRow(
                "orders",
                Arrays.asList("order_id", "order_time"),
                Arrays.asList(1, orderTime));

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testTimeValue() {
        String sqlCommand = "SELECT * FROM orders WHERE order_time = {t '15:30:45'}";
        Time orderTime = Time.valueOf("15:30:45");

        DataRow row = new DataRow(
                "orders",
                Arrays.asList("order_id", "order_time"),
                Arrays.asList(1, orderTime));

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testTimestampWithTimeZoneLiteralExpression() {
        String sqlCommand = "SELECT * FROM orders WHERE order_date = TIMESTAMPTZ '2025-01-22 15:30:45+02:00'";
        String timestampString = "2025-01-22 15:30:45+02:00";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssXXX");
        OffsetDateTime orderDate = OffsetDateTime.parse(timestampString, formatter);

        DataRow row = new DataRow(
                "orders",
                Arrays.asList("order_id", "order_date"),
                Arrays.asList(1, orderDate));

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testParenthesis() {
        String sqlCommand = "SELECT salary, bonus FROM Employees WHERE (salary + bonus) > 50000";
        DataRow row = new DataRow(
                "Employees",
                Arrays.asList("salary", "bonus"),
                Arrays.asList(40000, 20000));

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testBetweenStrings() {
        String sqlCommand = "SELECT name, age FROM Persons WHERE name BETWEEN 'A' AND 'Z'";
        DataRow row = new DataRow(
                "Persons",
                Arrays.asList("name", "age"),
                Arrays.asList("John", 23));

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testBetweenDates() {
        String sqlCommand = "SELECT birth_day FROM Persons WHERE birth_day BETWEEN '1990-07-01' AND '1990-07-31'";
        java.sql.Date birthDay = java.sql.Date.valueOf("1990-07-15");

        DataRow row = new DataRow(
                "Persons",
                Collections.singletonList("birth_day"),
                Collections.singletonList(birthDay));

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testBetweenTimes() {
        String sqlCommand = "SELECT start_time FROM Schedules WHERE start_time BETWEEN '09:00:00' AND '17:00:00'";
        Time startTime = Time.valueOf("12:00:00");

        DataRow row = new DataRow(
                "Schedules",
                Collections.singletonList("start_time"),
                Collections.singletonList(startTime));

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testBetweenTimestamps() {
        String sqlCommand = "SELECT event_timestamp FROM Events WHERE event_timestamp BETWEEN '2023-01-01 00:00:00' AND '2023-12-31 23:59:59'";
        Timestamp eventTimestamp = Timestamp.valueOf("2023-06-15 12:00:00");

        DataRow row = new DataRow(
                "Events",
                Collections.singletonList("event_timestamp"),
                Collections.singletonList(eventTimestamp));

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testXOrCondition() {
        String sqlCommand = "SELECT salary, age FROM Employees WHERE (age > 30) XOR (salary > 50000)";
        DataRow row = new DataRow(
                "Employees",
                Arrays.asList("age", "salary"),
                Arrays.asList(35, 40000));

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testModulo() {
        String sqlCommand = "SELECT salary FROM Employees WHERE (salary % 2) = 0";
        DataRow row = new DataRow(
                "Employees",
                Collections.singletonList("salary"),
                Collections.singletonList(40000));

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testBitwiseRightShift() {
        String sqlCommand = "SELECT * FROM Employees WHERE (salary >> 1) > 20000";
        DataRow row = new DataRow(
                "Employees",
                Collections.singletonList("salary"),
                Collections.singletonList(40010));

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testBitwiseLeftShift() {
        String sqlCommand = "SELECT * FROM Employees WHERE (salary << 1) > 20000";
        DataRow row = new DataRow(
                "Employees",
                Collections.singletonList("salary"),
                Collections.singletonList(10005));

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testHexValue() {
        String sqlCommand = "SELECT * FROM Employees WHERE salary = 0x1A";
        DataRow row = new DataRow(
                "Employees",
                Collections.singletonList("salary"),
                Collections.singletonList(26));

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testOverlapsTime() {
        String sqlCommand = "SELECT start_time, end_time FROM Events WHERE (start_time, end_time) OVERLAPS (TIME '10:00:00', TIME '12:00:00')";
        DataRow row = new DataRow(
                "Events",
                Arrays.asList("start_time", "end_time"),
                Arrays.asList(Time.valueOf("09:00:00"), Time.valueOf("11:00:00"))
        );

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testOverlapsDate() {
        String sqlCommand = "SELECT start_date, end_date FROM Events WHERE (start_date, end_date) OVERLAPS (DATE '2023-01-01', DATE '2024-01-01')";
        DataRow row = new DataRow(
                "Events",
                Arrays.asList("start_date", "end_date"),
                Arrays.asList(java.sql.Date.valueOf("2022-12-31"), java.sql.Date.valueOf("2023-06-01"))
        );

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testOverlapsTimestamp() {
        String sqlCommand = "SELECT start_timestamp, end_timestamp FROM Events WHERE (start_timestamp, end_timestamp) OVERLAPS (TIMESTAMP '2023-01-01 00:00:00', TIMESTAMP '2024-01-01 23:59:59')";
        DataRow row = new DataRow(
                "Events",
                Arrays.asList("start_timestamp", "end_timestamp"),
                Arrays.asList(Timestamp.valueOf("2023-05-01 00:00:00"), Timestamp.valueOf("2024-01-10 23:59:59"))
        );

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testNotExpression() {
        String sqlCommand = "SELECT name, age FROM Persons WHERE NOT age=18";
        DataRow row = new DataRow(
                "Persons",
                Arrays.asList("name", "age"),
                Arrays.asList("John", 23)
        );

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testConcat() {
        String sqlCommand = "SELECT * FROM Employees WHERE first_name || ' ' || last_name = 'John Doe'";
        DataRow row = new DataRow(
                "Employees",
                Arrays.asList("first_name", "last_name"),
                Arrays.asList("John", "Doe")
        );

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testBitwiseAnd() {
        String sqlCommand = "SELECT * FROM Employees WHERE (salary & 1) = 1";
        DataRow row = new DataRow(
                "Employees",
                Collections.singletonList("salary"),
                Collections.singletonList(1)
        );

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testBitwiseOr() {
        String sqlCommand = "SELECT * FROM Employees WHERE (salary | 1) = 3";
        DataRow row = new DataRow(
                "Employees",
                Collections.singletonList("salary"),
                Collections.singletonList(2)
        );

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testBitwiseXor() {
        String sqlCommand = "SELECT * FROM Employees WHERE (salary ^ 2) = 3";
        DataRow row = new DataRow(
                "Employees",
                Collections.singletonList("salary"),
                Collections.singletonList(1)
        );

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testRegExpMatchOperator() {
        String sqlCommand = "SELECT * FROM Employees WHERE name ~ 'John.*'";
        DataRow row = new DataRow(
                "Employees",
                Collections.singletonList("name"),
                Collections.singletonList("John Doe")
        );

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testSimilarToExpression() {
        String sqlCommand = "SELECT * FROM Employees WHERE name SIMILAR TO 'John%'";
        DataRow row = new DataRow(
                "Employees",
                Collections.singletonList("name"),
                Collections.singletonList("John Doe")
        );

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testArrayConstructor() {
        String sqlCommand = "SELECT * FROM Employees WHERE ARRAY[salary, bonus] = ARRAY[50000, 5000]";
        DataRow row = new DataRow(
                "Employees",
                Arrays.asList("salary", "bonus"),
                Arrays.asList(50000, 5000)
        );

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testArrayExpressionWithIndex() {
        String sqlCommand = "SELECT * FROM Employees WHERE ARRAY[salary, bonus][1] = 50000";
        DataRow row = new DataRow(
                "Employees",
                Arrays.asList("salary", "bonus"),
                Arrays.asList(50000, 5000)
        );

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testArrayExpressionUsingStartEndIndexes() {
        String sqlCommand = "SELECT * FROM Employees WHERE ARRAY[salary, bonus][2:2] = ARRAY[5000]";
        DataRow row = new DataRow(
                "Employees",
                Arrays.asList("salary", "bonus"),
                Arrays.asList(50000, 5000)
        );

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testArrayExpressionUsingStartEndIndexesAndIndex() {
        String sqlCommand = "SELECT * FROM Employees WHERE (ARRAY[salary, bonus][2:2])[1] = 5000";
        DataRow row = new DataRow(
                "Employees",
                Arrays.asList("salary", "bonus"),
                Arrays.asList(50000, 5000)
        );

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testNullAddition() {
        String sqlCommand = "SELECT salary,bonus FROM Employees WHERE salary+bonus IS NULL";
        DataRow row = new DataRow(
                "Employees",
                Arrays.asList("salary", "bonus"),
                Arrays.asList(null, 20000)
        );

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testNullSubtracion() {
        String sqlCommand = "SELECT salary,bonus FROM Employees WHERE salary-bonus IS NULL";
        DataRow row = new DataRow(
                "Employees",
                Arrays.asList("salary", "bonus"),
                Arrays.asList(null, 20000)
        );

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testNullBitwiseOr() {
        String sqlCommand = "SELECT salary,bonus FROM Employees WHERE salary | bonus IS NULL";
        DataRow row = new DataRow(
                "Employees",
                Arrays.asList("salary", "bonus"),
                Arrays.asList(null, 20000)
        );

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testNullBitwiseAnd() {
        String sqlCommand = "SELECT salary,bonus FROM Employees WHERE salary & bonus IS NULL";
        DataRow row = new DataRow(
                "Employees",
                Arrays.asList("salary", "bonus"),
                Arrays.asList(null, 20000)
        );

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testNullBitwiseXor() {
        String sqlCommand = "SELECT salary,bonus FROM Employees WHERE salary ^ bonus IS NULL";
        DataRow row = new DataRow(
                "Employees",
                Arrays.asList("salary", "bonus"),
                Arrays.asList(null, 20000)
        );

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testNullModulo() {
        String sqlCommand = "SELECT salary FROM Employees WHERE (salary % 2) IS NULL";
        DataRow row = new DataRow(
                "Employees",
                Collections.singletonList("salary"),
                Collections.singletonList(null)
        );

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testNullBitwiseRightShift() {
        String sqlCommand = "SELECT * FROM Employees WHERE (salary >> 1) IS NULL";
        DataRow row = new DataRow(
                "Employees",
                Collections.singletonList("salary"),
                Collections.singletonList(null)
        );

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testNullBitwiseLeftShift() {
        String sqlCommand = "SELECT * FROM Employees WHERE (salary << 1) IS NULL";
        DataRow row = new DataRow(
                "Employees",
                Collections.singletonList("salary"),
                Collections.singletonList(null)
        );

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testNullSignedExpression() {
        String sqlCommand = "SELECT price FROM Products WHERE +price IS NULL";
        DataRow row = new DataRow(
                "Products",
                Collections.singletonList("price"),
                Collections.singletonList(null)
        );

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testNullBetweenTimestamps() {
        String sqlCommand = "SELECT event_timestamp FROM Events WHERE NOT (event_timestamp BETWEEN NULL AND '2023-12-31 23:59:59')";
        String timestampString = "2023-01-14 12:30:45";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime localDateTime = LocalDateTime.parse(timestampString, formatter);
        Timestamp timestamp = Timestamp.valueOf(localDateTime);

        DataRow row = new DataRow(
                "Events",
                Collections.singletonList("event_timestamp"),
                Collections.singletonList(timestamp)
        );

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testNullOverlapsTime() {
        String sqlCommand = "SELECT start_time, end_time FROM Events WHERE NOT ((start_time, end_time) OVERLAPS (NULL, TIME '12:00:00'))";
        Time start_time = Time.valueOf("10:30:00");
        Time end_time = Time.valueOf("13:00:00");

        DataRow row = new DataRow(
                "Events",
                Arrays.asList("start_time", "end_time"),
                Arrays.asList(start_time, end_time)
        );

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testNullConcat() {
        String sqlCommand = "SELECT * FROM Employees WHERE first_name || ' ' || last_name IS NULL";
        DataRow row = new DataRow(
                "Employees",
                Arrays.asList("first_name", "last_name"),
                Arrays.asList(null, "Doe")
        );

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testNullRegExpMatchOperator() {
        String sqlCommand = "SELECT * FROM Employees WHERE NOT(name ~ 'John.*')";
        DataRow row = new DataRow(
                "Employees",
                Collections.singletonList("name"),
                Collections.singletonList(null)
        );

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testNullSimilarToExpression() {
        String sqlCommand = "SELECT * FROM Employees WHERE NOT(name SIMILAR TO 'John%')";
        DataRow row = new DataRow(
                "Employees",
                Collections.singletonList("name"),
                Collections.singletonList(null)
        );

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testNotSimilarToExpression() {
        String sqlCommand = "SELECT * FROM Employees WHERE name NOT SIMILAR TO 'John%'";
        DataRow row = new DataRow(
                "Employees",
                Collections.singletonList("name"),
                Collections.singletonList("Jack")
        );

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testLike() {
        String sqlCommand = "SELECT * FROM Employees WHERE name LIKE 'John%'";
        DataRow row = new DataRow(
                "Employees",
                Collections.singletonList("name"),
                Collections.singletonList("John Doe")
        );

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testNullLike() {
        String sqlCommand = "SELECT * FROM Employees WHERE NOT (name LIKE 'John%')";
        DataRow row = new DataRow(
                "Employees",
                Collections.singletonList("name"),
                Collections.singletonList(null)
        );

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testNotLike() {
        String sqlCommand = "SELECT * FROM Employees WHERE name NOT LIKE 'John%'";
        DataRow row = new DataRow(
                "Employees",
                Collections.singletonList("name"),
                Collections.singletonList("Jack")
        );

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testNotRegExpMatchOperator() {
        String sqlCommand = "SELECT * FROM Employees WHERE name !~ 'John.*'";
        DataRow row = new DataRow(
                "Employees",
                Collections.singletonList("name"),
                Collections.singletonList("Jack")
        );

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testInExpression() {
        String sqlCommand = "SELECT * FROM Employees WHERE department_id IN (1, 2, 3)";
        DataRow row = new DataRow(
                "Employees",
                Collections.singletonList("department_id"),
                Collections.singletonList(2)
        );

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testNotInExpression() {
        String sqlCommand = "SELECT * FROM Employees WHERE department_id NOT IN (1, 2, 3)";
        DataRow row = new DataRow(
                "Employees",
                Collections.singletonList("department_id"),
                Collections.singletonList(4)
        );

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testNullInExpression() {
        String sqlCommand = "SELECT * FROM Employees WHERE NOT (department_id IN (1, 2, 3))";
        DataRow row = new DataRow(
                "Employees",
                Collections.singletonList("department_id"),
                Collections.singletonList(null)
        );

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testNullValue() {
        String sqlCommand = "SELECT * FROM Employees WHERE NOT (salary = NULL)";
        DataRow row = new DataRow(
                "Employees",
                Collections.singletonList("salary"),
                Collections.singletonList(null)
        );

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }


}
