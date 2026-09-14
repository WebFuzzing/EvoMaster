package org.evomaster.client.java.controller.cassandra.dsl;

import org.evomaster.client.java.controller.api.dto.database.operations.CassandraInsertionDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CassandraDslTest {

    @Test
    public void testSingleInsertion() {

        List<CassandraInsertionDto> dtos = CassandraDsl.cassandra()
                .insertInto("aKeyspace", "aTable")
                .d("id", "1")
                .d("name", "'aName'")
                .dtos();

        assertEquals(1, dtos.size());
        CassandraInsertionDto dto = dtos.get(0);
        assertEquals("aKeyspace", dto.keyspaceName);
        assertEquals("aTable", dto.tableName);
        assertEquals(2, dto.data.size());
        assertEquals("id", dto.data.get(0).columnName);
        assertEquals("1", dto.data.get(0).printableValue);
        assertEquals("name", dto.data.get(1).columnName);
        assertEquals("'aName'", dto.data.get(1).printableValue);
    }

    @Test
    public void testMultipleInsertions() {

        List<CassandraInsertionDto> dtos = CassandraDsl.cassandra()
                .insertInto("aKeyspace", "aTable")
                .d("id", "1")
                .and()
                .insertInto("aKeyspace", "anotherTable")
                .d("id", "2")
                .dtos();

        assertEquals(2, dtos.size());
        assertEquals("aTable", dtos.get(0).tableName);
        assertEquals("anotherTable", dtos.get(1).tableName);
    }

    @Test
    public void testEmptyKeyspaceThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                CassandraDsl.cassandra().insertInto("", "aTable"));
    }

    @Test
    public void testNullKeyspaceThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                CassandraDsl.cassandra().insertInto(null, "aTable"));
    }

    @Test
    public void testEmptyTableThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                CassandraDsl.cassandra().insertInto("aKeyspace", ""));
    }

    @Test
    public void testEmptyColumnThrows() {
        CassandraStatementDsl statement = CassandraDsl.cassandra().insertInto("aKeyspace", "aTable");
        assertThrows(IllegalArgumentException.class, () -> statement.d("", "1"));
    }

    @Test
    public void testReuseAfterDtosThrows() {
        CassandraSequenceDsl sequence = CassandraDsl.cassandra();
        sequence.insertInto("aKeyspace", "aTable").d("id", "1").dtos();

        assertThrows(IllegalStateException.class, () ->
                sequence.insertInto("aKeyspace", "aTable"));
    }
}
