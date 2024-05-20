package org.evomaster.client.java.sql.advanced.schema_context;

import org.evomaster.client.java.sql.advanced.driver.Schema;
import org.evomaster.client.java.sql.advanced.select_query.QueryTable;
import org.junit.Test;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.evomaster.client.java.sql.advanced.CollectionsHelper.createMap;
import static org.evomaster.client.java.sql.advanced.schema_context.SchemaContextItem.createSchemaContextItem;
import static org.evomaster.client.java.sql.advanced.select_query.QueryColumn.createQueryColumn;
import static org.evomaster.client.java.sql.advanced.select_query.QueryTable.createQueryTable;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SchemaContextTest {

    @Test
    public void testIncludes() {
        SchemaContext context = new SchemaContext();
        List<QueryTable> tables = singletonList(createQueryTable("table"));
        Schema schema = new Schema(createMap("table", singletonList("column")));
        SchemaContextItem item = createSchemaContextItem(tables, schema);
        context.add(item);
        assertTrue(context.includes(createQueryColumn("column")));
    }

    @Test
    public void testNotIncludes() {
        SchemaContext context = new SchemaContext();
        assertFalse(context.includes(createQueryColumn("column")));
    }
}
