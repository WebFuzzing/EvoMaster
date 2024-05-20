package org.evomaster.client.java.sql.advanced.schema_context;

import org.evomaster.client.java.sql.advanced.select_query.QueryColumn;

import java.util.LinkedList;
import java.util.List;

public class SchemaContext {

    private List<SchemaContextItem> items;

    public SchemaContext() {
        this.items = new LinkedList<>();
    }

    private SchemaContext(List<SchemaContextItem> items) {
        this.items = items;
    }

    public void add(SchemaContextItem item) {
        items.add(item);
    }

    public Boolean includes(QueryColumn column) {
        return items.stream().anyMatch(item -> item.includes(column));
    }

    public SchemaContext copy() {
        return new SchemaContext(new LinkedList<>(items));
    }
}
