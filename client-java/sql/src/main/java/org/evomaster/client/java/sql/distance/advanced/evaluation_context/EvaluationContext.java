package org.evomaster.client.java.sql.distance.advanced.evaluation_context;

import net.sf.jsqlparser.schema.Column;
import org.evomaster.client.java.sql.distance.advanced.driver.row.Row;

public class EvaluationContext {

    private Row row;

    public EvaluationContext(Row row) {
        this.row = row;
    }

    public static EvaluationContext createEvaluationContext(Row row) {
        return new EvaluationContext(row);
    }

    public Boolean includes(Column column) {
        return row.containsKey(column.getColumnName());
    }

    public Object getValue(Column column) {
        return row.get(column.getColumnName());
    }

    @Override
    public String toString(){
        return row.toString();
    }
}
