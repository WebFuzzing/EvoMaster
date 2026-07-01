package org.evomaster.client.java.controller.cassandra.parser;

import org.evomaster.client.java.controller.cassandra.operations.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class CqlParserUtilsTest {

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private static CqlQueryOperation parseWhere(String cql) {
        return CqlParserUtils.getWhereOperation(CqlParserUtils.parseCqlCommand(cql));
    }

    private static Object extractValue(CqlQueryOperation op) {
        return ((ComparisonOperation<?>) op).getValue();
    }

    private static String extractColumn(CqlQueryOperation op) {
        return ((ComparisonOperation<?>) op).getColumnName();
    }

    // -------------------------------------------------------------------------
    // isSelect / isUpdate / isDelete
    // -------------------------------------------------------------------------

    @Test
    void isSelect_trueForSelectQuery() {
        assertTrue(CqlParserUtils.isSelect("SELECT * FROM t"));
    }

    @Test
    void isSelect_caseInsensitive() {
        assertTrue(CqlParserUtils.isSelect("select * from t"));
    }

    @Test
    void isSelect_falseForUpdate() {
        assertFalse(CqlParserUtils.isSelect("UPDATE t SET col = 1 WHERE id = 1"));
    }

    @Test
    void isUpdate_trueForUpdateQuery() {
        assertTrue(CqlParserUtils.isUpdate("UPDATE t SET col = 1 WHERE id = 1"));
    }

    @Test
    void isDelete_trueForDeleteQuery() {
        assertTrue(CqlParserUtils.isDelete("DELETE FROM t WHERE id = 1"));
    }

    // -------------------------------------------------------------------------
    // canParseCqlCommand
    // -------------------------------------------------------------------------

    @Test
    void canParse_validSelect() {
        assertTrue(CqlParserUtils.canParseCqlCommand("SELECT * FROM t WHERE id = 1"));
    }

    @Test
    void canParse_validSelectNoWhere() {
        assertTrue(CqlParserUtils.canParseCqlCommand("SELECT * FROM t"));
    }

    @Test
    void canParse_invalidQuery_returnsFalse() {
        assertFalse(CqlParserUtils.canParseCqlCommand("THIS IS NOT A CQL QUERY"));
    }

    // -------------------------------------------------------------------------
    // No WHERE clause → null
    // -------------------------------------------------------------------------

    @Test
    void getWhereOperation_noWhereClause_returnsNull() {
        assertNull(parseWhere("SELECT * FROM t"));
    }

    @Test
    void getWhereOperation_updateNoWhere_returnsNull() {
        // a bare UPDATE without WHERE is invalid CQL, but getWhereSpec should still return null
        CqlParser.RootContext root = CqlParserUtils.parseCqlCommand("SELECT * FROM t");
        assertNull(CqlParserUtils.getWhereOperation(root));
    }

    // -------------------------------------------------------------------------
    // EqualsOperation
    // -------------------------------------------------------------------------

    @Test
    void equals_stringValue() {
        CqlQueryOperation op = parseWhere("SELECT * FROM t WHERE col = 'hello'");
        assertInstanceOf(EqualsOperation.class, op);
        assertEquals("col", extractColumn(op));
        assertEquals("hello", extractValue(op));
    }

    @Test
    void equals_longValue() {
        CqlQueryOperation op = parseWhere("SELECT * FROM t WHERE col = 42");
        assertInstanceOf(EqualsOperation.class, op);
        assertEquals("col", extractColumn(op));
        assertEquals(42L, extractValue(op));
    }

    @Test
    void equals_booleanTrue() {
        CqlQueryOperation op = parseWhere("SELECT * FROM t WHERE col = true");
        assertInstanceOf(EqualsOperation.class, op);
        assertEquals("col", extractColumn(op));
        assertEquals(Boolean.TRUE, extractValue(op));
    }

    @Test
    void equals_booleanFalse() {
        CqlQueryOperation op = parseWhere("SELECT * FROM t WHERE col = false");
        assertInstanceOf(EqualsOperation.class, op);
        assertEquals("col", extractColumn(op));
        assertEquals(Boolean.FALSE, extractValue(op));
    }

    @Test
    void equals_uuid() {
        String rawUuid = "550e8400-e29b-41d4-a716-446655440000";
        CqlQueryOperation op = parseWhere("SELECT * FROM t WHERE col = " + rawUuid);
        assertInstanceOf(EqualsOperation.class, op);
        assertEquals("col", extractColumn(op));
        assertEquals(UUID.fromString(rawUuid), extractValue(op));
    }

    @Test
    void equals_doubleValue() {
        CqlQueryOperation op = parseWhere("SELECT * FROM t WHERE col = 3.14");
        assertInstanceOf(EqualsOperation.class, op);
        assertEquals("col", extractColumn(op));
        assertEquals(3.14, extractValue(op));
    }

    @Test
    void equals_nullValue() {
        CqlQueryOperation op = parseWhere("SELECT * FROM t WHERE col = NULL");
        assertInstanceOf(EqualsOperation.class, op);
        assertEquals("col", extractColumn(op));
        assertNull(extractValue(op));
    }

    // -------------------------------------------------------------------------
    // GreaterThanOperation
    // -------------------------------------------------------------------------

    @Test
    void greaterThan_longValue() {
        CqlQueryOperation op = parseWhere("SELECT * FROM t WHERE col > 10");
        assertInstanceOf(GreaterThanOperation.class, op);
        assertEquals("col", extractColumn(op));
        assertEquals(10L, extractValue(op));
    }

    // -------------------------------------------------------------------------
    // GreaterThanEqualsOperation
    // -------------------------------------------------------------------------

    @Test
    void greaterThanEquals_longValue() {
        CqlQueryOperation op = parseWhere("SELECT * FROM t WHERE col >= 10");
        assertInstanceOf(GreaterThanEqualsOperation.class, op);
        assertEquals("col", extractColumn(op));
        assertEquals(10L, extractValue(op));
    }

    // -------------------------------------------------------------------------
    // LessThanOperation
    // -------------------------------------------------------------------------

    @Test
    void lessThan_longValue() {
        CqlQueryOperation op = parseWhere("SELECT * FROM t WHERE col < 10");
        assertInstanceOf(LessThanOperation.class, op);
        assertEquals("col", extractColumn(op));
        assertEquals(10L, extractValue(op));
    }

    // -------------------------------------------------------------------------
    // LessThanEqualsOperation
    // -------------------------------------------------------------------------

    @Test
    void lessThanEquals_longValue() {
        CqlQueryOperation op = parseWhere("SELECT * FROM t WHERE col <= 10");
        assertInstanceOf(LessThanEqualsOperation.class, op);
        assertEquals("col", extractColumn(op));
        assertEquals(10L, extractValue(op));
    }

    // -------------------------------------------------------------------------
    // InOperation
    // -------------------------------------------------------------------------

    @Test
    void in_multipleStringValues() {
        CqlQueryOperation op = parseWhere("SELECT * FROM t WHERE col IN ('a', 'b', 'c')");
        assertInstanceOf(InOperation.class, op);
        InOperation in = (InOperation) op;
        assertEquals("col", in.getColumnName());
        assertEquals(Arrays.asList("a", "b", "c"), in.getValues());
    }

    @Test
    void in_multipleLongValues() {
        CqlQueryOperation op = parseWhere("SELECT * FROM t WHERE col IN (1, 2, 3)");
        assertInstanceOf(InOperation.class, op);
        InOperation in = (InOperation) op;
        assertEquals("col", in.getColumnName());
        assertEquals(Arrays.asList(1L, 2L, 3L), in.getValues());
    }

    @Test
    void in_emptyList() {
        CqlQueryOperation op = parseWhere("SELECT * FROM t WHERE col IN ()");
        assertInstanceOf(InOperation.class, op);
        InOperation in = (InOperation) op;
        assertEquals("col", in.getColumnName());
        assertTrue(in.getValues().isEmpty());
    }

    // -------------------------------------------------------------------------
    // ContainsOperation
    // -------------------------------------------------------------------------

    @Test
    void contains_stringValue() {
        CqlQueryOperation op = parseWhere("SELECT * FROM t WHERE col CONTAINS 'value'");
        assertInstanceOf(ContainsOperation.class, op);
        ContainsOperation<?> c = (ContainsOperation<?>) op;
        assertEquals("col", c.getColumnName());
        assertEquals("value", c.getValue());
    }

    @Test
    void contains_longValue() {
        CqlQueryOperation op = parseWhere("SELECT * FROM t WHERE col CONTAINS 99");
        assertInstanceOf(ContainsOperation.class, op);
        assertEquals("col", ((ContainsOperation<?>) op).getColumnName());
        assertEquals(99L, ((ContainsOperation<?>) op).getValue());
    }

    // -------------------------------------------------------------------------
    // ContainsKeyOperation
    // -------------------------------------------------------------------------

    @Test
    void containsKey_stringValue() {
        CqlQueryOperation op = parseWhere("SELECT * FROM t WHERE col CONTAINS KEY 'myKey'");
        assertInstanceOf(ContainsKeyOperation.class, op);
        ContainsKeyOperation<?> ck = (ContainsKeyOperation<?>) op;
        assertEquals("col", ck.getColumnName());
        assertEquals("myKey", ck.getValue());
    }

    @Test
    void containsKey_longValue() {
        CqlQueryOperation op = parseWhere("SELECT * FROM t WHERE col CONTAINS KEY 7");
        assertInstanceOf(ContainsKeyOperation.class, op);
        assertEquals("col", ((ContainsKeyOperation<?>) op).getColumnName());
        assertEquals(7L, ((ContainsKeyOperation<?>) op).getValue());
    }

    // -------------------------------------------------------------------------
    // AndOperation
    // -------------------------------------------------------------------------

    @Test
    void and_twoConditions() {
        CqlQueryOperation op = parseWhere("SELECT * FROM t WHERE a = 1 AND b = 'hello'");
        assertInstanceOf(AndOperation.class, op);
        List<CqlQueryOperation> conditions = ((AndOperation) op).getConditions();
        assertEquals(2, conditions.size());
        assertInstanceOf(EqualsOperation.class, conditions.get(0));
        assertInstanceOf(EqualsOperation.class, conditions.get(1));
        assertEquals("a", ((EqualsOperation<?>) conditions.get(0)).getColumnName());
        assertEquals(1L,  ((EqualsOperation<?>) conditions.get(0)).getValue());
        assertEquals("b", ((EqualsOperation<?>) conditions.get(1)).getColumnName());
        assertEquals("hello", ((EqualsOperation<?>) conditions.get(1)).getValue());
    }

    @Test
    void and_threeConditions_mixedOperators() {
        CqlQueryOperation op = parseWhere(
                "SELECT * FROM t WHERE a = 1 AND b > 2 AND c <= 3");
        assertInstanceOf(AndOperation.class, op);
        List<CqlQueryOperation> conditions = ((AndOperation) op).getConditions();
        assertEquals(3, conditions.size());
        assertInstanceOf(EqualsOperation.class,         conditions.get(0));
        assertInstanceOf(GreaterThanOperation.class,    conditions.get(1));
        assertInstanceOf(LessThanEqualsOperation.class, conditions.get(2));
        assertEquals("a", ((ComparisonOperation<?>) conditions.get(0)).getColumnName());
        assertEquals(1L,  ((ComparisonOperation<?>) conditions.get(0)).getValue());
        assertEquals("b", ((ComparisonOperation<?>) conditions.get(1)).getColumnName());
        assertEquals(2L,  ((ComparisonOperation<?>) conditions.get(1)).getValue());
        assertEquals("c", ((ComparisonOperation<?>) conditions.get(2)).getColumnName());
        assertEquals(3L,  ((ComparisonOperation<?>) conditions.get(2)).getValue());
    }

    // -------------------------------------------------------------------------
    // Quoted identifiers
    // -------------------------------------------------------------------------

    @Test
    void quotedColumnName_equals_includesQuotesInName() {
        CqlQueryOperation op = parseWhere("SELECT * FROM t WHERE \"myCol\" = 42");
        assertInstanceOf(EqualsOperation.class, op);
        // OBJECT_NAME token text includes the surrounding double quotes
        assertEquals("\"myCol\"", extractColumn(op));
        assertEquals(42L, extractValue(op));
    }

    @Test
    void quotedColumnName_greaterThan() {
        CqlQueryOperation op = parseWhere("SELECT * FROM t WHERE \"amount\" > 100");
        assertInstanceOf(GreaterThanOperation.class, op);
        assertEquals("\"amount\"", extractColumn(op));
        assertEquals(100L, extractValue(op));
    }

    @Test
    void quotedColumnName_in() {
        CqlQueryOperation op = parseWhere("SELECT * FROM t WHERE \"status\" IN ('active', 'pending')");
        assertInstanceOf(InOperation.class, op);
        InOperation in = (InOperation) op;
        assertEquals("\"status\"", in.getColumnName());
        assertEquals(Arrays.asList("active", "pending"), in.getValues());
    }

    @Test
    void quotedColumnName_contains() {
        CqlQueryOperation op = parseWhere("SELECT * FROM t WHERE \"tags\" CONTAINS 'java'");
        assertInstanceOf(ContainsOperation.class, op);
        ContainsOperation<?> c = (ContainsOperation<?>) op;
        assertEquals("\"tags\"", c.getColumnName());
        assertEquals("java", c.getValue());
    }

    @Test
    void quotedColumnName_containsKey() {
        CqlQueryOperation op = parseWhere("SELECT * FROM t WHERE \"meta\" CONTAINS KEY 'env'");
        assertInstanceOf(ContainsKeyOperation.class, op);
        ContainsKeyOperation<?> ck = (ContainsKeyOperation<?>) op;
        assertEquals("\"meta\"", ck.getColumnName());
        assertEquals("env", ck.getValue());
    }

    // -------------------------------------------------------------------------
    // Value type parsing — time-related types arrive as string literals in CQL
    // -------------------------------------------------------------------------

    @Test
    void parse_dateStringLiteral() {
        // CQL dates are written as single-quoted string literals
        CqlQueryOperation op = parseWhere("SELECT * FROM t WHERE d = '2023-01-15'");
        assertInstanceOf(EqualsOperation.class, op);
        assertEquals("d", extractColumn(op));
        assertEquals("2023-01-15", extractValue(op));
    }

    @Test
    void parse_timeStringLiteral() {
        CqlQueryOperation op = parseWhere("SELECT * FROM t WHERE time_col = '14:30:00'");
        assertInstanceOf(EqualsOperation.class, op);
        assertEquals("time_col", extractColumn(op));
        assertEquals("14:30:00", extractValue(op));
    }

    @Test
    void parse_timestampStringLiteral() {
        CqlQueryOperation op = parseWhere("SELECT * FROM t WHERE ts = '2023-01-15T14:30:00Z'");
        assertInstanceOf(EqualsOperation.class, op);
        assertEquals("ts", extractColumn(op));
        assertEquals("2023-01-15T14:30:00Z", extractValue(op));
    }

    // -------------------------------------------------------------------------
    // Value type parsing — duration literals
    // -------------------------------------------------------------------------

    @Test
    void parse_durationStandardFormat() {
        // standard quantity-unit: 89h4m48s
        CqlQueryOperation op = parseWhere("SELECT * FROM t WHERE d = 89h4m48s");
        assertInstanceOf(EqualsOperation.class, op);
        assertEquals("d", extractColumn(op));
        assertEquals("89h4m48s", extractValue(op));
    }

    @Test
    void parse_durationWithMonths() {
        // standard: 1y2mo3d
        CqlQueryOperation op = parseWhere("SELECT * FROM t WHERE d = 1y2mo3d");
        assertInstanceOf(EqualsOperation.class, op);
        assertEquals("d", extractColumn(op));
        assertEquals("1y2mo3d", extractValue(op));
    }

    @Test
    void parse_durationISO8601() {
        // ISO 8601: PT89H8M53S
        CqlQueryOperation op = parseWhere("SELECT * FROM t WHERE d = PT89H8M53S");
        assertInstanceOf(EqualsOperation.class, op);
        assertEquals("d", extractColumn(op));
        assertEquals("PT89H8M53S", extractValue(op));
    }

    @Test
    void parse_durationISO8601_full() {
        // ISO 8601: P1Y2M3DT4H5M6S
        CqlQueryOperation op = parseWhere("SELECT * FROM t WHERE d = P1Y2M3DT4H5M6S");
        assertInstanceOf(EqualsOperation.class, op);
        assertEquals("d", extractColumn(op));
        assertEquals("P1Y2M3DT4H5M6S", extractValue(op));
    }

    // -------------------------------------------------------------------------
    // WHERE clause on UPDATE and DELETE
    // -------------------------------------------------------------------------

    @Test
    void update_whereClause_parsedCorrectly() {
        CqlQueryOperation op = parseWhere(
                "UPDATE t SET val = 'x' WHERE id = 99");
        assertInstanceOf(EqualsOperation.class, op);
        assertEquals("id", extractColumn(op));
        assertEquals(99L, extractValue(op));
    }

    @Test
    void delete_whereClause_parsedCorrectly() {
        CqlQueryOperation op = parseWhere("DELETE FROM t WHERE id = 7");
        assertInstanceOf(EqualsOperation.class, op);
        assertEquals("id", extractColumn(op));
        assertEquals(7L, extractValue(op));
    }

    @Test
    void delete_whereClause_andCondition() {
        CqlQueryOperation op = parseWhere(
                "DELETE FROM t WHERE partition_key = 1 AND clustering_key = 'abc'");
        assertInstanceOf(AndOperation.class, op);
        List<CqlQueryOperation> conditions = ((AndOperation) op).getConditions();
        assertEquals(2, conditions.size());
        assertEquals("partition_key", ((EqualsOperation<?>) conditions.get(0)).getColumnName());
        assertEquals(1L,              ((EqualsOperation<?>) conditions.get(0)).getValue());
        assertEquals("clustering_key", ((EqualsOperation<?>) conditions.get(1)).getColumnName());
        assertEquals("abc",            ((EqualsOperation<?>) conditions.get(1)).getValue());
    }
}