package org.evomaster.client.java.sql.advanced.evaluation_context;

import org.evomaster.client.java.sql.advanced.driver.row.Row;
import org.evomaster.client.java.sql.advanced.select_query.QueryTable;
import org.junit.Test;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.evomaster.client.java.sql.advanced.CollectionsHelper.createList;
import static org.evomaster.client.java.sql.advanced.CollectionsHelper.createMap;
import static org.evomaster.client.java.sql.advanced.ObjectMother.createSimpleRow;
import static org.evomaster.client.java.sql.advanced.evaluation_context.EvaluationContext.createEvaluationContext;
import static org.evomaster.client.java.sql.advanced.select_query.QueryColumn.createQueryColumn;
import static org.evomaster.client.java.sql.advanced.select_query.QueryTable.createQueryTable;
import static org.junit.Assert.*;

public class EvaluationContextTest {

    @Test
    public void testIncludes() {
        List<QueryTable> tables = singletonList(createQueryTable("table"));
        Row row = createSimpleRow("table", "column", 1);
        EvaluationContext evaluationContext = createEvaluationContext(row, tables);
        assertTrue(evaluationContext.includes(createQueryColumn("alias", "column")));
    }

    @Test
    public void testNotIncludes() {
        List<QueryTable> tables = singletonList(createQueryTable("table"));
        Row row = createSimpleRow("table", "other_column", 1);
        EvaluationContext evaluationContext = createEvaluationContext(row, tables);
        assertFalse(evaluationContext.includes(createQueryColumn("column")));
    }

    @Test
    public void testGetValue() { //Column without table (ambiguity is not possible in results)
        List<QueryTable> tables = singletonList(createQueryTable("table"));
        Row row = createSimpleRow("table", "other_column", 1);
        row.put("table", createMap("column", 1));
        EvaluationContext evaluationContext = createEvaluationContext(row, tables);
        assertEquals(1, evaluationContext.getValue(createQueryColumn("column")));
    }

    @Test
    public void testGetValue2() { //Column with table in results
        List<QueryTable> tables = createList(createQueryTable("table"), createQueryTable("other_table"));
        Row row = new Row();
        row.put("table", createMap("column", 1));
        row.put("other_table", createMap("column", 2));
        EvaluationContext evaluationContext = createEvaluationContext(row, tables);
        assertEquals(1, evaluationContext.getValue(createQueryColumn("table", "column")));
    }

    @Test
    public void testGetValue3() { //Column with alias in results
        List<QueryTable> tables = createList(createQueryTable("table", "alias"), createQueryTable("other_table"));
        Row row = new Row();
        row.put("table", createMap("column", 1));
        row.put("other_table", createMap("column", 2));
        EvaluationContext evaluationContext = createEvaluationContext(row, tables);
        assertEquals(1, evaluationContext.getValue(createQueryColumn("alias", "column")));
    }

    @Test
    public void testGetValue4() { //Column without alias in results, but without ambiguity
        List<QueryTable> tables = singletonList(createQueryTable("table"));
        Row row = createSimpleRow("table", "column", 1);
        EvaluationContext evaluationContext = createEvaluationContext(row, tables);
        assertEquals(1, evaluationContext.getValue(createQueryColumn("alias", "column")));
    }

    @Test(expected = RuntimeException.class)
    public void testGetValue5() { //Column without alias in results and with ambiguity
        List<QueryTable> tables = createList(createQueryTable("table"), createQueryTable("other_table"));
        Row row = new Row();
        row.put("table", createMap("column", 1));
        row.put("other_table", createMap("column", 2));
        EvaluationContext evaluationContext = createEvaluationContext(row, tables);
        evaluationContext.getValue(createQueryColumn("alias", "column"));
    }
}
