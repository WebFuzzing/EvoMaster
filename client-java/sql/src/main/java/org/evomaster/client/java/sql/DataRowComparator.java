package org.evomaster.client.java.sql;

import net.sf.jsqlparser.statement.select.OrderByElement;

import java.util.Comparator;
import java.util.List;

/**
 * Comparator for DataRow based on a list of OrderByElement.
 * Used to sort query results according to SQL ORDER BY clauses.
 */
public class DataRowComparator implements Comparator<DataRow> {

    private final List<OrderByElement> orderByElements;

    public DataRowComparator(List<OrderByElement> orderByElements) {
        this.orderByElements = orderByElements;
    }

    @Override
    public int compare(DataRow r1, DataRow r2) {
        for (OrderByElement orderByElement : orderByElements) {
            String columnName = orderByElement.getExpression().toString();
            Object val1 = r1.getValueByName(columnName);
            Object val2 = r2.getValueByName(columnName);

            int comparison;
            if (val1 == null && val2 == null) {
                comparison = 0;
            } else if (val1 == null) {
                comparison = -1;
            } else if (val2 == null) {
                comparison = 1;
            } else {
                if (val1 instanceof Comparable && val2 instanceof Comparable) {
                    comparison = ((Comparable) val1).compareTo(val2);
                } else {
                    // Cannot compare, treat as equal
                    comparison = 0;
                }
            }

            if (comparison != 0) {
                return orderByElement.isAsc() ? comparison : -comparison;
            }
        }
        return 0;
    }
}
