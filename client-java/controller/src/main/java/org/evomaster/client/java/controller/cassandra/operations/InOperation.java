package org.evomaster.client.java.controller.cassandra.operations;

import java.util.List;

/**
 * Represents a CQL IN operation for a single column.
 *
 * <pre>
 * WHERE userid IN ('john', 'jane')
 * </pre>
 */
public class InOperation extends CqlQueryOperation {

    private final String columnName;
    private final List<Object> values;

    public InOperation(String columnName, List<Object> values) {
        this.columnName = columnName;
        this.values = values;
    }

    public String getColumnName() {
        return columnName;
    }

    public List<Object> getValues() {
        return values;
    }
}