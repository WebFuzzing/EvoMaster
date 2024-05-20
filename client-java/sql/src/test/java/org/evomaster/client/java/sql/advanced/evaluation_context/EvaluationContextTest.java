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
    public void testIncludedAndGetValue() {
        List<QueryTable> tables = createList(createQueryTable("table"), createQueryTable("other_table"));
        Row row = new Row();
        row.put("table", createMap("column", 1));
        row.put("other_table", createMap("column", 2));
        EvaluationContext evaluationContext = createEvaluationContext(tables, row);
        assertTrue(evaluationContext.includes(createQueryColumn("column")));
        assertEquals(1, evaluationContext.getValue(createQueryColumn("column")));
    }

    @Test
    public void testNotIncluded() {
        List<QueryTable> tables = singletonList(createQueryTable("table"));
        Row row = createSimpleRow("table", "other_column", 1);
        EvaluationContext evaluationContext = createEvaluationContext(tables, row);
        assertFalse(evaluationContext.includes(createQueryColumn("column")));
    }
}
