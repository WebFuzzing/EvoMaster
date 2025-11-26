package org.evomaster.client.java.sql.heuristic;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;
import org.evomaster.client.java.controller.api.dto.database.schema.ColumnDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto;
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto;
import org.evomaster.client.java.controller.api.dto.database.schema.TableIdDto;
import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.sql.DataRow;

import org.evomaster.client.java.sql.QueryResult;
import org.evomaster.client.java.sql.heuristic.function.NowFunction;
import org.evomaster.client.java.sql.internal.SqlParserUtils;
import org.evomaster.client.java.sql.internal.TaintHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Time;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class SqlExpressionEvaluatorTest {
    private DbInfoDto schema;

    private static ColumnDto createColumnDto(String columnName) {
        ColumnDto column = new ColumnDto();
        column.name = columnName;
        return column;
    }

    private static TableDto createTableDto(String tableName) {
        TableDto table = new TableDto();
        table.id = new TableIdDto();
        table.id.name = tableName;
        return table;
    }

    @BeforeEach
    void setUp() {

        schema = new DbInfoDto();
        TableDto personsTable = createTableDto("persons");
        personsTable.columns.add(createColumnDto("name"));
        personsTable.columns.add(createColumnDto("age"));
        personsTable.columns.add(createColumnDto("birth_day"));
        personsTable.columns.add(createColumnDto("is_member"));

        TableDto employeesTable = createTableDto("employees");
        employeesTable.columns.add(createColumnDto("salary"));
        employeesTable.columns.add(createColumnDto("bonus"));
        employeesTable.columns.add(createColumnDto("first_name"));
        employeesTable.columns.add(createColumnDto("last_name"));
        employeesTable.columns.add(createColumnDto("age"));
        employeesTable.columns.add(createColumnDto("name"));
        employeesTable.columns.add(createColumnDto("department_id"));
        employeesTable.columns.add(createColumnDto("employee_id"));
        employeesTable.columns.add(createColumnDto("hire_year"));

        TableDto schedulesTable = createTableDto("schedules");
        schedulesTable.columns.add(createColumnDto("start_time"));

        TableDto eventsTable = createTableDto("events");
        eventsTable.columns.add(createColumnDto("event_id"));
        eventsTable.columns.add(createColumnDto("event_timestamp"));
        eventsTable.columns.add(createColumnDto("event_date"));
        eventsTable.columns.add(createColumnDto("event_time"));
        eventsTable.columns.add(createColumnDto("start_date"));
        eventsTable.columns.add(createColumnDto("end_date"));
        eventsTable.columns.add(createColumnDto("start_time"));
        eventsTable.columns.add(createColumnDto("end_time"));
        eventsTable.columns.add(createColumnDto("start_timestamp"));
        eventsTable.columns.add(createColumnDto("end_timestamp"));

        TableDto usersTable = createTableDto("users");
        usersTable.columns.add(createColumnDto("permissions"));

        TableDto ordersTable = createTableDto("orders");
        ordersTable.columns.add(createColumnDto("order_id"));
        ordersTable.columns.add(createColumnDto("order_date"));
        ordersTable.columns.add(createColumnDto("order_time"));
        ordersTable.columns.add(createColumnDto("customer_id"));
        ordersTable.columns.add(createColumnDto("order_timestamp"));

        TableDto productsTable = createTableDto("products");
        productsTable.columns.add(createColumnDto("price"));
        productsTable.columns.add(createColumnDto("product_name"));
        productsTable.columns.add(createColumnDto("original_price"));
        productsTable.columns.add(createColumnDto("discount_price"));
        productsTable.columns.add(createColumnDto("quantity"));

        TableDto appointmentsTable = createTableDto("appointments");
        appointmentsTable.columns.add(createColumnDto("appointment_id"));
        appointmentsTable.columns.add(createColumnDto("appointment_datetime"));

        schema.tables.add(personsTable);
        schema.tables.add(employeesTable);
        schema.tables.add(schedulesTable);
        schema.tables.add(eventsTable);
        schema.tables.add(usersTable);
        schema.tables.add(ordersTable);
        schema.tables.add(productsTable);
        schema.tables.add(appointmentsTable);

    }

    private void assertSqlExpressionEvaluatesToTrue(String sqlCommand, QueryResult queryResult) {
        Statement parsedSqlCommand = SqlParserUtils.parseSqlCommand(sqlCommand);
        Select select = (Select) parsedSqlCommand;


        TableColumnResolver columnReferenceResolver = new TableColumnResolver(schema);
        TaintHandler taintHandler = null;

        columnReferenceResolver.enterStatementeContext(select);

        SqlExpressionEvaluator evaluator = new SqlExpressionEvaluator.SqlExpressionEvaluatorBuilder()
                .withTableColumnResolver(columnReferenceResolver)
                .withTaintHandler(taintHandler)
                .withCurrentDataRow(queryResult.seeRows().get(0)).build();

        select.getPlainSelect().getWhere().accept(evaluator);
        columnReferenceResolver.exitCurrentStatementContext();

        Truthness truthness = evaluator.getEvaluatedTruthness();
        assertTrue(truthness.isTrue());
    }


    private void assertSqlExpressionEvaluatesToTrue(String sqlCommand, DataRow... row) {
        Statement parsedSqlCommand = SqlParserUtils.parseSqlCommand(sqlCommand);
        Select select = (Select) parsedSqlCommand;


        TableColumnResolver columnReferenceResolver = new TableColumnResolver(schema);
        TaintHandler taintHandler = null;

        columnReferenceResolver.enterStatementeContext(select);
        SqlExpressionEvaluator evaluator = new SqlExpressionEvaluator.SqlExpressionEvaluatorBuilder()
                .withTableColumnResolver(columnReferenceResolver)
                .withTaintHandler(taintHandler)
                .withCurrentDataRow(row[0])
                .build();

        select.getPlainSelect().getWhere().accept(evaluator);
        columnReferenceResolver.exitCurrentStatementContext();

        Truthness truthness = evaluator.getEvaluatedTruthness();
        assertTrue(truthness.isTrue());
    }

    private void assertSqlExpressionEvaluatesToFalse(String sqlCommand, DataRow row) {
        Statement parsedSqlCommand = SqlParserUtils.parseSqlCommand(sqlCommand);
        Select select = (Select) parsedSqlCommand;

        TaintHandler taintHandler = null;
        TableColumnResolver columnReferenceResolver = new TableColumnResolver(schema);

        columnReferenceResolver.enterStatementeContext(select);
        SqlExpressionEvaluator evaluator = new SqlExpressionEvaluator.SqlExpressionEvaluatorBuilder()
                .withTableColumnResolver(columnReferenceResolver)
                .withTaintHandler(taintHandler)
                .withCurrentDataRow(row)
                .build();

        select.getPlainSelect().getWhere().accept(evaluator);
        columnReferenceResolver.exitCurrentStatementContext();

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
        String sqlCommand = "SELECT name, age, is_member FROM persons WHERE is_member=true";
        DataRow row = new DataRow(
                "persons",
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
        String sqlCommand = "SELECT price FROM products WHERE price<=19.99";
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
        String sqlCommand = "SELECT product_name, price, quantity FROM products WHERE price * quantity > 100";
        DataRow row = new DataRow(
                "products",
                Arrays.asList("product_name", "price", "quantity"),
                Arrays.asList("Laptop", 120, 2));

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testSubtraction() {
        String sqlCommand = "SELECT product_name, original_price, discount_price FROM products WHERE original_price - discount_price > 20";
        DataRow row = new DataRow(
                "products",
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
        String sqlCommand = "SELECT order_id, customer_id, order_timestamp FROM orders WHERE order_timestamp = '2025-01-14 12:30:45'";
        DataRow row = new DataRow(
                "orders",
                Arrays.asList("order_id", "customer_id", "order_timestamp"),
                Arrays.asList(1, 1, Timestamp.valueOf("2025-01-14 12:30:45")));

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testEqualsToDate() throws ParseException {
        String sqlCommand = "SELECT event_id, event_date FROM events WHERE event_date = '2025-01-14'";
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        java.util.Date date = dateFormat.parse("2025-01-14");

        DataRow row = new DataRow(
                "events",
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
        String sqlCommand = "SELECT appointment_id, appointment_datetime FROM appointments WHERE appointment_datetime = '2025-01-14 12:30:45'";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date dateTime = sdf.parse("2025-01-14 12:30:45");

        DataRow row = new DataRow(
                "appointments",
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
        String sqlCommand = "SELECT event_id, event_time FROM events WHERE event_time = '2025-01-14 12:30:45+02:00'";
        OffsetDateTime offsetDateTime = OffsetDateTime.parse("2025-01-14T12:30:45+02:00");

        DataRow row = new DataRow(
                "events",
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
        String sqlCommand = "SELECT start_time FROM schedules WHERE start_time BETWEEN '09:00:00' AND '17:00:00'";
        Time startTime = Time.valueOf("12:00:00");

        DataRow row = new DataRow(
                "schedules",
                Collections.singletonList("start_time"),
                Collections.singletonList(startTime));

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testBetweenTimestamps() {
        String sqlCommand = "SELECT event_timestamp FROM events WHERE event_timestamp BETWEEN '2023-01-01 00:00:00' AND '2023-12-31 23:59:59'";
        Timestamp eventTimestamp = Timestamp.valueOf("2023-06-15 12:00:00");

        DataRow row = new DataRow(
                "events",
                Collections.singletonList("event_timestamp"),
                Collections.singletonList(eventTimestamp));

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testXOrCondition() {
        String sqlCommand = "SELECT salary, age FROM employees WHERE (age > 30) XOR (salary > 50000)";
        DataRow row = new DataRow(
                "employees",
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
        String sqlCommand = "SELECT start_time, end_time FROM events WHERE (start_time, end_time) OVERLAPS (TIME '10:00:00', TIME '12:00:00')";
        DataRow row = new DataRow(
                "events",
                Arrays.asList("start_time", "end_time"),
                Arrays.asList(Time.valueOf("09:00:00"), Time.valueOf("11:00:00"))
        );

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testOverlapsDate() {
        String sqlCommand = "SELECT start_date, end_date FROM events WHERE (start_date, end_date) OVERLAPS (DATE '2023-01-01', DATE '2024-01-01')";
        DataRow row = new DataRow(
                "events",
                Arrays.asList("start_date", "end_date"),
                Arrays.asList(java.sql.Date.valueOf("2022-12-31"), java.sql.Date.valueOf("2023-06-01"))
        );

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testOverlapsTimestamp() {
        String sqlCommand = "SELECT start_timestamp, end_timestamp FROM events WHERE (start_timestamp, end_timestamp) OVERLAPS (TIMESTAMP '2023-01-01 00:00:00', TIMESTAMP '2024-01-01 23:59:59')";
        DataRow row = new DataRow(
                "events",
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
        String sqlCommand = "SELECT * FROM employees WHERE first_name || ' ' || last_name = 'John Doe'";
        DataRow row = new DataRow(
                "employees",
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
        String sqlCommand = "SELECT * FROM employees WHERE name SIMILAR TO 'John%'";
        DataRow row = new DataRow(
                "employees",
                Collections.singletonList("name"),
                Collections.singletonList("John Doe")
        );

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testArrayConstructor() {
        String sqlCommand = "SELECT * FROM employees WHERE ARRAY[salary, bonus] = ARRAY[50000, 5000]";
        DataRow row = new DataRow(
                "employees",
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
        String sqlCommand = "SELECT * FROM employees WHERE NOT (department_id IN (1, 2, 3))";
        DataRow row = new DataRow(
                "employees",
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


    @Test
    public void testTableAlias() {
        String sqlCommand = "SELECT * FROM Employees AS e WHERE e.salary > 100";
        DataRow row = new DataRow(
                "Employees",
                Collections.singletonList("salary"),
                Collections.singletonList(101)
        );

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);

    }

    @Test
    public void testColumnAlias() {
        String sqlCommand = "SELECT * FROM Employees AS e WHERE e.salary > 100";
        DataRow row = new DataRow(
                "Employees",
                Collections.singletonList("salary"),
                Collections.singletonList(101)
        );

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testCastAsInteger() {
        String sqlCommand = "SELECT * FROM Employees AS e WHERE e.salary > CAST(1.0 AS INTEGER)";

        DataRow row = new DataRow(
                "Employees",
                Collections.singletonList("salary"),
                Collections.singletonList(101)
        );

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testStringDecode() {
        String sqlCommand = "SELECT * FROM Employees AS e WHERE e.name = STRINGDECODE('\\uffff')";

        DataRow row = new DataRow(
                "Employees",
                Collections.singletonList("name"),
                Collections.singletonList(String.valueOf('\uffff'))
        );

        assertSqlExpressionEvaluatesToTrue(sqlCommand, row);
    }

    @Test
    public void testCount() {
        String sqlCommand = "SELECT COUNT(*) FROM Employees";
        Statement parsedSqlCommand = SqlParserUtils.parseSqlCommand(sqlCommand);
        PlainSelect plainSelect = (PlainSelect) parsedSqlCommand;
        SelectItem selectItem = plainSelect.getSelectItems().get(0);
        Expression countExpression = selectItem.getExpression();

        QueryResult queryResult = new QueryResult(Collections.singletonList("name"), "Employees");
        queryResult.addRow(new DataRow(
                "Employees",
                Collections.singletonList("name"),
                Collections.singletonList("John")
        ));

        queryResult.addRow(new DataRow(
                "Employees",
                Collections.singletonList("name"),
                Collections.singletonList("John")
        ));

        SqlExpressionEvaluator evaluator = new SqlExpressionEvaluator.SqlExpressionEvaluatorBuilder()
                .withCurrentQueryResult(queryResult)
                .build();

        countExpression.accept(evaluator);
        assertNotNull(evaluator.getEvaluatedValue());
        assertTrue(evaluator.getEvaluatedValue() instanceof Long);
        assertEquals(2L, evaluator.getEvaluatedValue());
    }

    @Test
    public void testSum() {
        String sqlCommand = "SELECT SUM(salary) FROM Employees";
        Statement parsedSqlCommand = SqlParserUtils.parseSqlCommand(sqlCommand);
        PlainSelect plainSelect = (PlainSelect) parsedSqlCommand;
        SelectItem selectItem = plainSelect.getSelectItems().get(0);
        Expression sumExpression = selectItem.getExpression();

        QueryResult queryResult = new QueryResult(Collections.singletonList("salary"), "Employees");
        queryResult.addRow(new DataRow(
                "Employees",
                Collections.singletonList("salary"),
                Collections.singletonList(10_000)
        ));

        queryResult.addRow(new DataRow(
                "Employees",
                Collections.singletonList("salary"),
                Collections.singletonList(20_000)
        ));

        TableColumnResolver tableColumnResolver = new TableColumnResolver(schema);
        tableColumnResolver.enterStatementeContext(parsedSqlCommand);
        SqlExpressionEvaluator evaluator = new SqlExpressionEvaluator.SqlExpressionEvaluatorBuilder()
                .withCurrentQueryResult(queryResult)
                .withTableColumnResolver(tableColumnResolver)
                .build();

        sumExpression.accept(evaluator);
        assertNotNull(evaluator.getEvaluatedValue());
        assertTrue(evaluator.getEvaluatedValue() instanceof Long);
        assertEquals(30_000L, evaluator.getEvaluatedValue());
    }

    @Test
    public void testAvg() {
        String sqlCommand = "SELECT AVG(salary) FROM Employees";
        Statement parsedSqlCommand = SqlParserUtils.parseSqlCommand(sqlCommand);
        PlainSelect plainSelect = (PlainSelect) parsedSqlCommand;
        SelectItem selectItem = plainSelect.getSelectItems().get(0);
        Expression avgExpression = selectItem.getExpression();

        QueryResult queryResult = new QueryResult(Collections.singletonList("salary"), "Employees");
        queryResult.addRow(new DataRow(
                "Employees",
                Collections.singletonList("salary"),
                Collections.singletonList(10_000)
        ));

        queryResult.addRow(new DataRow(
                "Employees",
                Collections.singletonList("salary"),
                Collections.singletonList(20_000)
        ));

        TableColumnResolver tableColumnResolver = new TableColumnResolver(schema);
        tableColumnResolver.enterStatementeContext(parsedSqlCommand);
        SqlExpressionEvaluator evaluator = new SqlExpressionEvaluator.SqlExpressionEvaluatorBuilder()
                .withCurrentQueryResult(queryResult)
                .withTableColumnResolver(tableColumnResolver)
                .build();

        avgExpression.accept(evaluator);
        assertNotNull(evaluator.getEvaluatedValue());
        assertTrue(evaluator.getEvaluatedValue() instanceof Long);
        assertEquals(15_000L, evaluator.getEvaluatedValue());
    }

    @Test
    public void testMax() {
        String sqlCommand = "SELECT MAX(salary) FROM Employees";
        Statement parsedSqlCommand = SqlParserUtils.parseSqlCommand(sqlCommand);
        PlainSelect plainSelect = (PlainSelect) parsedSqlCommand;
        SelectItem selectItem = plainSelect.getSelectItems().get(0);
        Expression maxExpression = selectItem.getExpression();

        QueryResult queryResult = new QueryResult(Collections.singletonList("salary"), "Employees");
        queryResult.addRow(new DataRow(
                "Employees",
                Collections.singletonList("salary"),
                Collections.singletonList(10_000)
        ));

        queryResult.addRow(new DataRow(
                "Employees",
                Collections.singletonList("salary"),
                Collections.singletonList(20_000)
        ));

        TableColumnResolver tableColumnResolver = new TableColumnResolver(schema);
        tableColumnResolver.enterStatementeContext(parsedSqlCommand);
        SqlExpressionEvaluator evaluator = new SqlExpressionEvaluator.SqlExpressionEvaluatorBuilder()
                .withCurrentQueryResult(queryResult)
                .withTableColumnResolver(tableColumnResolver)
                .build();

        maxExpression.accept(evaluator);
        assertNotNull(evaluator.getEvaluatedValue());
        assertTrue(evaluator.getEvaluatedValue() instanceof Integer);
        assertEquals(20_000, evaluator.getEvaluatedValue());
    }

    @Test
    public void testMin() {
        String sqlCommand = "SELECT MIN(salary) FROM Employees";
        Statement parsedSqlCommand = SqlParserUtils.parseSqlCommand(sqlCommand);
        PlainSelect plainSelect = (PlainSelect) parsedSqlCommand;
        SelectItem selectItem = plainSelect.getSelectItems().get(0);
        Expression minExpression = selectItem.getExpression();

        QueryResult queryResult = new QueryResult(Collections.singletonList("salary"), "Employees");
        queryResult.addRow(new DataRow(
                "Employees",
                Collections.singletonList("salary"),
                Collections.singletonList(10_000)
        ));

        queryResult.addRow(new DataRow(
                "Employees",
                Collections.singletonList("salary"),
                Collections.singletonList(20_000)
        ));

        TableColumnResolver tableColumnResolver = new TableColumnResolver(schema);
        tableColumnResolver.enterStatementeContext(parsedSqlCommand);
        SqlExpressionEvaluator evaluator = new SqlExpressionEvaluator.SqlExpressionEvaluatorBuilder()
                .withCurrentQueryResult(queryResult)
                .withTableColumnResolver(tableColumnResolver)
                .build();

        minExpression.accept(evaluator);
        assertNotNull(evaluator.getEvaluatedValue());
        assertTrue(evaluator.getEvaluatedValue() instanceof Integer);
        assertEquals(10_000, evaluator.getEvaluatedValue());
    }

    @Test
    public void testCoalesce() {
        String sql = "COALESCE(null, 'hello', 'world')";
        Expression expression = assertDoesNotThrow(() -> CCJSqlParserUtil.parseExpression(sql));

        SqlExpressionEvaluator.SqlExpressionEvaluatorBuilder builder = new SqlExpressionEvaluator.SqlExpressionEvaluatorBuilder();
        SqlExpressionEvaluator evaluator = builder
                .withTableColumnResolver(new TableColumnResolver(new DbInfoDto()))
                .build();

        expression.accept(evaluator);
        Object value = evaluator.getEvaluatedValue();
        assertEquals("hello", value);
    }

    @Test
    public void testCoalesceWithAllNull() {
        String sql = "COALESCE(null, null, null)";
        Expression expression = assertDoesNotThrow(() -> CCJSqlParserUtil.parseExpression(sql));

        SqlExpressionEvaluator.SqlExpressionEvaluatorBuilder builder = new SqlExpressionEvaluator.SqlExpressionEvaluatorBuilder();
        SqlExpressionEvaluator evaluator = builder
                .withTableColumnResolver(new TableColumnResolver(new DbInfoDto()))
                .build();

        expression.accept(evaluator);
        Object value = evaluator.getEvaluatedValue();
        assertNull(value);
    }

    @Test
    public void testCoalesceWithFirstValueNotNull() {
        String sql = "COALESCE('hello', 'world', null)";
        Expression expression = assertDoesNotThrow(() -> CCJSqlParserUtil.parseExpression(sql));

        SqlExpressionEvaluator.SqlExpressionEvaluatorBuilder builder = new SqlExpressionEvaluator.SqlExpressionEvaluatorBuilder();
        SqlExpressionEvaluator evaluator = builder
                .withTableColumnResolver(new TableColumnResolver(new DbInfoDto()))
                .build();

        expression.accept(evaluator);
        Object actualString = evaluator.getEvaluatedValue();
        assertEquals("hello", actualString);
    }

    @Test
    public void testUpper() {
        String sql = "UPPER('hello')";
        Expression expression = assertDoesNotThrow(() -> CCJSqlParserUtil.parseExpression(sql));

        SqlExpressionEvaluator.SqlExpressionEvaluatorBuilder builder = new SqlExpressionEvaluator.SqlExpressionEvaluatorBuilder();
        SqlExpressionEvaluator evaluator = builder
                .withTableColumnResolver(new TableColumnResolver(new DbInfoDto()))
                .build();

        expression.accept(evaluator);
        Object value = evaluator.getEvaluatedValue();
        assertEquals("HELLO", value);
    }

    @Test
    public void testTimeFunction() {
        String sql = "TIME('2025-01-14 12:30:45')";
        Expression expression = assertDoesNotThrow(() -> CCJSqlParserUtil.parseExpression(sql));

        SqlExpressionEvaluator.SqlExpressionEvaluatorBuilder builder = new SqlExpressionEvaluator.SqlExpressionEvaluatorBuilder();
        SqlExpressionEvaluator evaluator = builder
                .withTableColumnResolver(new TableColumnResolver(new DbInfoDto()))
                .build();

        expression.accept(evaluator);
        Object actualTime = evaluator.getEvaluatedValue();
        final Time exptectedTime = Time.valueOf("12:30:45");
        assertEquals(exptectedTime.toString(), actualTime.toString());
    }

    @Test
    public void testTimestampExpression() {
        String sql = "TIMESTAMP '2025-01-14 12:30:45'";
        Expression expression = assertDoesNotThrow(() -> CCJSqlParserUtil.parseExpression(sql));

        SqlExpressionEvaluator.SqlExpressionEvaluatorBuilder builder = new SqlExpressionEvaluator.SqlExpressionEvaluatorBuilder();
        SqlExpressionEvaluator evaluator = builder
                .withTableColumnResolver(new TableColumnResolver(new DbInfoDto()))
                .build();

        expression.accept(evaluator);
        Object actualTimestamp = evaluator.getEvaluatedValue();
        assertTrue(actualTimestamp instanceof Timestamp);
        final Timestamp exptectedTimestamp = java.sql.Timestamp.valueOf("2025-01-14 12:30:45");
        assertEquals(exptectedTimestamp, actualTimestamp);
    }

    @Test
    public void testDateExpression() {
        String sql = "DATE '2025-01-14'";
        Expression expression = assertDoesNotThrow(() -> CCJSqlParserUtil.parseExpression(sql));

        SqlExpressionEvaluator.SqlExpressionEvaluatorBuilder builder = new SqlExpressionEvaluator.SqlExpressionEvaluatorBuilder();
        SqlExpressionEvaluator evaluator = builder
                .withTableColumnResolver(new TableColumnResolver(new DbInfoDto()))
                .build();

        expression.accept(evaluator);
        Object actualDate = evaluator.getEvaluatedValue();

        assertTrue(actualDate instanceof java.sql.Date);

        final java.sql.Date expectedDate = java.sql.Date.valueOf("2025-01-14");
        assertEquals(expectedDate, actualDate);
    }

    @Test
    public void testTimeExpression() {
        String sql = "TIME '12:30:45'";
        Expression expression = assertDoesNotThrow(() -> CCJSqlParserUtil.parseExpression(sql));

        SqlExpressionEvaluator.SqlExpressionEvaluatorBuilder builder = new SqlExpressionEvaluator.SqlExpressionEvaluatorBuilder();
        SqlExpressionEvaluator evaluator = builder
                .withTableColumnResolver(new TableColumnResolver(new DbInfoDto()))
                .build();

        expression.accept(evaluator);
        Object actualTime = evaluator.getEvaluatedValue();

        assertTrue(actualTime instanceof java.sql.Time);

        final java.sql.Time expectedTime = java.sql.Time.valueOf("12:30:45");
        assertEquals(expectedTime, actualTime);
    }

    @Test
    public void testCoalesceOneExpression() {
        String sql = "COALESCE('hello')";
        Expression expression = assertDoesNotThrow(() -> CCJSqlParserUtil.parseExpression(sql));

        SqlExpressionEvaluator.SqlExpressionEvaluatorBuilder builder = new SqlExpressionEvaluator.SqlExpressionEvaluatorBuilder();
        SqlExpressionEvaluator evaluator = builder
                .withTableColumnResolver(new TableColumnResolver(new DbInfoDto()))
                .build();

        expression.accept(evaluator);
        Object value = evaluator.getEvaluatedValue();
        assertEquals("hello", value);
    }

    @Test
    public void dateTruncDayTruncatesTimestamp() {
        String sql = "DATE_TRUNC('day', TIMESTAMP '2025-01-14 12:30:45')";
        Expression expression = assertDoesNotThrow(() -> CCJSqlParserUtil.parseExpression(sql));

        SqlExpressionEvaluator.SqlExpressionEvaluatorBuilder builder = new SqlExpressionEvaluator.SqlExpressionEvaluatorBuilder();
        SqlExpressionEvaluator evaluator = builder
                .withTableColumnResolver(new TableColumnResolver(new DbInfoDto()))
                .build();

        expression.accept(evaluator);
        Object actual = evaluator.getEvaluatedValue();
        assertTrue(actual instanceof java.util.Date);
        Instant actualInstant = ((java.util.Date) actual).toInstant();
        Instant expectedInstant = Instant.parse("2025-01-14T00:00:00Z");
        assertEquals(expectedInstant, actualInstant);
    }

    @Test
    public void dateTruncHourTruncatesTimestamp() {
        String sql = "DATE_TRUNC('hour', TIMESTAMP '2025-01-14 12:30:45')";
        Expression expression = assertDoesNotThrow(() -> CCJSqlParserUtil.parseExpression(sql));

        SqlExpressionEvaluator.SqlExpressionEvaluatorBuilder builder = new SqlExpressionEvaluator.SqlExpressionEvaluatorBuilder();
        SqlExpressionEvaluator evaluator = builder
                .withTableColumnResolver(new TableColumnResolver(new DbInfoDto()))
                .build();

        expression.accept(evaluator);
        Object actual = evaluator.getEvaluatedValue();
        assertTrue(actual instanceof java.util.Date);
        Instant actualInstant = ((java.util.Date) actual).toInstant();
        final Timestamp expected = Timestamp.valueOf("2025-01-14 12:00:00");
        Instant expectedInstant = expected.toInstant();
        assertEquals(expectedInstant, actualInstant);
    }

    @Test
    public void dateTruncUnknownUnitThrowsIllegalArgumentException() {
        String sql = "DATE_TRUNC('millennium', TIMESTAMP '2025-01-14 12:30:45')";
        Expression expression = assertDoesNotThrow(() -> CCJSqlParserUtil.parseExpression(sql));

        SqlExpressionEvaluator.SqlExpressionEvaluatorBuilder builder = new SqlExpressionEvaluator.SqlExpressionEvaluatorBuilder();
        SqlExpressionEvaluator evaluator = builder
                .withTableColumnResolver(new TableColumnResolver(new DbInfoDto()))
                .build();

        assertThrows(IllegalArgumentException.class, () -> {
            expression.accept(evaluator);
            // if evaluation returns something, force get to trigger potential errors
            evaluator.getEvaluatedValue();
        });
    }

    @Test
    public void testTimestampTzExpression() {
        String sql = "TIMESTAMPTZ '2025-01-22 15:30:45+02:00'";
        Expression expression = assertDoesNotThrow(() -> CCJSqlParserUtil.parseExpression(sql));

        SqlExpressionEvaluator.SqlExpressionEvaluatorBuilder builder = new SqlExpressionEvaluator.SqlExpressionEvaluatorBuilder();
        SqlExpressionEvaluator evaluator = builder
                .withTableColumnResolver(new TableColumnResolver(new DbInfoDto()))
                .build();

        expression.accept(evaluator);
        Object actual = evaluator.getEvaluatedValue();

        assertTrue(actual instanceof java.time.OffsetDateTime);

        java.time.OffsetDateTime expected =
                java.time.OffsetDateTime.parse("2025-01-22T15:30:45+02:00");

        assertEquals(expected, actual);
    }

    @Test
    public void testNowStableValue() {
        String sql = "NOW()";
        Expression expression = assertDoesNotThrow(() -> CCJSqlParserUtil.parseExpression(sql));

        SqlExpressionEvaluator.SqlExpressionEvaluatorBuilder builder = new SqlExpressionEvaluator.SqlExpressionEvaluatorBuilder();
        SqlExpressionEvaluator evaluator = builder
                .withTableColumnResolver(new TableColumnResolver(new DbInfoDto()))
                .build();

        expression.accept(evaluator);
        Object actual = evaluator.getEvaluatedValue();
        assertTrue(actual instanceof Timestamp);
        assertEquals(NowFunction.CANONICAL_NOW_VALUE, actual);
    }

    @Test
    public void testLower() {
        String sql = "LOWER('HELLO')";
        Expression expression = assertDoesNotThrow(() -> CCJSqlParserUtil.parseExpression(sql));

        SqlExpressionEvaluator.SqlExpressionEvaluatorBuilder builder = new SqlExpressionEvaluator.SqlExpressionEvaluatorBuilder();
        SqlExpressionEvaluator evaluator = builder
                .withTableColumnResolver(new TableColumnResolver(new DbInfoDto()))
                .build();

        expression.accept(evaluator);
        Object value = evaluator.getEvaluatedValue();
        assertEquals("hello", value);
    }

    @Test
    public void testLowerCaseInsensitive() {
        String sql = "lower('HELLO')";
        Expression expression = assertDoesNotThrow(() -> CCJSqlParserUtil.parseExpression(sql));

        SqlExpressionEvaluator.SqlExpressionEvaluatorBuilder builder = new SqlExpressionEvaluator.SqlExpressionEvaluatorBuilder();
        SqlExpressionEvaluator evaluator = builder
                .withTableColumnResolver(new TableColumnResolver(new DbInfoDto()))
                .build();

        expression.accept(evaluator);
        Object value = evaluator.getEvaluatedValue();
        assertEquals("hello", value);
    }

    @Test
    public void testUpperCaseInsenstive() {
        String sql = "upper('hello')";
        Expression expression = assertDoesNotThrow(() -> CCJSqlParserUtil.parseExpression(sql));

        SqlExpressionEvaluator.SqlExpressionEvaluatorBuilder builder = new SqlExpressionEvaluator.SqlExpressionEvaluatorBuilder();
        SqlExpressionEvaluator evaluator = builder
                .withTableColumnResolver(new TableColumnResolver(new DbInfoDto()))
                .build();

        expression.accept(evaluator);
        Object value = evaluator.getEvaluatedValue();
        assertEquals("HELLO", value);
    }
}
