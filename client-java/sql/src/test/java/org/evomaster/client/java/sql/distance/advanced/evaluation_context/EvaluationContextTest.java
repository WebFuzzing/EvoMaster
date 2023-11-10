package org.evomaster.client.java.sql.distance.advanced.evaluation_context;

import org.evomaster.client.java.sql.distance.advanced.driver.row.Row;
import org.junit.Test;

import static org.evomaster.client.java.sql.distance.advanced.ObjectMother.createColumn;
import static org.evomaster.client.java.sql.distance.advanced.evaluation_context.EvaluationContext.createEvaluationContext;
import static org.junit.Assert.*;

public class EvaluationContextTest {

    @Test
    public void testIncludesAndGetValue() {
        Row row = new Row();
        row.put("column", "value");
        EvaluationContext context = createEvaluationContext(row);
        assertTrue(context.includes(createColumn("column")));
        assertEquals("value", context.getValue(createColumn("column")));
    }

    @Test
    public void testNotIncludes() {
        EvaluationContext context = createEvaluationContext(new Row());
        assertFalse(context.includes(createColumn("column")));
    }
}
