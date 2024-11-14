package org.evomaster.client.java.sql.internal;

import org.evomaster.client.java.controller.api.dto.database.operations.InsertionDto;
import org.evomaster.client.java.controller.api.dto.database.schema.ColumnDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto;
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto;
import org.evomaster.client.java.sql.DataRow;
import org.evomaster.client.java.sql.QueryResult;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.util.*;

import static org.evomaster.client.java.sql.dsl.SqlDsl.sql;
import static org.evomaster.client.java.sql.internal.QueryResultTransformer.convertInsertionDtosToQueryResults;
import static org.junit.jupiter.api.Assertions.*;

public class HeuristicsCalculatorWithInsertionDtoTest {

    private DbInfoDto createSchemaDtoWithFooTableAndXColumn(String xDataType){
        DbInfoDto schemaDto = new DbInfoDto();
        TableDto tableDto = new TableDto();
        tableDto.name = "Foo";
        ColumnDto dto = new ColumnDto();
        dto.name = "x";
        dto.type = xDataType;
        tableDto.columns.add(dto);
        schemaDto.tables.add(tableDto);
        return schemaDto;
    }

    private final Map<String, Set<String>> selectWhereXofFoo = new HashMap<String, Set<String>>(){{put("Foo", Collections.singleton("x"));}};


    @Test
    public void testEmptyWithInsertionDto() {

        String sql = "select x from Foo";

        QueryResult data = new QueryResult(Arrays.asList("x"), "Foo");

        double dist = HeuristicsCalculator.computeDistance(sql, data);
        assertTrue(dist > 0);

        List<InsertionDto> insertions = sql().insertInto("Foo", 1L)
                .d("x", "1")
                .dtos();
        QueryResult[] newData = convertInsertionDtosToQueryResults(insertions, selectWhereXofFoo, createSchemaDtoWithFooTableAndXColumn("INT"));

        dist = HeuristicsCalculator.computeDistance(sql, newData);
        assertEquals(0d, dist);
    }

    private void checkIncreasingTillCovered(String name,
                                            List<Object> values,
                                            List<InsertionDto> insertionDtos,
                                            Map<String, Set<String>> columns,
                                            DbInfoDto dto,
                                            String sql) {

        QueryResult data = new QueryResult(Arrays.asList(name), "Foo");

        double prev = -1;

        for (Object val : values) {
            data.addRow(new DataRow(name, val, "Foo"));
            double dist = HeuristicsCalculator.computeDistance(sql, data);
            assertTrue(dist > 0);
            if (prev >= 0) {
                assertTrue(dist < prev, "dist=" + dist + " , previous=" + prev);
            }
            prev = dist;
        }

        QueryResult[] newData = convertInsertionDtosToQueryResults(insertionDtos, columns, dto);
        double target = HeuristicsCalculator.computeDistance(sql, newData);
        assertTrue(target < prev);
        assertEquals(0d, target,"dist=" + target + " , previous=" + prev);
    }


    @Test
    public void testTrue() {
        String sql = "select a from Foo where x = true";


        List<InsertionDto> insertions = sql().insertInto("Foo", 1L)
                .d("x", "true")
                .dtos();
        checkIncreasingTillCovered("x", Arrays.asList(false), insertions, selectWhereXofFoo, createSchemaDtoWithFooTableAndXColumn("BOOL"),sql);
    }

    @Test
    public void testFalse() {
        String sql = "select a from Foo where x = false";

        List<InsertionDto> insertions = sql().insertInto("Foo", 1L)
                .d("x", "false")
                .dtos();

        checkIncreasingTillCovered("x", Arrays.asList(true), insertions, selectWhereXofFoo, createSchemaDtoWithFooTableAndXColumn("BOOL"), sql);
    }

    @Test
    public void testNotTrue() {
        String sql = "select a from Foo where x != true";

        List<InsertionDto> insertions = sql().insertInto("Foo", 1L)
                .d("x", "false")
                .dtos();

        checkIncreasingTillCovered("x", Arrays.asList(true), insertions, selectWhereXofFoo, createSchemaDtoWithFooTableAndXColumn("BOOL"), sql);
    }

    @Test
    public void testNotFalse() {
        String sql = "select a from Foo where x != FALSE";

        List<InsertionDto> insertions = sql().insertInto("Foo", 1L)
                .d("x", "true")
                .dtos();

        checkIncreasingTillCovered("x", Arrays.asList(false), insertions, selectWhereXofFoo, createSchemaDtoWithFooTableAndXColumn("BOOL"), sql);
    }

    @Test
    public void testWithParentheses() {

        String sql = "select a from Foo where x = (5)";

        List<InsertionDto> insertions = sql().insertInto("Foo", 1L)
                .d("x", "5")
                .dtos();

        checkIncreasingTillCovered("x", Arrays.asList(9, 3, 6), insertions, selectWhereXofFoo, createSchemaDtoWithFooTableAndXColumn("INT"), sql);
    }

    @Test
    public void testNegativeWithParentheses() {

        String sql = "select a from Foo where x = (-5)";

        List<InsertionDto> insertions = sql().insertInto("Foo", 1L)
                .d("x", "-5")
                .dtos();

        checkIncreasingTillCovered("x", Arrays.asList(9, 3, -7), insertions, selectWhereXofFoo, createSchemaDtoWithFooTableAndXColumn("INT"), sql);
    }

    @Test
    public void testEqualInt() {

        String sql = "select x from Foo where x=5";
        List<InsertionDto> insertions = sql().insertInto("Foo", 1L)
                .d("x", "5")
                .dtos();

        checkIncreasingTillCovered("x", Arrays.asList(9, 3, 6), insertions, selectWhereXofFoo, createSchemaDtoWithFooTableAndXColumn("INT"), sql);
    }

    @Test
    public void testEqualToNull() {
        String sql = "select x from Foo where x = NULL";

        List<InsertionDto> insertions = sql().insertInto("Foo", 1L)
                .d("x", null)
                .dtos();

        checkIncreasingTillCovered("x", Arrays.asList("foo"), insertions, selectWhereXofFoo, createSchemaDtoWithFooTableAndXColumn("CHARACTER"), sql);
    }

    @Test
    public void testIsNull() {
        String sql = "select x from Foo where x IS NULL";

        List<InsertionDto> insertions = sql().insertInto("Foo", 1L)
                .d("x", null)
                .dtos();

        checkIncreasingTillCovered("x", Arrays.asList("foo"), insertions, selectWhereXofFoo, createSchemaDtoWithFooTableAndXColumn("CHARACTER"), sql);
    }

    @Test
    public void testIsNotNull() {
        String sql = "select x from Foo where x IS NOT NULL";

        List<Object> list = new ArrayList<>();
        list.add(null);

        List<InsertionDto> insertions = sql().insertInto("Foo", 1L)
                .d("x", "foo")
                .dtos();

        checkIncreasingTillCovered("x", list, insertions, selectWhereXofFoo, createSchemaDtoWithFooTableAndXColumn("CHARACTER"), sql);
    }

    @Test
    public void testDifferentFromNull() {
        String sql = "select x from Foo where x != NULL";

        List<Object> list = new ArrayList<>();
        list.add(null);

        List<InsertionDto> insertions = sql().insertInto("Foo", 1L)
                .d("x", "foo")
                .dtos();

        checkIncreasingTillCovered("x", list, insertions, selectWhereXofFoo, createSchemaDtoWithFooTableAndXColumn("CHARACTER"),  sql);
    }


    @Test
    public void testInNumeric() {

        String sql = "select x from Foo where x IN (10, 20)";

        List<InsertionDto> insertions = sql().insertInto("Foo", 1L)
                .d("x", "10")
                .dtos();

        checkIncreasingTillCovered("x", Arrays.asList(-4, 6, 23, 12, 19), insertions, selectWhereXofFoo, createSchemaDtoWithFooTableAndXColumn("INT"), sql);
    }

    @Test
    public void testInNumericWithParenthesis() {

        String sql = "select x from Foo where (x IN (10, 20))";

        List<InsertionDto> insertions = sql().insertInto("Foo", 1L)
                .d("x", "10")
                .dtos();

        checkIncreasingTillCovered("x", Arrays.asList(-4, 6, 23, 12, 19), insertions, selectWhereXofFoo, createSchemaDtoWithFooTableAndXColumn("INT"), sql);
    }


    @Test
    public void testInStrings() {

        String sql = "select x from Foo where x IN ('a1', 'e5')";

        List<InsertionDto> insertions = sql().insertInto("Foo", 1L)
                .d("x", "a1")
                .dtos();

        checkIncreasingTillCovered("x", Arrays.asList("z9", "z7", "c7", "c2", "b2", "b1"), insertions, selectWhereXofFoo, createSchemaDtoWithFooTableAndXColumn("CHARACTER"), sql);
    }

    @Test
    public void testNotInNumeric() {

        String sql = "select x from Foo where x Not IN (10, 20)";

        List<InsertionDto> insertions = sql().insertInto("Foo", 1L)
                .d("x", "11")
                .dtos();

        checkIncreasingTillCovered("x", Arrays.asList(10), insertions, selectWhereXofFoo, createSchemaDtoWithFooTableAndXColumn("INT"), sql);
    }

//    @Disabled("Need to handle sub-selects. Not so simple, as they might have their own WHEREs")
//    @Test
//    public void testInSelect() {
//        String sql = "select * from Foo where 10 IN (select x from Foo)";
//
//        checkIncreasingTillCovered("x", Arrays.asList(20, 15, 8), 10, sql);
//    }
//
//
    @Test
    public void testEqualString() {

        String sql = "select t.bar as X from Foo t where X='abc123'";

        List<InsertionDto> insertions = sql().insertInto("Foo", 1L)
                .d("x", "abc123")
                .dtos();

        checkIncreasingTillCovered("x",
                Arrays.asList("a", "ab", "xxx123x", "xxx123", "axx123", "abc234"), insertions, selectWhereXofFoo, createSchemaDtoWithFooTableAndXColumn("CHARACTER"), sql);
    }

    @Test
    public void testNotEqualString() {

        String sql = "select t.bar as X from Foo t where X!='foo'";

        List<InsertionDto> insertions = sql().insertInto("Foo", 1L)
                .d("x", "blabla")
                .dtos();

        checkIncreasingTillCovered("x", Arrays.asList("foo"), insertions, selectWhereXofFoo, createSchemaDtoWithFooTableAndXColumn("CHARACTER"), sql);
    }

    @Test
    public void testNotEqual() {

        String sql = "select x from Foo where x != 5";

        List<InsertionDto> insertions = sql().insertInto("Foo", 1L)
                .d("x", "6")
                .dtos();

        checkIncreasingTillCovered("x", Arrays.asList(5), insertions, selectWhereXofFoo, createSchemaDtoWithFooTableAndXColumn("INT"), sql);
    }

    @Test
    public void testGreaterThanEquals() {

        String sql = "select x from Foo where x >= 5";

        List<InsertionDto> insertions = sql().insertInto("Foo", 1L)
                .d("x", "5")
                .dtos();

        checkIncreasingTillCovered("x", Arrays.asList(-4, 2, 3), insertions, selectWhereXofFoo, createSchemaDtoWithFooTableAndXColumn("INT"), sql);
    }

    @Test
    public void testGreaterThan() {

        String sql = "select x from Foo where x > 5";

        List<InsertionDto> insertions = sql().insertInto("Foo", 1L)
                .d("x", "6")
                .dtos();

        checkIncreasingTillCovered("x", Arrays.asList(-4, 2, 3, 5), insertions, selectWhereXofFoo, createSchemaDtoWithFooTableAndXColumn("INT"), sql);
    }

    @Test
    public void testMinorThan() {

        String sql = "select x from Foo where x < 5";

        List<InsertionDto> insertions = sql().insertInto("Foo", 1L)
                .d("x", "-2")
                .dtos();

        checkIncreasingTillCovered("x", Arrays.asList(10, 7, 6, 5), insertions, selectWhereXofFoo, createSchemaDtoWithFooTableAndXColumn("INT"), sql);
    }

    @Test
    public void testMinorThanEquals() {

        String sql = "select x from Foo where x <= 5";

        List<InsertionDto> insertions = sql().insertInto("Foo", 1L)
                .d("x", "5")
                .dtos();

        checkIncreasingTillCovered("x", Arrays.asList(10, 7, 6), insertions, selectWhereXofFoo, createSchemaDtoWithFooTableAndXColumn("INT"), sql);
    }


    @Test
    public void testAnd() {

        String sql = "select x from Foo where x > 5 and x < 10";

        List<InsertionDto> insertions = sql().insertInto("Foo", 1L)
                .d("x", "7")
                .dtos();

        checkIncreasingTillCovered("x", Arrays.asList(20, -1, 4), insertions, selectWhereXofFoo, createSchemaDtoWithFooTableAndXColumn("INT"),  sql);
    }

    @Test
    public void testOr() {

        String sql = "select x from Foo where x < 0 or x > 100";

        List<InsertionDto> insertions = sql().insertInto("Foo", 1L)
                .d("x", "-3")
                .dtos();

        checkIncreasingTillCovered("x", Arrays.asList(50, 60, 20, 90, 5), insertions, selectWhereXofFoo, createSchemaDtoWithFooTableAndXColumn("INT"), sql);
    }



    @Test
    public void testDeleteBase() {

        String sql = "delete from Foo where x=0";

        List<InsertionDto> insertions = sql().insertInto("Foo", 1L)
                .d("x", "0")
                .dtos();

        checkIncreasingTillCovered("x", Arrays.asList(10, -5, 2), insertions, selectWhereXofFoo, createSchemaDtoWithFooTableAndXColumn("INT"), sql);
    }

    @Test
    public void testUpdateBase() {

        String sql = "update Foo set x=42 where x=0";

        List<InsertionDto> insertions = sql().insertInto("Foo", 1L)
                .d("x", "0")
                .dtos();

        checkIncreasingTillCovered("x", Arrays.asList(10, -5, 2), insertions, selectWhereXofFoo, createSchemaDtoWithFooTableAndXColumn("INT"), sql);
    }


    @Test
    public void testTimestamp() {

        String sql = "select x from Foo where x > '28-Feb-17'";

        List<InsertionDto> insertions = sql().insertInto("Foo", 1L)
                .d("x",  "2017-03-01 00:00:00")
                .dtos();

        checkIncreasingTillCovered("x", Arrays.asList(
                        Timestamp.valueOf("1870-01-01 00:00:00"),
                        Timestamp.valueOf("1900-01-01 00:00:00"),
                        Timestamp.valueOf("2010-03-12 13:21:42"),
                        Timestamp.valueOf("2017-02-27 00:00:00")
                ),
                insertions, selectWhereXofFoo, createSchemaDtoWithFooTableAndXColumn("TIMESTAMP"),
                sql);
    }

    @Test
    public void testTimestampBetween() {

        String sql = "select x from Foo where x BETWEEN '28-Feb-17' AND '25-Mar-19'";

        List<InsertionDto> insertions = sql().insertInto("Foo", 1L)
                .d("x", "2018-03-01 00:00:00")
                .dtos();

        checkIncreasingTillCovered("x", Arrays.asList(
                        Timestamp.valueOf("1870-01-01 00:00:00"),
                        Timestamp.valueOf("1900-01-01 00:00:00"),
                        Timestamp.valueOf("2021-03-12 13:21:42"),
                        Timestamp.valueOf("2016-02-27 00:00:00")
                ),
                insertions, selectWhereXofFoo, createSchemaDtoWithFooTableAndXColumn("TIMESTAMP"),
                sql);
    }

    @Test
    public void testTimestampMinorThanEquals() {

        String sql = "select x from Foo where x <= TIMESTAMP '2022-11-30 16:00:00.0'";

        List<InsertionDto> insertions = sql().insertInto("Foo", 1L)
                .d("x", "2022-11-30 16:00:00.0")
                .dtos();

        checkIncreasingTillCovered("x", Arrays.asList(
                        Timestamp.valueOf("2023-11-30 00:00:00"),
                        Timestamp.valueOf("2023-06-30 16:00:00.0"),
                        Timestamp.valueOf("2022-12-30 16:00:00.0"),
                        Timestamp.valueOf("2022-11-30 17:00:00.0")
                ),
                insertions, selectWhereXofFoo, createSchemaDtoWithFooTableAndXColumn("TIMESTAMP"),
                sql);
    }

    @Test
    public void testTimestampMinorThan() {

        String sql = "select x from Foo where x < TIMESTAMP '2022-11-30 16:00:00.0'";

        List<InsertionDto> insertions = sql().insertInto("Foo", 1L)
                .d("x", "2022-11-29 16:00:00.0")
                .dtos();

        checkIncreasingTillCovered("x", Arrays.asList(
                        Timestamp.valueOf("2023-11-30 00:00:00"),
                        Timestamp.valueOf("2023-06-30 16:00:00.0"),
                        Timestamp.valueOf("2022-12-30 16:00:00.0"),
                        Timestamp.valueOf("2022-11-30 17:00:00.0")
                ),
                insertions, selectWhereXofFoo, createSchemaDtoWithFooTableAndXColumn("TIMESTAMP"),
                sql);
    }

    @Test
    public void testTimestampGreaterThan() {

        String sql = "select x from Foo where x > TIMESTAMP '2022-11-30 16:00:00.0'";

        List<InsertionDto> insertions = sql().insertInto("Foo", 1L)
                .d("x", "2022-12-01 16:00:00.0")
                .dtos();

        checkIncreasingTillCovered("x", Arrays.asList(
                        Timestamp.valueOf("2020-11-30 16:00:00"),
                        Timestamp.valueOf("2021-11-30 16:00:00.0"),
                        Timestamp.valueOf("2022-11-30 15:00:00.0"),
                        Timestamp.valueOf("2022-11-30 16:00:00.0")
                ),
                insertions, selectWhereXofFoo, createSchemaDtoWithFooTableAndXColumn("TIMESTAMP"),
                sql);
    }

    @Test
    public void testTimestampGreaterThanEquals() {

        String sql = "select x from Foo where x >= TIMESTAMP '2022-11-30 16:00:00.0'";

        List<InsertionDto> insertions = sql().insertInto("Foo", 1L)
                .d("x", "2022-11-30 16:00:00.0")
                .dtos();

        checkIncreasingTillCovered("x", Arrays.asList(
                        Timestamp.valueOf("2020-11-30 16:00:00"),
                        Timestamp.valueOf("2021-11-30 16:00:00.0"),
                        Timestamp.valueOf("2022-11-30 15:00:00.0")
                ),
                insertions, selectWhereXofFoo, createSchemaDtoWithFooTableAndXColumn("TIMESTAMP"),
                sql);
    }

    @Test
    public void testTimestampEqualsTo() {

        String sql = "select x from Foo where x = TIMESTAMP '2022-11-30 16:00:00.0'";

        List<InsertionDto> insertions = sql().insertInto("Foo", 1L)
                .d("x", "2022-11-30 16:00:00.0")
                .dtos();

        checkIncreasingTillCovered("x", Arrays.asList(
                        Timestamp.valueOf("2020-11-30 16:00:00"),
                        Timestamp.valueOf("2021-11-30 16:00:00.0"),
                        Timestamp.valueOf("2022-11-29 16:00:00.0")
                ),
                insertions, selectWhereXofFoo, createSchemaDtoWithFooTableAndXColumn("TIMESTAMP"),
                sql);
    }

}