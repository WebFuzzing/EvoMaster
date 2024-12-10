package org.evomaster.client.java.sql.internal;

import org.evomaster.client.java.controller.api.dto.database.operations.InsertionDto;
import org.evomaster.client.java.controller.api.dto.database.schema.ColumnDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto;
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto;
import org.evomaster.client.java.sql.QueryResult;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.evomaster.client.java.sql.dsl.SqlDsl.sql;
import static org.evomaster.client.java.sql.internal.QueryResultTransformer.convertInsertionDtosToQueryResults;
import static org.junit.jupiter.api.Assertions.*;

/**
 * created by manzhang on 2024/7/30
 */
public class QueryResultTransformerTest {


    private TableDto createTableDate(List<String> columnTypes, List<String> columnNames, String tableName){
        assertEquals(columnTypes.size(), columnNames.size());
        TableDto tableDto = new TableDto();
        tableDto.name = tableName;
        for (int i = 0; i < columnTypes.size(); i++){
            ColumnDto dto = new ColumnDto();
            dto.name = columnNames.get(i);
            dto.type = columnTypes.get(i);
            tableDto.columns.add(dto);
        }
        return tableDto;
    }

    @Test
    public void testConvertInsertionDtosToQueryResults(){

        List<InsertionDto> insertions = sql()
                .insertInto("FooTable", 1L)
                .d("fooA", "a1")
                .d("fooB", "b1")
                .d("fooC", "c1")
                .and()
                .insertInto("FooTable", 2L)
                .d("fooA", "a2")
                .d("fooB", "b2")
                .d("fooC", "c2")
                .and()
                .insertInto("BarTable", 3L)
                .d("barA", "11")
                .d("barB", "12")
                .d("barC", "13")
                .d("barD", "14")
                .d("barE", "15")
                .and()
                .insertInto("BarTable", 4L)
                .d("barA", "21")
                .d("barB", "22")
                .d("barC", "23")
                .d("barD", "24")
                .d("barE", "25")
                .dtos();


        Map<String, Set<String>> columns = new HashMap<String, Set<String>>() {{
            put("FooTable", new HashSet<>(Arrays.asList("fooA", "fooC")));
            put("BarTable", new HashSet<>(Arrays.asList("barB", "barC", "barD", "barE")));
        }};


        DbInfoDto schemaDto = new DbInfoDto();
        TableDto fooTable = createTableDate(Arrays.asList("CHARACTER","CHARACTER","CHARACTER"), Arrays.asList("fooA","fooB","fooC"), "FooTable");
        TableDto barTable = createTableDate(Arrays.asList("INT","INT","INT", "INT", "INT"), Arrays.asList("barA","barB","barC", "barD", "barE"), "BarTable");
        schemaDto.tables.add(fooTable);
        schemaDto.tables.add(barTable);

        QueryResult[] results = convertInsertionDtosToQueryResults(insertions, columns, schemaDto);

        assertNotNull(results);
        assertEquals(1, results.length);
        assertEquals(4, results[0].size());

        assertEquals("12,13,14,15,a1,c1", results[0].seeRows().get(0).seeValues().stream().map(Object::toString).collect(Collectors.joining(",")));
        assertEquals("12,13,14,15,a2,c2", results[0].seeRows().get(1).seeValues().stream().map(Object::toString).collect(Collectors.joining(",")));
        assertEquals("22,23,24,25,a1,c1", results[0].seeRows().get(2).seeValues().stream().map(Object::toString).collect(Collectors.joining(",")));
        assertEquals("22,23,24,25,a2,c2", results[0].seeRows().get(3).seeValues().stream().map(Object::toString).collect(Collectors.joining(",")));

    }


    @Test
    public void testCartesianProductIntList(){
        List<Integer> setA = Arrays.asList(1,2,3);
        List<Integer> setB = Arrays.asList(7,8,9,0);

        List<List<Integer>> intValues = new ArrayList<List<Integer>>(){{
            add(setA);
            add(setB);
        }};

        List<List<Integer>> results = QueryResultTransformer.cartesianProduct(intValues);

        assertEquals(3 * 4, results.size());

        int index = 0;
        for(int indexA = 0; indexA < setA.size(); indexA++){
            for (int indexB = 0; indexB < setB.size(); indexB++){
                List<Integer> row = results.get(index);
                assertEquals(2, row.size());
                assertEquals(setA.get(indexA), row.get(0));
                assertEquals(setB.get(indexB), row.get(1));
                index++;
            }
        }
    }


    @Test
    public void testCartesianProductStringList(){
        List<String> setA = Arrays.asList("aaa","bbb");
        List<String> setB = Arrays.asList("nmt", "xyz");
        List<String> setC = Arrays.asList("foo", "bar");

        List<List<String>> intValues = new ArrayList<List<String>>(){{
            add(setA);
            add(setB);
            add(setC);
        }};

        List<List<String>> results = QueryResultTransformer.cartesianProduct(intValues);

        List<String> expected = Arrays.asList(
                "aaa,nmt,foo",
                "aaa,nmt,bar",
                "aaa,xyz,foo",
                "aaa,xyz,bar",
                "bbb,nmt,foo",
                "bbb,nmt,bar",
                "bbb,xyz,foo",
                "bbb,xyz,bar"
        );

        assertEquals(expected.size(), results.size());

        for (int i = 0; i < expected.size(); i++){
            assertEquals(expected.get(i), String.join(",",results.get(i)));
        }

    }


    @Test
    public void testCartesianProductEmptyList(){
        List<List<String>> intValues = Collections.emptyList();

        List<List<String>> results = QueryResultTransformer.cartesianProduct(intValues);

        assertTrue(results.isEmpty());

    }

    @Test
    public void testCartesianProductContainEmptySet(){
        List<String> setA = Arrays.asList("aaa","bbb");
        List<String> setB = Arrays.asList("nmt", "xyz");
        List<String> setC = Collections.emptyList();

        List<List<String>> intValues = new ArrayList<List<String>>(){{
            add(setA);
            add(setB);
            add(setC);
        }};

        List<List<String>> results = QueryResultTransformer.cartesianProduct(intValues);

        List<String> expected = Arrays.asList(
                "aaa,nmt",
                "aaa,xyz",
                "bbb,nmt",
                "bbb,xyz"
        );

        assertEquals(expected.size(), results.size());

        for (int i = 0; i < expected.size(); i++){
            assertEquals(expected.get(i), String.join(",",results.get(i)));
        }

    }
}
