package org.evomaster.client.java.sql.internal;

import org.evomaster.client.java.controller.api.dto.database.operations.InsertionDto;
import org.evomaster.client.java.controller.api.dto.database.schema.ColumnDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DbSchemaDto;
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto;
import org.evomaster.client.java.sql.DataRow;
import org.evomaster.client.java.sql.QueryResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.evomaster.client.java.sql.dsl.SqlDsl.sql;
import static org.evomaster.client.java.sql.internal.QueryResultTransformer.convertInsertionDtosToQueryResults;
import static org.junit.jupiter.api.Assertions.*;

public class HeuristicsCalculatorWithInsertionDtoTest {

    private DbSchemaDto createSchemaDtoWithFooTableAndXColumn(String xDataType){
        DbSchemaDto schemaDto = new DbSchemaDto();
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
                                            DbSchemaDto dto,
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



}