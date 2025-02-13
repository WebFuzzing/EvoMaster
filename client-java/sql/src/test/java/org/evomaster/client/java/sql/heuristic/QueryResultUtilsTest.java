package org.evomaster.client.java.sql.heuristic;

import org.evomaster.client.java.sql.DataRow;
import org.evomaster.client.java.sql.QueryResult;
import org.evomaster.client.java.sql.VariableDescriptor;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class QueryResultUtilsTest {

    @Test
    public void testCreateUnionRowSetWithMultipleQueryResults() {
        QueryResult queryResult1 = new QueryResult(Arrays.asList(new VariableDescriptor("column1", null, null)));
        queryResult1.addRow(Arrays.asList("column1"), null, Arrays.asList("value1"));

        QueryResult queryResult2 = new QueryResult(Arrays.asList(new VariableDescriptor("column1", null, null)));
        queryResult2.addRow(Arrays.asList("column1"), null, Arrays.asList("value2"));

        QueryResult unionResult = QueryResultUtils.createUnionRowSet(Arrays.asList(queryResult1, queryResult2));

        assertEquals(2, unionResult.seeRows().size());
        assertEquals("value1", unionResult.seeRows().get(0).seeValues().get(0));
        assertEquals("value2", unionResult.seeRows().get(1).seeValues().get(0));
    }

    @Test
    public void testCreateCartesianProductWithEmptyRightQueryResult() {
        QueryResult left = new QueryResult(Arrays.asList(new VariableDescriptor("leftColumn", null, null)));
        left.addRow(Arrays.asList("leftColumn"), null, Arrays.asList("leftValue"));

        QueryResult right = new QueryResult(Arrays.asList(new VariableDescriptor("rightColumn", null, null)));

        QueryResult result = QueryResultUtils.createCartesianProduct(left, right);

        assertEquals(0, result.seeRows().size());
    }

    @Test
    public void testCreateJoinedRowWithDifferentColumnCounts() {
        DataRow leftRow = new DataRow(Arrays.asList(new VariableDescriptor("leftColumn", null, null)), Arrays.asList("leftValue"));
        DataRow rightRow = new DataRow(Arrays.asList(new VariableDescriptor("rightColumn1", null, null), new VariableDescriptor("rightColumn2", null, null)), Arrays.asList("rightValue1", "rightValue2"));

        DataRow joinedRow = QueryResultUtils.createJoinedRow(leftRow, rightRow, Arrays.asList(new VariableDescriptor("leftColumn", null, null), new VariableDescriptor("rightColumn1", null, null), new VariableDescriptor("rightColumn2", null, null)));

        assertEquals(3, joinedRow.seeValues().size());
        assertEquals("leftValue", joinedRow.seeValues().get(0));
        assertEquals("rightValue1", joinedRow.seeValues().get(1));
        assertEquals("rightValue2", joinedRow.seeValues().get(2));
    }

    @Test
    public void testCreateEmptyCartesianProductWithNoColumns() {
        QueryResult left = new QueryResult(Collections.emptyList());
        QueryResult right = new QueryResult(Collections.emptyList());

        QueryResult result = QueryResultUtils.createEmptyCartesianProduct(left, right);

        assertEquals(0, result.seeVariableDescriptors().size());
        assertEquals(0, result.seeRows().size());
    }

    @Test
    public void testCreateDataRowOfNullValuesWithMultipleColumns() {
        QueryResult queryResult = new QueryResult(Arrays.asList(new VariableDescriptor("column1", null, null), new VariableDescriptor("column2", null, null)));

        DataRow nullDataRow = QueryResultUtils.createDataRowOfNullValues(queryResult);

        assertEquals(2, nullDataRow.seeValues().size());
        assertNull(nullDataRow.seeValues().get(0));
        assertNull(nullDataRow.seeValues().get(1));
    }

}
